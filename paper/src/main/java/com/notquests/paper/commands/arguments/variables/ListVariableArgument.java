package com.notquests.paper.commands.arguments.variables;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.entity.Player;
import com.notquests.paper.NotQuests;
import com.notquests.paper.commands.framework.NQArgumentType;
import com.notquests.paper.structs.QuestPlayer;
import com.notquests.paper.variables.Variable;

import java.util.ArrayList;
import java.util.List;

/**
 * Native-framework port of {@code ListVariableValueParser}: a generic "variable value" argument whose
 * value is the raw string. Like the Cloud parser, {@code parse} reads a single token, so this keeps
 * the default <b>non-greedy</b> {@code string()} native type.
 */
public final class ListVariableArgument extends NQArgumentType<String> {
    private final NotQuests main;

    private final String identifier;
    private final Variable<?> variable;

    public ListVariableArgument(final String identifier, final Variable<?> variable) {
        this.main = NotQuests.getInstance();
        this.identifier = identifier;
        this.variable = variable;
    }

    public static ListVariableArgument listVariableArgument(final String identifier, final Variable<?> variable) {
        return new ListVariableArgument(identifier, variable);
    }

    public String getIdentifier() {
        return identifier;
    }

    @Override
    public String valueTypeName() {
        return "comma-separated list";
    }

    @Override
    public String convert(final String input) throws CommandSyntaxException {
        return input;
    }

    @Override
    protected List<String> suggest(final CommandContext<?> context, final String remaining) {
        final List<String> completions = new ArrayList<>();
        completions.add("<Enter Variables>");

        QuestPlayer questPlayer = null;
        if (context.getSource() instanceof CommandSourceStack source
                && source.getSender() instanceof Player player) {
            questPlayer = main.getQuestPlayerManager().getOrCreateQuestPlayer(player.getUniqueId());
        }
        final List<String> possibleValues = variable.getPossibleValues(questPlayer);
        if (possibleValues != null) {
            for (final String suggestion : possibleValues) {
                completions.add(suggestion);
            }
        }
        return completions;
    }
}
