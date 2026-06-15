package rocks.gravili.notquests.paper.structs.objectives;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.Test;
import rocks.gravili.notquests.paper.commands.arguments.wrappers.ItemStackSelection;

class BrewItemsObjectiveTest {
  @Test
  void countsOnlySelectedBrewedItems() {
    final ItemStackSelection selection = new ItemStackSelection(null);
    selection.addMaterial(Material.POTION);

    final BrewItemsObjective objective = new BrewItemsObjective(null);
    objective.setItemStackSelection(selection);

    assertTrue(objective.countsBrewedItem(new ItemStack(Material.POTION)));
    assertFalse(objective.countsBrewedItem(new ItemStack(Material.GLASS_BOTTLE)));
  }
}
