package com.notquests.paper.builtin.variables;

import com.notquests.paper.variables.Variable;
import com.notquests.paper.variables.VariableDataType;

import com.notquests.paper.NotQuests;
import com.notquests.paper.structs.QuestPlayer;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class DayOfWeekVariable extends Variable<String> {
  public DayOfWeekVariable(NotQuests main) {
    super(main);
  }

  @Override
  public String getValueInternally(QuestPlayer questPlayer, Object... objects) {
    DayOfWeek dayOfWeek = LocalDate.now().getDayOfWeek();

    return dayOfWeek.name().toLowerCase(Locale.ROOT);
  }

  @Override
  public boolean setValueInternally(String newValue, QuestPlayer questPlayer, Object... objects) {
    return false;
  }

  @Override
  public List<String> getPossibleValues(QuestPlayer questPlayer, Object... objects) {
    List<String> possibleValues = new ArrayList<>();
    for (DayOfWeek dayOfWeek : DayOfWeek.values()) {
      possibleValues.add(dayOfWeek.name().toLowerCase(Locale.ROOT));
    }
    return possibleValues;
  }

  @Override
  public String getPlural() {
    return "Day of Week";
  }

  @Override
  public String getSingular() {
    return "Day of Week";
  }
}
