package com.notquests.paper.events.hooks;

import com.gamingmesh.jobs.api.JobsLevelUpEvent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import com.notquests.paper.NotQuests;
import com.notquests.paper.builtin.objectives.JobsRebornReachJobLevel;
import com.notquests.paper.structs.ActiveObjective;
import com.notquests.paper.structs.ActiveQuest;
import com.notquests.paper.structs.QuestPlayer;

public class JobsRebornEvents implements Listener {
    private final NotQuests main;

    public JobsRebornEvents(NotQuests main) {
        this.main = main;
        startLevelSyncTask();
    }

    /**
     * "Reach job level" objectives should always reflect the player's real job level, however they
     * reached it. {@link JobsLevelUpEvent} only fires on natural level-ups (admin commands such as
     * {@code /jobs level <player> <job> add} bypass it), so we additionally poll every few seconds
     * and re-sync the progress to the live level. {@code setProgress} is a no-op when nothing changed,
     * so this is cheap and never double-counts.
     */
    private void startLevelSyncTask() {
        Bukkit.getScheduler()
                .runTaskTimer(
                        main.getMain(),
                        () -> {
                            if (main.getDataManager().isDisabled()
                                    || !main.getIntegrationsManager().isJobsRebornEnabled()) {
                                return;
                            }
                            for (final Player player : Bukkit.getOnlinePlayers()) {
                                final QuestPlayer questPlayer =
                                        main.getQuestPlayerManager().getActiveQuestPlayer(player.getUniqueId());
                                if (questPlayer == null || questPlayer.getActiveQuests().isEmpty()) {
                                    continue;
                                }
                                syncObjectives(questPlayer);
                            }
                        },
                        60L,
                        60L);
    }

    private void syncObjectives(final QuestPlayer questPlayer) {
        for (final ActiveQuest activeQuest : questPlayer.getActiveQuests()) {
            for (final ActiveObjective activeObjective : activeQuest.getActiveObjectives()) {
                if (activeObjective.isUnlocked()
                        && JobsRebornReachJobLevel.isJobsRebornReachJobLevel(activeObjective)) {
                    JobsRebornReachJobLevel.updateProgressToCurrentLevel(main, activeObjective);
                }
            }
            activeQuest.removeCompletedObjectives(true);
        }
        questPlayer.removeCompletedQuests();
    }

    @EventHandler
    public void onJobsLevelUp(JobsLevelUpEvent e) {
        if (e.isCancelled()) {
            return;
        }
        final QuestPlayer questPlayer =
                main.getQuestPlayerManager().getActiveQuestPlayer(e.getPlayer().getUniqueId());
        if (questPlayer == null || questPlayer.getActiveQuests().isEmpty()) {
            return;
        }
        for (final ActiveQuest activeQuest : questPlayer.getActiveQuests()) {
            for (final ActiveObjective activeObjective : activeQuest.getActiveObjectives()) {
                if (activeObjective.isUnlocked()
                        && JobsRebornReachJobLevel.isJobsRebornReachJobLevel(activeObjective)) {
                    if (!JobsRebornReachJobLevel.matchesJob(activeObjective, e.getJob().getName())) {
                        continue;
                    }
                    if (JobsRebornReachJobLevel.countsPreviousLevels(activeObjective)) {
                        JobsRebornReachJobLevel.updateProgressToCurrentLevel(main, activeObjective);
                    } else {
                        activeObjective.addProgress(1);
                    }
                }
            }
            activeQuest.removeCompletedObjectives(true);
        }
        questPlayer.removeCompletedQuests();
    }
}
