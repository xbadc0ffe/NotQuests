package com.notquests.paper.events.hooks;

import com.willfp.ecomobs.mob.EcoMob;
import com.willfp.ecomobs.mob.impl.ConfigDrivenEcoMobKt;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import com.notquests.paper.NotQuests;
import com.notquests.paper.builtin.objectives.KillMobs;
import com.notquests.paper.structs.ActiveObjective;
import com.notquests.paper.structs.ActiveQuest;
import com.notquests.paper.structs.QuestPlayer;

public class EcoMobsEvents implements Listener {

  private final NotQuests main;

  public EcoMobsEvents(NotQuests main) {
    this.main = main;
  }

  // We listen to the vanilla EntityDeathEvent (which always fires) and ask EcoMobs whether the dead
  // entity is one of its mobs, rather than relying on EcoMobs' own kill event (which does not fire
  // reliably for plain combat kills).
  @EventHandler
  public void onEcoMobDeath(final EntityDeathEvent event) {
    if (!(event.getEntity() instanceof final Mob mob)) {
      return;
    }
    final Player killer = mob.getKiller();
    if (killer == null) {
      return;
    }
    final EcoMob ecoMob = ConfigDrivenEcoMobKt.getEcoMob(mob);
    if (ecoMob == null) {
      return; // not an EcoMobs mob
    }

    final QuestPlayer questPlayer =
        main.getQuestPlayerManager().getActiveQuestPlayer(killer.getUniqueId());
    if (questPlayer == null || questPlayer.getActiveQuests().isEmpty()) {
      return;
    }

    final String killedMobID = ecoMob.getID();
    for (final ActiveQuest activeQuest : questPlayer.getActiveQuests()) {
      for (final ActiveObjective activeObjective : activeQuest.getActiveObjectives()) {
        if (activeObjective.isUnlocked()
            && KillMobs.isKillMobs(activeObjective.getObjective())) {
          final String target = KillMobs.target(activeObjective.getObjective());
          if (target.equalsIgnoreCase("any") || target.equalsIgnoreCase(killedMobID)) {
            activeObjective.addProgress(1);
          }
        }
      }
      activeQuest.removeCompletedObjectives(true);
    }
    questPlayer.removeCompletedQuests();
  }
}
