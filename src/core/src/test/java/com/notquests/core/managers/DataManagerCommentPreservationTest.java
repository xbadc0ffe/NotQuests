package com.notquests.core.managers;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * FORK DIVERGENCE: administrators keep section-header comments in their category files
 * (quests.yml and friends). Comments exist only on disk, so the save path must carry the
 * existing file's comments through the rewrite instead of serializing a comment-less
 * fresh tree - the production symptom was 20 hand-written section headers vanishing from
 * a 216-quest file on the first config save after the 7.0.0 upgrade.
 */
class DataManagerCommentPreservationTest {
    @TempDir
    Path folder;

    @Test
    void savePreservesCommentsOnSurvivingKeysAndDropsThemWithDeletedKeys() throws Exception {
        final Path file = folder.resolve("quests.yml");
        Files.writeString(file, """
                quests:
                  # ==========================================
                  # TRADERS GUILD - TIER 1 QUESTS
                  # ==========================================
                  First:
                    displayName: One
                  # a doomed note
                  Removed:
                    displayName: Gone
                """);

        final Map<String, Object> first = new LinkedHashMap<>(Map.of("displayName", "One updated"));
        final Map<String, Object> added = new LinkedHashMap<>(Map.of("displayName", "Two"));
        final Map<String, Object> quests = new LinkedHashMap<>();
        quests.put("First", first);
        quests.put("Added", added);
        final Map<String, Object> root = new LinkedHashMap<>();
        root.put("quests", quests);

        DataManager.saveYaml(file, root);

        final String saved = Files.readString(file);
        assertTrue(saved.contains("# TRADERS GUILD - TIER 1 QUESTS"),
                "comments on surviving keys must ride through the rewrite");
        assertTrue(saved.contains("One updated"), "the new data must actually be written");
        assertTrue(saved.contains("Added:"), "new keys must be written");
        assertFalse(saved.contains("doomed"), "comments attached to deleted keys go with them");
        assertFalse(saved.contains("Removed:"));
    }

    @Test
    void saveStillWritesWhenNoFileExists() throws Exception {
        final Path file = folder.resolve("fresh.yml");

        DataManager.saveYaml(file, Map.of("quests", Map.of("Solo", Map.of("displayName", "S"))));

        assertTrue(Files.readString(file).contains("Solo:"));
    }
}
