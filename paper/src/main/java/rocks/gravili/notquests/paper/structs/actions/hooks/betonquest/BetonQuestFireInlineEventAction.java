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
import java.util.List;

public class BetonQuestFireInlineEventAction extends Action {
  private String actionInstruction = "";

  public BetonQuestFireInlineEventAction(final NotQuests main) {
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
                "action",
                NQArguments.greedyStringArgument(),
                NQDescription.of(
                    "Inline BetonQuest action instruction to run. This was called an inline event in older BetonQuest versions."),
                (context, input) -> input.contains(" ") ? List.of() : betonQuestManager(main).actionTypes())
            .handler(
                context -> {
                  final BetonQuestFireInlineEventAction action =
                      new BetonQuestFireInlineEventAction(main);
                  action.setActionInstruction(context.get("action"));
                  main.getActionManager().addAction(action, context, actionFor);
                }));
  }

  private static BetonQuestManager betonQuestManager(final NotQuests main) {
    return main.getIntegrationsManager().getBetonQuestManager();
  }

  public String getActionInstruction() {
    return actionInstruction;
  }

  public void setActionInstruction(final String actionInstruction) {
    this.actionInstruction = actionInstruction == null ? "" : actionInstruction;
  }

  @Override
  protected void executeInternally(final QuestPlayer questPlayer, final Object... objects) {
    try {
      betonQuestManager(main).runInlineAction(questPlayer, getActionInstruction());
    } catch (final QuestException exception) {
      main.getLogManager()
          .warn(
              "Tried to execute BetonQuestFireInlineEvent action, but BetonQuest could not run '"
                  + getActionInstruction()
                  + "': "
                  + exception.getMessage());
    }
  }

  @Override
  public void save(final FileConfiguration configuration, final String initialPath) {
    configuration.set(initialPath + ".specifics.event", getActionInstruction());
  }

  @Override
  public void load(final FileConfiguration configuration, final String initialPath) {
    setActionInstruction(configuration.getString(initialPath + ".specifics.event"));
  }

  @Override
  public void deserializeFromSingleLineString(final ArrayList<String> arguments) {
    setActionInstruction(String.join(" ", arguments));
  }

  @Override
  public String getActionDescription(final QuestPlayer questPlayer, final Object... objects) {
    return "Executes inline BetonQuest action: " + getActionInstruction();
  }
}
