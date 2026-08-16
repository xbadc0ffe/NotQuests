package com.notquests.paper.builtin.conditions;

import java.util.ArrayList;
import java.util.List;
import com.notquests.paper.NotQuests;
import com.notquests.paper.commands.framework.NQDescription;
import com.notquests.paper.conditions.ConditionCatalog;
import com.notquests.paper.registry.ConditionType;
import com.notquests.paper.registry.DefinedCondition;
import com.notquests.paper.registry.FieldTypes;
import com.notquests.paper.structs.ActiveQuest;
import com.notquests.paper.structs.Quest;
import com.notquests.paper.conditions.ConditionFor;
import com.notquests.paper.objectives.Objective;
import com.notquests.paper.objectives.ObjectiveHolder;

import static com.notquests.paper.commands.arguments.ObjectiveArgument.findObjective;
import static com.notquests.paper.commands.arguments.ObjectiveArgument.objectiveArgument;

public final class CompletedObjective {
    private static final String OBJECTIVE_ID = "dependingObjectiveId";

    private CompletedObjective() {}

    public static void register(final NotQuests main, final ConditionCatalog conditions) {
        conditions.condition("CompletedObjective")
                .displayName("Completed Objective")
                .description("Checks whether another objective in the same quest has already been completed.")
                .field(
                        OBJECTIVE_ID,
                        FieldTypes.integer(-1).config("specifics.objectiveID"),
                        "Objective ID that must already be completed before this condition is fulfilled.")
                .commands((type, builder, conditionFor) -> registerCommands(main, type, builder, conditionFor))
                .singleLine((condition, arguments) ->
                        condition.setValue(OBJECTIVE_ID, Integer.parseInt(arguments.get(0))))
                .check((condition, questPlayer) -> {
                    final int objectiveId = condition.integer(OBJECTIVE_ID, -1);
                    final ObjectiveHolder holder = condition.condition().getObjectiveHolder();
                    final Objective objectiveToComplete =
                            holder == null ? null : holder.getObjectiveFromID(objectiveId);
                    if (objectiveToComplete == null) {
                        return "<RED>Error: Cannot find objective you have to complete first.";
                    }
                    if (holder == null) {
                        return "<RED>Error: Cannot find current quest.";
                    }
                    if (holder instanceof final Quest quest) {
                        final ActiveQuest activeQuest = questPlayer.getActiveQuest(quest);
                        if (activeQuest == null) {
                            return "<RED>Error: Cannot find current active quest.";
                        }
                        if (activeQuest.getActiveObjectiveFromID(objectiveId) != null) {
                            return "<YELLOW>Finish the following objective first: <highlight>"
                                    + objectiveToComplete.getDisplayNameOrIdentifier();
                        }
                    } else {
                        return "objectiveHolder is no Quest";
                    }
                    return "";
                })
                .conditionDescription((condition, questPlayer, objects) -> {
                    final int objectiveId = condition.integer(OBJECTIVE_ID, -1);
                    final ObjectiveHolder holder = condition.condition().getObjectiveHolder();
                    final Objective otherObjective = holder == null ? null : holder.getObjectiveFromID(objectiveId);
                    if (otherObjective != null) {
                        return "<GRAY>-- Finish Objective first: " + otherObjective.getDisplayNameOrIdentifier();
                    }
                    return "<GRAY>-- Finish otherObjective first: " + objectiveId;
                })
                .register();
    }

    private static void registerCommands(
            final NotQuests main,
            final ConditionType type,
            final com.notquests.paper.commands.framework.NQCommandBuilder builder,
            final ConditionFor conditionFor) {
        if (conditionFor != ConditionFor.OBJECTIVEUNLOCK
                && conditionFor != ConditionFor.OBJECTIVEPROGRESS
                && conditionFor != ConditionFor.OBJECTIVECOMPLETE) {
            return;
        }
        main.getCommandManager().getNQCommandManager().command(builder
                .required(
                        OBJECTIVE_ID,
                        objectiveArgument(main, 0),
                        NQDescription.of("Objective ID that must already be completed before this condition is fulfilled."),
                        (context, input) -> {
                            final List<String> completions = new ArrayList<>();
                            final Quest quest = context.get("quest");
                            final Objective currentObjective =
                                    main.getCommandManager().getObjectiveFromContextAndLevel(context, 0);
                            for (final Objective objective : quest.getObjectives()) {
                                if (objective.getObjectiveID() != currentObjective.getObjectiveID()) {
                                    completions.add(String.valueOf(objective.getObjectiveID()));
                                }
                            }
                            return completions;
                        })
                .handler(context -> {
                    final Quest quest = context.get("quest");
                    final Objective currentObjective =
                            main.getCommandManager().getObjectiveFromContextAndLevel(context, 0);
                    final Objective dependingObjective = findObjective(quest, context.get(OBJECTIVE_ID));
                    if (dependingObjective == currentObjective) {
                        context.sender().sendMessage(main.parse("<error>Error: You cannot set an objective to depend on itself!"));
                        return;
                    }
                    final DefinedCondition condition = type.createCondition();
                    condition.setValue(OBJECTIVE_ID, dependingObjective.getObjectiveID());
                    main.getConditionCatalog().addCondition(condition, context, conditionFor);
                }));
    }
}
