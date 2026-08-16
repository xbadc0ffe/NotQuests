package com.notquests.paper.builtin.actions;

import java.util.List;
import com.notquests.paper.NotQuests;
import com.notquests.paper.builtin.actions.support.VariableActionSupport;
import com.notquests.paper.commands.framework.NQArguments;
import com.notquests.paper.commands.framework.NQDescription;
import com.notquests.paper.managers.expressions.NumberExpression;
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
import static com.notquests.paper.commands.arguments.variables.BooleanVariableArgument.booleanVariableArgument;

public final class BooleanVariableAction {
    private BooleanVariableAction() {}

    public static void register(final NotQuests main, final ActionCatalog actions) {
        actions.action("Boolean")
                .displayName("Boolean Variable Action")
                .description("Changes a settable boolean variable.")
                .withoutTypeLiteral()
                .field(VARIABLE_NAME, FieldTypes.text().config("specifics.variableName"), "Boolean variable to change.")
                .field(OPERATOR, FieldTypes.text().config("specifics.operator"), "How to change the variable value.")
                .field(
                        EXPRESSION,
                        FieldTypes.text().config("specifics.expression"),
                        "Boolean expression used as the input value for this variable action.")
                .field(ADDITIONAL_STRINGS, FieldTypes.stringMap().config("specifics.additionalStrings"), "Extra text arguments passed to the variable.")
                .field(ADDITIONAL_NUMBERS, FieldTypes.numberExpressionMap().config("specifics.additionalNumbers"), "Extra number-expression arguments passed to the variable.")
                .field(ADDITIONAL_BOOLEANS, FieldTypes.numberExpressionMap().config("specifics.additionalBooleans"), "Extra boolean-expression arguments passed to the variable.")
                .commands((type, builder, actionFor) -> registerCommands(main, type, builder, actionFor))
                .singleLine((action, arguments) ->
                        VariableActionSupport.setSingleLineValues(main, action, arguments, VariableDataType.BOOLEAN))
                .execute((action, questPlayer, objects) -> {
                    final Variable<?> variable = VariableActionSupport.variable(main, action.action());
                    if (variable == null) {
                        main.sendMessage(questPlayer.getPlayer(), "<ERROR>Error: variable <highlight>" + action.text(VARIABLE_NAME) + "</highlight> not found. Report this to the Server owner.");
                        return;
                    }
                    VariableActionSupport.applyAdditionalArguments(variable, action.action());
                    final Object currentValueObject = variable.getValue(questPlayer, objects);
                    boolean nextValue = new NumberExpression(main, action.text(EXPRESSION)).calculateBooleanValue(questPlayer);
                    if (action.text(OPERATOR).equalsIgnoreCase("setNot")) {
                        nextValue = !nextValue;
                    }
                    setBooleanValue(main, variable, currentValueObject, nextValue, questPlayer, objects);
                })
                .actionDescription((action, questPlayer, objects) -> action.text(VARIABLE_NAME)
                        + ": "
                        + new NumberExpression(main, action.text(EXPRESSION)).calculateBooleanValue(questPlayer))
                .register();
    }

    private static void registerCommands(
            final NotQuests main,
            final ActionType type,
            final com.notquests.paper.commands.framework.NQCommandBuilder builder,
            final com.notquests.paper.actions.ActionFor actionFor) {
        for (final String variableString : main.getVariableCatalog().getVariableIdentifiers()) {
            final Variable<?> variable = main.getVariableCatalog().getVariableFromString(variableString);
            if (!VariableActionSupport.shouldRegister(main, variableString, VariableDataType.BOOLEAN)) {
                continue;
            }
            main.getCommandManager().getNQCommandManager().command(main.getVariableCatalog()
                    .registerVariableCommands(variableString, builder)
                    .required(
                            OPERATOR,
                            NQArguments.stringArgument(),
                            NQDescription.of("How to set the " + variableString + " boolean variable: set or setNot."),
                            (context, input) -> List.of("set", "setNot"))
                    .required(
                            EXPRESSION,
                            booleanVariableArgument(EXPRESSION, variable),
                            NQDescription.of("Boolean expression used as the new value for " + variableString + "."))
                    .handler(context -> {
                        final DefinedAction action = type.createAction();
                        VariableActionSupport.setCommandValues(
                                main, action, variable, context, context.get(OPERATOR), context.get(EXPRESSION));
                        main.getActionCatalog().addAction(action, context, actionFor);
                    }));
        }
    }

    @SuppressWarnings("unchecked")
    private static void setBooleanValue(
            final NotQuests main,
            final Variable<?> variable,
            final Object currentValueObject,
            final boolean nextValue,
            final com.notquests.paper.structs.QuestPlayer questPlayer,
            final Object... objects) {
        if (currentValueObject instanceof Boolean) {
            ((Variable<Boolean>) variable).setValue(nextValue, questPlayer, objects);
        } else {
            main.getLogManager().warn("Cannot execute boolean action because value type "
                    + currentValueObject.getClass().getName()
                    + " is invalid.");
        }
    }
}
