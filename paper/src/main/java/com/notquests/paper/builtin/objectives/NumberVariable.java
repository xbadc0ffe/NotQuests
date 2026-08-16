package com.notquests.paper.builtin.objectives;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import com.notquests.paper.NotQuests;
import com.notquests.paper.commands.arguments.variables.BooleanVariableValueParser;
import com.notquests.paper.commands.arguments.variables.NumberVariableValueParser;
import com.notquests.paper.commands.arguments.variables.StringVariableValueParser;
import com.notquests.paper.commands.framework.NQArguments;
import com.notquests.paper.commands.framework.NQCommandBuilder;
import com.notquests.paper.commands.framework.NQCommandContext;
import com.notquests.paper.commands.framework.NQDescription;
import com.notquests.paper.commands.framework.NQFlag;
import com.notquests.paper.managers.expressions.NumberExpression;
import com.notquests.paper.objectives.ObjectiveCatalog;
import com.notquests.paper.registry.DefinedObjective;
import com.notquests.paper.registry.FieldTypes;
import com.notquests.paper.registry.ObjectiveDataContext;
import com.notquests.paper.registry.ObjectiveType;
import com.notquests.paper.structs.ActiveObjective;
import com.notquests.paper.structs.QuestPlayer;
import com.notquests.paper.objectives.Objective;
import com.notquests.paper.variables.Variable;
import com.notquests.paper.variables.VariableDataType;

import static com.notquests.paper.commands.arguments.variables.NumberVariableArgument.numberVariableArgument;

public final class NumberVariable {
    private static final String TYPE = "NumberVariable";
    private static final String VARIABLE_NAME = "variableName";
    private static final String OPERATOR = "operator";
    private static final String AMOUNT = "amount";
    private static final String ADDITIONAL_STRINGS = "additionalStrings";
    private static final String ADDITIONAL_NUMBERS = "additionalNumbers";
    private static final String ADDITIONAL_BOOLEANS = "additionalBooleans";
    private static final String CHECK_ONLY_WHEN_VARIABLE_CHANGED = "checkOnlyWhenCorrespondingVariableValueChanged";

    private NumberVariable() {}

    public static void register(final NotQuests main, final ObjectiveCatalog objectives) {
        objectives.objective(TYPE)
                .displayName("Number Variable")
                .description("Counts when a number variable matches a configured comparison.")
                .field(
                        VARIABLE_NAME,
                        FieldTypes.text().config("specifics.variableName"),
                        "Number variable that should be checked.")
                .field(
                        OPERATOR,
                        FieldTypes.text().config("specifics.operator"),
                        "Comparison operator used for the number variable check.")
                .field(
                        AMOUNT,
                        FieldTypes.numberExpression(false).progressNeeded(),
                        "Target number expression the variable is compared against.")
                .field(
                        ADDITIONAL_STRINGS,
                        FieldTypes.stringMap().config("specifics.additionalStrings"),
                        "Extra text arguments passed to the variable.")
                .field(
                        ADDITIONAL_NUMBERS,
                        FieldTypes.numberExpressionMap().config("specifics.additionalNumbers"),
                        "Extra number-expression arguments passed to the variable.")
                .field(
                        ADDITIONAL_BOOLEANS,
                        FieldTypes.numberExpressionMap().config("specifics.additionalBooleans"),
                        "Extra boolean-expression arguments passed to the variable.")
                .field(
                        CHECK_ONLY_WHEN_VARIABLE_CHANGED,
                        FieldTypes.presenceFlag().config("specifics.checkOnlyWhenCorrespondingVariableValueChanged"),
                        "Whether this objective is checked only when the matching variable changes.")
                .commands((type, builder, level) -> registerCommands(main, type, builder, level))
                .taskDescription((objective, questPlayer, activeObjective) -> taskDescription(main, objective, questPlayer, activeObjective))
                .onUnlock((objective, activeObjective, startup) -> {
                    activeObjective.getQuestPlayer().setHasActiveVariableObjectives(true);
                    updateProgress(main, activeObjective);
                })
                .onCompleteOrLock((objective, activeObjective, startup, completed) ->
                        activeObjective.getQuestPlayer().setHasActiveVariableObjectives(false))
                .register();
    }

