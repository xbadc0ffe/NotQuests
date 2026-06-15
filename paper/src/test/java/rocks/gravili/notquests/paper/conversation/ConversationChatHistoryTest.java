package rocks.gravili.notquests.paper.conversation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.ArrayList;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.kyori.adventure.text.Component;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ConversationChatHistoryTest {

  @Test
  @DisplayName("conversation messages are not recorded as normal chat history")
  void skipsConversationMessagesWhenRecordingNormalChat() {
    final UUID playerId = UUID.randomUUID();
    final Map<UUID, ArrayList<Component>> chatHistory = new ConcurrentHashMap<>();
    final Map<UUID, ArrayList<Component>> conversationHistory = new ConcurrentHashMap<>();
    final Component conversationLine = Component.text("Choose an answer");

    ConversationManager.rememberConversationChatMessage(
        conversationHistory, playerId, conversationLine);
    ConversationManager.rememberNonConversationChatMessage(
        chatHistory, conversationHistory, 20, playerId, conversationLine);

    assertFalse(chatHistory.containsKey(playerId));
  }

  @Test
  @DisplayName("normal chat history is trimmed to the configured replay size")
  void trimsNormalChatHistory() {
    final UUID playerId = UUID.randomUUID();
    final Map<UUID, ArrayList<Component>> chatHistory = new ConcurrentHashMap<>();
    final Map<UUID, ArrayList<Component>> conversationHistory = new ConcurrentHashMap<>();
    final Component first = Component.text("first");
    final Component second = Component.text("second");
    final Component third = Component.text("third");

    ConversationManager.rememberNonConversationChatMessage(
        chatHistory, conversationHistory, 2, playerId, first);
    ConversationManager.rememberNonConversationChatMessage(
        chatHistory, conversationHistory, 2, playerId, second);
    ConversationManager.rememberNonConversationChatMessage(
        chatHistory, conversationHistory, 2, playerId, third);

    assertEquals(2, chatHistory.get(playerId).size());
    assertEquals(second, chatHistory.get(playerId).get(0));
    assertEquals(third, chatHistory.get(playerId).get(1));
  }
}
