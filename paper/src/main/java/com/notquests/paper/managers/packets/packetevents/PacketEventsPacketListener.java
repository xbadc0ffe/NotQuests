package com.notquests.paper.managers.packets.packetevents;

import com.github.retrooper.packetevents.event.PacketListener;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.chat.ChatTypes;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerChatMessage;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import com.notquests.paper.NotQuests;

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
