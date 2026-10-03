package com.notquests.core.migrations.v6_3_0;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.notquests.core.config.YamlConfig;
import com.notquests.core.migrations.ConfigurationMigrations.Context;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * FORK DIVERGENCE: released Bukkit Location serialization on newer Paper carries only
 * world_key (a dimension's namespaced key), not the world name. The v7 conversion used to
 * copy only "world", silently nulling the world of every key-form location - 174 of them
 * in the production data this fork serves. This pins the fallback: the raw key must
 * survive the migration verbatim (the runtime reader resolves it once worlds exist).
 */
class ForkWorldKeyLocationMigrationTest {
    @TempDir
    Path dataFolder;

    @Test
    void keyFormLocationsKeepTheirWorldKey() throws Exception {
        final Path quests = dataFolder.resolve("default/quests.yml");
        Files.createDirectories(quests.getParent());
        Files.writeString(quests.getParent().resolve("category.yml"), "displayName: Default\n");
        Files.writeString(quests, """
                quests:
                  Example:
                    objectives:
                      '1':
                        objectiveType: ReachLocation
                        specifics:
                          minLocation:
                            ==: org.bukkit.Location
                            world_key: minecraft:overworld
                            x: -166.0
                            y: 67.0
                            z: -142.0
                            pitch: 0.0
                            yaw: 0.0
                          maxLocation:
                            ==: org.bukkit.Location
                            world: world_nether
                            x: 10.0
                            y: 70.0
                            z: 20.0
                """);

        new Version630Migration().migrate(new Context(
                dataFolder,
                YamlConfig.empty(),
                (message, exception) -> {}));

        final String migrated = Files.readString(quests);
        assertTrue(migrated.contains("$type: location"), "locations must convert to the v7 shape");
        assertTrue(migrated.contains("minecraft:overworld"),
                "a key-form world must survive the migration verbatim");
        assertTrue(migrated.contains("world_nether"), "name-form worlds keep working");
        assertFalse(migrated.contains("world: null"), "no location may lose its world");
    }
}
