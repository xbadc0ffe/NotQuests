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

public class CompleteQuestAction extends Action {

    private String questToCompleteName = "";

    public CompleteQuestAction(final NotQuests main) {
        super(main);
    }

    public static void handleCommands(
            NotQuests main,
            NQCommandManager manager,
            NQCommandBuilder builder,
            ActionFor actionFor) {
        manager.command(builder.required("quest to complete", questArgument(main), NQDescription.of("Name of the Quest which should be completed for the player."))
                .handler(
                        (context) -> {
                            final Quest foundQuest = context.get("quest to complete");

                            CompleteQuestAction completeQuestAction = new CompleteQuestAction(main);
                            completeQuestAction.setQuestToCompleteName(foundQuest.getIdentifier());

                            main.getActionManager().addAction(completeQuestAction, context, actionFor);
                        }));
    }

    public final String getQuestToCompleteName() {
        return questToCompleteName;
    }

    public void setQuestToCompleteName(final String questName) {
        this.questToCompleteName = questName;
    }

    @Override
    public void executeInternally(final QuestPlayer questPlayer, Object... objects) {
        main.getLogManager().debug("Executing CompleteQuestAction");

        Quest foundQuest = main.getQuestManager().getQuest(getQuestToCompleteName());
        if (foundQuest == null) {
            main.getLogManager()
                    .warn(
                            "Tried to execute CompleteQuest action with null quest. Cannot find the following Quest: "
                                    + getQuestToCompleteName());
            return;
        }

        if (questPlayer == null) {
            main.getLogManager().warn("QuestPlayer is null during CompleteQuestAction...");
            return;
        }

        ActiveQuest foundActiveQuest = questPlayer.getActiveQuest(foundQuest);

        if (foundActiveQuest == null) {
            main.getLogManager().debug("foundActiveQuest == null.");
            return;
        }
        if (foundActiveQuest.isCompleted()) {
            main.getLogManager().debug("foundActiveQuest.isCompleted()");
            questPlayer.sendDebugMessage("Quest is already completed. Removing from active quests...");
            questPlayer.removeCompletedQuests();
            return;
        }

        questPlayer.forceActiveQuestCompleted(foundActiveQuest);
    }

    @Override
    public void save(FileConfiguration configuration, String initialPath) {
        configuration.set(initialPath + ".specifics.quest", getQuestToCompleteName());
    }

    @Override
    public void load(final FileConfiguration configuration, String initialPath) {
        this.questToCompleteName = configuration.getString(initialPath + ".specifics.quest");
    }

    @Override
    public void deserializeFromSingleLineString(ArrayList<String> arguments) {
        this.questToCompleteName = arguments.get(0);
    }

    @Override
    public String getActionDescription(final QuestPlayer questPlayer, final Object... objects) {
        return "Completes Quest: " + getQuestToCompleteName();
    }
}
