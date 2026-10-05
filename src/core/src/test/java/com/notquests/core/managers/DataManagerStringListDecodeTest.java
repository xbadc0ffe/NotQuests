package com.notquests.core.managers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.Test;

import java.util.List;

/**
 * stringList fields (the Action chain's specifics.actions) hold a comma-separated string at
 * runtime, but category YAML stores the list shape released 6.x wrote and the v7 migration
 * preserves. Without the join, a config-loaded chain stringifies the list to "[Name]" and
 * resolves no saved action - the production symptom was every reputation reward (Action
 * chains wrapping ConsoleCommand scoreboard actions) silently doing nothing after the
 * 7.0.0 rework.
 */
class DataManagerStringListDecodeTest {

    @Test
    void yamlListsCollapseToTheRuntimeCommaString() {
        assertEquals("KnightsRepP2", DataManager.stringListValue(List.of("KnightsRepP2")));
        assertEquals("KnightsRepP3,TradersRepP1", DataManager.stringListValue(
                List.of("KnightsRepP3", "TradersRepP1")));
        assertEquals("", DataManager.stringListValue(List.of()));
    }

    @Test
    void stringsAndOtherValuesPassThrough() {
        final String commaString = "KnightsRepP3,TradersRepP1";
        assertSame(commaString, DataManager.stringListValue(commaString));
        assertEquals(null, DataManager.stringListValue(null));
    }
}
