package com.notquests.paper.builtin.variables;

import com.notquests.paper.variables.Variable;
import com.notquests.paper.variables.VariableDataType;

import org.bukkit.Bukkit;
import org.bukkit.World;
import com.notquests.paper.NotQuests;
import com.notquests.paper.structs.QuestPlayer;

import java.util.List;

public class PlayerCurrentWorldVariable extends Variable<String> {
  public PlayerCurrentWorldVariable(NotQuests main) {
    super(main);
    setCanSetValue(true);
  }

  @Override
  public String getValueInternally(QuestPlayer questPlayer, Object... objects) {
    if (questPlayer != null) {
      return questPlayer.getPlayer().getWorld().getName();
    } else {
      return null;
    }
  }

  @Override
  public boolean setValueInternally(String newValue, QuestPlayer questPlayer, Object... objects) {
    if (questPlayer != null) {
      final World world = Bukkit.getWorld(newValue);
      if (world == null) {
        return false;
      }
      questPlayer.getPlayer().teleport(world.getSpawnLocation());
      return true;
    } else {
      return false;
    }
  }

  @Override
  public List<String> getPossibleValues(QuestPlayer questPlayer, Object... objects) {
    return Bukkit.getWorlds().stream().map(world -> world.getName()).toList();
  }

  @Override
  public String getPlural() {
    return "Worlds";
  }

  @Override
  public String getSingular() {
    return "World";
  }
}
