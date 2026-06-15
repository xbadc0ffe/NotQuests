package rocks.gravili.notquests.paper.commands.arguments.variables;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.commands.framework.NQArgumentType;
import rocks.gravili.notquests.paper.structs.variables.Variable;

import java.util.List;

/**
 * Native-framework port of {@code BooleanVariableValueParser}: a generic "variable value" argument
 * whose value is the raw string (a boolean or a boolean expression). Backed by a <b>greedy</b>
 * {@code greedyString()} (mirroring the Cloud mapping in {@code CommandManager.preSetupCommands()})
 * so that special symbols like a comma do not trigger Brigadier's red "invalid" colouring.
 */
public final class BooleanVariableArgument extends NQArgumentType<String> {
    private final NotQuests main;

    private final String identifier;
    private final Variable<?> variable;
    private final boolean greedy;

    public BooleanVariableArgument(final String identifier, final Variable<?> variable) {
        this(identifier, variable, true);
    }

    public BooleanVariableArgument(final String identifier, final Variable<?> variable, final boolean greedy) {
        this.main = NotQuests.getInstance();
        this.identifier = identifier;
        this.variable = variable;
        this.greedy = greedy;
    }

    /** A trailing boolean/expression argument (greedy: it is the last argument and may contain commas). */
    public static BooleanVariableArgument booleanVariableArgument(final String identifier, final Variable<?> variable) {
        return new BooleanVariableArgument(identifier, variable, true);
    }

    /** A positional boolean argument (non-greedy single token, so it doesn't swallow following args). */
    public static BooleanVariableArgument booleanVariableArgument(final String identifier, final Variable<?> variable, final boolean greedy) {
        return new BooleanVariableArgument(identifier, variable, greedy);
    }

    public String getIdentifier() {
        return identifier;
    }

    @Override
    public String valueTypeName() {
        return "true/false value or boolean expression";
    }

    @Override
    public ArgumentType<String> getNativeType() {
        // Greedy only for a trailing boolean expression (it may contain commas, which a non-greedy
        // string would split on). Positional boolean args are non-greedy so they don't swallow the
        // arguments that follow them.
        return greedy ? StringArgumentType.greedyString() : StringArgumentType.string();
    }

    @Override
    public String convert(final String input) throws CommandSyntaxException {
        return input;
    }

    @Override
    protected List<String> suggest(final CommandContext<?> context, final String remaining) {
        return List.of("true", "false");
    }
}
