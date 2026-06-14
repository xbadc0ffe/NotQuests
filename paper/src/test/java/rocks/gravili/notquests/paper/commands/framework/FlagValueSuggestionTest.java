/*
 * NotQuests - A Questing plugin for Minecraft Servers
 * Copyright (C) 2022 Alessio Gravili
 *
 * Licensed under the GNU General Public License v3. See the LICENSE file.
 */

package rocks.gravili.notquests.paper.commands.framework;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.suggestion.Suggestion;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.managers.UtilManager;

class FlagValueSuggestionTest {
    private NQCommandManager manager;

    @BeforeEach
    void setUp() {
        final NotQuests main = mock(NotQuests.class, RETURNS_DEEP_STUBS);
        final UtilManager utilManager = mock(UtilManager.class);
        when(main.getUtilManager()).thenReturn(utilManager);
        manager = new NQCommandManager(main, new NQCommands(main));
        manager.command(manager.commandBuilder("root", NQDescription.of("root"))
                .literal("action", NQDescription.of("action"))
                .flag(NQFlag.builder("delay", NQDescription.of("Delay before running the action."))
                        .withArgument(NQArguments.durationArgument())
                        .build())
                .flag(NQFlag.builder("player", NQDescription.of("Player used as the action target."))
                        .withArgument(NQArguments.stringArgument())
                        .build())
                .handler(context -> {}));
    }

    @SuppressWarnings("unchecked")
    private LiteralCommandNode<CommandSourceStack> compiledRoot() throws Exception {
        final var rootsField = NQCommandManager.class.getDeclaredField("roots");
        rootsField.setAccessible(true);
        final Map<String, Object> roots = (Map<String, Object>) rootsField.get(manager);
        final Object root = roots.get("root");

        final Method compile = NQCommandManager.class.getDeclaredMethod("compile", root.getClass(), List.class);
        compile.setAccessible(true);
        return (LiteralCommandNode<CommandSourceStack>) compile.invoke(manager, root, List.of(root));
    }

    private List<String> completionsFor(final String input) throws Exception {
        final CommandDispatcher<CommandSourceStack> dispatcher = new CommandDispatcher<>();
        dispatcher.getRoot().addChild(compiledRoot());
        return dispatcher.getCompletionSuggestions(dispatcher.parse(input, null))
                .get()
                .getList()
                .stream()
                .map(Suggestion::getText)
                .toList();
    }

    @Test
    void delayFlagSuggestsDurationsInsteadOfFlagNamesWhenAwaitingValue() throws Exception {
        final List<String> suggestions = completionsFor("root action --delay ");
        assertTrue(suggestions.contains("500ms"), "delay value suggestions should include millisecond examples: " + suggestions);
        assertTrue(suggestions.contains("1s"), "delay value suggestions should include duration examples: " + suggestions);
        assertFalse(suggestions.contains("--delay"), "delay value position must not re-suggest flags: " + suggestions);
        assertFalse(suggestions.contains("--player"), "delay value position must not re-suggest flags: " + suggestions);
    }

    @Test
    void valueFlagWithNoSuggestionsDoesNotFallBackToFlagNamesWhenAwaitingValue() throws Exception {
        final List<String> suggestions = completionsFor("root action --player ");
        assertFalse(suggestions.contains("--delay"), "player value position must not re-suggest flags: " + suggestions);
        assertFalse(suggestions.contains("--player"), "player value position must not re-suggest flags: " + suggestions);
    }
}
