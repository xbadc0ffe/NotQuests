package rocks.gravili.notquests.paper.managers.integrations.betonquest.events;

import org.betonquest.betonquest.api.QuestException;
import org.betonquest.betonquest.api.instruction.Instruction;
import org.betonquest.betonquest.api.profile.Profile;
import org.betonquest.betonquest.api.quest.action.PlayerAction;
import org.betonquest.betonquest.api.quest.action.PlayerActionFactory;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.managers.npc.NQNPC;
import rocks.gravili.notquests.paper.structs.ActiveObjective;
import rocks.gravili.notquests.paper.structs.ActiveQuest;
import rocks.gravili.notquests.paper.structs.QuestPlayer;
import rocks.gravili.notquests.paper.structs.objectives.TriggerCommandObjective;

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
            && activeObjective.getObjective()
                instanceof final TriggerCommandObjective triggerCommandObjective
            && triggerCommandObjective.getTriggerName().equalsIgnoreCase(triggerName)) {
          activeObjective.addProgress(1, (NQNPC) null);
        }
      }
      activeQuest.removeCompletedObjectives(true);
    }
    questPlayer.removeCompletedQuests();
  }
}
