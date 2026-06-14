/*
 * NotQuests - A Questing plugin for Minecraft Servers
 * Copyright (C) 2022 Alessio Gravili
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package rocks.gravili.notquests.paper.structs.variables.hooks;

import org.betonquest.betonquest.api.QuestException;
import org.bukkit.command.CommandSender;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.commands.arguments.variables.StringVariableValueParser;
import rocks.gravili.notquests.paper.managers.integrations.betonquest.BetonQuestManager;
import rocks.gravili.notquests.paper.structs.QuestPlayer;
import rocks.gravili.notquests.paper.structs.variables.Variable;

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
