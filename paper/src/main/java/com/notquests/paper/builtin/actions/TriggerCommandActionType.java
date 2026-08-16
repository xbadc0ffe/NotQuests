package com.notquests.paper.builtin.actions;

import java.util.ArrayList;
import java.util.List;
import com.notquests.paper.NotQuests;
import com.notquests.paper.managers.npc.NQNPC;
import com.notquests.paper.actions.ActionCatalog;
import com.notquests.paper.builtin.objectives.TriggerCommand;
import com.notquests.paper.registry.FieldTypes;
import com.notquests.paper.structs.ActiveObjective;
import com.notquests.paper.structs.ActiveQuest;
import com.notquests.paper.structs.Quest;
import com.notquests.paper.objectives.Objective;

public final class TriggerCommandActionType {
    private static final String TRIGGER_NAME = "triggerName";

    private TriggerCommandActionType() {}

    public static void register(final NotQuests main, final ActionCatalog actions) {
        actions.action("TriggerCommand")
                .displayName("Trigger Command")
                .description("Adds progress to active TriggerCommand objectives with the matching trigger name.")
                .field(
                        TRIGGER_NAME,
                        FieldTypes.text((context, input) -> triggerSuggestions(main)).config("specifics.triggerName"),
                        "Trigger name configured on one or more TriggerCommand objectives.")
                .singleLine((action, arguments) -> action.setValue(TRIGGER_NAME, arguments.get(0)))
                .execute((action, questPlayer, objects) -> {
                    if (questPlayer == null || questPlayer.getPlayer() == null) {
                        return;
                    }
                    for (final ActiveQuest activeQuest : questPlayer.getActiveQuests()) {
                        for (final ActiveObjective activeObjective : activeQuest.getActiveObjectives()) {
                            if (activeObjective.isUnlocked()
                                    && TriggerCommand.matches(activeObjective.getObjective(), action.text(TRIGGER_NAME))) {
                                activeObjective.addProgress(1, (NQNPC) null);
                            }
                        }
                        activeQuest.removeCompletedObjectives(true);
                    }
                    questPlayer.removeCompletedQuests();
                })
                .actionDescription((action, questPlayer, objects) -> "Triggers command objective: " + action.text(TRIGGER_NAME))
                .register();
    }

    private static List<String> triggerSuggestions(final NotQuests main) {
        final List<String> completions = new ArrayList<>();
        for (final Quest quest : main.getQuestManager().getAllQuests()) {
            for (final Objective objective : quest.getObjectives()) {
                final String triggerName = TriggerCommand.triggerName(objective);
                if (!triggerName.isBlank()) {
                    completions.add(triggerName);
                }
            }
        }
        return completions;
    }
}
