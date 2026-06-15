package rocks.gravili.notquests.paper.events.hooks;

import com.magmaguy.elitemobs.api.EliteMobDeathEvent;
import com.magmaguy.elitemobs.mobconstructor.EliteEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.structs.ActiveObjective;
import rocks.gravili.notquests.paper.structs.ActiveQuest;
import rocks.gravili.notquests.paper.structs.QuestPlayer;
import rocks.gravili.notquests.paper.structs.objectives.hooks.elitemobs.KillEliteMobsObjective;

import java.util.Locale;

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
                            if (activeObjective.getObjective() instanceof KillEliteMobsObjective killEliteMobsObjective) {
                                if (activeObjective.isUnlocked()) {


                                    //Check conditions

                                    if (!killEliteMobsObjective.getEliteMobToKillContainsName().isBlank()) {
                                        boolean foundOneNotFitting = false;
                                        for (final String namePart : killEliteMobsObjective.getEliteMobToKillContainsName().toLowerCase(Locale.ROOT).split(" ")) {
                                            if (!eliteMob.getName().toLowerCase(Locale.ROOT).contains(namePart)) {
                                                foundOneNotFitting = true;
                                            }
                                        }
                                        if (foundOneNotFitting) {
                                            continue;
                                        }
                                    }
                                    if (killEliteMobsObjective.getMinimumLevel() >= 0 && eliteMob.getLevel() < killEliteMobsObjective.getMinimumLevel()) {
                                        continue;
                                    }
                                    if (killEliteMobsObjective.getMaximumLevel() >= 0 && eliteMob.getLevel() > killEliteMobsObjective.getMaximumLevel()) {
                                        continue;
                                    }
                                    double damagePercentage = (eliteMob.getDamagers().get(player)) / eliteMob.getMaxHealth();


                                    if (killEliteMobsObjective.getMinimumDamagePercentage() != -1 && damagePercentage * 100 < killEliteMobsObjective.getMinimumDamagePercentage()) {
                                        continue;
                                    }
                                    if (!killEliteMobsObjective.getSpawnReason().isBlank() && !eliteMob.getSpawnReason().toString().toLowerCase(Locale.ROOT).equalsIgnoreCase(killEliteMobsObjective.getSpawnReason().toLowerCase(Locale.ROOT))) {
                                        continue;
                                    }
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

}
