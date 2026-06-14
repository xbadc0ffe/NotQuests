/*
 * NotQuests - A Questing plugin for Minecraft Servers
 * Copyright (C) 2022 Alessio Gravili
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package rocks.gravili.notquests.paper.events.hooks;

import org.betonquest.betonquest.api.bukkit.event.ConversationOptionEvent;
import org.betonquest.betonquest.api.bukkit.event.PlayerObjectiveChangeEvent;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.structs.QuestPlayer;
import rocks.gravili.notquests.paper.structs.objectives.hooks.betonquest.BetonQuestObjectiveStateChangeObjective;

public class BetonQuestEvents implements Listener {
  private final NotQuests main;

  public BetonQuestEvents(final NotQuests main) {
    this.main = main;
  }

  @EventHandler
  public void onBetonQuestObjectiveStateChange(final PlayerObjectiveChangeEvent event) {
    final QuestPlayer questPlayer =
        main.getQuestPlayerManager().getActiveQuestPlayer(event.getProfile().getProfileUUID());
    if (questPlayer == null || questPlayer.getActiveQuests().isEmpty()) {
      return;
    }

    questPlayer.queueObjectiveCheck(
        activeObjective -> {
          if (!activeObjective.isUnlocked()
              || !(activeObjective.getObjective()
                  instanceof final BetonQuestObjectiveStateChangeObjective objective)) {
            return;
          }
          if (event.getState() == objective.getObjectiveState()
              && event.getObjectiveID().getFull().equalsIgnoreCase(objective.getObjectiveFullID())) {
            activeObjective.addProgress(1);
          }
        });
    questPlayer.checkQueuedObjectives();
  }

  @EventHandler
  public void onConversationOption(final ConversationOptionEvent event) {
    if (main.getConversationManager() == null || event.getProfile().getOnlineProfile().isEmpty()) {
      return;
    }
    final Player player = event.getProfile().getOnlineProfile().get().getPlayer();
    if (player.isOnline()) {
      main.getConversationManager().removeOldMessages(player);
    }
  }
}
