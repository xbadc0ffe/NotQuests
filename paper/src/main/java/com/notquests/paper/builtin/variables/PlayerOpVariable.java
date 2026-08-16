package com.notquests.paper.builtin.variables;

import com.notquests.paper.variables.Variable;
import com.notquests.paper.variables.VariableDataType;

import com.notquests.paper.NotQuests;
import com.notquests.paper.structs.QuestPlayer;

import java.util.List;

public class PlayerOpVariable extends Variable<Boolean> {
  public PlayerOpVariable(NotQuests main) {
    super(main);
    setCanSetValue(true);
  }

  @Override
  public Boolean getValueInternally(QuestPlayer questPlayer, Object... objects) {
    if (questPlayer != null) {
      return questPlayer.getPlayer().isOp();
    } else {
      return false;
    }
  }

  @Override
  public boolean setValueInternally(Boolean newValue, QuestPlayer questPlayer, Object... objects) {
    if (questPlayer != null) {
      questPlayer.getPlayer().setOp(newValue);
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
    return "Op";
  }

  @Override
  public String getSingular() {
    return "Op";
  }
}
