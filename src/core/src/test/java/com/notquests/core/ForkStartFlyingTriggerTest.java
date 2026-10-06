package com.notquests.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.notquests.core.platform.NotQuestsAdapter;
import com.notquests.core.registry.NotQuestsRegistry;
import com.notquests.core.test.TestPlatformPlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * FORK DIVERGENCE: covers the STARTFLYING trigger — the platform reports an actual flight start
 * via {@link NotQuestsPlugin#playerStartedFlying}, and the trigger runs its action only while the
 * quest is active.
 */
class ForkStartFlyingTriggerTest {

  @Test
  void startFlyingTriggerFiresActionOnlyWhileQuestIsActive() {
    final NotQuestsPlugin plugin = NotQuestsPlugin.create();
    final List<String> executions = new ArrayList<>();
    final NotQuestsAdapter adapter =
        plugin.createRegistryAdapter(new NotQuestsRegistry.PlatformHooks(null, null, null));
    adapter.actions()
        .action("Record")
        .displayName("Record")
        .description("Records that an action ran.")
        .execute((action, questPlayer, objects) -> executions.add(questPlayer.playerIdentifier()))
        .register();
    plugin.getOrCreateQuest("ExampleQuest");
    assertTrue(plugin.syncSavedAction(
        "OnStartFlying",
        "Record",
        new TestData(Map.of()),
        null,
        "default",
        List.of()));
    assertTrue(plugin.syncQuestTrigger(
        "ExampleQuest",
        1,
        "STARTFLYING",
        new TestData(Map.of(
            "action", "OnStartFlying",
            "applyOn", 0,
            "amount", 1,
            "worldName", "ALL"))));
    final TestPlayer player = new TestPlayer("player-1");

    plugin.playerStartedFlying(player);
    assertEquals(List.of(), executions);

    plugin.setActiveQuestNames(player, List.of("ExampleQuest"));
    plugin.activateQuestProgress(player, "ExampleQuest", ignored -> {});
    plugin.playerStartedFlying(player);
    assertEquals(List.of("player-1"), executions);
  }

  @Test
  void startFlyingTriggerKeepsFiringForEveryFlightStartWhileActive() {
    final NotQuestsPlugin plugin = NotQuestsPlugin.create();
    final List<String> executions = new ArrayList<>();
    final NotQuestsAdapter adapter =
        plugin.createRegistryAdapter(new NotQuestsRegistry.PlatformHooks(null, null, null));
    adapter.actions()
        .action("Record")
        .displayName("Record")
        .description("Records that an action ran.")
        .execute((action, questPlayer, objects) -> executions.add(questPlayer.playerIdentifier()))
        .register();
    plugin.getOrCreateQuest("ExampleQuest");
    assertTrue(plugin.syncSavedAction(
        "OnStartFlying",
        "Record",
        new TestData(Map.of()),
        null,
        "default",
        List.of()));
    assertTrue(plugin.syncQuestTrigger(
        "ExampleQuest",
        1,
        "STARTFLYING",
        new TestData(Map.of(
            "action", "OnStartFlying",
            "applyOn", 0,
            "amount", 1,
            "worldName", "ALL"))));
    final TestPlayer player = new TestPlayer("player-1");
    plugin.setActiveQuestNames(player, List.of("ExampleQuest"));
    plugin.activateQuestProgress(player, "ExampleQuest", ignored -> {});

    plugin.playerStartedFlying(player);
    plugin.playerStartedFlying(player);
    assertEquals(List.of("player-1", "player-1"), executions);
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
