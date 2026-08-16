package com.notquests.paper.builtin.conditions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import com.notquests.paper.NotQuests;
import com.notquests.paper.builtin.conditions.support.VariableConditionSupport;
import com.notquests.paper.commands.framework.NQArguments;
import com.notquests.paper.commands.framework.NQDescription;
import com.notquests.paper.conditions.ConditionCatalog;
import com.notquests.paper.registry.ConditionType;
import com.notquests.paper.registry.DefinedCondition;
import com.notquests.paper.registry.FieldTypes;
import com.notquests.paper.structs.QuestPlayer;
import com.notquests.paper.conditions.ConditionFor;
import com.notquests.paper.variables.Variable;
import com.notquests.paper.variables.VariableDataType;

import static com.notquests.paper.commands.arguments.variables.ListVariableArgument.listVariableArgument;
import static com.notquests.paper.builtin.conditions.support.VariableConditionSupport.ADDITIONAL_BOOLEANS;
import static com.notquests.paper.builtin.conditions.support.VariableConditionSupport.ADDITIONAL_NUMBERS;
import static com.notquests.paper.builtin.conditions.support.VariableConditionSupport.ADDITIONAL_STRINGS;
import static com.notquests.paper.builtin.conditions.support.VariableConditionSupport.EXPRESSION;
import static com.notquests.paper.builtin.conditions.support.VariableConditionSupport.OPERATOR;
import static com.notquests.paper.builtin.conditions.support.VariableConditionSupport.VARIABLE_NAME;

public final class ListVariableCondition {
    private ListVariableCondition() {}

    public static void register(final NotQuests main, final ConditionCatalog conditions) {
        conditions.condition("List")
                .displayName("List Variable")
                .description("Compares a list variable with a comma-separated list expression.")
                .withoutTypeLiteral()
                .field(VARIABLE_NAME, FieldTypes.text().config("specifics.variableName"), "List variable to check.")
                .field(OPERATOR, FieldTypes.text().config("specifics.operator"), "List comparison operator to use.")
                .field(EXPRESSION, FieldTypes.text().config("specifics.expression"), "Comma-separated list expression compared with the variable value.")
                .field(ADDITIONAL_STRINGS, FieldTypes.stringMap().config("specifics.additionalStrings"), "Extra text arguments passed to the variable.")
                .field(ADDITIONAL_NUMBERS, FieldTypes.numberExpressionMap().config("specifics.additionalNumbers"), "Extra number-expression arguments passed to the variable.")
                .field(ADDITIONAL_BOOLEANS, FieldTypes.numberExpressionMap().config("specifics.additionalBooleans"), "Extra boolean-expression arguments passed to the variable.")
                .commands((type, builder, conditionFor) -> registerCommands(main, type, builder, conditionFor))
                .singleLine((condition, arguments) ->
                        VariableConditionSupport.setSingleLineValues(main, condition, arguments, VariableDataType.LIST))
                .check((condition, questPlayer) -> check(main, condition.condition(), questPlayer))
                .conditionDescription((condition, questPlayer, objects) -> description(condition.condition()))
                .register();
    }

    private static void registerCommands(
            final NotQuests main,
            final ConditionType type,
            final com.notquests.paper.commands.framework.NQCommandBuilder builder,
            final ConditionFor conditionFor) {
        for (final String variableString : main.getVariableCatalog().getVariableIdentifiers()) {
            final Variable<?> variable = main.getVariableCatalog().getVariableFromString(variableString);
            if (!VariableConditionSupport.shouldRegister(main, variableString, VariableDataType.LIST)) {
                continue;
            }
            main.getCommandManager().getNQCommandManager().command(main.getVariableCatalog()
                    .registerVariableCommands(variableString, builder)
                    .required(
                            OPERATOR,
                            NQArguments.stringArgument(),
                            NQDescription.of("How to compare the " + variableString + " list variable with the supplied expression."),
                            (context, input) -> List.of("equals", "equalsIgnoreCase", "contains", "containsIgnoreCase"))
                    .required(
                            EXPRESSION,
                            listVariableArgument(EXPRESSION, variable),
                            NQDescription.of("List value or expression to compare with the current " + variableString + " value."))
                    .handler(context -> {
                        final DefinedCondition condition = type.createCondition();
                        VariableConditionSupport.setCommandValues(
                                main, condition, variable, context, context.get(OPERATOR), context.get(EXPRESSION));
                        main.getConditionCatalog().addCondition(condition, context, conditionFor);
                    }));
        }
    }

