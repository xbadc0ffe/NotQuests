package com.notquests.paper.integrations;

import org.geysermc.floodgate.api.FloodgateApi;

import com.notquests.paper.NotQuests;

import java.util.UUID;

public class FloodgateIntegration {
  private final NotQuests main;

  public FloodgateIntegration(final NotQuests main) {
    this.main = main;
  }

  public final boolean isPlayerOnFloodgate(final UUID playerUUID) {
    try {
      return FloodgateApi.getInstance().isFloodgatePlayer(playerUUID);
    } catch (final LinkageError | RuntimeException exception) {
      // The API class is not reachable from this plugin's class loader (dependency not declared or
      // not loaded) or Floodgate is not initialised: treat the player as a Java player instead of
      // failing whatever asked (a conversation condition, an NPC click).
      if (!warned) {
        warned = true;
        main.getMain().getLogger().warning("Floodgate lookup failed; treating players as non-Floodgate: " + exception);
      }
      return false;
    }
  }

  private boolean warned;
}
