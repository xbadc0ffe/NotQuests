package com.notquests.paper.builtin.variables.hooks;

import org.betonquest.betonquest.api.QuestException;
import org.bukkit.command.CommandSender;
import com.notquests.paper.NotQuests;
import com.notquests.paper.commands.arguments.variables.StringVariableValueParser;
import com.notquests.paper.managers.integrations.betonquest.BetonQuestManager;
import com.notquests.paper.structs.QuestPlayer;
import com.notquests.paper.variables.Variable;

import java.util.List;

public class BetonQuestConditionVariable extends Variable<Boolean> {
  public BetonQuestConditionVariable(final NotQuests main) {
    super(main);

    addRequiredString(
        StringVariableValueParser.<CommandSender>of(
            "package",
            null,
            (context, input) -> betonQuestManager().packageNames()));
    addRequiredString(
        StringVariableValueParser.<CommandSender>of(
            "condition",
            null,
            (context, input) -> betonQuestManager().conditionNames(context.get("package"))));
  }

  private BetonQuestManager betonQuestManager() {
    return main.getIntegrationsManager().getBetonQuestManager();
  }

  @Override
  public Boolean getValueInternally(final QuestPlayer questPlayer, final Object... objects) {
    try {
      return betonQuestManager()
          .testCondition(
              questPlayer,
              getRequiredStringValue("package"),
              getRequiredStringValue("condition"));
    } catch (final QuestException exception) {
      main.getLogManager()
          .warn(
              "Tried to check BetonQuestCondition variable, but BetonQuest could not test "
                  + getRequiredStringValue("package")
                  + "."
                  + getRequiredStringValue("condition")
                  + ": "
                  + exception.getMessage());
      return false;
    }
  }

  @Override
  public boolean setValueInternally(
      final Boolean newValue, final QuestPlayer questPlayer, final Object... objects) {
    return false;
  }

  @Override
  public List<String> getPossibleValues(final QuestPlayer questPlayer, final Object... objects) {
    return List.of("true", "false");
  }

  @Override
  public String getPlural() {
    final String together = getRequiredStringValue("package") + "." + getRequiredStringValue("condition");
    return together.equals(".") ? "BetonQuest Conditions" : "BetonQuest " + together + " Conditions";
  }

  @Override
  public String getSingular() {
    final String together = getRequiredStringValue("package") + "." + getRequiredStringValue("condition");
    return together.equals(".") ? "BetonQuest Condition" : "BetonQuest " + together + " Condition";
  }
}
