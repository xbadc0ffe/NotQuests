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

package rocks.gravili.notquests.paper.managers.packets.packetevents;

import com.github.retrooper.packetevents.event.PacketListener;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.chat.ChatTypes;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerChatMessage;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import rocks.gravili.notquests.paper.NotQuests;

public class PacketEventsPacketListener implements PacketListener {
  private final NotQuests main;

  public PacketEventsPacketListener(final NotQuests main) {
    this.main = main;
  }

  public void handleMainChatHistorySavingLogic(
      final Component component, final Player player) {
    if (component == null) {
      return;
    }

    try {
      main.getConversationManager().rememberNonConversationChatMessage(player.getUniqueId(), component);
    } catch (Exception ignored) {
    }
  }

  @Override
  public void onPacketSend(PacketSendEvent event) {
    if (event.getPacketType() == PacketType.Play.Server.CHAT_MESSAGE) {

      WrapperPlayServerChatMessage wrapper = new WrapperPlayServerChatMessage(event);
      var message = wrapper.getMessage();

      // Skip actionbar messages
      if (message.getType() == ChatTypes.GAME_INFO) {
        return;
      }

      Component component = message.getChatContent();
      if (component == null) {
        return;
      }

      Player player = (Player) event.getPlayer();

      // Check for the conversation replay marker
      String plainText = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
          .plainText().serialize(component);

      if (!plainText.contains("fg9023zf729ofz")) {
        handleMainChatHistorySavingLogic(component, player);
      }
    }
  }
}
