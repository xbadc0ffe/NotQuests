package com.notquests.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.notquests.core.registry.NotQuestsRegistry;
import com.notquests.core.structs.Quest;
import com.notquests.core.test.TestPlatformPlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * FORK DIVERGENCE: chat-noise cleanup. Blanking a language key suppresses that line
 * entirely (the code used to send the empty string, painting blank chat lines - the
 * production symptom was five blanks before every quest-accept line), a %REWARDS%
 * placeholder in a completion string collapses the reward block into the same line,
 * and a failed quest accept reports the unmet requirements as one "&"-joined line,
 * preferring each requirement's configured description over the checker's generated
 * text ("PlaceholderAPI numbers must match moreOrEqualThan 640.0.").
 */
class ForkChatCleanupTest {

    @Test
    void acceptFlowSendsNothingForBlankedKeys() {
        final NotQuestsPlugin plugin = NotQuestsPlugin.create();
        for (final String key : List.of(
                "chat.objectives-label-after-quest-accepting",
                "chat.objectives.counter",
                "chat.objectives.description",
                "chat.objectives.progress",
                "chat.quest-description",
                "chat.missing-quest-description")) {
            plugin.languageManager().configuration().set(key, "");
        }
        plugin.languageManager().configuration().set(
                "chat.quest-successfully-accepted", "[Quest Accepted] %QUESTNAME%");
        plugin.getOrCreateQuest("TestQuest");
        final TestPlayer player = new TestPlayer("player-1");

        assertTrue(plugin.giveQuest(player, "TestQuest", false, ignored -> {}));

        assertEquals(List.of("[Quest Accepted] TestQuest"), player.messages,
                "blanked keys must not paint blank chat lines");
    }

    @Test
    void questCompletionCollapsesToOneLineWithInlineRewards() {
        final NotQuestsPlugin plugin = NotQuestsPlugin.create();
        registerNoopAction(plugin, "GiveCash");
        plugin.languageManager().configuration().set(
                "chat.quest-completed-and-rewards-given", "[Quest Completed] %QUESTNAME%%REWARDS%");
        final var quest = plugin.getOrCreateQuest("TestQuest");
        quest.addReward(1, "GiveCash", null).setDisplayName("$250");
        quest.addReward(2, "GiveCash", null).setDisplayName("+2 Trader Reputation");
        final TestPlayer player = new TestPlayer("player-1");
        assertTrue(plugin.giveQuest(player, "TestQuest", false, ignored -> {}));
        player.messages.clear();

        assertTrue(plugin.completeQuest(player, "TestQuest", ignored -> {}));

        final List<String> completed = player.messages.stream()
                .filter(message -> message.contains("[Quest Completed]"))
                .toList();
        assertEquals(1, completed.size(), () -> "messages: " + player.messages);
        final String line = completed.get(0);
        assertTrue(line.contains("$250") && line.contains("+2 Trader Reputation"),
                () -> "rewards must be inline: " + line);
        assertTrue(line.contains(" & "), () -> "rewards must be joined: " + line);
        assertFalse(line.contains("\n"), () -> "must be a single line: " + line);
    }

    @Test
    void questCompletionWithoutRewardsDropsThePlaceholderCleanly() {
        final NotQuestsPlugin plugin = NotQuestsPlugin.create();
        plugin.languageManager().configuration().set(
                "chat.quest-completed-and-rewards-given", "[Quest Completed] %QUESTNAME%%REWARDS%");
        plugin.getOrCreateQuest("TestQuest");
        final TestPlayer player = new TestPlayer("player-1");
        assertTrue(plugin.giveQuest(player, "TestQuest", false, ignored -> {}));
        player.messages.clear();

        assertTrue(plugin.completeQuest(player, "TestQuest", ignored -> {}));

        assertTrue(player.messages.contains("[Quest Completed] TestQuest"),
                () -> "placeholder must vanish without rewards: " + player.messages);
    }

