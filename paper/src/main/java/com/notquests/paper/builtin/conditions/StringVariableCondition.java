package com.notquests.paper.builtin.conditions;

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

import static com.notquests.paper.commands.arguments.variables.StringVariableArgument.stringVariableArgument;
import static com.notquests.paper.builtin.conditions.support.VariableConditionSupport.ADDITIONAL_BOOLEANS;
import static com.notquests.paper.builtin.conditions.support.VariableConditionSupport.ADDITIONAL_NUMBERS;
import static com.notquests.paper.builtin.conditions.support.VariableConditionSupport.ADDITIONAL_STRINGS;
import static com.notquests.paper.builtin.conditions.support.VariableConditionSupport.EXPRESSION;
import static com.notquests.paper.builtin.conditions.support.VariableConditionSupport.OPERATOR;
import static com.notquests.paper.builtin.conditions.support.VariableConditionSupport.VARIABLE_NAME;

public final class StringVariableCondition {
    private StringVariableCondition() {}

    public static void register(final NotQuests main, final ConditionCatalog conditions) {
        conditions.condition("String")
                .displayName("String Variable")
                .description("Compares a string variable with a text value.")
                .withoutTypeLiteral()
                .field(VARIABLE_NAME, FieldTypes.text().config("specifics.variableName"), "String variable to check.")
                .field(OPERATOR, FieldTypes.text().config("specifics.operator"), "String comparison operator to use.")
                .field(EXPRESSION, FieldTypes.text().config("specifics.string"), "Text value compared with the variable value.")
                .field(ADDITIONAL_STRINGS, FieldTypes.stringMap().config("specifics.additionalStrings"), "Extra text arguments passed to the variable.")
                .field(ADDITIONAL_NUMBERS, FieldTypes.numberExpressionMap().config("specifics.additionalNumbers"), "Extra number-expression arguments passed to the variable.")
                .field(ADDITIONAL_BOOLEANS, FieldTypes.numberExpressionMap().config("specifics.additionalBooleans"), "Extra boolean-expression arguments passed to the variable.")
                .commands((type, builder, conditionFor) -> registerCommands(main, type, builder, conditionFor))
                .singleLine((condition, arguments) -> {
                    VariableConditionSupport.setSingleLineValues(main, condition, arguments, VariableDataType.STRING);
                    condition.setValue(EXPRESSION, condition.text(EXPRESSION).replace("__", " "));
                })
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
            if (!VariableConditionSupport.shouldRegister(main, variableString, VariableDataType.STRING)) {
                continue;
            }
            main.getCommandManager().getNQCommandManager().command(main.getVariableCatalog()
                    .registerVariableCommands(variableString, builder)
                    .required(
                            OPERATOR,
                            NQArguments.stringArgument(),
                            NQDescription.of("How to compare the " + variableString + " string variable with the supplied value."),
                            (context, input) -> List.of(
                                    "equals", "equalsIgnoreCase", "contains", "startsWith", "endsWith", "isEmpty"))
                    .required(
                            "string",
                            stringVariableArgument("string", variable),
                            NQDescription.of("String value or expression to compare with the current " + variableString + " value."))
                    .handler(context -> {
                        final DefinedCondition condition = type.createCondition();
                        final String expression = context.<String>get("string").replace("__", " ");
                        VariableConditionSupport.setCommandValues(
                                main, condition, variable, context, context.get(OPERATOR), expression);
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
        final Object value = variable.getValue(questPlayer, questPlayer);
        if (!(value instanceof String current)) {
            return "<ERROR>Error: variable <highlight>" + condition.text(VARIABLE_NAME) + "</highlight> is not a String.";
        }
        final String required = condition.text(EXPRESSION);
        return switch (condition.text(OPERATOR)) {
            case "equals" -> current.equals(required) ? "" : "<YELLOW><highlight>" + variable.getSingular() + "</highlight> needs to equal " + required + ".";
            case "equalsIgnoreCase" -> current.equalsIgnoreCase(required) ? "" : "<YELLOW><highlight>" + variable.getSingular() + "</highlight> needs to equal (case-insensitive) " + required + ".";
            case "contains" -> current.contains(required) ? "" : "<YELLOW><highlight>" + variable.getSingular() + "</highlight> needs to contain " + required + ".";
            case "startsWith" -> current.startsWith(required) ? "" : "<YELLOW><highlight>" + variable.getSingular() + "</highlight> needs to start with " + required + ".";
            case "endsWith" -> current.endsWith(required) ? "" : "<YELLOW><highlight>" + variable.getSingular() + "</highlight> needs to end with " + required + ".";
            case "isEmpty" -> current.isBlank() ? "" : "<YELLOW><highlight>" + variable.getSingular() + "</highlight> needs to be empty.";
            default -> "<ERROR>Error: variable operator <highlight>" + condition.text(OPERATOR) + "</highlight> is invalid. Report this to the Server owner.";
        };
    }

    private static String description(final DefinedCondition condition) {
        final String variableName = condition.text(VARIABLE_NAME);
        final String value = condition.text(EXPRESSION);
        return switch (condition.text(OPERATOR)) {
            case "equals" -> "<GRAY>-- " + variableName + " needs to be equal to " + value + "</GRAY>";
            case "equalsIgnoreCase" -> "<GRAY>-- " + variableName + " needs to be equal (case-insensitive) to " + value + "</GRAY>";
            case "contains" -> "<GRAY>-- " + variableName + " needs to contain " + value + "</GRAY>";
            case "startsWith" -> "<GRAY>-- " + variableName + " needs to start with " + value + "</GRAY>";
            case "endsWith" -> "<GRAY>-- " + variableName + " needs to end with " + value + "</GRAY>";
            case "isEmpty" -> "<GRAY>-- " + variableName + " needs to be empty</GRAY>";
            default -> "<GRAY>-- Invalid String Operator";
        };
    }
}
