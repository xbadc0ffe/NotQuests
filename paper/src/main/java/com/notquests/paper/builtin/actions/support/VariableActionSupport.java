package com.notquests.paper.builtin.actions.support;

import java.util.ArrayList;
import java.util.HashMap;
import org.bukkit.command.CommandSender;
import com.notquests.paper.NotQuests;
import com.notquests.paper.commands.arguments.variables.BooleanVariableValueParser;
import com.notquests.paper.commands.arguments.variables.NumberVariableValueParser;
import com.notquests.paper.commands.arguments.variables.StringVariableValueParser;
import com.notquests.paper.commands.framework.NQCommandContext;
import com.notquests.paper.commands.framework.NQFlag;
import com.notquests.paper.managers.expressions.NumberExpression;
import com.notquests.paper.registry.DefinedAction;
import com.notquests.paper.variables.Variable;
import com.notquests.paper.variables.VariableDataType;

public final class VariableActionSupport {
    public static final String VARIABLE_NAME = "variableName";
    public static final String OPERATOR = "operator";
    public static final String EXPRESSION = "expression";
    public static final String ADDITIONAL_STRINGS = "additionalStrings";
    public static final String ADDITIONAL_NUMBERS = "additionalNumbers";
    public static final String ADDITIONAL_BOOLEANS = "additionalBooleans";

    private VariableActionSupport() {}

    public static boolean shouldRegister(
            final NotQuests main, final String variableName, final VariableDataType type) {
        final Variable<?> variable = main.getVariableCatalog().getVariableFromString(variableName);
        return variable != null
                && variable.isCanSetValue()
                && variable.getVariableDataType() == type
                && !main.getVariableCatalog().alreadyFullRegisteredVariables.contains(variableName);
    }

    public static void setCommandValues(
            final NotQuests main,
            final DefinedAction action,
            final Variable<?> variable,
            final NQCommandContext context,
            final String operator,
            final String expression) {
        action.setValue(VARIABLE_NAME, variable.getVariableType());
        action.setValue(OPERATOR, operator);
        action.setValue(EXPRESSION, expression);
        action.setValue(ADDITIONAL_STRINGS, additionalStringArguments(variable, context));
        action.setValue(ADDITIONAL_NUMBERS, additionalNumberArguments(main, variable, context));
        action.setValue(ADDITIONAL_BOOLEANS, additionalBooleanArguments(main, variable, context));
    }

    public static void setSingleLineValues(
            final NotQuests main, final DefinedAction action, final ArrayList<String> arguments, final VariableDataType type) {
        action.setValue(VARIABLE_NAME, arguments.get(0));
        action.setValue(OPERATOR, arguments.get(1));
        action.setValue(EXPRESSION, arguments.get(2));
        final Variable<?> variable = main.getVariableCatalog().getVariableFromString(arguments.get(0));
        if (variable == null || !variable.isCanSetValue() || variable.getVariableDataType() != type) {
            return;
        }
        final HashMap<String, String> strings = new HashMap<>();
        final HashMap<String, NumberExpression> numbers = new HashMap<>();
        final HashMap<String, NumberExpression> booleans = new HashMap<>();
        setAdditionalArgumentsFromSingleLine(main, action, arguments, variable, 3, strings, numbers, booleans);
    }

    public static void setAdditionalArgumentsFromSingleLine(
            final NotQuests main,
            final DefinedAction action,
            final ArrayList<String> arguments,
            final Variable<?> variable,
            final int firstValueIndex) {
        final HashMap<String, String> strings = new HashMap<>();
        final HashMap<String, NumberExpression> numbers = new HashMap<>();
        final HashMap<String, NumberExpression> booleans = new HashMap<>();
        setAdditionalArgumentsFromSingleLine(main, action, arguments, variable, firstValueIndex, strings, numbers, booleans);
    }

