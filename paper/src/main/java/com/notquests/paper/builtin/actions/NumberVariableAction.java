package com.notquests.paper.builtin.actions;

import java.util.List;
import com.notquests.paper.NotQuests;
import com.notquests.paper.builtin.actions.support.VariableActionSupport;
import com.notquests.paper.commands.framework.NQArguments;
import com.notquests.paper.commands.framework.NQDescription;
import com.notquests.paper.managers.expressions.NumberExpression;
import com.notquests.paper.actions.ActionCatalog;
import com.notquests.paper.registry.FieldTypes;
import com.notquests.paper.registry.ActionType;
import com.notquests.paper.registry.DefinedAction;
import com.notquests.paper.variables.Variable;
import com.notquests.paper.variables.VariableDataType;

import static com.notquests.paper.builtin.actions.support.VariableActionSupport.ADDITIONAL_BOOLEANS;
import static com.notquests.paper.builtin.actions.support.VariableActionSupport.ADDITIONAL_NUMBERS;
import static com.notquests.paper.builtin.actions.support.VariableActionSupport.ADDITIONAL_STRINGS;
import static com.notquests.paper.builtin.actions.support.VariableActionSupport.EXPRESSION;
import static com.notquests.paper.builtin.actions.support.VariableActionSupport.OPERATOR;
import static com.notquests.paper.builtin.actions.support.VariableActionSupport.VARIABLE_NAME;
import static com.notquests.paper.commands.arguments.variables.NumberVariableArgument.numberVariableArgument;

public final class NumberVariableAction {
    private NumberVariableAction() {}

    public static void register(final NotQuests main, final ActionCatalog actions) {
        actions.action("Number")
                .displayName("Number Variable Action")
                .description("Changes a settable number variable.")
                .withoutTypeLiteral()
                .field(VARIABLE_NAME, FieldTypes.text().config("specifics.variableName"), "Number variable to change.")
                .field(OPERATOR, FieldTypes.text().config("specifics.operator"), "How to change the variable value.")
                .field(
                        EXPRESSION,
                        FieldTypes.numberExpression().config("specifics.expression"),
                        "Number expression used as the input value for this variable action.")
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
                .commands((type, builder, actionFor) -> registerCommands(main, type, builder, actionFor))
                .singleLine((action, arguments) ->
                        VariableActionSupport.setSingleLineValues(main, action, arguments, VariableDataType.NUMBER))
                .execute((action, questPlayer, objects) -> {
                    final Variable<?> variable = VariableActionSupport.variable(main, action.action());
                    if (variable == null) {
                        main.sendMessage(
                                questPlayer.getPlayer(),
                                "<ERROR>Error: variable <highlight>"
                                        + action.text(VARIABLE_NAME)
                                        + "</highlight> not found. Report this to the Server owner.");
                        return;
                    }
                    VariableActionSupport.applyAdditionalArguments(variable, action.action());
                    final Object currentValueObject = variable.getValue(questPlayer, questPlayer, objects);
                    final double currentValue = currentValueObject instanceof Number number
                            ? number.doubleValue()
                            : (double) currentValueObject;
                    final double expressionValue = new NumberExpression(main, action.text(EXPRESSION)).calculateValue(questPlayer);
                    final double nextValue = applyOperator(main, questPlayer, action.text(OPERATOR), currentValue, expressionValue);
                    setNumberValue(main, variable, currentValueObject, nextValue, questPlayer, objects);
                })
                .actionDescription((action, questPlayer, objects) -> action.text(VARIABLE_NAME)
                        + ": "
                        + new NumberExpression(main, action.text(EXPRESSION)).calculateValue(questPlayer))
                .register();
    }

    private static void registerCommands(
            final NotQuests main,
            final ActionType type,
            final com.notquests.paper.commands.framework.NQCommandBuilder builder,
            final com.notquests.paper.actions.ActionFor actionFor) {
        for (final String variableString : main.getVariableCatalog().getVariableIdentifiers()) {
            final Variable<?> variable = main.getVariableCatalog().getVariableFromString(variableString);
            if (!VariableActionSupport.shouldRegister(main, variableString, VariableDataType.NUMBER)) {
                continue;
            }
            main.getCommandManager().getNQCommandManager().command(main.getVariableCatalog()
                    .registerVariableCommands(variableString, builder)
                    .required(
                            OPERATOR,
                            NQArguments.stringArgument(),
                            NQDescription.of("How to change the " + variableString + " number variable: set, add, deduct, multiply, or divide."),
                            (context, input) -> List.of("set", "add", "deduct", "multiply", "divide"))
                    .required(
                            "amount",
                            numberVariableArgument("amount", variable),
                            NQDescription.of("Number expression used as the input value for this " + variableString + " action."))
                    .handler(context -> {
                        final DefinedAction action = type.createAction();
                        VariableActionSupport.setCommandValues(
                                main, action, variable, context, context.get(OPERATOR), context.get("amount"));
                        main.getActionCatalog().addAction(action, context, actionFor);
                    }));
        }
    }

    private static double applyOperator(
            final NotQuests main,
            final com.notquests.paper.structs.QuestPlayer questPlayer,
            final String operator,
            final double currentValue,
            final double expressionValue) {
        if (operator.equalsIgnoreCase("set")) {
            return expressionValue;
        }
        if (operator.equalsIgnoreCase("add")) {
            return currentValue + expressionValue;
        }
        if (operator.equalsIgnoreCase("deduct")) {
            return currentValue - expressionValue;
        }
        if (operator.equalsIgnoreCase("multiply")) {
            return currentValue * expressionValue;
        }
        if (operator.equalsIgnoreCase("divide")) {
            if (expressionValue == 0) {
                main.sendMessage(
                        questPlayer.getPlayer(),
                        "<ERROR>Error: variable operator <highlight>divide</highlight> cannot divide by 0. Report this to the Server owner.");
                return currentValue;
            }
            return currentValue / expressionValue;
        }
        main.sendMessage(
                questPlayer.getPlayer(),
                "<ERROR>Error: variable operator <highlight>" + operator + "</highlight> is invalid. Report this to the Server owner.");
        return currentValue;
    }

    @SuppressWarnings("unchecked")
    private static void setNumberValue(
            final NotQuests main,
            final Variable<?> variable,
            final Object currentValueObject,
            final double nextValue,
            final com.notquests.paper.structs.QuestPlayer questPlayer,
            final Object... objects) {
        if (currentValueObject instanceof Long) {
            ((Variable<Long>) variable).setValue(Double.valueOf(nextValue).longValue(), questPlayer, objects);
        } else if (currentValueObject instanceof Float) {
            ((Variable<Float>) variable).setValue(Double.valueOf(nextValue).floatValue(), questPlayer, objects);
        } else if (currentValueObject instanceof Double) {
            ((Variable<Double>) variable).setValue(nextValue, questPlayer, objects);
        } else if (currentValueObject instanceof Integer) {
            ((Variable<Integer>) variable).setValue(Double.valueOf(nextValue).intValue(), questPlayer, objects);
        } else {
            main.getLogManager().warn(
                    "Cannot execute number action because value type "
                            + currentValueObject.getClass().getName()
                            + " is invalid.");
        }
    }
}
