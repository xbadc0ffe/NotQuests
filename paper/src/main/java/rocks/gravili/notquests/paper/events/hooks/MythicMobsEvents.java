package rocks.gravili.notquests.paper.events.hooks;


import io.lumine.mythic.api.mobs.MythicMob;
import io.lumine.mythic.bukkit.events.MythicMobDeathEvent;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.structs.ActiveObjective;
import rocks.gravili.notquests.paper.structs.ActiveQuest;
import rocks.gravili.notquests.paper.structs.QuestPlayer;
import rocks.gravili.notquests.paper.structs.objectives.KillMobsObjective;

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
                            if (activeObjective.getObjective() instanceof final KillMobsObjective killMobsObjective) {
                                if (activeObjective.isUnlocked()) {
                                    final MythicMob killedMob = event.getMobType();
                                    if (killMobsObjective.getMobToKill().equalsIgnoreCase("any")
                                            || killMobsObjective.getMobToKill().equals(killedMob.getInternalName())
                                            ||
                                            (
                                                    killMobsObjective.getMobToKill().startsWith("mmfaction:")
                                                    && (
                                                            killMobsObjective.getMobToKill().replace("mmfaction:", "").equals(killedMob.getFaction())
                                                            || (
                                                                    killedMob.getFaction() == null && killMobsObjective.getMobToKill().replace("mmfaction:", "").equals("none")
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
