/*
 * NotQuests - A Questing plugin for Minecraft Servers
 * Copyright (C) 2022 Alessio Gravili
 *
 * Licensed under the GNU General Public License v3. See the LICENSE file.
 */

package rocks.gravili.notquests.paper.structs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;
import net.kyori.adventure.bossbar.BossBar;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.world.WorldMock;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.managers.Configuration;

class QuestPlayerBeaconTest {
    private ServerMock server;
    private WorldMock world;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        world = server.addSimpleWorld("world");
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void trackedBeaconTargetIsClonedBeforeStoring() {
        final QuestPlayer questPlayer =
                new QuestPlayer(mock(NotQuests.class, RETURNS_DEEP_STUBS), UUID.randomUUID(), "default");
        final Location target = new Location(world, 10, 64, 10);

        questPlayer.trackBeacon("objective-1", target);
        target.add(5, 2, -3);

        final Location stored = questPlayer.getLocationsAndBeacons().get("objective-1");
        assertEquals(10, stored.getBlockX());
        assertEquals(64, stored.getBlockY());
        assertEquals(10, stored.getBlockZ());
    }

    @Test
    void beaconCleanupDoesNotMutateTheMarkerLocation() {
        final NotQuests main = mock(NotQuests.class, RETURNS_DEEP_STUBS);
        final Configuration configuration = mock(Configuration.class);
        when(main.getConfiguration()).thenReturn(configuration);
        when(configuration.getBeamMode()).thenReturn("beacon");
        final QuestPlayer questPlayer = new QuestPlayer(main, UUID.randomUUID(), "default");
        final Player player = mock(Player.class);
        final Location marker = new Location(world, 10, 64, 10);

        questPlayer.scheduleBeaconRemovalAt(marker, player);

        assertEquals(10, marker.getBlockX());
        assertEquals(64, marker.getBlockY());
        assertEquals(10, marker.getBlockZ());
    }

    @Test
    void sameBlockLocationComparesWorldAndBlockCoordinates() {
        final Location first = new Location(world, 1.1, 64.9, 3.2);
        final Location sameBlock = new Location(world, 1.8, 64.1, 3.9);
        final Location differentBlock = new Location(world, 2.0, 64.1, 3.9);
        final Location differentWorld = new Location(server.addSimpleWorld("other"), 1.1, 64.9, 3.2);

        assertTrue(QuestPlayer.sameBlockLocation(first, sameBlock));
        assertFalse(QuestPlayer.sameBlockLocation(first, differentBlock));
        assertFalse(QuestPlayer.sameBlockLocation(first, differentWorld));
    }

    @Test
    void endGatewayBeamRendersBelowSolidTargetBlock() {
        world.getBlockAt(10, 64, 10).setType(Material.CHEST);

        final Location renderLocation = QuestPlayer.beamRenderLocation(new Location(world, 10, 64, 10));

        assertEquals(10, renderLocation.getBlockX());
        assertEquals(63, renderLocation.getBlockY());
        assertEquals(10, renderLocation.getBlockZ());
    }

    @Test
    void solidTargetAtWorldFloorUsesTargetBlock() {
        final int minY = world.getMinHeight();
        world.getBlockAt(10, minY, 10).setType(Material.CHEST);

        final Location renderLocation = QuestPlayer.beamRenderLocation(new Location(world, 10, minY, 10));

        assertEquals(10, renderLocation.getBlockX());
        assertEquals(minY, renderLocation.getBlockY());
        assertEquals(10, renderLocation.getBlockZ());
    }

    @Test
    void unchangedBeamLocationIsResetAndResentToKeepClientFakeBlockAlive() {
        final NotQuests main = mock(NotQuests.class, RETURNS_DEEP_STUBS);
        final Configuration configuration = mock(Configuration.class);
        when(main.getConfiguration()).thenReturn(configuration);
        when(configuration.getBeamMode()).thenReturn("end_gateway");
        final QuestPlayer questPlayer = new QuestPlayer(main, UUID.randomUUID(), "default");
        final Player player = mock(Player.class);
        when(player.getWorld()).thenReturn(world);
        when(player.getLocation()).thenReturn(new Location(world, 11, 64, 10));
        world.getBlockAt(10, 64, 10).setType(Material.CHEST);
        questPlayer.getLocationsAndBeacons().put("objective-1", new Location(world, 10, 64, 10));

        questPlayer.updateBeaconLocations(player, true);
        questPlayer.updateBeaconLocations(player);

        verify(player, times(3)).sendBlockChange(eq(new Location(world, 10, 63, 10)), any(BlockData.class));
    }

    @Test
    void beamUsesTargetBlockWhenTargetBlockIsAlreadyAir() {
        final Location renderLocation = QuestPlayer.beamRenderLocation(new Location(world, 10, 64, 10));

        assertEquals(10, renderLocation.getBlockX());
        assertEquals(64, renderLocation.getBlockY());
        assertEquals(10, renderLocation.getBlockZ());
    }

    @Test
    void compassHelpersPointTowardTheMarker() {
        assertEquals(0.0, QuestPlayer.yawTo(new Location(world, 0, 64, 0), new Location(world, 0, 64, 10)));
        assertEquals(-90.0, QuestPlayer.yawTo(new Location(world, 0, 64, 0), new Location(world, 10, 64, 0)));
        assertEquals(-170.0, QuestPlayer.wrappedDegrees(190.0));

        assertEquals("^", QuestPlayer.compassDirection(0.0));
        assertEquals("<", QuestPlayer.compassDirection(-45.0));
        assertEquals(">>", QuestPlayer.compassDirection(120.0));
        assertEquals("behind", QuestPlayer.compassDirection(179.0));
        assertEquals(1.0f, QuestPlayer.compassProgress(0.0));
        assertEquals(0.0f, QuestPlayer.compassProgress(180.0));
        assertEquals(BossBar.Color.GREEN, QuestPlayer.compassColor(10.0));
        assertEquals(BossBar.Color.YELLOW, QuestPlayer.compassColor(60.0));
        assertEquals(BossBar.Color.RED, QuestPlayer.compassColor(120.0));
    }
}
