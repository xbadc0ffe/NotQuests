package rocks.gravili.notquests.paper.managers.integrations.betonquest.conversationInterceptors;

import net.kyori.adventure.text.Component;
import org.betonquest.betonquest.api.profile.OnlineProfile;
import org.betonquest.betonquest.conversation.interceptor.Interceptor;
import org.bukkit.entity.Player;
import rocks.gravili.notquests.paper.NotQuests;

public class NotQuestsInterceptor implements Interceptor {
  private final NotQuests main;
  private final Player player;

  public NotQuestsInterceptor(final NotQuests main, final OnlineProfile onlineProfile) {
    this.main = main;
    this.player = onlineProfile.getPlayer();
  }

  @Override
  public void begin() {
    // No listener registration needed in BetonQuest 3's interceptor API.
  }

  @Override
  public void sendMessage(final Component message) {
    if (main.getConfiguration().deletePreviousConversations && main.getConversationManager() != null) {
      main.getConversationManager().rememberConversationChatMessage(player.getUniqueId(), message);
    }
    if (player.isOnline()) {
      main.sendMessage(player, message);
    }
  }

  @Override
  public void end() {
    // Nothing to unregister.
  }
}
