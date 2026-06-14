/*
 * NotQuests - A Questing plugin for Minecraft Servers
 * Copyright (C) 2021-2022 Alessio Gravili
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

package rocks.gravili.notquests.paper.managers.integrations.betonquest.conversationInterceptors;

import net.kyori.adventure.text.Component;
import org.betonquest.betonquest.api.profile.OnlineProfile;
import org.betonquest.betonquest.conversation.interceptor.Interceptor;
import org.bukkit.entity.Player;
import rocks.gravili.notquests.paper.NotQuests;

import java.util.ArrayList;

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
      final ArrayList<Component> history =
          main.getConversationManager()
              .getConversationChatHistory()
              .getOrDefault(player.getUniqueId(), new ArrayList<>());
      history.add(message);
      main.getConversationManager().getConversationChatHistory().put(player.getUniqueId(), history);
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
