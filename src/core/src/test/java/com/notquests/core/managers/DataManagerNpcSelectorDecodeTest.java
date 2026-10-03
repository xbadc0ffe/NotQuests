package com.notquests.core.managers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.Test;

import java.util.Map;

/**
 * FORK DIVERGENCE: migrated 6.x data stores recipientNPC / npcToTalkTo as a typed map, but
 * npcSelector fields compare as "type:id" strings against interact events. Without this
 * collapse, every DeliverItems and TalkToNPC objective loaded from migrated data silently
 * never progresses - the production symptom was an NPC delivery that stopped completing
 * after the 7.0.0 upgrade.
 */
class DataManagerNpcSelectorDecodeTest {

    @Test
    void typedCitizensMapCollapsesToSelector() {
        assertEquals("citizens:0", DataManager.npcSelectorValue(
                Map.of("type", "citizens", "name", "Trader Joe", "integerID", 0)));
    }

    @Test
    void idKindsFollowTheMigrationPriority() {
        assertEquals("fancynpcs:merchant", DataManager.npcSelectorValue(
                Map.of("type", "fancynpcs", "stringID", "merchant", "integerID", 7)));
        assertEquals("armorstand:123e4567-e89b-12d3-a456-426614174000", DataManager.npcSelectorValue(
                Map.of("type", "armorstand", "uuidID", "123e4567-e89b-12d3-a456-426614174000")));
        assertEquals("citizens:5", DataManager.npcSelectorValue(
                Map.of("type", "Citizens", "id", 5)));
    }

    @Test
    void selectorStringsAndIncompleteMapsPassThrough() {
        assertEquals("citizens:1", DataManager.npcSelectorValue("citizens:1"));
        final Map<String, Object> missingId = Map.of("type", "citizens");
        assertSame(missingId, DataManager.npcSelectorValue(missingId));
        final Map<String, Object> missingType = Map.of("integerID", 3);
        assertSame(missingType, DataManager.npcSelectorValue(missingType));
    }
}
