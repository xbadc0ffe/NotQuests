package com.notquests.core.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.notquests.core.NotQuestsPlugin;
import com.notquests.core.commands.framework.CommandMessage;
import com.notquests.core.test.TestPlatformPlayer;

import java.util.ArrayList;
import java.util.List;

/**
 * FORK DIVERGENCE: takeEnabled=false is the long-standing way to make a quest NPC/GUI-only.
 * It must gate the typed take command, and ONLY the typed take command: the shipped
 * quest-preview GUI's accept button and admin-configured GiveQuest actions call giveQuest
 * directly and accepted such quests on every release before 7.0.0. These tests pin that
 * split so the gate cannot drift back into giveQuest.
 */
class TakeDisabledSemanticsTest {

    @Test
    void giveQuestAcceptsTakeDisabledQuests() {
        final NotQuestsPlugin plugin = NotQuestsPlugin.create();
        plugin.getOrCreateQuest("NpcOnlyQuest").setTakeEnabled(false);
        final TestPlayer player = new TestPlayer("player-1");

        assertTrue(plugin.giveQuest(player, "NpcOnlyQuest", false, player.messages::add),
                "GUI accepts and GiveQuest actions must work for take-disabled quests");
        assertTrue(player.messages.stream().anyMatch(message -> message.contains("[Quest Accepted]")));
    }

    @Test
    void takeCommandStillBlocksTakeDisabledQuests() {
        final NotQuestsPlugin plugin = NotQuestsPlugin.create();
        plugin.getOrCreateQuest("NpcOnlyQuest").setTakeEnabled(false);
        final TestPlayer player = new TestPlayer("player-1");

        final List<CommandMessage> result = PlayerQuestCommands.takeQuest(plugin, player, "NpcOnlyQuest");

        assertEquals(1, result.size());
        assertFalse(result.get(0).success());
        assertTrue(result.get(0).message().contains("is disabled"));
        assertFalse(player.messages.stream().anyMatch(message -> message.contains("[Quest Accepted]")),
                "the typed take command must not accept a take-disabled quest");
    }

    @Test
    void takeCommandStillAcceptsEnabledQuests() {
        final NotQuestsPlugin plugin = NotQuestsPlugin.create();
        plugin.getOrCreateQuest("OpenQuest");
        final TestPlayer player = new TestPlayer("player-1");

        final List<CommandMessage> result = PlayerQuestCommands.takeQuest(plugin, player, "OpenQuest");

        assertTrue(result.isEmpty());
        assertTrue(player.messages.stream().anyMatch(message -> message.contains("[Quest Accepted]")));
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
