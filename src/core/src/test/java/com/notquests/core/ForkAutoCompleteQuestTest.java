package com.notquests.core;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.notquests.core.registry.NotQuestsRegistry;
import com.notquests.core.test.TestPlatformPlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * FORK DIVERGENCE: a quest with no objectives completes the moment it is accepted -
 * the supported way to build instant shop/service quests. Requirements still gate the
 * accept and rewards fire on the immediate completion.
 */
class ForkAutoCompleteQuestTest {

    @Test
    void questWithoutObjectivesCompletesOnAcceptAndFiresRewards() {
        final NotQuestsPlugin plugin = NotQuestsPlugin.create();
        plugin.createRegistryAdapter(new NotQuestsRegistry.PlatformHooks(null, null, null))
                .actions()
                .action("GiveCash")
                .displayName("GiveCash")
                .description("Test reward action.")
                .execute((action, questPlayer, objects) -> {})
                .register();
        final var buyBread = plugin.getOrCreateQuest("BuyBread");
        buyBread.setAutoComplete(true);
        buyBread.addReward(1, "GiveCash", null).setDisplayName("16 Bread");
        final TestPlayer player = new TestPlayer("player-1");

        assertTrue(plugin.giveQuest(player, "BuyBread", false, ignored -> {}));

        assertTrue(player.messages.stream().anyMatch(message -> message.contains("[Quest Accepted]")),
                () -> String.valueOf(player.messages));
        assertTrue(player.messages.stream().anyMatch(message -> message.contains("[Quest Completed]")),
                () -> "must auto-complete on accept: " + player.messages);
        assertTrue(player.messages.stream().anyMatch(message -> message.contains("16 Bread")),
                () -> "rewards must fire: " + player.messages);
        assertTrue(plugin.questPlayer("player-1", "default").getActiveQuests().isEmpty(),
                "the quest must not stay active");
    }

    @Test
    void unflaggedObjectiveLessQuestKeepsClassicStayActiveBehavior() {
        final NotQuestsPlugin plugin = NotQuestsPlugin.create();
        plugin.getOrCreateQuest("Placeholder");
        final TestPlayer player = new TestPlayer("player-1");

        assertTrue(plugin.giveQuest(player, "Placeholder", false, ignored -> {}));

        assertFalse(player.messages.stream().anyMatch(message -> message.contains("[Quest Completed]")));
        assertFalse(plugin.questPlayer("player-1", "default").getActiveQuests().isEmpty());
    }

    @Test
    void questWithObjectivesStillWaitsForThem() {
        final NotQuestsPlugin plugin = NotQuestsPlugin.create();
        plugin.createRegistryAdapter(new NotQuestsRegistry.PlatformHooks(null, null, null))
                .objectives()
                .objective("Dummy")
                .displayName("Dummy")
                .description("Test objective.")
                .register();
        plugin.getOrCreateQuest("Journey").addObjective(1, "Dummy", new TestData(Map.of()), "");
        final TestPlayer player = new TestPlayer("player-1");

        assertTrue(plugin.giveQuest(player, "Journey", false, ignored -> {}));

        assertFalse(player.messages.stream().anyMatch(message -> message.contains("[Quest Completed]")),
                () -> String.valueOf(player.messages));
        assertFalse(plugin.questPlayer("player-1", "default").getActiveQuests().isEmpty());
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
