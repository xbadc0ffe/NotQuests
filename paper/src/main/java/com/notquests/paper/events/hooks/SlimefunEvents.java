package com.notquests.paper.events.hooks;


import io.github.thebusybiscuit.slimefun4.api.events.PlayerPreResearchEvent;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import com.notquests.paper.NotQuests;
import com.notquests.paper.builtin.objectives.SlimefunResearch;
import com.notquests.paper.structs.ActiveObjective;
import com.notquests.paper.structs.ActiveQuest;
import com.notquests.paper.structs.QuestPlayer;

public class SlimefunEvents implements Listener {
    private final NotQuests main;


    public SlimefunEvents(NotQuests main) {
        this.main = main;
    }

    @EventHandler
    public void onPlayerResearch(final PlayerPreResearchEvent e) {
        if (!e.isCancelled()) {
            final Player player = e.getPlayer();
            final QuestPlayer questPlayer = main.getQuestPlayerManager().getActiveQuestPlayer(player.getUniqueId());
            if (questPlayer != null) {
                if (questPlayer.getActiveQuests().size() > 0) {
                    for (final ActiveQuest activeQuest : questPlayer.getActiveQuests()) {
                        for (final ActiveObjective activeObjective : activeQuest.getActiveObjectives()) {
                            if (activeObjective.isUnlocked()) {
                                if (SlimefunResearch.isSlimefunResearch(activeObjective)) {
                                    activeObjective.addProgress(e.getResearch().getCost());

                                }
                            }

                        }
                        activeQuest.removeCompletedObjectives(true);
                    }
                    questPlayer.removeCompletedQuests();
                }
            }
        }
    }


}
