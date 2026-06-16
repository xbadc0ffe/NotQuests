package com.notquests.paper.builtin.conditions;

import java.util.List;
import java.util.Map;
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

import static com.notquests.paper.commands.arguments.variables.BooleanVariableArgument.booleanVariableArgument;
import static com.notquests.paper.builtin.conditions.support.VariableConditionSupport.ADDITIONAL_BOOLEANS;
import static com.notquests.paper.builtin.conditions.support.VariableConditionSupport.ADDITIONAL_NUMBERS;
import static com.notquests.paper.builtin.conditions.support.VariableConditionSupport.ADDITIONAL_STRINGS;
import static com.notquests.paper.builtin.conditions.support.VariableConditionSupport.EXPRESSION;
import static com.notquests.paper.builtin.conditions.support.VariableConditionSupport.OPERATOR;
import static com.notquests.paper.builtin.conditions.support.VariableConditionSupport.VARIABLE_NAME;

public final class BooleanVariableCondition {
    private BooleanVariableCondition() {}

    public static void register(final NotQuests main, final ConditionCatalog conditions) {
        conditions.condition("Boolean")
                .displayName("Boolean Variable")
                .description("Compares a boolean variable with a boolean expression.")
                .withoutTypeLiteral()
                .field(VARIABLE_NAME, FieldTypes.text().config("specifics.variableName"), "Boolean variable to check.")
                .field(OPERATOR, FieldTypes.text().config("specifics.operator"), "Boolean comparison operator to use.")
                .field(EXPRESSION, FieldTypes.numberExpression().config("specifics.expression"), "Boolean expression compared with the variable value.")
                .field(ADDITIONAL_STRINGS, FieldTypes.stringMap().config("specifics.additionalStrings"), "Extra text arguments passed to the variable.")
                .field(ADDITIONAL_NUMBERS, FieldTypes.numberExpressionMap().config("specifics.additionalNumbers"), "Extra number-expression arguments passed to the variable.")
                .field(ADDITIONAL_BOOLEANS, FieldTypes.numberExpressionMap().config("specifics.additionalBooleans"), "Extra boolean-expression arguments passed to the variable.")
                .commands((type, builder, conditionFor) -> registerCommands(main, type, builder, conditionFor))
                .singleLine((condition, arguments) ->
                        VariableConditionSupport.setSingleLineValues(main, condition, arguments, VariableDataType.BOOLEAN))
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
            if (!VariableConditionSupport.shouldRegister(main, variableString, VariableDataType.BOOLEAN)) {
                continue;
            }
            main.getCommandManager().getNQCommandManager().command(main.getVariableCatalog()
                    .registerVariableCommands(variableString, builder)
                    .required(
                            OPERATOR,
                            NQArguments.stringArgument(),
                            NQDescription.of("How to compare the " + variableString + " boolean variable: and, equals, or."),
                            (context, input) -> List.of("and", "equals", "or"))
                    .required(
                            EXPRESSION,
                            booleanVariableArgument(EXPRESSION, variable),
                            NQDescription.of("Boolean expression to compare with the current " + variableString + " value."))
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
            return main.getLanguageManager().getString(
                    "chat.conditions.boolean.variable-not-found",
                    questPlayer.getPlayer(),
                    questPlayer,
                    Map.of("%VARIABLENAME%", condition.text(VARIABLE_NAME)));
        }
        VariableConditionSupport.applyAdditionalArguments(variable, condition);
        final boolean required = new NumberExpression(main, condition.text(EXPRESSION)).calculateBooleanValue(questPlayer);
        final Object value = variable.getValue(questPlayer);
        if (!(value instanceof Boolean current)) {
            return "<ERROR>Error: variable <highlight>" + condition.text(VARIABLE_NAME) + "</highlight> is not a boolean.";
        }
        final boolean fulfilled = switch (condition.text(OPERATOR)) {
            case "equals" -> required == current;
            case "or" -> required || current;
            case "and" -> required && current;
            default -> false;
        };
        if (fulfilled) {
            return "";
        }
        if (!condition.text(OPERATOR).equals("equals")
                && !condition.text(OPERATOR).equals("or")
                && !condition.text(OPERATOR).equals("and")) {
            return main.getLanguageManager().getString(
                    "chat.conditions.boolean.wrong-operator",
                    questPlayer.getPlayer(),
                    questPlayer,
                    Map.of("%OPERATOR%", condition.text(OPERATOR)));
        }
        return main.getLanguageManager().getString(
                "chat.conditions.boolean.not-fulfilled",
                questPlayer.getPlayer(),
                questPlayer,
                Map.of(
                        "%OPERATOR%", condition.text(OPERATOR),
                        "%BOOLEANREQUIREMENT%", String.valueOf(required),
                        "%VARIABLESINGULAR%", variable.getSingular(),
                        "%VARIABLEPLURAL%", variable.getPlural()));
    }

    private static String description(
            final NotQuests main, final DefinedCondition condition, final QuestPlayer questPlayer) {
        final boolean required = new NumberExpression(main, condition.text(EXPRESSION)).calculateBooleanValue(questPlayer);
        if (condition.text(OPERATOR).equalsIgnoreCase("equals")) {
            return "<GRAY>-- " + condition.text(VARIABLE_NAME) + " needs to be " + required + "</GRAY>";
        }
        return "<GRAY>-- " + condition.text(VARIABLE_NAME) + " needed: " + required + "</GRAY>";
    }
}
