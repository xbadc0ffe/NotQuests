package com.notquests.paper.commands.arguments.variables;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.notquests.paper.NotQuests;
import com.notquests.paper.commands.framework.NQArgumentType;
import com.notquests.paper.variables.Variable;

import java.util.List;

/**
 * Native-framework port of {@code NumberVariableValueParser}: a generic "variable value" argument
 * whose value is the raw string (a number or a mathematical/variable expression). Backed by a
 * <b>greedy</b> {@code greedyString()} (mirroring the Cloud mapping in
 * {@code CommandManager.preSetupCommands()}) so that special symbols like a comma do not trigger
 * Brigadier's red "invalid" colouring.
 */
public final class NumberVariableArgument extends NQArgumentType<String> {
    private final NotQuests main;

    private final String identifier;
    private final Variable<?> variable;
    private final boolean greedy;

    public NumberVariableArgument(final String identifier, final Variable<?> variable) {
        this(identifier, variable, true);
    }

    public NumberVariableArgument(final String identifier, final Variable<?> variable, final boolean greedy) {
        this.main = NotQuests.getInstance();
        this.identifier = identifier;
        this.variable = variable;
        this.greedy = greedy;
    }

    /** A trailing number/expression argument (greedy: it is the last argument and may contain commas). */
    public static NumberVariableArgument numberVariableArgument(final String identifier, final Variable<?> variable) {
        return new NumberVariableArgument(identifier, variable, true);
    }

    /** A positional number argument (non-greedy single token, so it doesn't swallow following args). */
    public static NumberVariableArgument numberVariableArgument(final String identifier, final Variable<?> variable, final boolean greedy) {
        return new NumberVariableArgument(identifier, variable, greedy);
    }

    public String getIdentifier() {
        return identifier;
    }

    @Override
    public String valueTypeName() {
        return "number or numeric expression";
    }

    @Override
    public ArgumentType<String> getNativeType() {
        // Greedy ONLY for trailing number expressions: an expression may contain commas (variable
        // function-args like "Block(world:w,x:1,y:2,z:3)"), and every non-greedy Brigadier string
        // mode stops at a comma. Positional number args (x/y/z, min/max) are followed by more
        // arguments, so they must be a single non-greedy token to avoid swallowing them.
        return greedy ? StringArgumentType.greedyString() : StringArgumentType.string();
    }

    @Override
    public String convert(final String input) throws CommandSyntaxException {
        return input;
    }

    @Override
    protected List<String> suggest(final CommandContext<?> context, final String remaining) {
        return List.of("1", "2", "3", "5", "10", "25", "50", "100");
    }
}
