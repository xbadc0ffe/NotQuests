package com.notquests.paper.commands;

import static com.mojang.brigadier.builder.LiteralArgumentBuilder.literal;
import static com.mojang.brigadier.builder.RequiredArgumentBuilder.argument;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static com.notquests.paper.commands.arguments.variables.NumberVariableArgument.numberVariableArgument;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.suggestion.Suggestion;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import com.notquests.paper.commands.framework.NQArguments;

class PositionalNumberArgumentSuggestionTest {
    private ServerMock server;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        server.addSimpleWorld("world");
        server.addSimpleWorld("arena");
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void followingArgumentSuggestionsAppearAfterPositionalNumberArgument() throws Exception {
        final CommandDispatcher<Object> dispatcher = new CommandDispatcher<>();
        dispatcher.register(literal("Interact")
                .then(argument("amount", numberVariableArgument("amount", null, false))
                        .then(argument("world", NQArguments.worldArgument()))
                        .then(literal("looking"))));

        final List<String> completions = dispatcher.getCompletionSuggestions(
                        dispatcher.parse("Interact 1 ", new Object()))
                .get()
                .getList()
                .stream()
                .map(Suggestion::getText)
                .toList();

        assertTrue(completions.contains("world"), "world suggestions should appear after amount: " + completions);
        assertTrue(completions.contains("arena"), "all loaded world names should be suggested: " + completions);
        assertTrue(completions.contains("looking"), "the looking shortcut should be suggested after amount: " + completions);
    }

    @Test
    void coordinateArgumentsSuggestUsefulNumberExamples() throws Exception {
        final CommandDispatcher<Object> dispatcher = new CommandDispatcher<>();
        dispatcher.register(literal("ShootArrow")
                .then(argument("amount", numberVariableArgument("amount", null, false))
                        .then(argument("world", NQArguments.worldArgument())
                                .then(argument("x", NQArguments.doubleArgument())
                                        .then(argument("y", NQArguments.doubleArgument())
                                                .then(argument("z", NQArguments.doubleArgument())
                                                        .then(argument("radius", NQArguments.doubleArgument()))))))
                        .then(literal("worldeditselection"))));

        assertTrue(
                completionsFor(dispatcher, "ShootArrow 1 ").contains("worldeditselection"),
                "WorldEdit selection shortcut should be suggested next to world names");
        assertTrue(
                completionsFor(dispatcher, "ShootArrow 1 world ").contains("4"),
                "x coordinate should suggest number examples");
        assertTrue(
                completionsFor(dispatcher, "ShootArrow 1 world 4 ").contains("5"),
                "y coordinate should suggest number examples");
        assertTrue(
                completionsFor(dispatcher, "ShootArrow 1 world 4 5 ").contains("4"),
                "z coordinate should suggest number examples");
        assertTrue(
                completionsFor(dispatcher, "ShootArrow 1 world 4 5 4 ").contains("5"),
                "radius should suggest number examples");
    }

    private static List<String> completionsFor(final CommandDispatcher<Object> dispatcher, final String input)
            throws Exception {
        return dispatcher.getCompletionSuggestions(dispatcher.parse(input, new Object()))
                .get()
                .getList()
                .stream()
                .map(Suggestion::getText)
                .toList();
    }
}
