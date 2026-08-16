package com.notquests.paper.builtin.actions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import com.notquests.paper.NotQuests;
import com.notquests.paper.builtin.actions.support.VariableActionSupport;
import com.notquests.paper.commands.framework.NQArguments;
import com.notquests.paper.commands.framework.NQDescription;
import com.notquests.paper.actions.ActionCatalog;
import com.notquests.paper.registry.ActionType;
import com.notquests.paper.registry.DefinedAction;
import com.notquests.paper.registry.FieldTypes;
import com.notquests.paper.variables.Variable;
import com.notquests.paper.variables.VariableDataType;

import static com.notquests.paper.builtin.actions.support.VariableActionSupport.ADDITIONAL_BOOLEANS;
import static com.notquests.paper.builtin.actions.support.VariableActionSupport.ADDITIONAL_NUMBERS;
import static com.notquests.paper.builtin.actions.support.VariableActionSupport.ADDITIONAL_STRINGS;
import static com.notquests.paper.builtin.actions.support.VariableActionSupport.EXPRESSION;
import static com.notquests.paper.builtin.actions.support.VariableActionSupport.OPERATOR;
import static com.notquests.paper.builtin.actions.support.VariableActionSupport.VARIABLE_NAME;
import static com.notquests.paper.commands.arguments.variables.ListVariableArgument.listVariableArgument;

public final class ListVariableAction {
    private ListVariableAction() {}

    public static void register(final NotQuests main, final ActionCatalog actions) {
        actions.action("List")
                .displayName("List Variable Action")
                .description("Changes a settable list variable.")
                .withoutTypeLiteral()
                .field(VARIABLE_NAME, FieldTypes.text().config("specifics.variableName"), "List variable to change.")
                .field(OPERATOR, FieldTypes.text().config("specifics.operator"), "How to change the variable value.")
                .field(
                        EXPRESSION,
                        FieldTypes.text().config("specifics.expression"),
                        "Comma-separated list value or expression used by this variable action.")
                .field(ADDITIONAL_STRINGS, FieldTypes.stringMap().config("specifics.additionalStrings"), "Extra text arguments passed to the variable.")
                .field(ADDITIONAL_NUMBERS, FieldTypes.numberExpressionMap().config("specifics.additionalNumbers"), "Extra number-expression arguments passed to the variable.")
                .field(ADDITIONAL_BOOLEANS, FieldTypes.numberExpressionMap().config("specifics.additionalBooleans"), "Extra boolean-expression arguments passed to the variable.")
                .commands((type, builder, actionFor) -> registerCommands(main, type, builder, actionFor))
                .singleLine((action, arguments) ->
                        VariableActionSupport.setSingleLineValues(main, action, arguments, VariableDataType.LIST))
                .execute((action, questPlayer, objects) -> {
                    final Variable<?> variable = VariableActionSupport.variable(main, action.action());
                    if (variable == null) {
                        main.sendMessage(questPlayer.getPlayer(), "<ERROR>Error: variable <highlight>" + action.text(VARIABLE_NAME) + "</highlight> not found. Report this to the Server owner.");
                        return;
                    }
                    VariableActionSupport.applyAdditionalArguments(variable, action.action());
                    final Object currentValueObject = variable.getValue(questPlayer, questPlayer, objects);
                    final String[] currentValue = currentValue(currentValueObject);
                    final String[] expressionValue = action.text(EXPRESSION).split(",");
                    final String[] nextValue = applyOperator(main, questPlayer, action.text(OPERATOR), currentValue, expressionValue);
                    setListValue(main, variable, currentValueObject, nextValue, questPlayer, objects);
                })
                .actionDescription((action, questPlayer, objects) ->
                        action.text(VARIABLE_NAME) + ": " + Arrays.toString(action.text(EXPRESSION).split(",")))
                .register();
    }

    private static void registerCommands(
            final NotQuests main,
            final ActionType type,
            final com.notquests.paper.commands.framework.NQCommandBuilder builder,
            final com.notquests.paper.actions.ActionFor actionFor) {
        for (final String variableString : main.getVariableCatalog().getVariableIdentifiers()) {
            final Variable<?> variable = main.getVariableCatalog().getVariableFromString(variableString);
            if (!VariableActionSupport.shouldRegister(main, variableString, VariableDataType.LIST)) {
                continue;
            }
            main.getCommandManager().getNQCommandManager().command(main.getVariableCatalog()
                    .registerVariableCommands(variableString, builder)
                    .required(
                            OPERATOR,
                            NQArguments.stringArgument(),
                            NQDescription.of("How to change the " + variableString + " list variable: set, add, remove, or clear."),
                            (context, input) -> List.of("set", "add", "remove", "clear"))
                    .required(
                            EXPRESSION,
                            listVariableArgument(EXPRESSION, variable),
                            NQDescription.of("Comma-separated list value or expression used by this " + variableString + " action."))
                    .handler(context -> {
                        final DefinedAction action = type.createAction();
                        VariableActionSupport.setCommandValues(
                                main, action, variable, context, context.get(OPERATOR), context.get(EXPRESSION));
                        main.getActionCatalog().addAction(action, context, actionFor);
                    }));
        }
    }

    private static String[] currentValue(final Object currentValueObject) {
        if (currentValueObject instanceof String[] stringList) {
            return stringList;
        }
        if (currentValueObject instanceof ArrayList<?> stringList) {
            return stringList.toArray(new String[0]);
        }
        return (String[]) currentValueObject;
    }

    private static String[] applyOperator(
            final NotQuests main,
            final com.notquests.paper.structs.QuestPlayer questPlayer,
            final String operator,
            final String[] currentValue,
            final String[] expressionValue) {
        if (operator.equalsIgnoreCase("set")) {
            return expressionValue;
        }
        if (operator.equalsIgnoreCase("add")) {
            final ArrayList<String> next = new ArrayList<>(Arrays.asList(expressionValue));
            next.addAll(List.of(currentValue));
            return next.toArray(new String[0]);
        }
        if (operator.equalsIgnoreCase("remove")) {
            final ArrayList<String> next = new ArrayList<>(Arrays.asList(currentValue));
            next.removeAll(List.of(expressionValue));
            return next.toArray(new String[0]);
        }
        if (operator.equalsIgnoreCase("clear")) {
            return new String[0];
        }
        main.sendMessage(
                questPlayer.getPlayer(),
                "<ERROR>Error: variable operator <highlight>" + operator + "</highlight> is invalid. Report this to the Server owner.");
        return currentValue;
    }

    @SuppressWarnings("unchecked")
    private static void setListValue(
            final NotQuests main,
            final Variable<?> variable,
            final Object currentValueObject,
            final String[] nextValue,
            final com.notquests.paper.structs.QuestPlayer questPlayer,
            final Object... objects) {
        if (currentValueObject instanceof String[]) {
            ((Variable<String[]>) variable).setValue(nextValue, questPlayer, objects);
        } else if (currentValueObject instanceof ArrayList<?>) {
            ((Variable<ArrayList<?>>) variable).setValue(new ArrayList<>(Arrays.asList(nextValue)), questPlayer, objects);
        } else {
            main.getLogManager().warn("Cannot execute list action because value type "
                    + currentValueObject.getClass().getName()
                    + " is invalid.");
        }
    }
}
