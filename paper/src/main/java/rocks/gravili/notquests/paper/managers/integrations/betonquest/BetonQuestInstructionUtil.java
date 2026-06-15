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
