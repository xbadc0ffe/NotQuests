package com.notquests.paper.events;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.notquests.core.NotQuestsPlugin;
import com.notquests.paper.NotQuests;
import com.notquests.paper.PaperNotQuestsAdapter;
import com.notquests.paper.PaperPlayer;
import org.bukkit.Location;
import org.bukkit.damage.DamageSource;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.IronGolem;
import org.bukkit.entity.Wolf;
import org.bukkit.entity.Zombie;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import org.mockbukkit.mockbukkit.world.WorldMock;

import com.google.common.base.Function;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * FORK DIVERGENCE: shared kill credit, Paper side. Every uncancelled player hit on a mob is
 * reported to core with the player behind it resolved; a mob death is reported once as the
 * vanilla kill and once as a shared kill carrying the death location.
 */
class ForkSharedKillCreditEventsTest {
    private ServerMock server;
    private WorldMock world;
    private NotQuests main;
    private NotQuestsPlugin core;
    private PaperNotQuestsAdapter adapter;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        world = server.addSimpleWorld("hunting-grounds");
        main = mock(NotQuests.class);
        core = mock(NotQuestsPlugin.class);
        adapter = mock(PaperNotQuestsAdapter.class);
        when(main.getCorePlugin()).thenReturn(core);
        when(main.getRegistryAdapter()).thenReturn(adapter);
        server.getPluginManager().registerEvents(new QuestEvents(main), MockBukkit.createMockPlugin());
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void playerHitsOnMobsAreRememberedUnlessCancelledOrAgainstPlayers() {
        final PlayerMock attacker = server.addPlayer();
        final PlayerMock victimPlayer = server.addPlayer();
        final PaperPlayer questPlayer = questPlayer(attacker);
        final Zombie zombie = world.spawn(new Location(world, 0, 64, 0), Zombie.class);

        server.getPluginManager().callEvent(hit(attacker, zombie));
        verify(core).entityDamagedByPlayer(zombie.getUniqueId().toString(), questPlayer);

        server.getPluginManager().callEvent(hit(attacker, victimPlayer));
        verify(core, never()).entityDamagedByPlayer(eq(victimPlayer.getUniqueId().toString()), any());

        final var plugin = MockBukkit.createMockPlugin("canceller");
        server.getPluginManager().registerEvent(EntityDamageByEntityEvent.class, new Listener() {}, EventPriority.LOW,
                (listener, event) -> ((EntityDamageByEntityEvent) event).setCancelled(true), plugin);
        final Zombie protectedZombie = world.spawn(new Location(world, 5, 64, 0), Zombie.class);
        server.getPluginManager().callEvent(hit(attacker, protectedZombie));
        verify(core, never()).entityDamagedByPlayer(eq(protectedZombie.getUniqueId().toString()), any());
    }

    @Test
    void mobDeathsReportTheVanillaKillAndTheSharedKillWithTheDeathLocation() {
        final PlayerMock killer = server.addPlayer();
        final PaperPlayer questPlayer = questPlayer(killer);
        final Zombie zombie = world.spawn(new Location(world, 3, 64, 7), Zombie.class);
        zombie.setKiller(killer);

        server.getPluginManager().callEvent(new EntityDeathEvent(zombie, mock(DamageSource.class), List.of()));

        verify(core).entityDied(isNull(), isNull(), eq(questPlayer), any());
        verify(core).entityDiedSharedCredit(eq(zombie.getUniqueId().toString()), eq(questPlayer), any(), any());
    }

    @Test
    void mobDeathsWithoutAKillerStillReportTheSharedKill() {
        final Zombie zombie = world.spawn(new Location(world, 3, 64, 7), Zombie.class);

        server.getPluginManager().callEvent(new EntityDeathEvent(zombie, mock(DamageSource.class), List.of()));

        verify(core).entityDied(isNull(), isNull(), isNull(), isNull());
        verify(core).entityDiedSharedCredit(eq(zombie.getUniqueId().toString()), isNull(), any(), any());
    }

    @Test
    void playerDeathsNeverReportSharedCredit() {
        final PlayerMock dead = server.addPlayer();
        final PaperPlayer deadQuestPlayer = questPlayer(dead);

        server.getPluginManager().callEvent(new EntityDeathEvent(dead, mock(DamageSource.class), List.of()));

        verify(core).entityDied(eq(deadQuestPlayer), any(), isNull(), isNull());
        verify(core, never()).entityDiedSharedCredit(any(), any(), any(), any());
    }

    @Test
    void attackingPlayerResolvesDirectHitsProjectilesAndTamedPetsOnly() {
        final PlayerMock player = server.addPlayer();
        final Arrow arrow = mock(Arrow.class);
        when(arrow.getShooter()).thenReturn(player);
        final Wolf wolf = mock(Wolf.class);
        when(wolf.isTamed()).thenReturn(true);
        when(wolf.getOwner()).thenReturn(player);
        final Wolf wildWolf = mock(Wolf.class);
        when(wildWolf.isTamed()).thenReturn(false);

        assertSame(player, QuestEvents.attackingPlayer(player));
        assertSame(player, QuestEvents.attackingPlayer(arrow));
        assertSame(player, QuestEvents.attackingPlayer(wolf));
        assertNull(QuestEvents.attackingPlayer(wildWolf));
        assertNull(QuestEvents.attackingPlayer(mock(IronGolem.class)));
        assertNull(QuestEvents.attackingPlayer(null));
    }

    private PaperPlayer questPlayer(final PlayerMock player) {
        final PaperPlayer questPlayer = mock(PaperPlayer.class);
        when(adapter.activePaperPlayer(player.getUniqueId())).thenReturn(questPlayer);
        return questPlayer;
    }

    private static EntityDamageByEntityEvent hit(final PlayerMock attacker, final org.bukkit.entity.Entity victim) {
        final Map<EntityDamageEvent.DamageModifier, Double> modifiers =
                new EnumMap<>(Map.of(EntityDamageEvent.DamageModifier.BASE, 2.0));
        final Map<EntityDamageEvent.DamageModifier, Function<? super Double, Double>> modifierFunctions =
                new EnumMap<>(Map.of(EntityDamageEvent.DamageModifier.BASE, damage -> -0.0));
        return new EntityDamageByEntityEvent(
                attacker,
                victim,
                EntityDamageEvent.DamageCause.ENTITY_ATTACK,
                mock(DamageSource.class),
                modifiers,
                modifierFunctions,
                false);
    }
}
