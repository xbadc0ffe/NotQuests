package com.notquests.paper.conversation;

import org.bukkit.event.Listener;
import com.notquests.paper.NotQuests;

public class ConversationEvents implements Listener {
  private final NotQuests main;
  private final ConversationManager conversationManager;

  public ConversationEvents(final NotQuests main, final ConversationManager conversationManager) {
    this.main = main;
    this.conversationManager = conversationManager;
  }
}
