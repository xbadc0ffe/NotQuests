package com.notquests.paper.builtin.variables;

import com.notquests.paper.variables.Variable;
import com.notquests.paper.variables.VariableDataType;

import org.bukkit.inventory.ItemStack;
import com.notquests.paper.NotQuests;
import com.notquests.paper.commands.framework.NQDescription;
import com.notquests.paper.commands.framework.NQFlag;
import com.notquests.paper.structs.QuestPlayer;

import java.util.HashMap;
import java.util.List;

public class InventoryVariable extends Variable<ItemStack[]> {
  public InventoryVariable(NotQuests main) {
    super(main);
    setCanSetValue(true);
    addRequiredBooleanFlag(
        NQFlag.presence(
            "skipItemIfInventoryFull",
            NQDescription.of("Does not drop the item if inventory full if flag set")));
  }

  @Override
  public ItemStack[] getValueInternally(QuestPlayer questPlayer, Object... objects) {
    return questPlayer.getPlayer().getInventory().getContents();
  }

  @Override
  public boolean setValueInternally(
      ItemStack[] newValue, QuestPlayer questPlayer, Object... objects) {
    if (getRequiredBooleanValue("add", questPlayer)) {

      HashMap<Integer, ItemStack> left = questPlayer.getPlayer().getInventory().addItem(newValue);
      if (!getRequiredBooleanValue("skipItemIfInventoryFull", questPlayer)) {
        for (ItemStack leftItemStack : left.values()) {
          questPlayer
              .getPlayer()
              .getWorld()
              .dropItem(questPlayer.getPlayer().getLocation(), leftItemStack);
        }
      }
    } else if (getRequiredBooleanValue("remove", questPlayer)) {

      questPlayer.getPlayer().getInventory().removeItemAnySlot(newValue);
    } else {
      questPlayer.getPlayer().getInventory().setContents(newValue);
    }
    return true;
  }

  @Override
  public List<String> getPossibleValues(QuestPlayer questPlayer, Object... objects) {
    return null;
  }

  @Override
  public String getPlural() {
    return "Inventory";
  }

  @Override
  public String getSingular() {
    return "Inventory";
  }
}
