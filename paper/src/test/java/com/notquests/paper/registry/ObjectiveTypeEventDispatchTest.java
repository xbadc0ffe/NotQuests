package com.notquests.paper.registry;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCreativeEvent;
import org.bukkit.event.inventory.SmithItemEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import org.mockbukkit.mockbukkit.plugin.PluginMock;
import com.notquests.paper.NotQuests;
import com.notquests.paper.objectives.ObjectiveCatalog;
import com.notquests.paper.structs.ActiveObjective;
import com.notquests.paper.structs.ActiveQuest;
import com.notquests.paper.structs.QuestPlayer;

/**
 * Covers the event-dispatch guard in {@link ObjectiveType}.
 *
 * <p>Bukkit resolves the class a listener registers for to a {@code HandlerList} by walking up the
 * superclass chain to the first class that declares {@code getHandlerList()}, and then delivers
 * every event on that list. A binding registered for a subclass therefore also receives that
 * subclass's parent and siblings. {@code ObjectiveType} supplies a hand-written
 * {@code EventExecutor}, so it does not get the {@code isInstance} filter that Bukkit builds into
 * the executors it generates for {@code @EventHandler} methods, and must apply that filter itself.
 *
 * <p>These tests run against MockBukkit's real {@code PluginManagerMock}, whose
 * {@code getRegistrationClass} performs the identical superclass walk, so the sibling delivery
 * being guarded against is genuinely reproduced rather than simulated.
 */
class ObjectiveTypeEventDispatchTest {

  /** Parent that owns the {@link HandlerList}; the child and sibling below inherit it. */
  public static class ParentProbeEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();

    @Override
    public HandlerList getHandlers() {
      return HANDLERS;
    }