    private static void setAdditionalArgumentsFromSingleLine(
            final NotQuests main,
            final DefinedAction action,
            final ArrayList<String> arguments,
            final Variable<?> variable,
            final int firstValueIndex,
            final HashMap<String, String> strings,
            final HashMap<String, NumberExpression> numbers,
            final HashMap<String, NumberExpression> booleans) {
        int valueIndex = firstValueIndex;
        for (final StringVariableValueParser<?> parser : variable.getRequiredStrings()) {
            if (arguments.size() <= valueIndex) {
                break;
            }
            strings.put(parser.getIdentifier(), arguments.get(valueIndex++));
        }
        for (final NumberVariableValueParser<?> parser : variable.getRequiredNumbers()) {
            if (arguments.size() <= valueIndex) {
                break;
            }
            numbers.put(parser.getIdentifier(), new NumberExpression(main, arguments.get(valueIndex++)));
        }
        for (final BooleanVariableValueParser<?> parser : variable.getRequiredBooleans()) {
            if (arguments.size() <= valueIndex) {
                break;
            }
            booleans.put(parser.getIdentifier(), new NumberExpression(main, arguments.get(valueIndex++)));
        }
        for (final NQFlag flag : variable.getRequiredBooleanFlags()) {
            if (arguments.size() <= valueIndex) {
                break;
            }
            booleans.put(flag.name(), new NumberExpression(main, arguments.get(valueIndex++)));
        }
        action.setValue(ADDITIONAL_STRINGS, strings);
        action.setValue(ADDITIONAL_NUMBERS, numbers);
        action.setValue(ADDITIONAL_BOOLEANS, booleans);
    }

    public static Variable<?> variable(final NotQuests main, final DefinedAction action) {
        return main.getVariableCatalog().getVariableFromString(action.text(VARIABLE_NAME));
    }

    @SuppressWarnings("unchecked")
    public static HashMap<String, String> stringMap(final DefinedAction action) {
        final Object value = action.value(ADDITIONAL_STRINGS, Object.class);
        return value instanceof HashMap<?, ?> map ? (HashMap<String, String>) map : new HashMap<>();
    }

    @SuppressWarnings("unchecked")
    public static HashMap<String, NumberExpression> expressionMap(final DefinedAction action, final String key) {
        final Object value = action.value(key, Object.class);
        return value instanceof HashMap<?, ?> map ? (HashMap<String, NumberExpression>) map : new HashMap<>();
    }

    public static void applyAdditionalArguments(final Variable<?> variable, final DefinedAction action) {
        final HashMap<String, String> strings = stringMap(action);
        if (!strings.isEmpty()) {
            variable.setAdditionalStringArguments(strings);
        }
        final HashMap<String, NumberExpression> numbers = expressionMap(action, ADDITIONAL_NUMBERS);
        if (!numbers.isEmpty()) {
            variable.setAdditionalNumberArguments(numbers);
        }
        final HashMap<String, NumberExpression> booleans = expressionMap(action, ADDITIONAL_BOOLEANS);
        if (!booleans.isEmpty()) {
            variable.setAdditionalBooleanArguments(booleans);
        }
    }

    private static HashMap<String, String> additionalStringArguments(
            final Variable<?> variable,
            final NQCommandContext context) {
        final HashMap<String, String> values = new HashMap<>();
        for (final StringVariableValueParser<CommandSender> parser : variable.getRequiredStrings()) {
            values.put(parser.getIdentifier(), context.get(parser.getIdentifier()));
        }
        return values;
    }

    private static HashMap<String, NumberExpression> additionalNumberArguments(
            final NotQuests main,
            final Variable<?> variable,
            final NQCommandContext context) {
        final HashMap<String, NumberExpression> values = new HashMap<>();
        for (final NumberVariableValueParser<CommandSender> parser : variable.getRequiredNumbers()) {
            values.put(parser.getIdentifier(), new NumberExpression(main, context.get(parser.getIdentifier())));
        }
        return values;
    }

    private static HashMap<String, NumberExpression> additionalBooleanArguments(
            final NotQuests main,
            final Variable<?> variable,
            final NQCommandContext context) {
        final HashMap<String, NumberExpression> values = new HashMap<>();
        for (final BooleanVariableValueParser<CommandSender> parser : variable.getRequiredBooleans()) {
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
}
