package com.notquests.paper.managers.integrations;

import com.sk89q.worldedit.IncompleteRegionException;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.bukkit.BukkitPlayer;
import com.sk89q.worldedit.bukkit.WorldEditPlugin;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.session.SessionManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import com.notquests.paper.NotQuests;
import com.notquests.paper.objectives.support.ObjectiveRegion;

public class WorldEditManager {
  private final NotQuests main;
  private final WorldEditPlugin worldEditPlugin;

  public WorldEditManager(final NotQuests main) {
    this.main = main;
    worldEditPlugin =
        (WorldEditPlugin) Bukkit.getServer().getPluginManager().getPlugin("WorldEdit");
  }

  public ObjectiveRegion getSelectionRegionOrNull(final Player player) {
    try {
      return getSelectionRegion(player);
    } catch (IncompleteRegionException ignored) {
      return null;
    }
  }

  private ObjectiveRegion getSelectionRegion(final Player player) throws IncompleteRegionException {
    BukkitPlayer actor =
        BukkitAdapter.adapt(player); // WorldEdit's native Player class extends Actor
    SessionManager manager =
        main.getIntegrationsManager()
            .getWorldEditManager()
            .getWorldEdit()
            .getWorldEdit()
            .getSessionManager();
    LocalSession localSession = manager.get(actor);

    com.sk89q.worldedit.world.World selectionWorld = localSession.getSelectionWorld();
    if (selectionWorld == null) {
      throw new IncompleteRegionException();
    }

    Region region = localSession.getSelection(selectionWorld);
    final Location min =
        new Location(
            BukkitAdapter.adapt(selectionWorld),
            region.getMinimumPoint().x(),
            region.getMinimumPoint().y(),
            region.getMinimumPoint().z());
    final Location max =
        new Location(
            BukkitAdapter.adapt(selectionWorld),
            region.getMaximumPoint().x(),
            region.getMaximumPoint().y(),
            region.getMaximumPoint().z());
    return new ObjectiveRegion(min, max);
  }

  public WorldEditPlugin getWorldEdit() {
    return worldEditPlugin;
  }
}
