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
import rocks.gravili.notquests.paper.structs.Quest;

public class BQStartQuestEvent implements PlayerActionFactory {
  private final NotQuests main;

  public BQStartQuestEvent(final NotQuests main) {
    this.main = main;
  }

  @Override
  public PlayerAction parsePlayer(final Instruction instruction) throws QuestException {
    final String questName = instruction.nextElement();
    final String flags = String.join(" ", instruction.getValueParts());
    final boolean forced = flags.contains("-force");
    final boolean silent = flags.contains("-silent");
    final boolean triggers = !flags.contains("-notriggers");
    return profile -> startQuest(profile, questName, forced, silent, triggers);
  }

  private void startQuest(
      final Profile profile,
      final String questName,
      final boolean forced,
      final boolean silent,
      final boolean triggers)
      throws QuestException {
    final Quest quest = main.getQuestManager().getQuest(questName);
    if (quest == null) {
      throw new QuestException("NotQuests quest '" + questName + "' does not exist.");
    }
    if (forced) {
      main.getQuestPlayerManager().forceAcceptQuestSilent(profile.getProfileUUID(), quest);
    } else {
      main.getQuestPlayerManager()
          .acceptQuest(
              main.getQuestPlayerManager().getOrCreateQuestPlayer(profile.getProfileUUID()),
              quest,
              triggers,
              !silent);
    }
  }
}
