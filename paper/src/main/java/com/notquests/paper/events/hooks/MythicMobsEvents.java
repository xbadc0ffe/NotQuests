package com.notquests.paper.events.hooks;


import io.lumine.mythic.api.mobs.MythicMob;
import io.lumine.mythic.bukkit.events.MythicMobDeathEvent;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import com.notquests.paper.NotQuests;
import com.notquests.paper.builtin.objectives.KillMobs;
import com.notquests.paper.structs.ActiveObjective;
import com.notquests.paper.structs.ActiveQuest;
import com.notquests.paper.structs.QuestPlayer;

public class MythicMobsEvents implements Listener {
    private final NotQuests main;

    public MythicMobsEvents(NotQuests main) {
        this.main = main;
    }

    @EventHandler
    public void onMythicMobDeath(final MythicMobDeathEvent event) {
        //KillMobs objectives
        if (event.getKiller() instanceof final Player player) {
            final QuestPlayer questPlayer = main.getQuestPlayerManager().getActiveQuestPlayer(player.getUniqueId());
            if (questPlayer != null) {
                if (questPlayer.getActiveQuests().size() > 0) {
                    for (final ActiveQuest activeQuest : questPlayer.getActiveQuests()) {
                        for (final ActiveObjective activeObjective : activeQuest.getActiveObjectives()) {
                            if (KillMobs.isKillMobs(activeObjective.getObjective())) {
                                if (activeObjective.isUnlocked()) {
                                    final MythicMob killedMob = event.getMobType();
                                    final String target = KillMobs.target(activeObjective.getObjective());
                                    if (target.equalsIgnoreCase("any")
                                            || target.equals(killedMob.getInternalName())
                                            ||
                                            (
                                                    target.startsWith("mmfaction:")
                                                    && (
                                                            target.replace("mmfaction:", "").equals(killedMob.getFaction())
                                                            || (
                                                                    killedMob.getFaction() == null && target.replace("mmfaction:", "").equals("none")
                                                               )
                                                    )
                                            )
                                    ) {
                                        if (event.getEntity() != event.getKiller()) { //Suicide prevention
                                            activeObjective.addProgress(1);
                                        }

                                    }
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
