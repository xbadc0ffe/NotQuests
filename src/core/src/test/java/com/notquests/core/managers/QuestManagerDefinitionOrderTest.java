package com.notquests.core.managers;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import java.util.List;

/**
 * FORK DIVERGENCE: quest listings follow definition order - the order quests appear in
 * quests.yml, with new quests appended - never an alphabetical re-sort. Administrators
 * order that file deliberately, and both the GUIs and the config re-save iterate
 * getAllQuests(), so a sort here scrambles the visible quest order AND rewrites the
 * hand-ordered file. These tests pin the restored pre-7.0.0 semantics.
 */
class QuestManagerDefinitionOrderTest {

    @Test
    void listingsKeepDefinitionOrderNotAlphabetical() {
        final QuestManager manager = new QuestManager();
        manager.getOrCreateQuest("Zebra");
        manager.getOrCreateQuest("Apple");
        manager.getOrCreateQuest("Mango");

        assertEquals(List.of("Zebra", "Apple", "Mango"), manager.getQuestNames());
    }

    @Test
    void createdAndClonedQuestsAppend() {
        final QuestManager manager = new QuestManager();
        manager.getOrCreateQuest("Second");
        manager.createQuest("Another", null);
        manager.cloneQuest("Second", "AClone");

        assertEquals(List.of("Second", "Another", "AClone"), manager.getQuestNames());
    }

    @Test
    void removalKeepsTheRemainingOrder() {
        final QuestManager manager = new QuestManager();
        manager.getOrCreateQuest("One");
        manager.getOrCreateQuest("Two");
        manager.getOrCreateQuest("Three");
        manager.removeQuest("Two");

        assertEquals(List.of("One", "Three"), manager.getQuestNames());
    }
}
