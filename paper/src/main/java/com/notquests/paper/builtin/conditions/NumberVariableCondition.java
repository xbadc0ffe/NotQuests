package com.notquests.paper.builtin.conditions;

import java.util.List;
import com.notquests.paper.NotQuests;
import com.notquests.paper.builtin.conditions.support.VariableConditionSupport;
import com.notquests.paper.commands.framework.NQArguments;
import com.notquests.paper.commands.framework.NQDescription;
import com.notquests.paper.managers.expressions.NumberExpression;
import com.notquests.paper.conditions.ConditionCatalog;
import com.notquests.paper.registry.ConditionType;
import com.notquests.paper.registry.DefinedCondition;
import com.notquests.paper.registry.FieldTypes;
import com.notquests.paper.structs.QuestPlayer;
import com.notquests.paper.conditions.ConditionFor;
import com.notquests.paper.variables.Variable;
import com.notquests.paper.variables.VariableDataType;

import static com.notquests.paper.commands.arguments.variables.NumberVariableArgument.numberVariableArgument;
import static com.notquests.paper.builtin.conditions.support.VariableConditionSupport.ADDITIONAL_BOOLEANS;
import static com.notquests.paper.builtin.conditions.support.VariableConditionSupport.ADDITIONAL_NUMBERS;
import static com.notquests.paper.builtin.conditions.support.VariableConditionSupport.ADDITIONAL_STRINGS;
import static com.notquests.paper.builtin.conditions.support.VariableConditionSupport.EXPRESSION;
import static com.notquests.paper.builtin.conditions.support.VariableConditionSupport.OPERATOR;
import static com.notquests.paper.builtin.conditions.support.VariableConditionSupport.VARIABLE_NAME;

public final class NumberVariableCondition {
    private NumberVariableCondition() {}

    public static void register(final NotQuests main, final ConditionCatalog conditions) {
        conditions.condition("Number")
                .displayName("Number Variable")
                .description("Compares a number variable with a number expression.")
                .withoutTypeLiteral()
                .field(VARIABLE_NAME, FieldTypes.text().config("specifics.variableName"), "Number variable to check.")
                .field(OPERATOR, FieldTypes.text().config("specifics.operator"), "Number comparison operator to use.")
                .field(
                        EXPRESSION,
                        FieldTypes.numberExpression().config("specifics.expression"),
                        "Number expression compared with the variable value.")
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
                .commands((type, builder, conditionFor) -> registerCommands(main, type, builder, conditionFor))
                .singleLine((condition, arguments) ->
                        VariableConditionSupport.setSingleLineValues(main, condition, arguments, VariableDataType.NUMBER))
                .check((condition, questPlayer) -> check(main, condition.condition(), questPlayer))
                .conditionDescription((condition, questPlayer, objects) -> description(main, condition.condition(), questPlayer))
                .register();
    }

    private static void registerCommands(
            final NotQuests main,
            final ConditionType type,
            final com.notquests.paper.commands.framework.NQCommandBuilder builder,
            final ConditionFor conditionFor) {
        for (final String variableString : main.getVariableCatalog().getVariableIdentifiers()) {
            final Variable<?> variable = main.getVariableCatalog().getVariableFromString(variableString);
            if (!VariableConditionSupport.shouldRegister(main, variableString, VariableDataType.NUMBER)) {
                continue;
            }
            main.getCommandManager().getNQCommandManager().command(main.getVariableCatalog()
                    .registerVariableCommands(variableString, builder)
                    .required(
                            OPERATOR,
                            NQArguments.stringArgument(),
                            NQDescription.of("How to compare the " + variableString + " number variable against the expression."),
                            (context, input) -> List.of(
                                    "equals", "lessThan", "moreThan", "moreOrEqualThan", "lessOrEqualThan"))
                    .required(
                            "amount",
                            numberVariableArgument("amount", null),
                            NQDescription.of("Number expression to compare with the current " + variableString + " value."))
                    .handler(context -> {
                        final DefinedCondition condition = type.createCondition();
                        VariableConditionSupport.setCommandValues(
                                main, condition, variable, context, context.get(OPERATOR), context.get("amount"));
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
        if (!(value instanceof Number number)) {
            return "<ERROR>Error: variable <highlight>" + condition.text(VARIABLE_NAME) + "</highlight> is not a number.";
        }
        final double current = number.doubleValue();
        final double required = new NumberExpression(main, condition.text(EXPRESSION)).calculateValue(questPlayer);
        return switch (condition.text(OPERATOR)) {
            case "moreThan" -> current > required
                    ? ""
                    : "<YELLOW>You need <highlight>" + (required + 1 - current) + "</highlight> more " + variable.getPlural() + ".";
            case "moreOrEqualThan" -> current >= required
                    ? ""
                    : "<YELLOW>You need <highlight>" + (required - current) + "</highlight> more " + variable.getPlural() + ".";
            case "lessThan" -> current < required
                    ? ""
                    : "<YELLOW>You have <highlight>" + (current + 1 - required) + "</highlight> too many " + variable.getPlural() + ".";
            case "lessOrEqualThan" -> current <= required
                    ? ""
                    : "<YELLOW>You have <highlight>" + (current - required) + "</highlight> too many " + variable.getPlural() + ".";
            case "equals" -> Double.compare(current, required) == 0
                    ? ""
                    : "<YELLOW>You need EXACTLY <highlight>" + required + "</highlight> " + variable.getPlural() + " - no more or less.";
            default -> "<ERROR>Error: variable operator <highlight>" + condition.text(OPERATOR) + "</highlight> is invalid. Report this to the Server owner.";
        };
    }

    private static String description(
            final NotQuests main, final DefinedCondition condition, final QuestPlayer questPlayer) {
        final double required = new NumberExpression(main, condition.text(EXPRESSION)).calculateValue(questPlayer);
        return switch (condition.text(OPERATOR)) {
            case "moreThan" -> "<GRAY>-- " + condition.text(VARIABLE_NAME) + " needed: More than " + required + "</GRAY>";
            case "moreOrEqualThan" -> "<GRAY>-- " + condition.text(VARIABLE_NAME) + " needed: More or equal than " + required + "</GRAY>";
            case "lessThan" -> "<GRAY>-- " + condition.text(VARIABLE_NAME) + " needed: Less than " + required + "</GRAY>";
            case "lessOrEqualThan" -> "<GRAY>-- " + condition.text(VARIABLE_NAME) + " needed: Less or equal than " + required + "</GRAY>";
            case "equals" -> "<GRAY>-- " + condition.text(VARIABLE_NAME) + " needed: Exactly " + required + "</GRAY>";
            default -> "<GRAY>-- " + condition.text(VARIABLE_NAME) + " needed: " + required + "</GRAY>";
        };
    }
}
