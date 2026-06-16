package com.notquests.paper.builtin.variables;

import com.notquests.paper.variables.Variable;
import com.notquests.paper.variables.VariableDataType;

import com.notquests.paper.NotQuests;
import com.notquests.paper.structs.ActiveQuest;
import com.notquests.paper.structs.Quest;
import com.notquests.paper.structs.QuestPlayer;

import java.util.List;

public class ActiveQuestsVariable extends Variable<String[]> {
  public ActiveQuestsVariable(NotQuests main) {
    super(main);
    setCanSetValue(true);
  }

  @Override
  public String[] getValueInternally(QuestPlayer questPlayer, Object... objects) {
    String[] activeQuests;
    if (questPlayer == null) {
      return null;
    }

    activeQuests =
        questPlayer.getActiveQuests().stream()
            .map(ActiveQuest::getQuestIdentifier)
            .toArray(String[]::new);

    return activeQuests;
  }

  @Override
  public boolean setValueInternally(String[] newValue, QuestPlayer questPlayer, Object... objects) {
    if (questPlayer == null) {
      return false;
    }

    for (final ActiveQuest acceptedQuest : questPlayer.getActiveQuests()) {
      boolean foundQuest = false;
      for (int i = 0; i < newValue.length; i++) {
        if (newValue[i].equalsIgnoreCase(acceptedQuest.getQuestIdentifier())) {
          foundQuest = true;
          break;
        }
      }
      if (!foundQuest) {
        questPlayer.failQuest(acceptedQuest);
      }
    }

    for (int i = 0; i < newValue.length; i++) {
      Quest quest = main.getQuestManager().getQuest(newValue[i]);
      if (quest != null && !questPlayer.hasAcceptedQuest(quest)) {
        main.getQuestPlayerManager().forceAcceptQuestSilent(questPlayer.getUniqueId(), quest);
      }
    }

    return true;
  }

  @Override
  public List<String> getPossibleValues(QuestPlayer questPlayer, Object... objects) {
    return main.getQuestManager().getAllQuests().stream().map(quest -> quest.getIdentifier()).toList();
  }

  @Override
  public String getPlural() {
    return "Active Quests";
  }

  @Override
  public String getSingular() {
    return "Active Quest";
  }
}
