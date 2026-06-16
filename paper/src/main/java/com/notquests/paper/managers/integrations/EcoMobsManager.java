package com.notquests.paper.managers.integrations;

import com.willfp.ecomobs.mob.EcoMob;
import com.willfp.ecomobs.mob.EcoMobs;
import com.willfp.ecomobs.mob.SpawnReason;
import java.util.ArrayList;
import java.util.Collection;
import java.util.function.UnaryOperator;
import org.bukkit.Location;
import com.notquests.paper.NotQuests;

/**
 * Integration with EcoMobs (formerly EcoBosses). EcoMobs registers its mobs in the
 * {@link EcoMobs#INSTANCE} registry; we cache their ids for tab-completion and spawn them via the
 * the action's configured location/radius values.
 */
public class EcoMobsManager {
  private final NotQuests main;
  private final ArrayList<String> mobNames;

  public EcoMobsManager(final NotQuests main) {
    this.main = main;
    mobNames = new ArrayList<>();

    try {
      for (final EcoMob ecoMob : EcoMobs.INSTANCE.values()) {
        final String id = ecoMob.getID();
        mobNames.add(id);
        main.getLogManager().info("Registered EcoMob: <highlight>" + id);
      }
      main.getLogManager()
          .info("Registered <highlight>" + EcoMobs.INSTANCE.values().size() + "</highlight> EcoMobs.");
    } catch (final Exception ignored) {
      main.getLogManager().warn("Failed to load EcoMobs mobs. Are you on the latest version?");
    }
  }

  public final Collection<String> getMobNames() {
    return mobNames;
  }

  public final boolean isEcoMob(final String mobToSpawnType) {
    return EcoMobs.INSTANCE.getByID(mobToSpawnType) != null;
  }

  public void spawnMob(
      final String mobToSpawnType,
      final Location location,
      final int amount,
      final UnaryOperator<Location> locationRandomizer) {
    final EcoMob foundEcoMob = EcoMobs.INSTANCE.getByID(mobToSpawnType);
    if (foundEcoMob == null) {
      main.getLogManager()
          .warn("Tried to spawn EcoMob, but the spawn " + mobToSpawnType + " was not found.");
      return;
    }
    if (location == null) {
      main.getLogManager().warn("Tried to spawn EcoMob, but the spawn location is invalid.");
      return;
    }
    if (location.getWorld() == null) {
      main.getLogManager().warn("Tried to spawn EcoMob, but the spawn location world is invalid.");
      return;
    }

    try {
      for (int i = 0; i < amount; i++) {
        foundEcoMob.spawn(
            locationRandomizer.apply(location), SpawnReason.COMMAND);
      }
    } catch (final Exception ignored) {
      main.getLogManager().warn("Failed to spawn EcoMob. Are you on the latest version?");
    }
  }
}
