package com.notquests.paper.builtin.variables.hooks;

import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import com.notquests.paper.NotQuests;
import com.notquests.paper.structs.QuestPlayer;
import com.notquests.paper.variables.Variable;

import java.util.List;

public class TownyTownPlotCountVariable extends Variable<Integer> {
  public TownyTownPlotCountVariable(NotQuests main) {
    super(main);
  }

  @Override
  public Integer getValueInternally(QuestPlayer questPlayer, Object... objects) {
    if (!main.getIntegrationsManager().isTownyEnabled()) {
      return 0;
    }
    if (questPlayer != null) {
      Resident resident = TownyUniverse.getInstance().getResident(questPlayer.getUniqueId());
      if (resident != null && resident.getTownOrNull() != null && resident.hasTown()) {
        Town town = resident.getTownOrNull();
        return town.getPlotGroups().size();
      } else {
        return 0;
      }

    } else {
      return 0;
    }
  }

  @Override
  public boolean setValueInternally(Integer newValue, QuestPlayer questPlayer, Object... objects) {
    return false;
  }

  @Override
  public List<String> getPossibleValues(QuestPlayer questPlayer, Object... objects) {
    return null;
  }

  @Override
  public String getPlural() {
    return "Plots in Town";
  }

  @Override
  public String getSingular() {
    return "Plot in Town";
  }
}
