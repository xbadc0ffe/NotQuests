package com.notquests.paper.managers.integrations.betonquest.events;

import org.betonquest.betonquest.api.QuestException;
import org.betonquest.betonquest.api.instruction.Instruction;
import org.betonquest.betonquest.api.profile.Profile;
import org.betonquest.betonquest.api.quest.action.PlayerAction;
import org.betonquest.betonquest.api.quest.action.PlayerActionFactory;
import com.notquests.paper.NotQuests;
import com.notquests.paper.structs.ActiveQuest;
import com.notquests.paper.structs.Quest;
import com.notquests.paper.structs.QuestPlayer;

public class BQAbortQuestEvent implements PlayerActionFactory {
  private final NotQuests main;

  public BQAbortQuestEvent(final NotQuests main) {
    this.main = main;
  }

  @Override
  public PlayerAction parsePlayer(final Instruction instruction) throws QuestException {
    final String questName = instruction.nextElement();
    return profile -> abortQuest(profile, questName);
  }

  private void abortQuest(final Profile profile, final String questName) throws QuestException {
    final Quest quest = main.getQuestManager().getQuest(questName);
    if (quest == null) {
      throw new QuestException("NotQuests quest '" + questName + "' does not exist.");
    }
    final QuestPlayer questPlayer =
        main.getQuestPlayerManager().getActiveQuestPlayer(profile.getProfileUUID());
    if (questPlayer == null) {
      return;
    }
    final ActiveQuest activeQuest = questPlayer.getActiveQuest(quest);
    if (activeQuest != null) {
      questPlayer.getActiveQuests().remove(activeQuest);
    }
  }
}
