package com.notquests.paper.events.notquests;

import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import com.notquests.paper.structs.ActiveQuest;
import com.notquests.paper.structs.QuestPlayer;

public class QuestFailEvent extends Event implements Cancellable {

  private static final HandlerList HANDLERS = new HandlerList();
  private final QuestPlayer questPlayer;
  private final ActiveQuest activeQuest;
  private boolean isCancelled;

  public QuestFailEvent(final QuestPlayer questPlayer, final ActiveQuest activeQuest) {
    super(true);

    this.questPlayer = questPlayer;
    this.activeQuest = activeQuest;

    this.isCancelled = false;
  }

  public static HandlerList getHandlerList() {
    return HANDLERS;
  }

  @Override
  public boolean isCancelled() {
    return this.isCancelled;
  }

  @Override
  public void setCancelled(boolean isCancelled) {
    this.isCancelled = isCancelled;
  }

  @NotNull
  @Override
  public HandlerList getHandlers() {
    return HANDLERS;
  }

  public QuestPlayer getQuestPlayer() {
    return this.questPlayer;
  }

  public ActiveQuest getActiveQuest() {
    return this.activeQuest;
  }
}
