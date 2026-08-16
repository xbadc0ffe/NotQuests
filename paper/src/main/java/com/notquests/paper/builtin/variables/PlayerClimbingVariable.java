package com.notquests.paper.builtin.variables;

import com.notquests.paper.variables.Variable;
import com.notquests.paper.variables.VariableDataType;

import com.notquests.paper.NotQuests;
import com.notquests.paper.structs.QuestPlayer;

import java.util.List;

public class PlayerClimbingVariable extends Variable<Boolean> {
  public PlayerClimbingVariable(NotQuests main) {
    super(main);
    setCanSetValue(false);
  }

  @Override
  public Boolean getValueInternally(QuestPlayer questPlayer, Object... objects) {
    if (questPlayer != null) {
      return questPlayer.getPlayer().isClimbing();
    } else {
      return false;
    }
  }

  @Override
  public boolean setValueInternally(Boolean newValue, QuestPlayer questPlayer, Object... objects) {
    return false;
  }

  @Override
  public List<String> getPossibleValues(QuestPlayer questPlayer, Object... objects) {
    return null;
  }

  @Override
  public String getPlural() {
    return "Climbing";
  }

  @Override
  public String getSingular() {
    return "Climbing";
  }
}
