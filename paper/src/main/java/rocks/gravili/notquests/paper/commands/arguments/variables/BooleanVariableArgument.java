/*
 * NotQuests - A Questing plugin for Minecraft Servers
 * Copyright (C) 2022 Alessio Gravili
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package rocks.gravili.notquests.paper.commands.arguments.variables;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.incendo.cloud.context.CommandInput;
import org.incendo.cloud.parser.flag.CommandFlag;
import org.incendo.cloud.suggestion.Suggestion;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.commands.framework.NQArgumentType;
import rocks.gravili.notquests.paper.structs.QuestPlayer;
import rocks.gravili.notquests.paper.structs.variables.Variable;
import rocks.gravili.notquests.paper.structs.variables.VariableDataType;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

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

    public BooleanVariableArgument(final String identifier, final Variable<?> variable) {
        this.main = NotQuests.getInstance();
        this.identifier = identifier;
        this.variable = variable;
    }

    public static BooleanVariableArgument booleanVariableArgument(final String identifier, final Variable<?> variable) {
        return new BooleanVariableArgument(identifier, variable);
    }

    public String getIdentifier() {
        return identifier;
    }

    @Override
    public ArgumentType<String> getNativeType() {
        return StringArgumentType.greedyString();
    }

    @Override
    public String convert(final String input) throws CommandSyntaxException {
        return input;
    }

    @Override
    protected List<String> suggest(final CommandContext<?> context, final String remaining) {
        final CommandSender sender =
                context.getSource() instanceof CommandSourceStack source ? source.getSender() : null;

        final List<String> completions = new ArrayList<>();
        completions.add("true");
        completions.add("false");

        final String rawInput = remaining;
        for (final String variableString : main.getVariablesManager().getVariableIdentifiers()) {
            final Variable<?> variable = main.getVariablesManager().getVariableFromString(variableString);
            if (variable == null || (variable.getVariableDataType() != VariableDataType.NUMBER && variable.getVariableDataType() != VariableDataType.BOOLEAN)) {
                continue;
            }
            if (variable.getRequiredStrings().isEmpty() && variable.getRequiredNumbers().isEmpty() && variable.getRequiredBooleans().isEmpty() && variable.getRequiredBooleanFlags().isEmpty()) {
                completions.add(variableString);
            } else {
                if (!rawInput.endsWith(variableString + "(")) {
                    if (rawInput.endsWith(",") && rawInput.contains(variableString + "(")) {
                        for (StringVariableValueParser<CommandSender> stringParser : variable.getRequiredStrings()) {
                            if (!rawInput.contains(stringParser.getIdentifier())) {
                                completions.add(rawInput + stringParser.getIdentifier() + ":");
                            }
                        }
                        for (NumberVariableValueParser<CommandSender> numberParser : variable.getRequiredNumbers()) {
                            if (!rawInput.contains(numberParser.getIdentifier())) {
                                completions.add(rawInput + numberParser.getIdentifier() + ":");
                            }
                        }
                        for (BooleanVariableValueParser<CommandSender> booleanParser : variable.getRequiredBooleans()) {
                            if (!rawInput.contains(booleanParser.getIdentifier())) {
                                completions.add(rawInput + booleanParser.getIdentifier() + ":");
                            }
                        }
                        for (CommandFlag<Void> flag : variable.getRequiredBooleanFlags()) {
                            if (!rawInput.contains(flag.name())) {
                                completions.add(rawInput + "--" + flag.name() + "");
                            }
                        }
                    } else if (!rawInput.endsWith(")")) {
                        if (rawInput.contains(variableString + "(") && (!rawInput.contains(")") || (rawInput.lastIndexOf("(") < rawInput.lastIndexOf(")")))) {
                            final String subStringAfter = rawInput.substring(rawInput.indexOf(variableString + "("));

                            try {
                                for (final StringVariableValueParser<CommandSender> stringParser : variable.getRequiredStrings()) {
                                    if (subStringAfter.contains(":")) {
                                        Iterable<? extends Suggestion> suggestions = nestedSuggestions(stringParser.suggestionProvider(), sender, rawInput);
                                        if (subStringAfter.endsWith(":")) {
                                            suggestions.forEach(suggestion -> completions.add(rawInput + suggestion.suggestion()));
                                        } else {
                                            final String[] splitDoubleDots = subStringAfter.split(":");
                                            final String stringAfterLastDoubleDot = splitDoubleDots[splitDoubleDots.length - 1];
                                            suggestions.forEach(suggestion -> completions.add(rawInput.substring(0, rawInput.length() - stringAfterLastDoubleDot.length() - 1) + ":" + suggestion.suggestion()));
                                        }
                                    } else {
                                        completions.add(variableString + "(" + stringParser.getIdentifier() + ":");
                                    }
                                }
                                for (final NumberVariableValueParser<CommandSender> numberParser : variable.getRequiredNumbers()) {
                                    if (subStringAfter.contains(":")) {
                                        Iterable<? extends Suggestion> suggestions = nestedSuggestions(numberParser.suggestionProvider(), sender, rawInput);
                                        if (subStringAfter.endsWith(":")) {
                                            suggestions.forEach(suggestion -> completions.add(rawInput + suggestion.suggestion()));
                                        } else {
                                            final String[] splitDoubleDots = subStringAfter.split(":");
                                            final String stringAfterLastDoubleDot = splitDoubleDots[splitDoubleDots.length - 1];
                                            suggestions.forEach(suggestion -> completions.add(rawInput.substring(0, rawInput.length() - stringAfterLastDoubleDot.length() - 1) + ":" + suggestion.suggestion()));
                                        }
                                    } else {
                                        completions.add(variableString + "(" + numberParser.getIdentifier() + ":");
                                    }
                                }
                                for (BooleanVariableValueParser<CommandSender> booleanParser : variable.getRequiredBooleans()) {
                                    if (subStringAfter.contains(":")) {
                                        Iterable<? extends Suggestion> suggestions = nestedSuggestions(booleanParser.suggestionProvider(), sender, rawInput);
                                        if (subStringAfter.endsWith(":")) {
                                            suggestions.forEach(suggestion -> completions.add(rawInput + suggestion.suggestion()));
                                        } else {

                                            final String[] splitDoubleDots = subStringAfter.split(":");
                                            final String stringAfterLastDoubleDot = splitDoubleDots[splitDoubleDots.length - 1];
                                            suggestions.forEach(suggestion -> completions.add(rawInput.substring(0, rawInput.length() - stringAfterLastDoubleDot.length() - 1) + ":" + suggestion.suggestion()));
                                        }
                                    } else {
                                        completions.add(variableString + "(" + booleanParser.getIdentifier() + ":");
                                    }
                                }
                            } catch (InterruptedException | ExecutionException e) {
                                throw new RuntimeException(e);
                            }

                            for (CommandFlag<Void> flag : variable.getRequiredBooleanFlags()) {
                                completions.add(variableString + "(--" + flag.name() + "");
                            }
                        } else {
                            completions.add(variableString + "(");
                        }
                    }
                } else {
                    for (StringVariableValueParser<CommandSender> stringParser : variable.getRequiredStrings()) {
                        completions.add(variableString + "(" + stringParser.getIdentifier() + ":");
                    }
                    for (NumberVariableValueParser<CommandSender> numberParser : variable.getRequiredNumbers()) {
                        completions.add(variableString + "(" + numberParser.getIdentifier() + ":");
                    }
                    for (BooleanVariableValueParser<CommandSender> booleanParser : variable.getRequiredBooleans()) {
                        completions.add(variableString + "(" + booleanParser.getIdentifier() + ":");
                    }
                    for (CommandFlag<Void> flag : variable.getRequiredBooleanFlags()) {
                        completions.add(variableString + "(--" + flag.name() + "");
                    }
                }
            }
        }

        if (sender instanceof final Player player) {
            final QuestPlayer questPlayer = main.getQuestPlayerManager().getOrCreateQuestPlayer(player.getUniqueId());
            if (variable.getPossibleValues(questPlayer) == null) {
                return completions;
            }
            for (final Suggestion suggestion : variable.getPossibleValues(questPlayer)) {
                completions.add(suggestion.suggestion());
            }
        } else {
            if (variable.getPossibleValues(null) == null) {
                return completions;
            }
            for (final Suggestion suggestion : variable.getPossibleValues(null)) {
                completions.add(suggestion.suggestion());
            }
        }
        return completions;
    }

    /**
     * Drives a nested Cloud required-parser suggestion provider with the live sender and the current
     * partial input, reproducing the Cloud {@code suggestionsFuture(context.get(identifier), input)}
     * recursion from the original parser.
     */
    private Iterable<? extends Suggestion> nestedSuggestions(
            final org.incendo.cloud.suggestion.SuggestionProvider<CommandSender> provider,
            final CommandSender sender,
            final String rawInput)
            throws InterruptedException, ExecutionException {
        final org.incendo.cloud.context.CommandContext<CommandSender> cloudContext =
                new org.incendo.cloud.context.CommandContext<>(
                        true, sender, main.getCommandManager().getPaperCommandManager());
        cloudContext.store(identifier, rawInput);
        return provider.suggestionsFuture(cloudContext, CommandInput.of(rawInput)).get();
    }
}