    private static String check(
            final NotQuests main, final DefinedCondition condition, final QuestPlayer questPlayer) {
        final Variable<?> variable = VariableConditionSupport.variable(main, condition);
        if (variable == null) {
            return "<ERROR>Error: variable <highlight>" + condition.text(VARIABLE_NAME) + "</highlight> not found. Report this to the Server owner.";
        }
        VariableConditionSupport.applyAdditionalArguments(variable, condition);
        final Object value = variable.getValue(questPlayer);
        if (value == null) {
            return "<YELLOW>You don't have any " + variable.getPlural() + "!";
        }
        final String[] current = asStringArray(value);
        final String[] required = required(condition);
        return switch (condition.text(OPERATOR)) {
            case "equals" -> Arrays.equals(current, required)
                    ? ""
                    : "<YELLOW>The " + variable.getPlural() + " need to be: <highlight>" + Arrays.toString(required) + "</highlight>.";
            case "equalsIgnoreCase" -> equalsIgnoreCase(current, required)
                    ? ""
                    : "<YELLOW>The " + variable.getPlural() + " need to be: <highlight>" + Arrays.toString(required) + "</highlight>.";
            case "contains" -> containsAll(current, required, false)
                    ? ""
                    : "<YELLOW>The " + variable.getPlural() + " need to contain: <highlight>" + Arrays.toString(required) + "</highlight>.";
            case "containsIgnoreCase" -> containsAll(current, required, true)
                    ? ""
                    : "<YELLOW>The " + variable.getPlural() + " need to contain: <highlight>" + Arrays.toString(required) + "</highlight>.";
            default -> "<ERROR>Error: variable operator <highlight>" + condition.text(OPERATOR) + "</highlight> is invalid. Report this to the Server owner.";
        };
    }

    private static String description(final DefinedCondition condition) {
        final String required = Arrays.toString(required(condition));
        return switch (condition.text(OPERATOR)) {
            case "equals" -> "<GRAY>-- " + condition.text(VARIABLE_NAME) + " needs to be equal " + required + "</GRAY>";
            case "equalsIgnoreCase" -> "<GRAY>-- " + condition.text(VARIABLE_NAME) + " needs to be equal " + required + " (case-insensitive)</GRAY>";
            case "contains" -> "<GRAY>-- " + condition.text(VARIABLE_NAME) + " needs to contain " + required + "</GRAY>";
            case "containsIgnoreCase" -> "<GRAY>-- " + condition.text(VARIABLE_NAME) + " needs to contain " + required + " (case-insensitive)</GRAY>";
            default -> "<GRAY>Error: invalid expression.</GRAY>";
        };
    }

    private static String[] required(final DefinedCondition condition) {
        return condition.text(EXPRESSION).split(",");
    }

    private static String[] asStringArray(final Object value) {
        if (value instanceof String[] array) {
            return array;
        }
        if (value instanceof ArrayList<?> list) {
            return list.toArray(new String[0]);
        }
        return (String[]) value;
    }

    private static boolean equalsIgnoreCase(final String[] current, final String[] required) {
        if (current.length != required.length) {
            return false;
        }
        for (int i = 0; i < current.length; i++) {
            if (!current[i].equalsIgnoreCase(required[i])) {
                return false;
            }
        }
        return true;
    }

    private static boolean containsAll(final String[] current, final String[] required, final boolean ignoreCase) {
        for (final String expected : required) {
            final boolean found = Arrays.stream(current)
                    .anyMatch(value -> ignoreCase ? value.equalsIgnoreCase(expected) : value.equals(expected));
            if (!found) {
                return false;
            }
        }
        return true;
    }
}
