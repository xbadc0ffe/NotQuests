package com.notquests.paper.builtin.actions;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Locale;
import java.util.Random;
import com.notquests.paper.NotQuests;
import com.notquests.paper.commands.arguments.ActionList;
import com.notquests.paper.actions.ActionCatalog;
import com.notquests.paper.registry.FieldTypes;
import com.notquests.paper.actions.Action;
import com.notquests.paper.conditions.Condition;

public final class ActionChain {
    private static final String ACTIONS = "actions";
    private static final String AMOUNT = "amount";
    private static final String IGNORE_CONDITIONS = "ignoreConditions";
    private static final String MIN_RANDOM = "minRandom";
    private static final String MAX_RANDOM = "maxRandom";
    private static final String EXECUTED_ACTION_DELAY = "executedActionDelay";
    private static final String ONLY_COUNT_RANDOM_IF_CONDITIONS_FULFILLED = "onlyCountForRandomIfConditionsFulfilled";

    private ActionChain() {}

    public static void register(final NotQuests main, final ActionCatalog actions) {
        actions.action("Action")
                .displayName("Action Chain")
                .description("Executes one or more saved actions, optionally multiple times or as a random subset.")
                .field(
                        ACTIONS,
                        FieldTypes.actionList().config("specifics.actions"),
                        "Saved actions to execute. Use one action name or a comma-separated list.")
                .field(
                        AMOUNT,
                        FieldTypes.integer(1).config("specifics.amount"),
                        "Number of times to execute the selected saved actions.")
                .flag(
                        IGNORE_CONDITIONS,
                        FieldTypes.presenceFlag().config("specifics.ignoreConditions"),
                        "Executes referenced actions without checking their attached conditions.")
                .flag(
                        MIN_RANDOM,
                        FieldTypes.integer(-1).config("specifics.minRandom"),
                        "Minimum number of referenced actions to randomly choose each time this action runs.")
                .flag(
                        MAX_RANDOM,
                        FieldTypes.integer(-1).config("specifics.maxRandom"),
                        "Maximum number of referenced actions to randomly choose each time this action runs.")
                .flag(
                        EXECUTED_ACTION_DELAY,
                        FieldTypes.duration(Duration.ofMillis(-1)).config("specifics.executedActionDelay"),
                        "Delay applied to each referenced action; overrides that action's own delay.")
                .flag(
                        ONLY_COUNT_RANDOM_IF_CONDITIONS_FULFILLED,
                        FieldTypes.presenceFlag().config("specifics.onlyCountForRandomIfConditionsFulfilled"),
                        "When choosing random actions, skip actions whose conditions fail without counting them toward the random limit.")
                .singleLine((action, arguments) -> {
                    action.setValue(ACTIONS, actionList(main, arguments.get(0)));
                    action.setValue(AMOUNT, arguments.size() >= 2 ? Integer.parseInt(arguments.get(1)) : 1);
                    action.setValue(
                            IGNORE_CONDITIONS,
                            String.join(" ", arguments).toLowerCase(Locale.ROOT).contains("--ignoreconditions"));
                })
                .execute((action, questPlayer, objects) -> {
                    final ActionList actionList = action.actionList(ACTIONS);
                    if (actionList == null || actionList.getValues().isEmpty()) {
                        main.getLogManager().warn("Tried to execute Action action with no valid referenced actions.");
                        return;
                    }
                    final ArrayList<Action> referencedActions = new ArrayList<>(actionList.getValues());
                    final int amount = Math.max(1, action.integer(AMOUNT, 1));
                    final boolean ignoreConditions = action.flag(IGNORE_CONDITIONS);
                    final int minRandom = action.integer(MIN_RANDOM, -1);
                    final int maxRandom = action.integer(MAX_RANDOM, -1);
                    final int delayMillis = (int) action.duration(EXECUTED_ACTION_DELAY, Duration.ofMillis(-1)).toMillis();

                    if (minRandom == -1 && maxRandom == -1) {
                        for (int run = 0; run < amount; run++) {
                            for (final Action referencedAction : referencedActions) {
                                executeReferenced(main, referencedAction, questPlayer, ignoreConditions, delayMillis, objects);
                            }
                        }
                        return;
                    }

                    final Random random = new Random();
                    for (int run = 0; run < amount; run++) {
                        Collections.shuffle(referencedActions);
                        final int low = Math.max(0, minRandom);
                        final int high = maxRandom < low ? low : maxRandom;
                        int amountToExecute = low == high ? low : random.nextInt(high + 1 - low) + low;
                        for (int i = 0; i < amountToExecute && i < referencedActions.size(); i++) {
                            final Action referencedAction = referencedActions.get(i);
                            if (!ignoreConditions
                                    && action.flag(ONLY_COUNT_RANDOM_IF_CONDITIONS_FULFILLED)
                                    && !conditionsFulfilled(referencedAction, questPlayer)) {
                                amountToExecute++;
                                continue;
                            }
                            executeReferenced(main, referencedAction, questPlayer, ignoreConditions, delayMillis, objects);
                        }
                    }
                })
                .actionDescription((action, questPlayer, objects) -> "Executes saved actions.")
                .register();
    }

    private static ActionList actionList(final NotQuests main, final String actionNames) {
        final ActionList actionList = new ActionList();
        for (final String actionName : actionNames.split(",")) {
            final Action action = main.getSavedActions().getAction(actionName);
            if (action != null) {
                actionList.addValue(action);
            } else {
                main.getLogManager().warn("Action chain references unknown action '" + actionName + "'.");
            }
        }
        return actionList;
    }

    private static void executeReferenced(
            final NotQuests main,
            final Action action,
            final com.notquests.paper.structs.QuestPlayer questPlayer,
            final boolean ignoreConditions,
            final int delayMillis,
            final Object... objects) {
        if (ignoreConditions) {
            action.execute(questPlayer, delayMillis, objects);
        } else {
            main.getActionRunner().executeActionWithConditions(action, questPlayer, null, true, delayMillis, objects);
        }
    }

    private static boolean conditionsFulfilled(
            final Action action, final com.notquests.paper.structs.QuestPlayer questPlayer) {
        for (final Condition condition : action.getConditions()) {
            if (!condition.check(questPlayer).fulfilled()) {
                return false;
            }
        }
        return true;
    }
}
