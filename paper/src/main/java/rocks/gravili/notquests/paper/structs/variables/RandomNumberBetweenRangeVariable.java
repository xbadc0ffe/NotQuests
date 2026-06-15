package rocks.gravili.notquests.paper.structs.variables;

import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.commands.arguments.variables.NumberVariableValueParser;
import rocks.gravili.notquests.paper.structs.QuestPlayer;

import java.util.List;
import java.util.Random;

public class RandomNumberBetweenRangeVariable extends Variable<Integer> {
  public RandomNumberBetweenRangeVariable(NotQuests main) {
    super(main);
    addRequiredNumber(NumberVariableValueParser.of("min", null, null));
    addRequiredNumber(NumberVariableValueParser.of("max", null, null));
  }

  @Override
  public Integer getValueInternally(QuestPlayer questPlayer, Object... objects) {
    final Random r = new Random();

    int min = (int) Math.round(getRequiredNumberValue("min", questPlayer));

    int max = (int) Math.round(getRequiredNumberValue("max", questPlayer));

    return (min == max) ? min : r.nextInt(max + 1 - min) + min;
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
    return "Random numbers";
  }

  @Override
  public String getSingular() {
    return "Random number";
  }
}
