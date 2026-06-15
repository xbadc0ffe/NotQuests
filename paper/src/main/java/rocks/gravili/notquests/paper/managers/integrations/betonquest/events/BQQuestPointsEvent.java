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
