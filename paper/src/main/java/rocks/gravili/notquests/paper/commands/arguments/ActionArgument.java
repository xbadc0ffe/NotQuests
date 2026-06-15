package rocks.gravili.notquests.paper.commands.arguments;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.commands.framework.NQArgumentType;
import rocks.gravili.notquests.paper.structs.actions.Action;

import java.util.ArrayList;
import java.util.List;

/** Native-framework port of {@code ActionParser}: resolves an {@link Action} by identifier. */
public final class ActionArgument extends NQArgumentType<Action> {
    private final NotQuests main;

    public ActionArgument(final NotQuests main) {
        this.main = main;
    }

    public static ActionArgument actionArgument(final NotQuests main) {
        return new ActionArgument(main);
    }

    @Override
    public String valueTypeName() {
        return "saved action name";
    }

    @Override
    public Action convert(final String input) throws CommandSyntaxException {
        final Action foundAction = main.getActionsYMLManager().getAction(input);
        if (foundAction == null) {
            throw fail("Action '" + input + "' does not exist!");
        }
        return foundAction;
    }

    @Override
    protected List<String> suggest(final CommandContext<?> context, final String remaining) {
        return new ArrayList<>(main.getActionsYMLManager().getActionsAndIdentifiers().keySet());
    }
}
