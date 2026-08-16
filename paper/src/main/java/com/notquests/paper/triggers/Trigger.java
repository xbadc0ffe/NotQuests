package com.notquests.paper.triggers;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import com.notquests.paper.NotQuests;
import com.notquests.paper.managers.data.Category;
import com.notquests.paper.structs.ActiveQuest;
import com.notquests.paper.structs.Quest;
import com.notquests.paper.actions.Action;

public abstract class Trigger {
  protected final NotQuests main;
  private Quest quest = null;
  private int triggerID = 1;
  private Action action = null;
  private int applyOn =
      0; // 0 is for the whole quest. Positive numbers = objectives (JUST INTERNALLY HERE, NOT IN
         // THE ADMIN COMMAND)
  private String worldName = "ALL";
  private long amountNeeded = 0; // 0 or 1 means every trigger() triggers it
  private Category category;

  public Trigger(final NotQuests main) {
    this.main = main;
    category = main.getDataManager().getDefaultCategory();
  }

  public final Category getCategory() {
    return category;
  }

  public void setCategory(final Category category) {
    this.category = category;
  }

  public void setAction(final Action action) {
    this.action = action;
  }

  public final Quest getQuest() {
    return quest;
  }

  public void setQuest(final Quest quest) {
    this.quest = quest;
  }

  public final int getTriggerID() {
    return triggerID;
  }

  public void setTriggerID(final int triggerID) {
    this.triggerID = triggerID;
  }

  public final String getTriggerType() {
    return main.getTriggerCatalog().getTriggerType(this);
  }

  public final String getWorldName() {
    return worldName;
  }

  public void setWorldName(final String worldName) {
    this.worldName = worldName;
  }

  public final Action getTriggerAction() {
    return action;
  }

  public final int getApplyOn() {
    return applyOn;
  }

  public void setApplyOn(final int applyOn) {
    this.applyOn = applyOn;
  }

  public final long getAmountNeeded() {
    return amountNeeded;
  }

  public void setAmountNeeded(final long amountNeeded) {
    this.amountNeeded = amountNeeded;
  }

  public void trigger(ActiveQuest activeQuest) { // or void completeTrigger() or finishTrigger()
    // execute action here
    final Player player = Bukkit.getPlayer(activeQuest.getQuestPlayer().getUniqueId());

    if (player != null) {
      activeQuest
          .getQuestPlayer()
          .sendDebugMessage(
              "Trigger: Executing action "
                  + action.getActionName()
                  + " for Quest "
                  + activeQuest.getQuest().getIdentifier());
      if (quest != null) {
        main.getActionRunner()
            .executeActionWithConditions(action, activeQuest.getQuestPlayer(), null, true, quest);
      } else {
        main.getActionRunner()
            .executeActionWithConditions(action, activeQuest.getQuestPlayer(), null, true);
      }
    } else {
      main.getLogManager().warn("Tried to execute trigger for offline player - ABORTED!");
    }
  }

  public abstract void save(final FileConfiguration configuration, final String initialPath);

  public abstract void load(final FileConfiguration configuration, final String initialPath);

  public abstract String getTriggerDescription();
}
