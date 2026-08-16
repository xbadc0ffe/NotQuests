package com.notquests.paper.managers.integrations;

import io.lumine.mythic.api.MythicPlugin;
import io.lumine.mythic.api.MythicProvider;
import io.lumine.mythic.api.mobs.MythicMob;
import io.lumine.mythic.bukkit.BukkitAdapter;
import org.bukkit.Location;
import com.notquests.paper.NotQuests;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Optional;
import java.util.function.UnaryOperator;

public class MythicMobsManager {
  private final NotQuests main;
  private final MythicPlugin mythicPlugin;

  public MythicMobsManager(final NotQuests main) {
    this.main = main;
    this.mythicPlugin = MythicProvider.get();
  }

  public final MythicPlugin getMythicPlugin() {
    return mythicPlugin;
  }

  public final Collection<String> getMobNames() {
    return mythicPlugin.getMobManager().getMobNames();
  }

  public final Collection<String> getFactionNames(final String prefix) {
    final Collection<String> factionNames = new ArrayList<>();
    for(final MythicMob mobType : mythicPlugin.getMobManager().getMobTypes()){
      if(!factionNames.contains(mobType.getFaction())) {
        if(mobType.getFaction() == null){
          factionNames.add(prefix+ "none");
        }else {
          factionNames.add(prefix+ mobType.getFaction());
        }
      }
    }
    return factionNames;
  }

  public void spawnMob(
      final String mobToSpawnType,
      final Location location,
      final int amount,
      final UnaryOperator<Location> locationRandomizer) {
    final Optional<MythicMob> foundMythicMob =
        mythicPlugin.getMobManager().getMythicMob(mobToSpawnType);
    if (foundMythicMob.isEmpty()) {
      main.getLogManager()
          .warn(
              "Tried to spawn mythic mob, but the mythic mob "
                  + mobToSpawnType
                  + " was not found.");
      return;
    }
    if (location == null) {
      main.getLogManager().warn("Tried to spawn mythic mob, but the spawn location is invalid.");
      return;
    }
    if (location.getWorld() == null) {
      main.getLogManager()
          .warn("Tried to spawn mythic mob, but the spawn location world is invalid.");
      return;
    }

    for (int i = 0; i < amount; i++) {
      foundMythicMob
          .get()
          .spawn(BukkitAdapter.adapt(locationRandomizer.apply(location)), 1);
    }
  }

  public final boolean isMythicMob(final String mobToSpawnType) {
    return mythicPlugin.getMobManager().getMythicMob(mobToSpawnType).isPresent();
  }
}
