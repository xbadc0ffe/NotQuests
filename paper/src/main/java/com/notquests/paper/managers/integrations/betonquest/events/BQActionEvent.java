package com.notquests.paper.managers.integrations.betonquest.events;

import org.betonquest.betonquest.api.QuestException;
import org.betonquest.betonquest.api.instruction.Instruction;
import org.betonquest.betonquest.api.profile.Profile;
import org.betonquest.betonquest.api.quest.action.PlayerAction;
import org.betonquest.betonquest.api.quest.action.PlayerActionFactory;
import com.notquests.paper.NotQuests;
import com.notquests.paper.managers.integrations.betonquest.BetonQuestInstructionUtil;
import com.notquests.paper.actions.Action;

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
