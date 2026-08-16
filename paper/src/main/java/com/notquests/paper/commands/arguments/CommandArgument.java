package com.notquests.paper.commands.arguments;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.notquests.paper.NotQuests;
import com.notquests.paper.commands.framework.NQArgumentType;

import java.util.ArrayList;
import java.util.List;

/**
 * Native-framework port of {@code CommandParser}: reads the remainder of the line as a (console)
 * command string and offers command-map tab completions. Uses a greedy native string so the entire
 * trailing command is captured rather than a single token.
 */
public final class CommandArgument extends NQArgumentType<String> {
    private final NotQuests main;

    public CommandArgument(final NotQuests main) {
        this.main = main;
    }

    public static CommandArgument commandArgument(final NotQuests main) {
        return new CommandArgument(main);
    }

    @Override
    public String valueTypeName() {
        return "command text";
    }

    @Override
    public ArgumentType<String> getNativeType() {
        // The Cloud parser consumed the rest of the input; greedyString mirrors that.
        return StringArgumentType.greedyString();
    }

    @Override
    public String convert(final String input) throws CommandSyntaxException {
        return input;
    }

    @Override
    protected List<String> suggest(final CommandContext<?> context, final String remaining) {
        final List<String> completions = new ArrayList<>();

        if (main.getCommandManager().getCommandMap() != null) {
            final List<String> compl =
                    main.getCommandManager()
                            .getCommandMap()
                            .tabComplete(main.getMain().getServer().getConsoleSender(), remaining);
            if (compl != null) {
                completions.addAll(compl);
            }
        }

        if (remaining.startsWith("{")) {
            main.getCommandManager().getAdminCommands().placeholders.forEach(completions::add);
        }
        return completions;
    }
}
