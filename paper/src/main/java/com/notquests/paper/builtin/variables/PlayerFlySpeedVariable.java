package com.notquests.paper.builtin.variables;

import com.notquests.paper.variables.Variable;
import com.notquests.paper.variables.VariableDataType;

import com.notquests.paper.NotQuests;
import com.notquests.paper.structs.QuestPlayer;

import java.util.List;

public class PlayerFlySpeedVariable extends Variable<Float> {
  public PlayerFlySpeedVariable(NotQuests main) {
    super(main);
    setCanSetValue(true);
  }

  @Override
  public Float getValueInternally(QuestPlayer questPlayer, Object... objects) {
    if (questPlayer != null) {
      return questPlayer.getPlayer().getFlySpeed();
    } else {
      return 0f;
    }
  }

  @Override
  public boolean setValueInternally(Float newValue, QuestPlayer questPlayer, Object... objects) {
    if (questPlayer != null) {
      questPlayer.getPlayer().setFlySpeed(newValue);
      return true;
    }
    return false;
  }

  @Override
  public List<String> getPossibleValues(QuestPlayer questPlayer, Object... objects) {
    return null;
  }

  @Override
  public String getPlural() {
    return "Fly speed";
  }

  @Override
  public String getSingular() {
    return "Fly speed";
  }
}
