package com.notquests.paper.objectives;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.Test;
import com.notquests.paper.commands.arguments.wrappers.ItemStackSelection;
import com.notquests.paper.builtin.objectives.BrewItems;

class BrewItemsObjectiveTest {
  @Test
  void countsOnlySelectedBrewedItems() {
    final ItemStackSelection selection = new ItemStackSelection(null);
    selection.addMaterial(Material.POTION);

    assertTrue(BrewItems.countsBrewedItem(selection, new ItemStack(Material.POTION)));
    assertFalse(BrewItems.countsBrewedItem(selection, new ItemStack(Material.GLASS_BOTTLE)));
  }
}
