package com.notquests.paper.commands.arguments;

import static com.mojang.brigadier.builder.LiteralArgumentBuilder.literal;
import static com.mojang.brigadier.builder.RequiredArgumentBuilder.argument;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.suggestion.Suggestion;
import java.util.List;
import org.junit.jupiter.api.Test;
import com.notquests.paper.NotQuests;
import com.notquests.paper.commands.framework.NQArguments;

class ItemStackSelectionArgumentTest {

    @Test
    void serverParserStopsBeforeFollowingAmount() throws Exception {
        final ItemStackSelectionArgument argument =
                ItemStackSelectionArgument.itemStackSelectionArgument(mock(NotQuests.class, RETURNS_DEEP_STUBS));
        final StringReader reader = new StringReader("acacia_boat,acacia_boat 4");

        argument.parse(reader);

        assertEquals(" 4", reader.getRemaining(), "item selection must not swallow the following amount argument");
    }

    @Test
    void amountSuggestionsAppearAfterItemSelection() throws Exception {
        final ItemStackSelectionArgument itemSelection =
                ItemStackSelectionArgument.itemStackSelectionArgument(mock(NotQuests.class, RETURNS_DEEP_STUBS));
        final CommandDispatcher<Object> dispatcher = new CommandDispatcher<>();
        dispatcher.register(literal("execute")
                .then(literal("GiveItem")
                        .then(argument("material", itemSelection)
                                .then(argument("amount", NQArguments.integerArgument())))));

        final List<String> completions = dispatcher.getCompletionSuggestions(
                        dispatcher.parse("execute GiveItem acacia_boat ", new Object()))
                .get()
                .getList()
                .stream()
                .map(Suggestion::getText)
                .toList();

        assertTrue(completions.contains("1"), "amount should suggest '1' after item selection: " + completions);
        assertTrue(completions.contains("64"), "amount should suggest '64' after item selection: " + completions);
    }
}
