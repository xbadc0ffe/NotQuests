package rocks.gravili.notquests.paper.commands.category.tag;

import org.bukkit.entity.Player;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.commands.framework.NQArguments;
import rocks.gravili.notquests.paper.commands.framework.NQCommandBuilder;
import rocks.gravili.notquests.paper.commands.framework.NQCommandManager;
import rocks.gravili.notquests.paper.commands.framework.NQDescription;
import rocks.gravili.notquests.paper.commands.framework.NQFlag;
import rocks.gravili.notquests.paper.managers.data.Category;
import rocks.gravili.notquests.paper.managers.tags.Tag;
import rocks.gravili.notquests.paper.managers.tags.TagType;


public class AdminTagCommands {
    private final NotQuests main;
    private final NQCommandManager manager;
    private final NQCommandBuilder editBuilder;

    public AdminTagCommands(final NotQuests main, NQCommandManager manager, NQCommandBuilder editBuilder) {
        this.main = main;
        this.manager = manager;
        this.editBuilder = editBuilder;

        manager.command(editBuilder.commandDescription(NQDescription.of("Creates a new tag of given type"))
                .literal("create", NQDescription.of("Creates a new player tag with the selected value type."))
                .required("type", rocks.gravili.notquests.paper.commands.framework.NQArguments.enumArgument(TagType.class),
                        NQDescription.of("Value type for the new tag: BOOLEAN, INTEGER, FLOAT, DOUBLE, or STRING."))
                .required("name", NQArguments.stringArgument(), NQDescription.of("Unique name for the tag to create."))
                .handler(commandContext -> {
                    var tagType = (TagType) commandContext.get("type");
                    var tagName = (String) commandContext.get("name");

                    if (main.getTagManager().getTag(tagName) != null) {
                        commandContext.sender().sendMessage(main.parse("<error>Error: The tag <highlight>" + tagName + "</highlight> already exists!"));
                        return;
                    }

                    var tag = new Tag(main, tagName, tagType);
                    if (commandContext.flags().isPresent(main.getCommandManager().categoryFlag)) {
                        final Category category = commandContext.flags().getValue(
                                main.getCommandManager().categoryFlag,
                                main.getDataManager().getDefaultCategory()
                        );
                        tag.setCategory(category);
                    }
                    main.getTagManager().addTag(tag);

                    commandContext.sender().sendMessage(main.parse(
                            "<success>The " + tagType.name().toLowerCase() + " tag <highlight>" + tagName
                                    + "</highlight> has been added successfully!")
                    );
                })

        );

        manager.command(editBuilder.commandDescription(NQDescription.of("Lists all tags"))
                .literal("list", NQDescription.of("Lists every configured player tag."))
                .handler((context) -> {
                    context.sender().sendMessage(main.parse("<highlight>All tags:"));
                    int counter = 1;

                    for (final Tag tag : main.getTagManager().getTags()) {
                        context.sender().sendMessage(main.parse(
                                "<highlight>"
                                        + counter
                                        + ".</highlight> <main>"
                                        + tag.getTagName()
                                        + "</main> <highlight2>Type: <main>"
                                        + tag.getTagType().name())
                        );
                        counter++;
                    }
                }));

        manager.command(editBuilder.commandDescription(NQDescription.of("Deletes an existing tag."))
                .literal("delete", NQDescription.of("Deletes the selected player tag."), "remove")
                .required("tag-name", NQArguments.stringArgument(), NQDescription.of("Name of the existing tag to delete."), (context, input) ->
                        main.getTagManager().getTags().stream().map(Tag::getTagName).toList())
                .handler((context) -> {
                    final String tagName = context.get("tag-name");

                    var foundTag = main.getTagManager().getTag(tagName);
                    if (foundTag == null) {
                        context.sender().sendMessage(main.parse(
                                "<error>Error: The tag <highlight>"
                                        + tagName
                                        + "</highlight> doesn't exists!")
                        );
                        return;
                    }

                    main.getTagManager().deleteTag(foundTag);

                    context.sender().sendMessage(main.parse(
                            "<success>The tag <highlight>"
                                    + tagName
                                    + "</highlight> has been deleted successfully!")
                    );
                }));

        final NQFlag tagCheckPlayerFlag =
                NQFlag.builder(
                                "player",
                                NQDescription.of("Player whose tag value should be checked; defaults to the command sender when possible."))
                        .withArgument(rocks.gravili.notquests.paper.commands.framework.NQArguments.playerArgument())
                        .build();

        manager.command(editBuilder.commandDescription(NQDescription.of("Displays a player's stored value for a tag."))
                .literal("check", NQDescription.of("Displays the selected tag's stored value for a player."))
                .required("tag-name", NQArguments.stringArgument(), NQDescription.of("Name of the tag whose stored value should be shown."), (context, input) ->
                        main.getTagManager().getTags().stream().map(Tag::getTagName).toList())
                .flag(tagCheckPlayerFlag)
                .handler((context) -> {
                    final String tagName = context.get("tag-name");

                    final Tag foundTag = main.getTagManager().getTag(tagName);
                    if (foundTag == null) {
                        context.sender().sendMessage(main.parse(
                                "<error>Error: The tag <highlight>"
                                        + tagName
                                        + "</highlight> doesn't exists!")
                        );
                        return;
                    }

                    final Player playerSelector = context.flags().getValue(tagCheckPlayerFlag, null);
                    final Player player;
                    if (playerSelector != null) {
                        player = playerSelector;
                    } else if (context.sender() instanceof final Player senderPlayer) {
                        player = senderPlayer;
                    } else {
                        context.sender().sendMessage(main.parse(
                                "<error>Error: Run this in-game, or specify a player with <highlight>--player</highlight> from console.")
                        );
                        return;
                    }

                    final Object tagValue = main.getQuestPlayerManager()
                            .getOrCreateQuestPlayer(player.getUniqueId())
                            .getTagValue(foundTag.getTagName());

                    context.sender().sendMessage(main.parse(
                            "<main>" + foundTag.getTagType().name().toLowerCase() + " tag <highlight>"
                                    + foundTag.getTagName() + "</highlight> for <highlight2>" + player.getName()
                                    + "</highlight2>:</main> <highlight>" + (tagValue != null ? tagValue : "not set"))
                    );
                }));
    }
}
