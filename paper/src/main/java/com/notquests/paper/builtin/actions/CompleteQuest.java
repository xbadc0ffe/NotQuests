package com.notquests.paper.builtin.actions;

import com.notquests.paper.NotQuests;
import com.notquests.paper.actions.ActionCatalog;
import com.notquests.paper.registry.FieldTypes;
import com.notquests.paper.structs.ActiveQuest;
import com.notquests.paper.structs.Quest;

public final class CompleteQuest {
    private static final String QUEST = "quest";

    private CompleteQuest() {}

    public static void register(final NotQuests main, final ActionCatalog actions) {
        actions.action("CompleteQuest")
                .displayName("Complete Quest")
                .description("Completes another active quest for the target player.")
                .field(
                        QUEST,
                        FieldTypes.questName().config("specifics.quest"),
                        "Active quest that should be completed for the target player.")
                .singleLine((action, arguments) -> action.setValue(QUEST, arguments.get(0)))
                .execute((action, questPlayer, objects) -> {
                    if (questPlayer == null) {
                        return;
                    }
                    final Quest quest = main.getQuestManager().getQuest(action.text(QUEST));
                    if (quest == null) {
                        main.getLogManager().warn("Tried to execute CompleteQuest action with unknown quest: " + action.text(QUEST));
                        return;
                    }
                    final ActiveQuest activeQuest = questPlayer.getActiveQuest(quest);
                    if (activeQuest != null && !activeQuest.isCompleted()) {
                        questPlayer.forceActiveQuestCompleted(activeQuest);
                    } else if (activeQuest != null) {
                        questPlayer.removeCompletedQuests();
                    }
                })
                .actionDescription((action, questPlayer, objects) -> "Completes quest: " + action.text(QUEST))
                .register();
    }
}
