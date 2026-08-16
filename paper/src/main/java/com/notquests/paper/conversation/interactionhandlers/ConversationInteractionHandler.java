package com.notquests.paper.conversation.interactionhandlers;

import org.bukkit.entity.Player;
import com.notquests.paper.conversation.Conversation;
import com.notquests.paper.conversation.ConversationLine;
import com.notquests.paper.conversation.ConversationPlayer;
import com.notquests.paper.conversation.Speaker;
import com.notquests.paper.structs.QuestPlayer;

public interface ConversationInteractionHandler {

  void sendText(final String text, final Speaker speaker, final Player player, final QuestPlayer questPlayer, final
  Conversation conversation, final ConversationLine conversationLine, final boolean deletePrevious, final ConversationPlayer conversationPlayer);

  void sendOption(final String optionMessage, final Speaker speaker, final Player player, final QuestPlayer questPlayer, final
  Conversation conversation, final ConversationLine conversationLine, final ConversationPlayer conversationPlayer);
}
