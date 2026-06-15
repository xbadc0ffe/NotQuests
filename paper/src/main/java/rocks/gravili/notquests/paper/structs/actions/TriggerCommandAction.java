package rocks.gravili.notquests.paper.structs.actions;

import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.commands.framework.NQArguments;
import rocks.gravili.notquests.paper.commands.framework.NQCommandBuilder;
import rocks.gravili.notquests.paper.commands.framework.NQCommandManager;
import rocks.gravili.notquests.paper.commands.framework.NQDescription;
import rocks.gravili.notquests.paper.managers.npc.NQNPC;
import rocks.gravili.notquests.paper.structs.ActiveObjective;
import rocks.gravili.notquests.paper.structs.ActiveQuest;
import rocks.gravili.notquests.paper.structs.Quest;
import rocks.gravili.notquests.paper.structs.QuestPlayer;
import rocks.gravili.notquests.paper.structs.objectives.Objective;
import rocks.gravili.notquests.paper.structs.objectives.TriggerCommandObjective;

import java.util.ArrayList;
import java.util.List;

public class TriggerCommandAction extends Action {

    private String triggerCommandName = "";


    public TriggerCommandAction(final NotQuests main) {
        super(main);
    }

    public static void handleCommands(NotQuests main, NQCommandManager manager, NQCommandBuilder builder, ActionFor actionFor) {
        manager.command(builder.required("Trigger Name", NQArguments.stringArgument(), NQDescription.of("Name of the trigger which should be triggered."), (context, input) -> {
                            List<String> completions = new ArrayList<>();
                            for (final Quest quest : main.getQuestManager().getAllQuests()) {
                                for (final Objective objective : quest.getObjectives()) {
                                    if (objective instanceof final TriggerCommandObjective triggerCommandObjective) {
                                        completions.add(triggerCommandObjective.getTriggerName());
                                    }
                                }
                            }
                            return completions;
                        }
                )
                .handler((context) -> {
                    final String triggerName = context.get("Trigger Name");

                    TriggerCommandAction triggerCommandAction = new TriggerCommandAction(main);
                    triggerCommandAction.setTriggerCommand(triggerName);

                    main.getActionManager().addAction(triggerCommandAction, context, actionFor);
                }));
    }

    public final String getTriggerCommand() {
        return triggerCommandName;
    }

    public void setTriggerCommand(final String triggerCommandName) {
        this.triggerCommandName = triggerCommandName;
    }


    @Override
    public void executeInternally(final QuestPlayer questPlayer, Object... objects) {

        if (questPlayer == null || questPlayer.getPlayer() == null) {
            return;
        }

        if (questPlayer.getActiveQuests().size() > 0) {
            for (ActiveQuest activeQuest : questPlayer.getActiveQuests()) {
                for (ActiveObjective activeObjective : activeQuest.getActiveObjectives()) {
                    if (activeObjective.isUnlocked()) {
                        if (activeObjective.getObjective() instanceof TriggerCommandObjective triggerCommandObjective) {
                            if (triggerCommandObjective.getTriggerName().equalsIgnoreCase(getTriggerCommand())) {
                                activeObjective.addProgress(1, (NQNPC) null);
                            }
                        }
                    }

                }
                activeQuest.removeCompletedObjectives(true);
            }
            questPlayer.removeCompletedQuests();
        }
    }

    @Override
    public void save(FileConfiguration configuration, String initialPath) {
        configuration.set(initialPath + ".specifics.triggerName", getTriggerCommand());
    }

    @Override
    public void load(final FileConfiguration configuration, String initialPath) {
        this.triggerCommandName = configuration.getString(initialPath + ".specifics.triggerName");
    }

    @Override
    public void deserializeFromSingleLineString(ArrayList<String> arguments) {
        this.triggerCommandName = arguments.get(0);
    }


    @Override
    public String getActionDescription(final QuestPlayer questPlayer, final Object... objects) {
        return "Triggers TriggerCommand: " + getTriggerCommand();
    }
}
