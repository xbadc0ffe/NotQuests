package com.notquests.paper.builtin.conditions.support;

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
import com.notquests.paper.registry.DefinedCondition;
import com.notquests.paper.variables.Variable;
import com.notquests.paper.variables.VariableDataType;

public final class VariableConditionSupport {
    public static final String VARIABLE_NAME = "variableName";
    public static final String OPERATOR = "operator";
    public static final String EXPRESSION = "expression";
    public static final String ADDITIONAL_STRINGS = "additionalStrings";
    public static final String ADDITIONAL_NUMBERS = "additionalNumbers";
    public static final String ADDITIONAL_BOOLEANS = "additionalBooleans";

    private VariableConditionSupport() {}

    public static boolean shouldRegister(
            final NotQuests main, final String variableName, final VariableDataType type) {
        final Variable<?> variable = main.getVariableCatalog().getVariableFromString(variableName);
        return variable != null
                && variable.getVariableDataType() == type
                && !main.getVariableCatalog().alreadyFullRegisteredVariables.contains(variableName);
    }

    public static void setCommandValues(
            final NotQuests main,
            final DefinedCondition condition,
            final Variable<?> variable,
            final NQCommandContext context,
            final String operator,
            final Object expression) {
        condition.setValue(VARIABLE_NAME, variable.getVariableType());
        condition.setValue(OPERATOR, operator);
        condition.setValue(EXPRESSION, expression);
        condition.setValue(ADDITIONAL_STRINGS, additionalStringArguments(variable, context));
        condition.setValue(ADDITIONAL_NUMBERS, additionalNumberArguments(main, variable, context));
        condition.setValue(ADDITIONAL_BOOLEANS, additionalBooleanArguments(main, variable, context));
    }

    public static void setSingleLineValues(
            final NotQuests main,
            final DefinedCondition condition,
            final ArrayList<String> arguments,
            final VariableDataType type) {
        condition.setValue(VARIABLE_NAME, arguments.get(0));
        condition.setValue(OPERATOR, arguments.get(1));
        condition.setValue(EXPRESSION, arguments.get(2));
        final Variable<?> variable = main.getVariableCatalog().getVariableFromString(arguments.get(0));
        if (variable == null || variable.getVariableDataType() != type) {
            return;
        }
        setAdditionalArgumentsFromSingleLine(main, condition, arguments, variable, 3);
    }

    public static void setAdditionalArgumentsFromSingleLine(
            final NotQuests main,
            final DefinedCondition condition,
            final ArrayList<String> arguments,
            final Variable<?> variable,
            final int firstValueIndex) {
        final HashMap<String, String> strings = new HashMap<>();
        final HashMap<String, NumberExpression> numbers = new HashMap<>();
        final HashMap<String, NumberExpression> booleans = new HashMap<>();
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
        condition.setValue(ADDITIONAL_STRINGS, strings);
        condition.setValue(ADDITIONAL_NUMBERS, numbers);
        condition.setValue(ADDITIONAL_BOOLEANS, booleans);
    }

    public static Variable<?> variable(final NotQuests main, final DefinedCondition condition) {
        return main.getVariableCatalog().getVariableFromString(condition.text(VARIABLE_NAME));
    }

    @SuppressWarnings("unchecked")
    public static HashMap<String, String> stringMap(final DefinedCondition condition) {
        final Object value = condition.value(ADDITIONAL_STRINGS, Object.class);
        return value instanceof HashMap<?, ?> map ? (HashMap<String, String>) map : new HashMap<>();
    }

    @SuppressWarnings("unchecked")
    public static HashMap<String, NumberExpression> expressionMap(final DefinedCondition condition, final String key) {
        final Object value = condition.value(key, Object.class);
        return value instanceof HashMap<?, ?> map ? (HashMap<String, NumberExpression>) map : new HashMap<>();
    }

    public static void applyAdditionalArguments(final Variable<?> variable, final DefinedCondition condition) {
        final HashMap<String, String> strings = stringMap(condition);
        if (!strings.isEmpty()) {
            variable.setAdditionalStringArguments(strings);
        }
        final HashMap<String, NumberExpression> numbers = expressionMap(condition, ADDITIONAL_NUMBERS);
        if (!numbers.isEmpty()) {
            variable.setAdditionalNumberArguments(numbers);
        }
        final HashMap<String, NumberExpression> booleans = expressionMap(condition, ADDITIONAL_BOOLEANS);
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
