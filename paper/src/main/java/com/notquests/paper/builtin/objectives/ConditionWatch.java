package com.notquests.paper.builtin.objectives;

import com.notquests.paper.NotQuests;
import com.notquests.paper.builtin.conditions.support.VariableConditionSupport;
import com.notquests.paper.objectives.ObjectiveCatalog;
import com.notquests.paper.registry.DefinedCondition;
import com.notquests.paper.registry.DefinedObjective;
import com.notquests.paper.registry.FieldTypes;
import com.notquests.paper.structs.ActiveObjective;
import com.notquests.paper.structs.QuestPlayer;
import com.notquests.paper.conditions.Condition;
import com.notquests.paper.objectives.Objective;

public final class ConditionWatch {
    private ConditionWatch() {}

    public static final String TYPE = "Condition";
    private static final String CONDITION = "condition";
    private static final String CHECK_ONLY_WHEN_VARIABLE_CHANGED = "checkOnlyWhenCorrespondingVariableValueChanged";

    public static boolean isConditionWatch(final Objective objective) {
        return objective instanceof DefinedObjective defined && defined.isType(TYPE);
    }

    public static Condition condition(final Objective objective) {
        return objective instanceof DefinedObjective defined && defined.isType(TYPE)
                ? defined.value(CONDITION, Condition.class)
                : null;
    }

    public static boolean checkOnlyWhenCorrespondingVariableValueChanged(final Objective objective) {
        return objective instanceof DefinedObjective defined
                && defined.isType(TYPE)
                && Boolean.TRUE.equals(defined.value(CHECK_ONLY_WHEN_VARIABLE_CHANGED, Boolean.class));
    }

    public static void updateProgressIfFulfilled(final ActiveObjective activeObjective, final QuestPlayer questPlayer) {
        final Condition condition = condition(activeObjective.getObjective());
        if (condition != null && condition.check(questPlayer).fulfilled()) {
            activeObjective.addProgress(1);
        }
    }

    public static String watchedVariableName(final Objective objective) {
        final Condition condition = condition(objective);
        if (condition instanceof final DefinedCondition definedCondition
                && (definedCondition.definition().id().equals("Number")
                || definedCondition.definition().id().equals("String")
                || definedCondition.definition().id().equals("Boolean")
                || definedCondition.definition().id().equals("List")
                || definedCondition.definition().id().equals("ItemStackList"))) {
            return definedCondition.text(VariableConditionSupport.VARIABLE_NAME);
        }
        return "";
    }

    public static void register(final NotQuests main, final ObjectiveCatalog objectives) {
        objectives.objective(TYPE)
                .displayName("Condition")
                .description("Completes when a saved NotQuests condition is fulfilled.")
                .field(
                        CONDITION,
                        FieldTypes.condition().config("specifics.condition"),
                        "Saved condition this objective should watch.")
                .flag(
                        CHECK_ONLY_WHEN_VARIABLE_CHANGED,
                        FieldTypes.presenceFlag().config("specifics.checkOnlyWhenCorrespondingVariableValueChanged"),
                        "Only check this objective when the corresponding variable value changes instead of on the regular condition timer.")
                .taskDescription((objective, questPlayer, activeObjective) -> {
                    final Condition condition = objective.value(CONDITION, Condition.class);
                    if (condition == null) {
                        return "<YELLOW>Error: Condition not found.";
                    }
                    return condition.isHidden(questPlayer)
                            ? "Hidden"
                            : condition.getConditionDescription(
                                    questPlayer,
                                    activeObjective == null ? new Object[0] : new Object[] {activeObjective.getObjectiveHolder()});
                })
                .onUnlock((objective, activeObjective, loading) ->
                        activeObjective.getQuestPlayer().setHasActiveConditionObjectives(true))
                .onCompleteOrLock((objective, activeObjective, loading, completed) ->
                        activeObjective.getQuestPlayer().setHasActiveConditionObjectives(false))
                .register();
    }
}
