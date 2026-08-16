package com.notquests.paper.builtin.objectives;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import com.gamingmesh.jobs.Jobs;
import com.gamingmesh.jobs.container.Job;
import com.gamingmesh.jobs.container.JobProgression;
import com.gamingmesh.jobs.container.JobsPlayer;
import com.notquests.paper.NotQuests;
import com.notquests.paper.commands.arguments.variables.NumberVariableArgument;
import com.notquests.paper.commands.framework.NQArguments;
import com.notquests.paper.commands.framework.NQCommandBuilder;
import com.notquests.paper.commands.framework.NQCommandContext;
import com.notquests.paper.commands.framework.NQDescription;
import com.notquests.paper.commands.framework.NQFlag;
import com.notquests.paper.objectives.ObjectiveCatalog;
import com.notquests.paper.registry.DefinedObjective;
import com.notquests.paper.registry.FieldTypes;
import com.notquests.paper.registry.ObjectiveDataContext;
import com.notquests.paper.registry.ObjectiveType;
import com.notquests.paper.structs.ActiveObjective;

public final class JobsRebornReachJobLevel {
    private static final String TYPE = "JobsRebornReachJobLevel";
    private static final String JOB_NAME = "jobName";
    private static final String LEVEL = "level";
    private static final String DO_NOT_COUNT_PREVIOUS_LEVELS = "doNotCountPreviousLevels";

    private JobsRebornReachJobLevel() {}

    public static void register(final NotQuests main, final ObjectiveCatalog objectives) {
        if (!main.getIntegrationsManager().isJobsRebornEnabled()) {
            return;
        }
        objectives.objective(TYPE)
                .displayName("Reach Jobs Reborn Level")
                .description("Counts when the player reaches a target level in a Jobs Reborn job.")
                .field(JOB_NAME, FieldTypes.text(), "Jobs Reborn job the player must level.")
                .field(
                        LEVEL,
                        FieldTypes.numberExpression(false).progressNeeded(),
                        "Target job level the player must reach.")
                .flag(
                        DO_NOT_COUNT_PREVIOUS_LEVELS,
                        FieldTypes.presenceFlag().invertedBooleanConfig("specifics.countPreviousLevels"),
                        "Only count job levels gained after this objective unlocks; existing levels do not count.")
                .commands((type, builder, level) -> registerCommands(main, type, builder, level))
                .taskDescription((objective, questPlayer, activeObjective) -> taskDescription(main, objective, questPlayer, activeObjective))
                .onUnlock((objective, activeObjective, startup) -> {
                    if (startup) {
                        return;
                    }
                    if (countsPreviousLevels(objective)) {
                        updateProgressToCurrentLevel(main, activeObjective);
                    } else if (activeObjective.getCurrentProgress() == 0) {
                        activeObjective.addProgress(1);
                    }
                })
                .afterLoad((objective, context) -> {
                    final Job job = Jobs.getJob(objective.text(JOB_NAME));
                    if (job == null) {
                        main.getLogManager()
                                .warn("The job <highlight>" + objective.text(JOB_NAME) + "</highlight> does not exist.");
                    }
                })
                .register();
    }

    private static void registerCommands(
            final NotQuests main,
            final ObjectiveType type,
            final NQCommandBuilder builder,
            final int objectiveLevel) {
        final NQFlag doNotCountPreviousLevels = NQFlag.builder(
                        DO_NOT_COUNT_PREVIOUS_LEVELS,
                        NQDescription.of("Only count job levels gained after this objective unlocks; existing levels do not count."))
                .build();
        main.getCommandManager().getNQCommandManager().command(builder
                .required("Job Name", NQArguments.stringArgument(), NQDescription.of("Jobs Reborn job the player must level."), (context, input) -> {
                    final List<String> completions = new ArrayList<>();
                    for (final Job job : Jobs.getJobs()) {
                        completions.add(job.getName());
                    }
                    return completions;
                })
                .required(
                        LEVEL,
                        NumberVariableArgument.numberVariableArgument(LEVEL, null, false),
                        NQDescription.of("Target job level the player must reach."))
                .flag(doNotCountPreviousLevels)
                .handler(context -> addObjective(main, type, context, objectiveLevel, doNotCountPreviousLevels)));
    }

    private static void addObjective(
            final NotQuests main,
            final ObjectiveType type,
            final NQCommandContext context,
            final int objectiveLevel,
            final NQFlag doNotCountPreviousLevels) {
        final String jobName = context.get("Job Name");
        if (Jobs.getJob(jobName) == null) {
            context.sender()
                    .sendMessage(main.parse("<error>Error: The Job with the name <highlight>"
                            + jobName
                            + "</highlight> was not found!"));
            return;
        }

        final String level = context.get(LEVEL);
        final DefinedObjective objective = type.createObjective();
        objective.setValue(JOB_NAME, jobName);
        objective.setValue(LEVEL, level);
        objective.setProgressNeededExpression(level);
        objective.setValue(DO_NOT_COUNT_PREVIOUS_LEVELS, context.flags().isPresent(doNotCountPreviousLevels));
        main.getObjectiveCatalog().addObjective(objective, context, objectiveLevel);
    }

    public static boolean isJobsRebornReachJobLevel(final ActiveObjective activeObjective) {
        return activeObjective.getObjective() instanceof final DefinedObjective objective && objective.isType(TYPE);
    }

    public static boolean matchesJob(final ActiveObjective activeObjective, final String jobName) {
        return activeObjective.getObjective() instanceof final DefinedObjective objective
                && objective.isType(TYPE)
                && objective.text(JOB_NAME).equalsIgnoreCase(jobName);
    }

    public static boolean countsPreviousLevels(final DefinedObjective objective) {
        return !Boolean.TRUE.equals(objective.value(DO_NOT_COUNT_PREVIOUS_LEVELS, Boolean.class));
    }

    public static boolean countsPreviousLevels(final ActiveObjective activeObjective) {
        return activeObjective.getObjective() instanceof final DefinedObjective objective && countsPreviousLevels(objective);
    }

    public static void updateProgressToCurrentLevel(final NotQuests main, final ActiveObjective activeObjective) {
        if (!main.getIntegrationsManager().isJobsRebornEnabled()
                || !(activeObjective.getObjective() instanceof final DefinedObjective objective)
                || !objective.isType(TYPE)
                || !countsPreviousLevels(objective)) {
            return;
        }
        final Job job = Jobs.getJob(objective.text(JOB_NAME));
        if (job == null) {
            return;
        }
        final JobsPlayer jobsPlayer =
                Jobs.getPlayerManager().getJobsPlayer(activeObjective.getQuestPlayer().getUniqueId());
        if (jobsPlayer == null) {
            return;
        }
        final JobProgression jobProgression = jobsPlayer.getJobProgression(job);
        activeObjective.setProgress(jobProgression == null ? 0 : jobProgression.getLevel(), true);
    }

    private static String taskDescription(
            final NotQuests main,
            final ObjectiveDataContext objective,
            final com.notquests.paper.structs.QuestPlayer questPlayer,
            final ActiveObjective activeObjective) {
        return main.getLanguageManager()
                .getString(
                        "chat.objectives.taskDescription.jobsRebornReachJobLevel.base",
                        questPlayer,
                        Map.of(
                                "%AMOUNT%",
                                ""
                                        + (activeObjective != null
                                                ? activeObjective.getProgressNeeded()
                                                : objective.text(LEVEL)),
                                "%JOB%",
                                objective.text(JOB_NAME)));
    }
}
