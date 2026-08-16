package com.notquests.paper.builtin.actions;

import java.time.Duration;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import com.notquests.paper.NotQuests;
import com.notquests.paper.commands.framework.NQArguments;
import com.notquests.paper.commands.framework.NQDescription;
import com.notquests.paper.actions.ActionCatalog;
import com.notquests.paper.registry.ActionType;
import com.notquests.paper.registry.DefinedAction;
import com.notquests.paper.registry.FieldTypes;

public final class ShowTitle {
    private static final String TITLE = "title";
    private static final String FADE_IN = "fadeIn";
    private static final String STAY = "stay";
    private static final String FADE_OUT = "fadeOut";

    private ShowTitle() {}

    public static void register(final NotQuests main, final ActionCatalog actions) {
        actions.action("ShowTitle")
                .displayName("Show Title")
                .description("Shows a title overlay in the center of the target player's screen.")
                .field(
                        FADE_IN,
                        FieldTypes.duration(Duration.ofMillis(500)).config("specifics.fadeInMillis"),
                        "How long the title should fade in.")
                .field(
                        STAY,
                        FieldTypes.duration(Duration.ofMillis(3000)).config("specifics.stayMillis"),
                        "How long the title should stay fully visible.")
                .field(
                        FADE_OUT,
                        FieldTypes.duration(Duration.ofMillis(500)).config("specifics.fadeOutMillis"),
                        "How long the title should fade out.")
                .field(
                        TITLE,
                        FieldTypes.greedyText().config("specifics.title"),
                        "Title text shown in the center of the target player's screen. Use a vertical bar (`|`) to add a subtitle.")
                .commands((type, builder, actionFor) -> registerCommands(main, type, builder, actionFor))
                .singleLine((action, arguments) -> action.setValue(TITLE, String.join(" ", arguments)))
                .execute((action, questPlayer, objects) -> {
                    final String rawTitle = action.text(TITLE);
                    if (questPlayer == null || questPlayer.getPlayer() == null || rawTitle.isBlank()) {
                        return;
                    }
                    final String resolved =
                            SendMessage.resolve(main, action.action(), questPlayer, rawTitle, objects);
                    final String[] parts = resolved.split("\\|", 2);
                    final Component title = main.parse(parts[0]);
                    final Component subtitle = parts.length > 1 ? main.parse(parts[1]) : Component.empty();
                    questPlayer.getPlayer().showTitle(Title.title(
                            title,
                            subtitle,
                            Title.Times.times(
                                    action.duration(FADE_IN, Duration.ofMillis(500)),
                                    action.duration(STAY, Duration.ofMillis(3000)),
                                    action.duration(FADE_OUT, Duration.ofMillis(500)))));
                })
                .actionDescription((action, questPlayer, objects) -> "Shows title: " + action.text(TITLE))
                .register();
    }

    private static void registerCommands(
            final NotQuests main,
            final ActionType type,
            final com.notquests.paper.commands.framework.NQCommandBuilder builder,
            final com.notquests.paper.actions.ActionFor actionFor) {
        main.getCommandManager().getNQCommandManager().command(builder
                .required(
                        TITLE,
                        NQArguments.greedyStringArgument(),
                        NQDescription.of("Title text shown in the center of the target player's screen. Use `|` to add a subtitle."))
                .handler(context -> {
                    final DefinedAction action = type.createAction();
                    action.setValue(TITLE, context.get(TITLE));
                    main.getActionCatalog().addAction(action, context, actionFor);
                }));
        main.getCommandManager().getNQCommandManager().command(builder
                .literal("timed", NQDescription.of("Creates a title action with custom fade-in, stay, and fade-out durations."))
                .required(FADE_IN, NQArguments.durationArgument(), NQDescription.of("How long the title should fade in. Examples: 250ms, 1s."))
                .required(STAY, NQArguments.durationArgument(), NQDescription.of("How long the title should stay fully visible. Examples: 3s, 1500ms."))
                .required(FADE_OUT, NQArguments.durationArgument(), NQDescription.of("How long the title should fade out. Examples: 500ms, 1s."))
                .required(
                        TITLE,
                        NQArguments.greedyStringArgument(),
                        NQDescription.of("Title text shown in the center of the target player's screen. Use `|` to add a subtitle."))
                .handler(context -> {
                    final DefinedAction action = type.createAction();
                    action.setValue(FADE_IN, context.get(FADE_IN));
                    action.setValue(STAY, context.get(STAY));
                    action.setValue(FADE_OUT, context.get(FADE_OUT));
                    action.setValue(TITLE, context.get(TITLE));
                    main.getActionCatalog().addAction(action, context, actionFor);
                }));
    }
}
