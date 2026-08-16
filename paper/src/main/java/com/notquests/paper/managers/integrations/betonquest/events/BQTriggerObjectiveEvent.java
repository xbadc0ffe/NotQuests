package com.notquests.paper.managers.integrations.betonquest.events;

import org.betonquest.betonquest.api.QuestException;
import org.betonquest.betonquest.api.instruction.Instruction;
import org.betonquest.betonquest.api.profile.Profile;
import org.betonquest.betonquest.api.quest.action.PlayerAction;
import org.betonquest.betonquest.api.quest.action.PlayerActionFactory;
import com.notquests.paper.NotQuests;
import com.notquests.paper.managers.npc.NQNPC;
import com.notquests.paper.builtin.objectives.TriggerCommand;
import com.notquests.paper.structs.ActiveObjective;
import com.notquests.paper.structs.ActiveQuest;
import com.notquests.paper.structs.QuestPlayer;

public class BQTriggerObjectiveEvent implements PlayerActionFactory {
  private final NotQuests main;

  public BQTriggerObjectiveEvent(final NotQuests main) {
    this.main = main;
  }

  @Override
  public PlayerAction parsePlayer(final Instruction instruction) throws QuestException {
    final String triggerName = instruction.nextElement();
    return profile -> trigger(profile, triggerName);
  }

  private void trigger(final Profile profile, final String triggerName) {
    final QuestPlayer questPlayer =
        main.getQuestPlayerManager().getActiveQuestPlayer(profile.getProfileUUID());
    if (questPlayer == null) {
      return;
    }

    for (final ActiveQuest activeQuest : questPlayer.getActiveQuests()) {
      for (final ActiveObjective activeObjective : activeQuest.getActiveObjectives()) {
        if (activeObjective.isUnlocked()
            && TriggerCommand.matches(activeObjective.getObjective(), triggerName)) {
          activeObjective.addProgress(1, (NQNPC) null);
        }
      }
      activeQuest.removeCompletedObjectives(true);
    }
    questPlayer.removeCompletedQuests();
  }
}
