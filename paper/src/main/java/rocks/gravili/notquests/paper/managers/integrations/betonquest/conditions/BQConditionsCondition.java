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

package rocks.gravili.notquests.paper.managers.integrations.betonquest.conditions;

import org.betonquest.betonquest.api.QuestException;
import org.betonquest.betonquest.api.instruction.Instruction;
import org.betonquest.betonquest.api.profile.Profile;
import org.betonquest.betonquest.api.quest.condition.PlayerCondition;
import org.betonquest.betonquest.api.quest.condition.PlayerConditionFactory;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.managers.integrations.betonquest.BetonQuestInstructionUtil;
import rocks.gravili.notquests.paper.structs.conditions.Condition;

import java.util.List;

public class BQConditionsCondition implements PlayerConditionFactory {
  private final NotQuests main;

  public BQConditionsCondition(final NotQuests main) {
    this.main = main;
  }

  @Override
  public PlayerCondition parsePlayer(final Instruction instruction) {
    final String conditionLine = BetonQuestInstructionUtil.valueLine(instruction);
    return profile -> check(profile, conditionLine);
  }

  private boolean check(final Profile profile, final String conditionLine) throws QuestException {
    try {
      final Condition condition =
          main.getConversationManager().parseConditionsString(List.of(conditionLine)).get(0);
      if (condition == null) {
        throw new QuestException("NotQuests condition line could not be parsed: " + conditionLine);
      }
      return condition
          .check(main.getQuestPlayerManager().getOrCreateQuestPlayer(profile.getProfileUUID()))
          .fulfilled();
    } catch (final QuestException exception) {
      throw exception;
    } catch (final Exception exception) {
      throw new QuestException(
          "Invalid NotQuests condition line '" + conditionLine + "': " + exception.getMessage(),
          exception);
    }
  }
}
