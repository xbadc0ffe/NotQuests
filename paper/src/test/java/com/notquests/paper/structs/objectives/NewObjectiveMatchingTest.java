package com.notquests.paper.objectives;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.event.block.Action;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import com.notquests.paper.commands.arguments.wrappers.ItemStackSelection;
import com.notquests.paper.builtin.objectives.Die;
import com.notquests.paper.builtin.objectives.Interact;
import com.notquests.paper.builtin.objectives.SmithItems;
import com.notquests.paper.builtin.objectives.ShootArrow;
import com.notquests.paper.builtin.objectives.TameMobs;
import com.notquests.paper.builtin.objectives.TradeWithVillager;
import com.notquests.paper.objectives.support.ObjectiveRegion;

class NewObjectiveMatchingTest {
    @BeforeEach
    void setUp() {
        MockBukkit.mock();
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void tradeObjectiveCountsOnlySelectedTradeResults() {
        final ItemStackSelection selection = new ItemStackSelection(null);
        selection.addMaterial(Material.EMERALD);

        assertTrue(TradeWithVillager.countsTradeResult(selection, new ItemStack(Material.EMERALD)));
        assertFalse(TradeWithVillager.countsTradeResult(selection, new ItemStack(Material.DIAMOND)));
    }

    @Test
    void smithObjectiveCountsOnlySelectedSmithingResults() {
        final ItemStackSelection selection = new ItemStackSelection(null);
        selection.addMaterial(Material.NETHERITE_SWORD);

        assertTrue(SmithItems.countsSmithingResult(selection, new ItemStack(Material.NETHERITE_SWORD)));
        assertFalse(SmithItems.countsSmithingResult(selection, new ItemStack(Material.IRON_SWORD)));
    }

    @Test
    void tameObjectiveSupportsSpecificEntityOrAny() {
        assertTrue(TameMobs.countsEntityType("wolf", "WOLF"));
        assertFalse(TameMobs.countsEntityType("wolf", "CAT"));
        assertTrue(TameMobs.countsEntityType("any", "WOLF"));
        assertTrue(TameMobs.countsEntityType("any", "CAT"));
    }

    @Test
    void dieObjectiveSupportsOptionalDamageCause() {
        assertTrue(Die.countsDamageType("", "fall"));
        assertTrue(Die.countsDamageType("", "player_attack"));
        assertTrue(Die.countsDamageType("fall", "FALL"));
        assertFalse(Die.countsDamageType("fall", "lava"));
    }

    @Test
    void interactObjectiveCountsBothClicksWhenNoClickFlagWasConfigured() {
        assertTrue(Interact.countsInteractionAction(Action.RIGHT_CLICK_BLOCK, false, false));
        assertTrue(Interact.countsInteractionAction(Action.LEFT_CLICK_BLOCK, false, false));
        assertFalse(Interact.countsInteractionAction(Action.RIGHT_CLICK_AIR, false, false));
    }

    @Test
    void interactObjectiveRespectsExplicitClickFlags() {
        assertTrue(Interact.countsInteractionAction(Action.RIGHT_CLICK_BLOCK, false, true));
        assertFalse(Interact.countsInteractionAction(Action.LEFT_CLICK_BLOCK, false, true));
    }

    @Test
    void shootArrowObjectiveCountsOnlyArrowsInsideTargetRadiusAndWorld() {
        final World world = mock(World.class);
        final World otherWorld = mock(World.class);

        final Location target = new Location(world, 10, 64, -5);

        assertTrue(ShootArrow.countsArrowLocation(target, null, null, 3.0, new Location(world, 12, 64, -5)));
        assertTrue(ShootArrow.countsArrowLocation(target, null, null, 3.0, new Location(world, 13, 64, -5)));
        assertFalse(ShootArrow.countsArrowLocation(target, null, null, 3.0, new Location(world, 14, 64, -5)));
        assertFalse(ShootArrow.countsArrowLocation(target, null, null, 3.0, new Location(otherWorld, 10, 64, -5)));
    }

    @Test
    void shootArrowWorldEditRegionCountsOnlyArrowsInsideTheSelectedCuboid() {
        final World world = mock(World.class);
        final World otherWorld = mock(World.class);

        final ObjectiveRegion region = new ObjectiveRegion(
                new Location(world, 10, 64, -5),
                new Location(world, 12, 66, -3));

        assertTrue(ShootArrow.targetRegion(region.min(), region.max()) != null);
        assertTrue(ShootArrow.countsArrowLocation(null, region.min(), region.max(), null, new Location(world, 10, 64, -5)));
        assertTrue(ShootArrow.countsArrowLocation(null, region.min(), region.max(), null, new Location(world, 12, 66, -3)));
        assertFalse(ShootArrow.countsArrowLocation(null, region.min(), region.max(), null, new Location(world, 13, 66, -3)));
        assertFalse(ShootArrow.countsArrowLocation(null, region.min(), region.max(), null, new Location(otherWorld, 11, 65, -4)));
    }
}
