package rocks.gravili.notquests.paper.commands.arguments;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.commands.framework.NQArgumentType;
import rocks.gravili.notquests.paper.structs.conditions.Condition;

import java.util.ArrayList;
import java.util.List;

/** Native-framework port of {@code ConditionParser}: resolves a {@link Condition} by identifier. */
public final class ConditionArgument extends NQArgumentType<Condition> {
    private final NotQuests main;

    public ConditionArgument(final NotQuests main) {
        this.main = main;
    }

    public static ConditionArgument conditionArgument(final NotQuests main) {
        return new ConditionArgument(main);
    }

    @Override
    public String valueTypeName() {
        return "saved condition name";
    }

    @Override
    public Condition convert(final String input) throws CommandSyntaxException {
        final Condition foundCondition = main.getConditionsYMLManager().getCondition(input);
        if (foundCondition == null) {
            throw fail("Condition '" + input + "' does not exist!");
        }
        return foundCondition;
    }

    @Override
    protected List<String> suggest(final CommandContext<?> context, final String remaining) {
        return new ArrayList<>(main.getConditionsYMLManager().getConditionsAndIdentifiers().keySet());
    }
}
