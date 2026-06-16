package com.notquests.paper.builtin.variables;

import com.notquests.paper.variables.Variable;
import com.notquests.paper.variables.VariableDataType;

import com.notquests.paper.NotQuests;
import com.notquests.paper.structs.QuestPlayer;

import java.util.List;

// Useful for some conditions
public class FalseVariable extends Variable<Boolean> {
  public FalseVariable(NotQuests main) {
    super(main);
  }

  @Override
  public Boolean getValueInternally(QuestPlayer questPlayer, Object... objects) {
    return false;
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
    return "False";
  }

  @Override
  public String getSingular() {
    return "False";
  }
}
