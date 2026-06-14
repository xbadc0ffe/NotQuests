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
import rocks.gravili.notquests.paper.structs.QuestPlayer;

import java.util.Locale;

public class BQQuestPointsEvent implements PlayerActionFactory {
  private final NotQuests main;

  public BQQuestPointsEvent(final NotQuests main) {
    this.main = main;
  }

  @Override
  public PlayerAction parsePlayer(final Instruction instruction) throws QuestException {
    final String action = instruction.nextElement().toLowerCase(Locale.ROOT);
    final long amount;
    try {
      amount = Long.parseLong(instruction.nextElement());
    } catch (final NumberFormatException exception) {
      throw new QuestException("Invalid NotQuests quest-points amount.", exception);
    }
    if (!action.equals("set") && !action.equals("add") && !action.equals("remove")) {
      throw new QuestException("Quest-points action must be set, add, or remove.");
    }
    final boolean silent = String.join(" ", instruction.getValueParts()).contains("-silent");
    return profile -> updateQuestPoints(profile, action, amount, silent);
  }

  private void updateQuestPoints(
      final Profile profile, final String action, final long amount, final boolean silent) {
    final QuestPlayer questPlayer =
        main.getQuestPlayerManager().getActiveQuestPlayer(profile.getProfileUUID());
    if (questPlayer == null) {
      return;
    }
    switch (action) {
      case "set" -> questPlayer.setQuestPoints(amount, !silent);
      case "add" -> questPlayer.addQuestPoints(amount, !silent);
      case "remove" -> questPlayer.removeQuestPoints(amount, !silent);
      default -> {
        // Validated while parsing.
      }
    }
  }
}
