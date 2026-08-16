package com.notquests.paper.events.notquests;

import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import com.notquests.paper.structs.QuestPlayer;

public class QuestPointsChangeEvent extends Event implements Cancellable {

  private static final HandlerList HANDLERS = new HandlerList();
  private final QuestPlayer questPlayer;
  private final long newQuestPointsAmount;
  private boolean isCancelled;

  public QuestPointsChangeEvent(final QuestPlayer questPlayer, final long newQuestPointsAmount) {
    super(true);

    this.questPlayer = questPlayer;
    this.newQuestPointsAmount = newQuestPointsAmount;

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

  public long getNewQuestPointsAmount() {
    return this.newQuestPointsAmount;
  }
}
