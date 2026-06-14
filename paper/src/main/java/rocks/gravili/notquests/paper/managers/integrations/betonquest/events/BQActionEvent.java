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

package rocks.gravili.notquests.paper.managers.integrations.betonquest.events;

import org.betonquest.betonquest.api.QuestException;
import org.betonquest.betonquest.api.instruction.Instruction;
import org.betonquest.betonquest.api.profile.Profile;
import org.betonquest.betonquest.api.quest.action.PlayerAction;
import org.betonquest.betonquest.api.quest.action.PlayerActionFactory;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.managers.integrations.betonquest.BetonQuestInstructionUtil;
import rocks.gravili.notquests.paper.structs.actions.Action;

import java.util.List;

public class BQActionEvent implements PlayerActionFactory {
  private final NotQuests main;

  public BQActionEvent(final NotQuests main) {
    this.main = main;
  }

  @Override
  public PlayerAction parsePlayer(final Instruction instruction) {
    final String actionLine = BetonQuestInstructionUtil.valueLine(instruction);
    return profile -> executeAction(profile, actionLine);
  }

  private void executeAction(final Profile profile, final String actionLine) throws QuestException {
    try {
      final Action action = main.getConversationManager().parseActionString(List.of(actionLine)).get(0);
      if (action == null) {
        throw new QuestException("NotQuests action line could not be parsed: " + actionLine);
      }
      action.execute(main.getQuestPlayerManager().getOrCreateQuestPlayer(profile.getProfileUUID()));
    } catch (final QuestException exception) {
      throw exception;
    } catch (final Exception exception) {
      throw new QuestException("Invalid NotQuests action line '" + actionLine + "': " + exception.getMessage(), exception);
    }
  }
}