    private static void registerCommands(
            final NotQuests main,
            final ObjectiveType type,
            final NQCommandBuilder builder,
            final int level) {
        for (final String variableString : main.getVariableCatalog().getVariableIdentifiers()) {
            final Variable<?> variable = main.getVariableCatalog().getVariableFromString(variableString);
            if (variable == null
                    || !variable.isCanSetValue()
                    || variable.getVariableDataType() != VariableDataType.NUMBER
                    || main.getVariableCatalog().alreadyFullRegisteredVariables.contains(variableString)) {
                continue;
            }

            main.getCommandManager().getNQCommandManager().command(main.getVariableCatalog()
                    .registerVariableCommands(variableString, builder)
                    .required(
                            OPERATOR,
                            NQArguments.stringArgument(),
                            NQDescription.of("How to compare the " + variableString + " number variable against the target expression."),
                            (context, input) -> List.of(
                                    "equals", "lessThan", "moreThan", "moreOrEqualThan", "lessOrEqualThan"))
                    .required(
                            AMOUNT,
                            numberVariableArgument(AMOUNT, null, false),
                            NQDescription.of("Target number expression this " + variableString + " objective must reach."))
                    .flag(NQFlag.builder(
                                    CHECK_ONLY_WHEN_VARIABLE_CHANGED,
                                    NQDescription.of("Check this objective only when this variable's value changes via an action."))
                            .build())
                    .handler(context -> addObjective(main, type, context, level, variable)));
        }
    }

    private static void addObjective(
            final NotQuests main,
            final ObjectiveType type,
            final NQCommandContext context,
            final int level,
            final Variable<?> variable) {
        String amountExpression = context.get(AMOUNT);
        final String mathOperator = context.get(OPERATOR);
        if (mathOperator.equalsIgnoreCase("moreThan")) {
            amountExpression += "+1";
        }

        final DefinedObjective objective = type.createObjective();
        objective.setValue(VARIABLE_NAME, variable.getVariableType());
        objective.setValue(OPERATOR, mathOperator);
        objective.setValue(AMOUNT, amountExpression);
        objective.setProgressNeededExpression(amountExpression);
        objective.setValue(CHECK_ONLY_WHEN_VARIABLE_CHANGED, context.flags().isPresent(CHECK_ONLY_WHEN_VARIABLE_CHANGED));
        objective.setValue(ADDITIONAL_STRINGS, additionalStringArguments(variable, context));
        objective.setValue(ADDITIONAL_NUMBERS, additionalNumberArguments(main, variable, context));
        objective.setValue(ADDITIONAL_BOOLEANS, additionalBooleanArguments(main, variable, context));
        main.getObjectiveCatalog().addObjective(objective, context, level);
    }

    private static HashMap<String, String> additionalStringArguments(
            final Variable<?> variable,
            final NQCommandContext context) {
        final HashMap<String, String> values = new HashMap<>();
        for (final StringVariableValueParser<?> parser : variable.getRequiredStrings()) {
            values.put(parser.getIdentifier(), context.get(parser.getIdentifier()));
        }
        return values;
    }

    private static HashMap<String, NumberExpression> additionalNumberArguments(
            final NotQuests main,
            final Variable<?> variable,
            final NQCommandContext context) {
        final HashMap<String, NumberExpression> values = new HashMap<>();
        for (final NumberVariableValueParser<?> parser : variable.getRequiredNumbers()) {
            values.put(parser.getIdentifier(), new NumberExpression(main, context.get(parser.getIdentifier())));
        }
        return values;
    }

    private static HashMap<String, NumberExpression> additionalBooleanArguments(
            final NotQuests main,
            final Variable<?> variable,
            final NQCommandContext context) {
        final HashMap<String, NumberExpression> values = new HashMap<>();
        for (final BooleanVariableValueParser<?> parser : variable.getRequiredBooleans()) {
            values.put(parser.getIdentifier(), new NumberExpression(main, context.get(parser.getIdentifier())));
        }
        for (final NQFlag flag : variable.getRequiredBooleanFlags()) {
            values.put(
                    flag.name(),
                    context.flags().isPresent(flag.name())
                            ? NumberExpression.ofStatic(main, 1)
                            : NumberExpression.ofStatic(main, 0));
        }
        return values;
    }

    public static boolean isNumberVariable(final Objective objective) {
        return objective instanceof final DefinedObjective definedObjective && definedObjective.isType(TYPE);
    }

    public static boolean checkOnlyWhenCorrespondingVariableValueChanged(final Objective objective) {
        return objective instanceof final DefinedObjective definedObjective
                && Boolean.TRUE.equals(definedObjective.value(CHECK_ONLY_WHEN_VARIABLE_CHANGED, Boolean.class));
    }

    public static String variableName(final Objective objective) {
        return objective instanceof final DefinedObjective definedObjective ? definedObjective.text(VARIABLE_NAME) : "";
    }

