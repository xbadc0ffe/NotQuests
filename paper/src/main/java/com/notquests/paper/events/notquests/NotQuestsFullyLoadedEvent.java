package com.notquests.paper.events.notquests;

import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import com.notquests.paper.NotQuests;

public class NotQuestsFullyLoadedEvent extends Event implements Cancellable {

  private static final HandlerList HANDLERS = new HandlerList();
  private final NotQuests main;

  private boolean isCancelled;

  public NotQuestsFullyLoadedEvent(final NotQuests main) {
    super(true);

    this.main = main;

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

  public final NotQuests getNotQuests() {
    return this.main;
  }
}
