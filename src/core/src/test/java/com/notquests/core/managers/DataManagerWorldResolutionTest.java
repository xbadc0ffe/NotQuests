package com.notquests.core.managers;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import java.util.List;

/**
 * FORK DIVERGENCE: stored locations migrated from released Bukkit serialization can carry a
 * dimension key (minecraft:overworld) instead of a world name. Quest loading resolves those
 * against the live world list; this pins the mapping and its keep-verbatim fallback.
 */
class DataManagerWorldResolutionTest {
    private static final List<String> WORLDS = List.of("world", "world_nether", "world_the_end", "Hub");

    @Test
    void vanillaDimensionKeysMapToTheConventionalWorlds() {
        assertEquals("world", DataManager.resolvedWorldName("minecraft:overworld", WORLDS));
        assertEquals("world_nether", DataManager.resolvedWorldName("minecraft:the_nether", WORLDS));
        assertEquals("world_the_end", DataManager.resolvedWorldName("minecraft:the_end", WORLDS));
    }

    @Test
    void overworldMapsToTheMainWorldWhateverItsName() {
        assertEquals("earth", DataManager.resolvedWorldName(
                "minecraft:overworld", List.of("earth", "earth_nether")));
    }

    @Test
    void customKeysResolveByPathPartOrStayVerbatim() {
        assertEquals("Hub", DataManager.resolvedWorldName("minecraft:hub", WORLDS));
        assertEquals("some:unknown", DataManager.resolvedWorldName("some:unknown", WORLDS));
    }

    @Test
    void plainNamesAndNullKeepTheExistingBehavior() {
        assertEquals("world_nether", DataManager.resolvedWorldName("world_nether", WORLDS));
        assertEquals("world", DataManager.resolvedWorldName(null, WORLDS));
        assertEquals("minecraft:overworld", DataManager.resolvedWorldName("minecraft:overworld", List.of()));
    }
}
