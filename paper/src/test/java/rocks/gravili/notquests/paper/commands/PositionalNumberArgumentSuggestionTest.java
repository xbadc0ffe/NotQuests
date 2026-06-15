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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package rocks.gravili.notquests.paper.commands;

import static com.mojang.brigadier.builder.LiteralArgumentBuilder.literal;
import static com.mojang.brigadier.builder.RequiredArgumentBuilder.argument;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static rocks.gravili.notquests.paper.commands.arguments.variables.NumberVariableArgument.numberVariableArgument;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.suggestion.Suggestion;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import rocks.gravili.notquests.paper.commands.framework.NQArguments;

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
                        .then(argument("world", NQArguments.worldArgument()))));

        final List<String> completions = dispatcher.getCompletionSuggestions(
                        dispatcher.parse("Interact 1 ", new Object()))
                .get()
                .getList()
                .stream()
                .map(Suggestion::getText)
                .toList();

        assertTrue(completions.contains("world"), "world suggestions should appear after amount: " + completions);
        assertTrue(completions.contains("arena"), "all loaded world names should be suggested: " + completions);
    }
}
