package com.notquests.builtin.actions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.notquests.builtin.BuiltInPack;
import com.notquests.builtin.TestPlatformPlayer;
import com.notquests.core.NotQuestsPlugin;
import com.notquests.core.actions.Action;
import com.notquests.core.managers.DataManager;
import com.notquests.core.managers.DataManager.ReloadTarget;
import com.notquests.core.platform.NotQuestsAdapter;
import com.notquests.core.registry.NotQuestsRegistry;
import com.notquests.core.structs.Quest;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Category YAML stores an Action chain's references as a string list
 * (specifics.actions: [Name]) - the shape released 6.x wrote and the v7
 * migration preserves - while the single-line command parser stores a
 * comma-separated string. Both shapes must resolve the referenced saved
 * actions; the production symptom was every config-loaded reputation reward
 * (ConsoleCommand chains) silently matching no saved action after the
 * 7.0.0 rework because the list stringified to "[Name]".
 */
class ActionChainConfigLoadTest {
    @TempDir
    Path tempDir;

    @Test
    void rewardChainLoadedFromYamlListExecutesTheSavedAction() throws Exception {
        final Path category = tempDir.resolve("default");
        Files.createDirectories(category);
        Files.writeString(category.resolve("category.yml"), """
                id: default
                """);
        Files.writeString(category.resolve("actions.yml"), """
                actions:
                  Notify:
                    actionType: SendMessage
                    displayName: Notify
                    specifics:
                      message: rep granted
                    conditions: {}
                """);
        Files.writeString(category.resolve("quests.yml"), """
                quests:
                  RepQuest:
                    rewards:
                      '1':
                        actionType: Action
                        specifics:
                          actions:
                          - Notify
                          amount: 1
                        displayName: +2 Rep
                """);

        final NotQuestsPlugin plugin = NotQuestsPlugin.create();
        final NotQuestsAdapter adapter =
                plugin.createRegistryAdapter(new NotQuestsRegistry.PlatformHooks(null, null, null));
        BuiltInPack.register(plugin, adapter);
        final DataManager persistence = new DataManager(plugin, adapter, tempDir);
        assertTrue(persistence.reload(ReloadTarget.ALL));

        final Quest quest = plugin.quest("RepQuest");
        final Action reward = quest.getRewards().get(0);
        assertEquals("Notify", reward.text("actions"));

        final MessagePlayer player = new MessagePlayer();
        plugin.registry().actions().stream()
                .filter(type -> type.id().equals("Action"))
                .findFirst()
                .orElseThrow()
                .executor()
                .execute(reward, player);
        assertEquals(List.of("rep granted"), player.messages);
    }

    private static final class MessagePlayer implements TestPlatformPlayer {
        private final List<String> messages = new ArrayList<>();

        @Override
        public boolean hasPlayer() {
            return true;
        }

        @Override
        public String playerIdentifier() {
            return "player-1";
        }

        @Override
        public com.notquests.core.platform.NQLocation lookingAtBlock(final double maxDistance) {
            return null;
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
        public void showTitle(
                final String title,
                final String subtitle,
                final java.time.Duration fadeIn,
                final java.time.Duration stay,
                final java.time.Duration fadeOut) {}

        @Override
        public void chat(final String message) {}

        @Override
        public void performCommand(final String command) {}

        @Override
        public void closeInventory() {}
    }
}
