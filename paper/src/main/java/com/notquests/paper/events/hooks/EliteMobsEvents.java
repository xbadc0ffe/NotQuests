package com.notquests.paper.events.hooks;

import com.magmaguy.elitemobs.api.EliteMobDeathEvent;
import com.magmaguy.elitemobs.mobconstructor.EliteEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import com.notquests.paper.NotQuests;
import com.notquests.paper.builtin.objectives.KillEliteMobs;
import com.notquests.paper.structs.ActiveObjective;
import com.notquests.paper.structs.ActiveQuest;
import com.notquests.paper.structs.QuestPlayer;

public class EliteMobsEvents implements Listener {
    private final NotQuests main;

    public EliteMobsEvents(NotQuests main) {
        this.main = main;
    }

    @EventHandler
    public void onEliteMobDeath(EliteMobDeathEvent event) {
        final EliteEntity eliteMob = event.getEliteEntity();

        for (final Player player : eliteMob.getDamagers().keySet()) {
            final QuestPlayer questPlayer = main.getQuestPlayerManager().getActiveQuestPlayer(player.getUniqueId());
            if (questPlayer != null) {
                if (questPlayer.getActiveQuests().size() > 0) {
                    for (final ActiveQuest activeQuest : questPlayer.getActiveQuests()) {
                        for (final ActiveObjective activeObjective : activeQuest.getActiveObjectives()) {
                            if (activeObjective.isUnlocked()
                                    && KillEliteMobs.countsEliteMobKill(
                                            activeObjective,
                                            eliteMob.getName(),
                                            eliteMob.getLevel(),
                                            eliteMob.getDamagers().get(player),
                                            eliteMob.getMaxHealth(),
                                            eliteMob.getSpawnReason().toString())) {
                                activeObjective.addProgress(1);
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
