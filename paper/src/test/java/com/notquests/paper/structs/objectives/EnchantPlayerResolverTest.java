package com.notquests.paper.objectives;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.UUID;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import org.mockbukkit.mockbukkit.plugin.PluginMock;
import org.mockito.ArgumentCaptor;
import com.notquests.paper.NotQuests;
import com.notquests.paper.builtin.objectives.Enchant;
import com.notquests.paper.registry.ObjectiveType;

/**
 * Covers the player resolver on the {@code Enchant} objective.
 *
 * <p>{@code ObjectiveType}'s two-argument {@code .on(...)} form resolves the player reflectively by
 * looking for a {@code getPlayer()} method on the event. {@link EnchantItemEvent} names its
 * accessor {@code getEnchanter()} instead, so that lookup fails, the failure is swallowed, and
 * dispatch stops before the handler — leaving the objective unable to progress at all. Enchant
 * therefore has to pass an explicit resolver.
 */
class EnchantPlayerResolverTest {

  private ServerMock server;
  private NotQuests main;
  private ObjectiveCatalog catalog;

  @BeforeEach
  void setUp() {
    server = MockBukkit.mock();
    final PluginMock plugin = MockBukkit.createMockPlugin();
    main = mock(NotQuests.class, RETURNS_DEEP_STUBS);
    when(main.getMain()).thenReturn(plugin);
    catalog = mock(ObjectiveCatalog.class);
  }

  @AfterEach
  void tearDown() {
    HandlerList.unregisterAll();
    MockBukkit.unmock();
  }

  @Test
  @DisplayName("Enchant resolves the enchanting player from an EnchantItemEvent")
  void enchantResolvesThePlayerFromTheEnchantItemEvent() {
    final PlayerMock player = server.addPlayer("Steve");
    when(catalog.objective("Enchant")).thenReturn(new ObjectiveType.Builder(main, catalog, "Enchant"));
    when(main.getQuestPlayerManager().getActiveQuestPlayer(any(UUID.class))).thenReturn(null);

    Enchant.register(main, catalog);

    final ArgumentCaptor<ObjectiveType> registered = ArgumentCaptor.forClass(ObjectiveType.class);
    verify(catalog).registerObjective(registered.capture());
    registered.getValue().registerEventListeners();

    server.getPluginManager().callEvent(enchantItemEvent(player));

    // Reaching the quest-player lookup at all proves a player was resolved from the event; with the
    // reflective getPlayer() lookup this returned null and dispatch stopped one line earlier.
    verify(main.getQuestPlayerManager()).getActiveQuestPlayer(player.getUniqueId());
  }

  @Test
  @DisplayName("EnchantItemEvent exposes getEnchanter, not getPlayer")
  void enchantItemEventHasNoGetPlayerAccessor() {
    // Why Enchant needs an explicit resolver: the default one reflects on "getPlayer".
    assertThrows(NoSuchMethodException.class, () -> EnchantItemEvent.class.getMethod("getPlayer"));
    assertDoesNotThrow(() -> EnchantItemEvent.class.getMethod("getEnchanter"));
  }

  private EnchantItemEvent enchantItemEvent(final Player player) {
    return new EnchantItemEvent(
        player,
        mock(InventoryView.class),
        mock(Block.class),
        new ItemStack(Material.DIAMOND_SWORD),
        30,
        Map.of(),
        Enchantment.SHARPNESS,
        1,
        0);
  }
}
