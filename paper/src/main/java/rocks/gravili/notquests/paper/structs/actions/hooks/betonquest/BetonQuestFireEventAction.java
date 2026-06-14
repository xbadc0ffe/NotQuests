/*
 * NotQuests - A Questing plugin for Minecraft Servers
 * Copyright (C) 2021-2022 Alessio Gravili
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

package rocks.gravili.notquests.paper.structs.actions.hooks.betonquest;

import org.betonquest.betonquest.api.QuestException;
import org.bukkit.configuration.file.FileConfiguration;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.commands.framework.NQArguments;
import rocks.gravili.notquests.paper.commands.framework.NQCommandBuilder;
import rocks.gravili.notquests.paper.commands.framework.NQCommandManager;
import rocks.gravili.notquests.paper.commands.framework.NQDescription;
import rocks.gravili.notquests.paper.managers.integrations.betonquest.BetonQuestManager;
import rocks.gravili.notquests.paper.structs.QuestPlayer;
import rocks.gravili.notquests.paper.structs.actions.Action;
import rocks.gravili.notquests.paper.structs.actions.ActionFor;

import java.util.ArrayList;

public class BetonQuestFireEventAction extends Action {
  private String packageName = "";
  private String actionName = "";

  public BetonQuestFireEventAction(final NotQuests main) {
    super(main);
  }

  public static void handleCommands(
      final NotQuests main,
      final NQCommandManager manager,
      final NQCommandBuilder builder,
      final ActionFor actionFor) {
    manager.command(
        builder
            .required(
                "package",
                NQArguments.stringArgument(),
                NQDescription.of("BetonQuest package which contains the action to run."),
                (context, input) -> betonQuestManager(main).packageNames())
            .required(
                "action",
                NQArguments.stringArgument(),
                NQDescription.of(
                    "BetonQuest action name to run. This was called an event in older BetonQuest versions."),
                (context, input) -> betonQuestManager(main).actionNames(context.get("package")))
            .handler(
                context -> {
                  final BetonQuestFireEventAction action = new BetonQuestFireEventAction(main);
                  action.setPackageName(context.get("package"));
                  action.setBetonQuestActionName(context.get("action"));
                  main.getActionManager().addAction(action, context, actionFor);
                }));
  }

  private static BetonQuestManager betonQuestManager(final NotQuests main) {
    return main.getIntegrationsManager().getBetonQuestManager();
  }

  public String getPackageName() {
    return packageName;
  }

  public void setPackageName(final String packageName) {
    this.packageName = packageName == null ? "" : packageName;
  }

  public String getBetonQuestActionName() {
    return actionName;
  }

  public void setBetonQuestActionName(final String actionName) {
    this.actionName = actionName == null ? "" : actionName;
  }

  @Override
  protected void executeInternally(final QuestPlayer questPlayer, final Object... objects) {
    try {
      betonQuestManager(main).runAction(questPlayer, getPackageName(), getBetonQuestActionName());
    } catch (final QuestException exception) {
      main.getLogManager()
          .warn(
              "Tried to execute BetonQuestFireEvent action, but BetonQuest could not run "
                  + getPackageName()
                  + "."
                  + getBetonQuestActionName()
                  + ": "
                  + exception.getMessage());
    }
  }

  @Override
  public void save(final FileConfiguration configuration, final String initialPath) {
    configuration.set(initialPath + ".specifics.packageName", getPackageName());
    configuration.set(initialPath + ".specifics.eventName", getBetonQuestActionName());
  }

  @Override
  public void load(final FileConfiguration configuration, final String initialPath) {
    setPackageName(configuration.getString(initialPath + ".specifics.packageName"));
    setBetonQuestActionName(configuration.getString(initialPath + ".specifics.eventName"));
  }

  @Override
  public void deserializeFromSingleLineString(final ArrayList<String> arguments) {
    if (arguments.size() >= 2) {
      setPackageName(arguments.get(0));
      setBetonQuestActionName(arguments.get(1));
    }
  }

  @Override
  public String getActionDescription(final QuestPlayer questPlayer, final Object... objects) {
    return "Executes BetonQuest action: " + getPackageName() + "." + getBetonQuestActionName();
  }
}
