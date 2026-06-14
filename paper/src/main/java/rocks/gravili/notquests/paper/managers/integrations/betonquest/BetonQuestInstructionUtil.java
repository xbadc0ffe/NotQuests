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

package rocks.gravili.notquests.paper.managers.integrations.betonquest;

import org.betonquest.betonquest.api.instruction.Instruction;

import java.util.StringJoiner;

public final class BetonQuestInstructionUtil {
  private BetonQuestInstructionUtil() {}

  public static String valueLine(final Instruction instruction) {
    final StringJoiner joiner = new StringJoiner(" ");
    for (final String part : instruction.getValueParts()) {
      if (!part.isBlank()) {
        joiner.add(rewriteLegacySemicolonPart(part));
      }
    }
    String line = joiner.toString();
    if (line.startsWith("nq_action ")) {
      line = line.substring("nq_action ".length());
    } else if (line.startsWith("nq_condition ")) {
      line = line.substring("nq_condition ".length());
    }
    return line;
  }

  private static String rewriteLegacySemicolonPart(final String part) {
    final String[] semicolonSplit = part.split(";");
    if (semicolonSplit.length >= 4) {
      return semicolonSplit[3]
          + " "
          + semicolonSplit[0]
          + " "
          + semicolonSplit[1]
          + " "
          + semicolonSplit[2];
    }
    return part;
  }
}
