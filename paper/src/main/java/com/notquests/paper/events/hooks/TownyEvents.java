package com.notquests.paper.events.hooks;

import com.palmergames.bukkit.towny.event.NationAddTownEvent;
import com.palmergames.bukkit.towny.event.TownAddResidentEvent;
import com.palmergames.bukkit.towny.event.TownRemoveResidentEvent;
import com.palmergames.bukkit.towny.object.Resident;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import com.notquests.paper.NotQuests;
import com.notquests.paper.builtin.objectives.TownyNationReachTownCount;
import com.notquests.paper.builtin.objectives.TownyReachResidentCount;
import com.notquests.paper.structs.ActiveObjective;
import com.notquests.paper.structs.ActiveQuest;
import com.notquests.paper.structs.QuestPlayer;

public class TownyEvents implements Listener {
  private final NotQuests main;

  public TownyEvents(NotQuests main) {
    this.main = main;
  }

  @EventHandler
  public void onTownAddToNation(NationAddTownEvent e) {
    for (final Resident resident : e.getNation().getResidents()) {
      final QuestPlayer questPlayer =
          main.getQuestPlayerManager().getActiveQuestPlayer(resident.getUUID());
      if (questPlayer != null) {
        if (questPlayer.getActiveQuests().size() > 0) {
          for (final ActiveQuest activeQuest : questPlayer.getActiveQuests()) {
            for (final ActiveObjective activeObjective : activeQuest.getActiveObjectives()) {
              if (activeObjective.isUnlocked()) {
                if (TownyNationReachTownCount.isTownyNationReachTownCount(activeObjective)) {
                  activeObjective.addProgress(1);
                }
              }
            }
            activeQuest.removeCompletedObjectives(true);
          }
          questPlayer.removeCompletedQuests();
        }
      }
    }
  }

  @EventHandler
  public void onTownRemoveFromNation(NationAddTownEvent e) {
    for (final Resident resident : e.getNation().getResidents()) {
      final QuestPlayer questPlayer =
          main.getQuestPlayerManager().getActiveQuestPlayer(resident.getUUID());
      if (questPlayer != null) {
        if (questPlayer.getActiveQuests().size() > 0) {
          for (final ActiveQuest activeQuest : questPlayer.getActiveQuests()) {
            for (final ActiveObjective activeObjective : activeQuest.getActiveObjectives()) {
              if (activeObjective.isUnlocked()) {
                if (TownyNationReachTownCount.isTownyNationReachTownCount(activeObjective)) {
                  activeObjective.removeProgress(1, true);
                }
              }
            }
            activeQuest.removeCompletedObjectives(true);
          }
          questPlayer.removeCompletedQuests();
        }
      }
    }
  }

  @EventHandler
  public void onResidentAdd(TownAddResidentEvent e) {
    for (final Resident resident : e.getTown().getResidents()) {
      final QuestPlayer questPlayer =
          main.getQuestPlayerManager().getActiveQuestPlayer(resident.getUUID());
      if (questPlayer != null) {
        if (questPlayer.getActiveQuests().size() > 0) {
          for (final ActiveQuest activeQuest : questPlayer.getActiveQuests()) {
            for (final ActiveObjective activeObjective : activeQuest.getActiveObjectives()) {
              if (activeObjective.isUnlocked()) {
                if (TownyReachResidentCount.isTownyReachResidentCount(activeObjective)) {
                  activeObjective.addProgress(1);
                }
              }
            }
            activeQuest.removeCompletedObjectives(true);
          }
          questPlayer.removeCompletedQuests();
        }
      }
    }
  }

  @EventHandler
  public void onResidentRemove(TownRemoveResidentEvent e) {
    for (final Resident resident : e.getTown().getResidents()) {
      final QuestPlayer questPlayer =
          main.getQuestPlayerManager().getActiveQuestPlayer(resident.getUUID());
      if (questPlayer != null) {
        if (!questPlayer.getActiveQuests().isEmpty()) {
          for (final ActiveQuest activeQuest : questPlayer.getActiveQuests()) {
            for (final ActiveObjective activeObjective : activeQuest.getActiveObjectives()) {
              if (activeObjective.isUnlocked()) {
                if (TownyReachResidentCount.isTownyReachResidentCount(activeObjective)) {
                  activeObjective.removeProgress(1, true);
                }
              }
            }
            activeQuest.removeCompletedObjectives(true);
          }
          questPlayer.removeCompletedQuests();
        }
      }
    }
  }
}
