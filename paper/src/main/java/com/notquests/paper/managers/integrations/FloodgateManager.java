package com.notquests.paper.managers.integrations;

import org.geysermc.floodgate.api.FloodgateApi;
import com.notquests.paper.NotQuests;

import java.util.UUID;

public class FloodgateManager {
  private final NotQuests main;

  public FloodgateManager(final NotQuests main) {
    this.main = main;
  }

  public final boolean isPlayerOnFloodgate(final UUID playerUUID) {
    return FloodgateApi.getInstance().isFloodgatePlayer(playerUUID);
  }
}
