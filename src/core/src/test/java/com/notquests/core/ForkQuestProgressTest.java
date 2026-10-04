package com.notquests.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.notquests.core.gui.GuiContext;
import com.notquests.core.gui.GuiService;
import com.notquests.core.gui.GuiService.ResolvedGui;
import com.notquests.core.registry.NotQuestsRegistry;
import com.notquests.core.test.TestPlatformPlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * FORK DIVERGENCE: GUI quest progress. %COMPLETEDOBJECTIVESCOUNT% and
 * %ALLOBJECTIVESCOUNT% resolve as GUI placeholders, and the %QUESTOBJECTIVESPROGRESS%
 * lore token expands to one line per top-level objective with live numbers - the
 * production symptom was the Questinfo progress item showing the raw placeholder text.
 */
class ForkQuestProgressTest {

    @Test
    void progressListShowsLiveNumbersAndCompletionMarks() {
        final NotQuestsPlugin plugin = plugin();
        final TestPlayer player = new TestPlayer("player-1");
        assertTrue(plugin.giveQuest(player, "Story", false, ignored -> {}));

        assertEquals(0, plugin.questObjectivesCompletedCount(player, "Story"));
        final List<String> lines = plugin.questObjectivesProgressList(player, "Story");
        assertEquals(2, lines.size(), () -> String.valueOf(lines));
        assertTrue(lines.get(0).contains("Dummy") && lines.get(0).contains("0")
                && lines.get(0).contains("1"), () -> lines.get(0));

        plugin.questPlayer("player-1", "default")
                .getActiveQuests()
                .get(0)
                .removeActiveObjective(new int[] {1});

        assertEquals(1, plugin.questObjectivesCompletedCount(player, "Story"));
        final List<String> updated = plugin.questObjectivesProgressList(player, "Story");
        assertEquals(2, updated.size());
        assertTrue(updated.get(0).contains("✔"), () -> updated.get(0));
        assertTrue(updated.get(1).contains("Dummy") && updated.get(1).contains("0"), () -> updated.get(1));
    }

    @Test
    void guiLoreResolvesCountsAndExpandsTheProgressToken() {
        final NotQuestsPlugin plugin = plugin();
        plugin.languageManager().configuration().set(
                "gui.active-quest-info.button.infobook.lore",
                List.of(
                        "Objectives: %COMPLETEDOBJECTIVESCOUNT% / %ALLOBJECTIVESCOUNT%",
                        "%QUESTOBJECTIVESPROGRESS%"));
        final TestPlayer player = new TestPlayer("player-1");
        assertTrue(plugin.giveQuest(player, "Story", false, ignored -> {}));

        final ResolvedGui view = new GuiService(plugin).build(
                "active-quest-info",
                player,
                new GuiContext("Story", "", "", null));

        final List<String> lore = view.slots().stream()
                .flatMap(slot -> slot.lore().stream())
                .toList();
        assertTrue(lore.contains("Objectives: 0 / 2"), () -> String.valueOf(lore));
        assertTrue(lore.stream().anyMatch(line -> line.contains("Dummy:")),
                () -> "per-objective progress must expand: " + lore);
        assertTrue(lore.stream().noneMatch(line -> line.contains("%COMPLETEDOBJECTIVESCOUNT%")),
                () -> "placeholders must not leak: " + lore);
    }

    private static NotQuestsPlugin plugin() {
        final NotQuestsPlugin plugin = NotQuestsPlugin.create();
        plugin.createRegistryAdapter(new NotQuestsRegistry.PlatformHooks(null, null, null))
                .objectives()
                .objective("Dummy")
                .displayName("Dummy")
                .description("Test objective.")
                .register();
        final var quest = plugin.getOrCreateQuest("Story");
        quest.addObjective(1, "Dummy", new TestData(Map.of()), "");
        quest.addObjective(2, "Dummy", new TestData(Map.of()), "");
        return plugin;
    }

    private static final class TestPlayer implements TestPlatformPlayer {
        private final String playerIdentifier;
        final List<String> messages = new ArrayList<>();

        private TestPlayer(final String playerIdentifier) {
            this.playerIdentifier = playerIdentifier;
        }

        @Override
        public boolean hasPlayer() {
            return true;
        }

        @Override
        public String playerIdentifier() {
            return playerIdentifier;
        }

        @Override
        public String playerName() {
            return playerIdentifier;
        }

        @Override
        public void sendMessage(final String miniMessage) {
            messages.add(miniMessage);
        }

        @Override
        public long currentWorldTimeTicks() {
            return 0;
        }

        @Override
        public void sendActionBar(final String miniMessage) {}

        @Override
        public void showProgressBossBar(final String miniMessage, final double progress) {}

        @Override
        public void hideProgressBossBar() {}

        @Override
        public void chat(final String message) {}

        @Override
        public void performCommand(final String command) {}

        @Override
        public void closeInventory() {}

        @Override
        public com.notquests.core.platform.NQLocation lookingAtBlock(final double maxDistance) {
            return null;
        }

        @Override
        public void showTitle(
                final String title,
                final String subtitle,
                final java.time.Duration fadeIn,
                final java.time.Duration stay,
                final java.time.Duration fadeOut) {}
    }
}
