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
