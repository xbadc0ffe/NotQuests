package com.notquests.paper.builtin.variables;

import com.notquests.paper.variables.Variable;
import com.notquests.paper.variables.VariableDataType;

import org.bukkit.Statistic;
import com.notquests.paper.NotQuests;
import com.notquests.paper.structs.QuestPlayer;

import java.util.List;

public class PlayerPlaytimeTicksVariable extends Variable<Integer> {
  public PlayerPlaytimeTicksVariable(NotQuests main) {
    super(main);
    setCanSetValue(true);
  }

  @Override
  public Integer getValueInternally(QuestPlayer questPlayer, Object... objects) {
    if (questPlayer != null) {
      return questPlayer.getPlayer().getStatistic(Statistic.PLAY_ONE_MINUTE);
    } else {
      return null;
    }
  }

  @Override
  public boolean setValueInternally(Integer newValue, QuestPlayer questPlayer, Object... objects) {
    if (questPlayer != null) {
      questPlayer.getPlayer().setStatistic(Statistic.PLAY_ONE_MINUTE, newValue);
      return true;
    } else {
      return false;
    }
  }

  @Override
  public List<String> getPossibleValues(QuestPlayer questPlayer, Object... objects) {
    return null;
  }

  @Override
  public String getPlural() {
    return "Playtime in ticks";
  }

  @Override
  public String getSingular() {
    return "Playtime in tick";
  }
}
