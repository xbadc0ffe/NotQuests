package com.notquests.paper.builtin.variables;

import com.notquests.paper.variables.Variable;
import com.notquests.paper.variables.VariableDataType;

import com.notquests.paper.NotQuests;
import com.notquests.paper.commands.arguments.variables.NumberVariableValueParser;
import com.notquests.paper.structs.QuestPlayer;

import java.util.List;

public class ChanceVariable extends Variable<Boolean> {

  public ChanceVariable(NotQuests main) {
    super(main);

    addRequiredNumber(NumberVariableValueParser.of("chance", null));
  }

  @Override
  public Boolean getValueInternally(QuestPlayer questPlayer, Object... objects) {
    double chanceToHave = getRequiredNumberValue("chance", questPlayer);

    double random = Math.random() * 100;
    return random < chanceToHave;
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
    return "Chances";
  }

  @Override
  public String getSingular() {
    return "Chance";
  }
}