    @Test
    void legacyRewardBlockSkipsBlankedParts() {
        final NotQuestsPlugin plugin = NotQuestsPlugin.create();
        registerNoopAction(plugin, "GiveCash");
        plugin.languageManager().configuration().set(
                "chat.quest-completed-and-rewards-given", "[Quest Completed] %QUESTNAME%");
        plugin.languageManager().configuration().set("chat.quest-completed-rewards-prefix", "");
        plugin.languageManager().configuration().set("chat.quest-completed-rewards-suffix", "");
        plugin.languageManager().configuration().set(
                "chat.quest-completed-rewards-rewardformat", " - %reward%");
        final var quest = plugin.getOrCreateQuest("TestQuest");
        quest.addReward(1, "GiveCash", null).setDisplayName("$250");
        final TestPlayer player = new TestPlayer("player-1");
        assertTrue(plugin.giveQuest(player, "TestQuest", false, ignored -> {}));
        player.messages.clear();

        assertTrue(plugin.completeQuest(player, "TestQuest", ignored -> {}));

        final String line = player.messages.stream()
                .filter(message -> message.contains("[Quest Completed]"))
                .findFirst()
                .orElseThrow();
        assertTrue(line.contains(" - $250"), () -> line);
        assertFalse(line.contains("\n\n"), () -> "no blank lines inside: " + line);
        assertFalse(line.endsWith("\n"), () -> "no trailing blank line: " + line);
    }

    @Test
    void requirementFailureListsCustomDescriptionsOnOneLine() {
        final NotQuestsPlugin plugin = NotQuestsPlugin.create();
        registerFailingCondition(plugin, "Static");
        plugin.getOrCreateQuest("TestQuest");
        plugin.syncQuestRequirement("TestQuest", 1, "Static", new TestData(Map.of()),
                new Quest.ConditionSettings(1, false, "Requires 640 Wizard Reputation", "", false));
        plugin.syncQuestRequirement("TestQuest", 2, "Static", new TestData(Map.of()),
                new Quest.ConditionSettings(1, false, "Requires 10 Emeralds", "", false));
        final TestPlayer player = new TestPlayer("player-1");

        assertFalse(plugin.giveQuest(player, "TestQuest", false, ignored -> {}));

        assertEquals(List.of(
                        "<negative>You do not fulfill all the requirements this quest needs!\n"
                                + "<YELLOW>Requires 640 Wizard Reputation & <YELLOW>Requires 10 Emeralds"),
                player.messages,
                "custom descriptions must replace checker text, joined by & on one line");
    }

    @Test
    void requirementFailureKeepsCheckerTextWithoutCustomDescription() {
        final NotQuestsPlugin plugin = NotQuestsPlugin.create();
        registerFailingCondition(plugin, "Static");
        plugin.getOrCreateQuest("TestQuest");
        plugin.syncQuestRequirement("TestQuest", 1, "Static", new TestData(Map.of()));
        final TestPlayer player = new TestPlayer("player-1");

        assertFalse(plugin.giveQuest(player, "TestQuest", false, ignored -> {}));

        assertEquals(List.of(
                        "<negative>You do not fulfill all the requirements this quest needs!\n"
                                + "<negative>Requirement missing."),
                player.messages,
                "a requirement without a description must fall back to the checker text");
    }

    private static void registerFailingCondition(final NotQuestsPlugin plugin, final String id) {
        plugin.createRegistryAdapter(new NotQuestsRegistry.PlatformHooks(null, null, null))
                .conditions()
                .condition(id)
                .displayName(id)
                .description("Always fails in this test.")
                .check((condition, questPlayer) -> "<negative>Requirement missing.")
                .register();
    }

    private static void registerNoopAction(final NotQuestsPlugin plugin, final String id) {
        plugin.createRegistryAdapter(new NotQuestsRegistry.PlatformHooks(null, null, null))
                .actions()
                .action(id)
                .displayName(id)
                .description("Test reward action.")
                .execute((action, questPlayer, objects) -> {})
                .register();
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