    public static void updateProgress(final NotQuests main, final ActiveObjective activeObjective) {
        if (!(activeObjective.getObjective() instanceof final DefinedObjective objective) || !objective.isType(TYPE)) {
            return;
        }
        final QuestPlayer questPlayer = activeObjective.getQuestPlayer();
        final String variableName = objective.text(VARIABLE_NAME);
        questPlayer.sendDebugMessage("Updating progress for number variable objective. Variable: " + variableName);

        final Variable<?> variable = main.getVariableCatalog().getVariableFromString(variableName);
        if (variable == null) {
            questPlayer.sendDebugMessage("Variable is null.");
            return;
        }
        final HashMap<String, String> additionalStrings = stringMap(objective, ADDITIONAL_STRINGS);
        if (!additionalStrings.isEmpty()) {
            variable.setAdditionalStringArguments(additionalStrings);
        }
        final HashMap<String, NumberExpression> additionalNumbers = expressionMap(objective, ADDITIONAL_NUMBERS);
        if (!additionalNumbers.isEmpty()) {
            variable.setAdditionalNumberArguments(additionalNumbers);
        }
        final HashMap<String, NumberExpression> additionalBooleans = expressionMap(objective, ADDITIONAL_BOOLEANS);
        if (!additionalBooleans.isEmpty()) {
            variable.setAdditionalBooleanArguments(additionalBooleans);
        }

        final Object value = variable.getValue(questPlayer);
        final Double numberValue = asDouble(value);
        if (numberValue == null) {
            questPlayer.sendDebugMessage("Variable value is not numeric: " + value);
            return;
        }

        final double numberRequirement = activeObjective.getProgressNeeded();
        final String operator = objective.text(OPERATOR);
        if (operator.equalsIgnoreCase("moreThan") || operator.equalsIgnoreCase("moreOrEqualThan")) {
            activeObjective.setProgress(numberValue, false);
        } else if (operator.equalsIgnoreCase("lessThan")) {
            if (numberValue < numberRequirement) {
                activeObjective.setProgress(activeObjective.getProgressNeeded(), false);
            }
        } else if (operator.equalsIgnoreCase("lessOrEqualThan")) {
            if (numberValue <= numberRequirement) {
                activeObjective.setProgress(activeObjective.getProgressNeeded(), false);
            }
        } else if (operator.equalsIgnoreCase("equals")) {
            if (numberValue == numberRequirement) {
                activeObjective.setProgress(activeObjective.getProgressNeeded(), false);
            } else if (numberValue < numberRequirement) {
                activeObjective.setProgress(numberValue, false);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static HashMap<String, String> stringMap(final DefinedObjective objective, final String field) {
        final HashMap<String, String> values = objective.value(field, HashMap.class);
        return values == null ? new HashMap<>() : values;
    }

    @SuppressWarnings("unchecked")
    private static HashMap<String, NumberExpression> expressionMap(final DefinedObjective objective, final String field) {
        final HashMap<String, NumberExpression> values = objective.value(field, HashMap.class);
        return values == null ? new HashMap<>() : values;
    }

    private static Double asDouble(final Object value) {
        return value instanceof Number number ? number.doubleValue() : null;
    }

    private static String taskDescription(
            final NotQuests main,
            final ObjectiveDataContext objective,
            final QuestPlayer questPlayer,
            final ActiveObjective activeObjective) {
        final String variableName = objective.text(VARIABLE_NAME);
        if (variableName.isBlank()) {
            return "<YELLOW>Error: Variable not found.";
        }
        final double expressionValue =
                activeObjective != null
                        ? activeObjective.getProgressNeeded()
                        : new NumberExpression(main, objective.text(AMOUNT)).calculateValue(questPlayer);
        final String operator = objective.text(OPERATOR);
        if (operator.equalsIgnoreCase("moreThan")) {
            return "<GRAY>-- " + variableName + " needed: More than " + (expressionValue - 1) + "</GRAY>";
        }
        if (operator.equalsIgnoreCase("moreOrEqualThan")) {
            return "<GRAY>-- " + variableName + " needed: More or equal than " + expressionValue + "</GRAY>";
        }
        if (operator.equalsIgnoreCase("lessThan")) {
            return "<GRAY>-- " + variableName + " needed: Less than " + expressionValue + "</GRAY>";
        }
        if (operator.equalsIgnoreCase("lessOrEqualThan")) {
            return "<GRAY>-- " + variableName + " needed: Less or equal than" + expressionValue + "</GRAY>";
        }
        if (operator.equalsIgnoreCase("equals")) {
            return "<GRAY>-- " + variableName + " needed: Exactly " + expressionValue + "</GRAY>";
        }
        return "<GRAY>-- " + variableName + " needed: " + expressionValue + "</GRAY>";
    }
}
