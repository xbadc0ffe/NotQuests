package rocks.gravili.notquests.paper.structs.objectives;

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
import rocks.gravili.notquests.paper.commands.arguments.wrappers.ItemStackSelection;

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

        final TradeWithVillagerObjective objective = new TradeWithVillagerObjective(null);
        objective.setItemStackSelection(selection);

        assertTrue(objective.countsTradeResult(new ItemStack(Material.EMERALD)));
        assertFalse(objective.countsTradeResult(new ItemStack(Material.DIAMOND)));
    }

    @Test
    void smithObjectiveCountsOnlySelectedSmithingResults() {
        final ItemStackSelection selection = new ItemStackSelection(null);
        selection.addMaterial(Material.NETHERITE_SWORD);

        final SmithItemsObjective objective = new SmithItemsObjective(null);
        objective.setItemStackSelection(selection);

        assertTrue(objective.countsSmithingResult(new ItemStack(Material.NETHERITE_SWORD)));
        assertFalse(objective.countsSmithingResult(new ItemStack(Material.IRON_SWORD)));
    }

    @Test
    void tameObjectiveSupportsSpecificEntityOrAny() {
        final TameMobsObjective wolfObjective = new TameMobsObjective(null);
        wolfObjective.setEntityToTameType("wolf");

        assertTrue(wolfObjective.countsEntityType("WOLF"));
        assertFalse(wolfObjective.countsEntityType("CAT"));

        final TameMobsObjective anyObjective = new TameMobsObjective(null);
        anyObjective.setEntityToTameType("any");
        assertTrue(anyObjective.countsEntityType("WOLF"));
        assertTrue(anyObjective.countsEntityType("CAT"));
    }

    @Test
    void dieObjectiveSupportsOptionalDamageCause() {
        final DieObjective anyDeathObjective = new DieObjective(null);
        assertTrue(anyDeathObjective.countsDamageType("fall"));
        assertTrue(anyDeathObjective.countsDamageType("player_attack"));

        final DieObjective fallDeathObjective = new DieObjective(null);
        fallDeathObjective.setDamageType("fall");
        assertTrue(fallDeathObjective.countsDamageType("FALL"));
        assertFalse(fallDeathObjective.countsDamageType("lava"));
    }

    @Test
    void interactObjectiveCountsBothClicksWhenNoClickFlagWasConfigured() {
        final InteractObjective objective = new InteractObjective(null);

        assertTrue(objective.countsInteractionAction(Action.RIGHT_CLICK_BLOCK));
        assertTrue(objective.countsInteractionAction(Action.LEFT_CLICK_BLOCK));
        assertFalse(objective.countsInteractionAction(Action.RIGHT_CLICK_AIR));
    }

    @Test
    void interactObjectiveRespectsExplicitClickFlags() {
        final InteractObjective objective = new InteractObjective(null);
        objective.setRightClick(true);

        assertTrue(objective.countsInteractionAction(Action.RIGHT_CLICK_BLOCK));
        assertFalse(objective.countsInteractionAction(Action.LEFT_CLICK_BLOCK));
    }

    @Test
    void shootArrowObjectiveCountsOnlyArrowsInsideTargetRadiusAndWorld() {
        final World world = mock(World.class);
        final World otherWorld = mock(World.class);

        final ShootArrowObjective objective = new ShootArrowObjective(null);
        objective.setTargetLocation(new Location(world, 10, 64, -5));
        objective.setRadius(3);

        assertTrue(objective.countsArrowLocation(new Location(world, 12, 64, -5)));
        assertTrue(objective.countsArrowLocation(new Location(world, 13, 64, -5)));
        assertFalse(objective.countsArrowLocation(new Location(world, 14, 64, -5)));
        assertFalse(objective.countsArrowLocation(new Location(otherWorld, 10, 64, -5)));
    }

    @Test
    void shootArrowWorldEditRegionCountsOnlyArrowsInsideTheSelectedCuboid() {
        final World world = mock(World.class);
        final World otherWorld = mock(World.class);

        final ShootArrowObjective objective = new ShootArrowObjective(null);
        objective.setTargetRegion(new ObjectiveRegion(
                new Location(world, 10, 64, -5),
                new Location(world, 12, 66, -3)));

        assertTrue(objective.hasTargetRegion());
        assertTrue(objective.countsArrowLocation(new Location(world, 10, 64, -5)));
        assertTrue(objective.countsArrowLocation(new Location(world, 12, 66, -3)));
        assertFalse(objective.countsArrowLocation(new Location(world, 13, 66, -3)));
        assertFalse(objective.countsArrowLocation(new Location(otherWorld, 11, 65, -4)));
    }
}
