package com.notquests.paper.commands.category.admin.structs;

import org.bukkit.entity.Player;
import com.notquests.paper.NotQuests;
import com.notquests.paper.commands.BaseCommand;
import com.notquests.paper.commands.framework.NQArguments;
import com.notquests.paper.commands.framework.NQCommandBuilder;
import com.notquests.paper.commands.framework.NQCommandManager;
import com.notquests.paper.commands.framework.NQDescription;
import com.notquests.paper.managers.npc.NQNPC;
import com.notquests.paper.builtin.objectives.TriggerCommand;
import com.notquests.paper.structs.ActiveObjective;
import com.notquests.paper.structs.ActiveQuest;
import com.notquests.paper.structs.Quest;
import com.notquests.paper.structs.QuestPlayer;
import com.notquests.paper.objectives.Objective;

import java.util.ArrayList;
import java.util.List;

public class ObjectiveTriggerCommand extends BaseCommand {

    public ObjectiveTriggerCommand(NotQuests notQuests, NQCommandBuilder builder) {
        super(notQuests, builder);
    }

    @Override
    public void apply(NQCommandManager commandManager) {
        commandManager.command(builder.commandDescription(NQDescription.of("This triggers the Trigger Command which is needed to complete a TriggerObjective (don't mistake it with Triggers & actions)."))
                .literal("triggerObjective", NQDescription.of("Triggers objective progress manually."))
                .required("trigger-name", NQArguments.stringArgument(), NQDescription.of("Name of the trigger which should be triggered."), (context, input) -> {
                            final List<String> completions = new ArrayList<>();
                            for (final Quest quest : notQuests.getQuestManager().getAllQuests()) {
                                for (final Objective objective : quest.getObjectives()) {
                                    final String triggerName = TriggerCommand.triggerName(objective);
                                    if (!triggerName.isBlank()) {
                                        completions.add(triggerName);
                                    }
                                }
                            }
                            return completions;
                        }
                )
                .required("player", NQArguments.playerArgument(), NQDescription.of("Player whose trigger should e triggered."))
                .handler((context) -> {
                    final String triggerName = context.get("trigger-name");
                    final Player player = context.get("player");
                    final QuestPlayer questPlayer = notQuests.getQuestPlayerManager().getActiveQuestPlayer(player.getUniqueId());
                    if (questPlayer != null) {
                        if (!questPlayer.getActiveQuests().isEmpty()) {
                            for (ActiveQuest activeQuest : questPlayer.getActiveQuests()) {
                                for (ActiveObjective activeObjective : activeQuest.getActiveObjectives()) {
                                    if (activeObjective.isUnlocked()) {
                                        if (TriggerCommand.matches(activeObjective.getObjective(), triggerName)) {
                                            activeObjective.addProgress(1, (NQNPC) null);
                                        }
                                    }
                                }
                                activeQuest.removeCompletedObjectives(true);
                            }
                            questPlayer.removeCompletedQuests();
                        }
                    }
                }));
    }
}