    public static HandlerList getHandlerList() {
      return HANDLERS;
    }
  }

  /** Stands in for PlayerDeathEvent / CraftItemEvent — registered for, but not solely delivered. */
  public static class ChildProbeEvent extends ParentProbeEvent {}

  /** Stands in for SmithItemEvent / InventoryCreativeEvent — a sibling on the same handler list. */
  public static class SiblingProbeEvent extends ParentProbeEvent {}

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
  @DisplayName("a binding registered for a subclass drops a parent instance without throwing")
  void bindingForSubclassDropsParentEvent() {
    final AtomicInteger resolverCalls = new AtomicInteger();
    final AtomicInteger handlerCalls = new AtomicInteger();
    registerProbe(null, resolverCalls, handlerCalls);

    assertDoesNotThrow(() -> server.getPluginManager().callEvent(new ParentProbeEvent()));

    assertEquals(0, resolverCalls.get(), "a parent instance must not reach the player resolver");
    assertEquals(0, handlerCalls.get(), "a parent instance must not reach the objective handler");
  }

  @Test
  @DisplayName("a binding registered for a subclass drops a sibling instance without throwing")
  void bindingForSubclassDropsSiblingEvent() {
    final AtomicInteger resolverCalls = new AtomicInteger();
    final AtomicInteger handlerCalls = new AtomicInteger();
    registerProbe(null, resolverCalls, handlerCalls);

    assertDoesNotThrow(() -> server.getPluginManager().callEvent(new SiblingProbeEvent()));

    assertEquals(0, resolverCalls.get(), "a sibling instance must not reach the player resolver");
    assertEquals(0, handlerCalls.get(), "a sibling instance must not reach the objective handler");
  }

  @Test
  @DisplayName("a binding registered for a subclass still runs its handler for its own event")
  void bindingForSubclassStillRunsHandlerForItsOwnEvent() {
    // The anti-trivial case: a guard that rejected everything would satisfy the two tests above
    // while silently breaking every objective on the server. This is the test that catches that.
    final PlayerMock player = server.addPlayer("Steve");
    final AtomicInteger resolverCalls = new AtomicInteger();
    final AtomicInteger handlerCalls = new AtomicInteger();
    final ObjectiveType type = registerProbe(player, resolverCalls, handlerCalls);
    wireActiveObjective(player, type.createObjective());

    server.getPluginManager().callEvent(new ChildProbeEvent());

    assertEquals(1, resolverCalls.get(), "a genuine match must reach the player resolver");
    assertEquals(1, handlerCalls.get(), "a genuine match must reach the objective handler");
  }

  @Test
  @DisplayName("a global .listen binding drops parents and siblings but still receives its own event")
  void globalBindingFiltersToItsOwnEventType() {
    // The .listen(...) path runs its handler immediately, with no player or quest lookup in the
    // way, so this exercises the guard in globalEventExecutor end to end with nothing mocked out.
    final AtomicInteger calls = new AtomicInteger();
    new ObjectiveType.Builder(main, catalog, "GlobalProbe")
        .displayName("Global Probe")
        .description("Counts global dispatches so the guard can be observed.")
        .listen(ChildProbeEvent.class, event -> calls.incrementAndGet())
        .register()
        .registerEventListeners();

    assertDoesNotThrow(() -> server.getPluginManager().callEvent(new ParentProbeEvent()));
    assertEquals(0, calls.get(), "a parent instance must not reach a global handler");

    assertDoesNotThrow(() -> server.getPluginManager().callEvent(new SiblingProbeEvent()));
    assertEquals(0, calls.get(), "a sibling instance must not reach a global handler");

    server.getPluginManager().callEvent(new ChildProbeEvent());
    assertEquals(1, calls.get(), "a genuine match must still reach the global handler");
  }

  @Test
  @DisplayName("the Bukkit events from the bench run really do share a handler list")
  void bukkitEventsFromTheBenchRunShareHandlerLists() {
    // Locks in the premise the guard exists for. If a future Paper release gave any of these its
    // own getHandlerList(), this test would start failing and the guard could be revisited.
    assertThrows(NoSuchMethodException.class, () -> PlayerDeathEvent.class.getDeclaredMethod("getHandlerList"));
    assertThrows(NoSuchMethodException.class, () -> CraftItemEvent.class.getDeclaredMethod("getHandlerList"));
    assertThrows(NoSuchMethodException.class, () -> SmithItemEvent.class.getDeclaredMethod("getHandlerList"));
    assertThrows(NoSuchMethodException.class, () -> InventoryCreativeEvent.class.getDeclaredMethod("getHandlerList"));

    // Positive control for the same probe: the parents this walks up to do declare one.
    assertDoesNotThrow(() -> EntityDeathEvent.class.getDeclaredMethod("getHandlerList"));
    assertDoesNotThrow(() -> InventoryClickEvent.class.getDeclaredMethod("getHandlerList"));
  }

  // ---------------------------------------------------------------------------------- helpers

  private ObjectiveType registerProbe(
      final Player resolved, final AtomicInteger resolverCalls, final AtomicInteger handlerCalls) {
    final ObjectiveType type =
        new ObjectiveType.Builder(main, catalog, "DispatchProbe")
            .displayName("Dispatch Probe")
            .description("Records dispatches so the cast guard can be observed.")
            .on(
                ChildProbeEvent.class,
                event -> {
                  resolverCalls.incrementAndGet();
                  return resolved;
                },
                (event, objective) -> handlerCalls.incrementAndGet())
            .register();
    type.registerEventListeners();
    return type;
  }

  /**
   * Stands up the minimum QuestPlayer machinery {@code handleEvent} walks through after the cast:
   * an active quest player holding one active objective whose definition is the type under test.
   */
  private void wireActiveObjective(final Player player, final DefinedObjective objective) {
    final QuestPlayer questPlayer = mock(QuestPlayer.class);
    when(main.getQuestPlayerManager().getActiveQuestPlayer(player.getUniqueId())).thenReturn(questPlayer);
    when(questPlayer.getActiveQuests())
        .thenReturn(new CopyOnWriteArrayList<>(List.of(mock(ActiveQuest.class))));

    final ActiveObjective activeObjective = mock(ActiveObjective.class);
    when(activeObjective.getObjective()).thenReturn(objective);

    final List<Consumer<ActiveObjective>> queued = new ArrayList<>();
    doAnswer(
            invocation -> {
              queued.add(invocation.getArgument(0));
              return null;
            })
        .when(questPlayer)
        .queueObjectiveCheck(any());
    doAnswer(
            invocation -> {
              queued.forEach(check -> check.accept(activeObjective));
              queued.clear();
              return null;
            })
        .when(questPlayer)
        .checkQueuedObjectives();
  }
}
