package com.notquests.paper.structs;

import org.bukkit.Bukkit;
import com.notquests.paper.NotQuests;
import com.notquests.paper.events.notquests.QuestFailEvent;
import com.notquests.paper.builtin.objectives.EscortNPC;
import com.notquests.paper.triggers.ActiveTrigger;
import com.notquests.paper.triggers.Trigger;

import java.util.ArrayList;

/**
 * This is a special object for active quests. Apart from the Quest itself, it stores additional
 * objects to track the quest progress. This includes the active objectives and completed
 * objectives, as well as triggers and the quest player who accepted the Quest.
 *
 * <p>All this information is saved in the Database, so the Player can continue from where they left
 * off if the server or the plugin restarts.
 *
 * @author Alessio Gravili
 */
public class ActiveQuest extends ActiveObjectiveHolder {
  private final ArrayList<ActiveTrigger> activeTriggers;

  private final Quest quest;


  public ActiveQuest(final NotQuests main, final Quest quest, final QuestPlayer questPlayer) {
    super(main, questPlayer, quest, 0);
    this.quest = quest;
    activeTriggers = new ArrayList<>();

    int triggerID = 1;
    for (final Trigger trigger : quest.getTriggers()) {
      ActiveTrigger activeTrigger = new ActiveTrigger(triggerID, trigger, this);
      activeTriggers.add(activeTrigger);
      triggerID++;
    }
  }

  public final ArrayList<ActiveTrigger> getActiveTriggers() {
    return activeTriggers;
  }

  public void fail() {

    QuestFailEvent questFailEvent = new QuestFailEvent(getQuestPlayer(), this);
    if (Bukkit.isPrimaryThread()) {
      Bukkit.getScheduler()
          .runTaskAsynchronously(
              getMain().getMain(), () -> Bukkit.getPluginManager().callEvent(questFailEvent));
    } else {
      Bukkit.getPluginManager().callEvent(questFailEvent);
    }

    if (questFailEvent.isCancelled()) {
      return;
    }

    getQuestPlayer().sendMessage(
        getMain().getLanguageManager().getString("chat.quest-failed", getQuestPlayer().getPlayer(), this));

    for (final ActiveObjective activeObjective : getActiveObjectives()) {
      getQuestPlayer().disableTrackingObjective(activeObjective);
      EscortNPC.stopEscortIfMatching(getMain(), activeObjective);
    }
  }



  public final String getQuestIdentifier() {
    return getObjectiveHolder().getIdentifier();
  }

  public final Quest getQuest(){
    return this.quest;
  }
}
