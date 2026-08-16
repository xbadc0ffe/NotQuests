package com.notquests.paper.events.notquests;

import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import com.notquests.paper.structs.ActiveObjective;
import com.notquests.paper.structs.ActiveObjectiveHolder;
import com.notquests.paper.structs.QuestPlayer;

public class ObjectiveCompleteEvent extends Event implements Cancellable {

  private static final HandlerList HANDLERS = new HandlerList();
  private final QuestPlayer questPlayer;
  private final ActiveObjective activeObjective;
  private final ActiveObjectiveHolder activeObjectiveHolder;
  private boolean isCancelled;

  public ObjectiveCompleteEvent(
      final QuestPlayer questPlayer,
      final ActiveObjective activeObjective,
      final ActiveObjectiveHolder activeObjectiveHolder) {
    super(true);

    this.questPlayer = questPlayer;
    this.activeObjective = activeObjective;
    this.activeObjectiveHolder = activeObjectiveHolder;

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

  public ActiveObjective getActiveObjective() {
    return this.activeObjective;
  }

  public ActiveObjectiveHolder getActiveObjectiveHolder() {
    return this.activeObjectiveHolder;
  }
}
