package rocks.gravili.notquests.paper.structs.actions;

import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.commands.framework.NQCommandBuilder;
import rocks.gravili.notquests.paper.commands.framework.NQCommandManager;
import rocks.gravili.notquests.paper.commands.framework.NQDescription;
import rocks.gravili.notquests.paper.structs.ActiveQuest;
import rocks.gravili.notquests.paper.structs.Quest;
import rocks.gravili.notquests.paper.structs.QuestPlayer;

import java.util.ArrayList;

import static rocks.gravili.notquests.paper.commands.arguments.QuestArgument.questArgument;

public class FailQuestAction extends Action {

    private String questToFailName = "";

    public FailQuestAction(final NotQuests main) {
        super(main);
    }

    public static void handleCommands(
            NotQuests main,
            NQCommandManager manager,
            NQCommandBuilder builder,
            ActionFor actionFor) {
        manager.command(builder.required("quest to fail", questArgument(main), NQDescription.of("Name of the Quest which should be failed for the player."))
                .handler(
                        (context) -> {
                            final Quest foundQuest = context.get("quest to fail");

                            FailQuestAction failQuestAction = new FailQuestAction(main);
                            failQuestAction.setQuestToFailName(foundQuest.getIdentifier());

                            main.getActionManager().addAction(failQuestAction, context, actionFor);
                        }));
    }

    public final String getQuestToFailName() {
        return questToFailName;
    }

    public void setQuestToFailName(final String questName) {
        this.questToFailName = questName;
    }

    @Override
    public void executeInternally(final QuestPlayer questPlayer, Object... objects) {
        Quest foundQuest = main.getQuestManager().getQuest(getQuestToFailName());
        if (foundQuest == null) {
            main.getLogManager()
                    .warn(
                            "Tried to execute FailQuest action with null quest. Cannot find the following Quest: "
                                    + getQuestToFailName());
            return;
        }

        if (questPlayer == null) {
            return;
        }

        ActiveQuest foundActiveQuest = questPlayer.getActiveQuest(foundQuest);

        if (foundActiveQuest == null) {
            return;
        }

        questPlayer.failQuest(foundActiveQuest);
    }

    @Override
    public void save(FileConfiguration configuration, String initialPath) {
        configuration.set(initialPath + ".specifics.quest", getQuestToFailName());
    }

    @Override
    public void load(final FileConfiguration configuration, String initialPath) {
        this.questToFailName = configuration.getString(initialPath + ".specifics.quest");
    }

    @Override
    public void deserializeFromSingleLineString(ArrayList<String> arguments) {
        this.questToFailName = arguments.get(0);
    }

    @Override
    public String getActionDescription(final QuestPlayer questPlayer, final Object... objects) {
        return "Fails Quest: " + getQuestToFailName();
    }
}
