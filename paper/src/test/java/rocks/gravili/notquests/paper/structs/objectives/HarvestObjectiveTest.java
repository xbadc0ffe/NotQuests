/*
 * NotQuests - A Questing plugin for Minecraft Servers
 * Copyright (C) 2022 Alessio Gravili
 *
 * Licensed under the GNU General Public License v3. See the LICENSE file.
 */

package rocks.gravili.notquests.paper.structs.objectives;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.world.WorldMock;
import rocks.gravili.notquests.paper.commands.arguments.wrappers.ItemStackSelection;

class HarvestObjectiveTest {
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
  void ageableCropsOnlyCountAtMaximumAge() {
    final Block wheat = world.getBlockAt(0, 64, 0);
    wheat.setBlockData(ageable(Material.WHEAT, false));

    assertFalse(HarvestObjective.isFullyGrownHarvestable(wheat));

    wheat.setBlockData(ageable(Material.WHEAT, true));

    assertTrue(HarvestObjective.isFullyGrownHarvestable(wheat));
  }

  @Test
  void stemsDoNotCountAsHarvestedPumpkinsOrMelons() {
    final Block stem = world.getBlockAt(1, 64, 0);
    stem.setBlockData(ageable(Material.PUMPKIN_STEM, true));

    assertFalse(HarvestObjective.isFullyGrownHarvestable(stem));
  }

  @Test
  void melonAndPumpkinFruitCountAsHarvestable() {
    final Block melon = world.getBlockAt(2, 64, 0);
    melon.setType(Material.MELON);
    final Block pumpkin = world.getBlockAt(3, 64, 0);
    pumpkin.setType(Material.PUMPKIN);

    assertTrue(HarvestObjective.isFullyGrownHarvestable(melon));
    assertTrue(HarvestObjective.isFullyGrownHarvestable(pumpkin));
  }

  @Test
  void cactusAndSugarCaneCountOnlyAboveAnotherPlantSegment() {
    final Block sugarCaneBase = world.getBlockAt(4, 64, 0);
    sugarCaneBase.setType(Material.SUGAR_CANE);
    final Block sugarCaneTop = world.getBlockAt(4, 65, 0);
    sugarCaneTop.setType(Material.SUGAR_CANE);

    final Block cactusBase = world.getBlockAt(5, 64, 0);
    cactusBase.setType(Material.CACTUS);
    final Block cactusTop = world.getBlockAt(5, 65, 0);
    cactusTop.setType(Material.CACTUS);

    assertFalse(HarvestObjective.isFullyGrownHarvestable(sugarCaneBase));
    assertTrue(HarvestObjective.isFullyGrownHarvestable(sugarCaneTop));
    assertFalse(HarvestObjective.isFullyGrownHarvestable(cactusBase));
    assertTrue(HarvestObjective.isFullyGrownHarvestable(cactusTop));
  }

  @Test
  void directlyPlaceableHarvestBlocksAreTrackedAsPlayerPlaced() {
    final Block melon = world.getBlockAt(6, 64, 0);
    melon.setType(Material.MELON);
    final Block immatureWheat = world.getBlockAt(7, 64, 0);
    immatureWheat.setBlockData(ageable(Material.WHEAT, false));

    assertTrue(HarvestObjective.shouldTrackAsPlayerPlacedHarvestBlock(melon));
    assertFalse(HarvestObjective.shouldTrackAsPlayerPlacedHarvestBlock(immatureWheat));
  }

  @Test
  void cropItemNamesMatchTheirHarvestedCropBlocks() {
    final Block carrots = world.getBlockAt(8, 64, 0);
    carrots.setBlockData(ageable(Material.CARROTS, true));

    final ItemStackSelection selection = new ItemStackSelection(null);
    selection.addMaterial(Material.CARROT);
    final HarvestObjective objective = new HarvestObjective(null);
    objective.setItemStackSelection(selection);

    assertTrue(objective.countsHarvest(carrots, false));
  }

  private static Ageable ageable(final Material material, final boolean mature) {
    final Ageable ageable = (Ageable) Bukkit.createBlockData(material);
    ageable.setAge(mature ? ageable.getMaximumAge() : 0);
    return ageable;
  }
}
