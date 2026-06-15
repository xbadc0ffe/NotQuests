package rocks.gravili.notquests.paper.commands.arguments;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.commands.framework.NQArgumentType;
import rocks.gravili.notquests.paper.structs.actions.Action;

import java.util.ArrayList;
import java.util.List;

/**
 * Native-framework port of {@code MultiActionsParser}: resolves a comma-separated list of action
 * names into an {@link ActionList}.
 */
public final class MultiActionsArgument extends NQArgumentType<ActionList> {
    private final NotQuests main;

    public MultiActionsArgument(final NotQuests main) {
        this.main = main;
    }

    public static MultiActionsArgument multiActionsArgument(final NotQuests main) {
        return new MultiActionsArgument(main);
    }

    @Override
    public String valueTypeName() {
        return "comma-separated saved action names";
    }

    @Override
    public ArgumentType<String> getNativeType() {
        return StringArgumentType.greedyString();
    }

    @Override
    public ActionList convert(final String input) throws CommandSyntaxException {
        if (input == null || input.isEmpty()) {
            throw fail("No Input provided!");
        }
        final ActionList actions = new ActionList();
        for (final String inputAction : input.split(",")) {
            final Action action = main.getActionsYMLManager().getAction(inputAction);
            if (action == null) {
                throw fail("Action '" + inputAction + "' does not exist!");
            }
            actions.addValue(action);
        }
        if (actions.getValues().isEmpty()) {
            throw fail("No valid action found!");
        }

        return actions;
    }

    @Override
    protected List<String> suggest(final CommandContext<?> context, final String remaining) {
        final List<String> suggestions = new ArrayList<>();

        final String rawData = remaining;
        if (rawData.endsWith(",")) {
            for (final String actionName : main.getActionsYMLManager().getActionsAndIdentifiers().keySet()) {
                suggestions.add(rawData + actionName);
            }
        } else {
            if (!rawData.contains(",")) {
                for (final String actionName : main.getActionsYMLManager().getActionsAndIdentifiers().keySet()) {
                    suggestions.add(actionName);
                    if (rawData.endsWith(actionName)) {
                        suggestions.add(actionName + ",");
                    }
                }
            } else {
                for (final String actionName : main.getActionsYMLManager().getActionsAndIdentifiers().keySet()) {
                    suggestions.add(rawData.substring(0, rawData.lastIndexOf(",") + 1) + actionName);
                    if (rawData.endsWith(actionName)) {
                        suggestions.add(rawData + ",");
                    }
                }
            }
        }

        return suggestions;
    }
}
