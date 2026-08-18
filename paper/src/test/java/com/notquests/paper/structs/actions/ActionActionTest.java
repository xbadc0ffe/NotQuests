package com.notquests.paper.actions;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyBoolean;
import static org.mockito.Mockito.anyInt;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.nullable;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicReference;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import com.notquests.paper.NotQuests;
import com.notquests.paper.builtin.actions.ActionChain;
import com.notquests.paper.commands.arguments.ActionList;
import com.notquests.paper.managers.data.Category;
import com.notquests.paper.actions.ActionCatalog;
import com.notquests.paper.actions.ActionRunner;
import com.notquests.paper.registry.ActionType;
import com.notquests.paper.registry.DefinedAction;
import com.notquests.paper.structs.QuestPlayer;
import com.notquests.paper.conditions.Condition;

class ActionActionTest {
    private NotQuests main;
    private ActionCatalog actionCatalog;
    private ActionRunner actionRunner;

    @BeforeEach
    void setUp() {
        MockBukkit.mock();
        main = mock(NotQuests.class, RETURNS_DEEP_STUBS);
        actionCatalog = mock(ActionCatalog.class);
        actionRunner = mock(ActionRunner.class);
        when(main.getDataManager().getDefaultCategory()).thenReturn(mock(Category.class));
        when(main.getDataManager().isDisabled()).thenReturn(false);
        when(main.getActionCatalog()).thenReturn(actionCatalog);
        when(main.getActionRunner()).thenReturn(actionRunner);
        doAnswer(invocation -> {
                    final Action action = invocation.getArgument(0);
                    final QuestPlayer questPlayer = invocation.getArgument(1);
                    action.executeInternally(questPlayer);
                    return null;
                })
                .when(actionRunner)
                .executeActionWithConditions(
                        any(Action.class),
                        nullable(QuestPlayer.class),
                        nullable(CommandSender.class),
                        anyBoolean(),
                        anyInt(),
                        any(Object[].class));
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void randomActionSkipsConditionFailuresWhenOnlyCountingFulfilledActions() {
        final RecordingAction blockedAction = new RecordingAction(main);
        blockedAction.getConditions().add(new StaticCondition(main, false));
        final RecordingAction eligibleAction = new RecordingAction(main);

        final ActionType actionType = actionChainType();
        final DefinedAction actionAction = actionType.createAction();
        final ActionList actions = new ActionList();
        actions.addValue(blockedAction);
        actions.addValue(eligibleAction);
        actionAction.setValue("actions", actions);
        actionAction.setValue("amount", 1);
        actionAction.setValue("minRandom", 1);
        actionAction.setValue("maxRandom", 1);
        actionAction.setValue("onlyCountForRandomIfConditionsFulfilled", true);

        ((Action) actionAction).executeInternally(null);

        assertFalse(blockedAction.executed, "action with failed conditions must be skipped");
        assertTrue(eligibleAction.executed, "random quota should continue to the next eligible action");
    }

    private ActionType actionChainType() {
        final AtomicReference<ActionType> registeredType = new AtomicReference<>();
        when(actionCatalog.action("Action")).thenReturn(new ActionType.Builder(main, actionCatalog, "Action"));
        doAnswer(invocation -> {
                    registeredType.set(invocation.getArgument(0));
                    return null;
                })
                .when(actionCatalog)
                .registerAction(any(ActionType.class));
        ActionChain.register(main, actionCatalog);
        return registeredType.get();
    }

    private static final class RecordingAction extends Action {
        private boolean executed;

        private RecordingAction(final NotQuests main) {
            super(main);
        }

        @Override
        protected void executeInternally(final QuestPlayer questPlayer, final Object... objects) {
            executed = true;
        }

        @Override
        public String getActionDescription(final QuestPlayer questPlayer, final Object... objects) {
            return "recording action";
        }

        @Override
        public void save(final FileConfiguration configuration, final String initialPath) {
        }

        @Override
        public void load(final FileConfiguration configuration, final String initialPath) {
        }

        @Override
        public void deserializeFromSingleLineString(final ArrayList<String> arguments) {
        }
    }

    private static final class StaticCondition extends Condition {
        private final boolean fulfilled;

        private StaticCondition(final NotQuests main, final boolean fulfilled) {
            super(main);
            this.fulfilled = fulfilled;
        }

        @Override
        protected String checkInternally(final QuestPlayer questPlayer) {
            return fulfilled ? "" : "blocked";
        }

        @Override
        protected String getConditionDescriptionInternally(
                final QuestPlayer questPlayer, final Object... objects) {
            return "static condition";
        }

        @Override
        public void save(final FileConfiguration configuration, final String initialPath) {
        }

        @Override
        public void load(final FileConfiguration configuration, final String initialPath) {
        }

        @Override
        public void deserializeFromSingleLineString(final ArrayList<String> arguments) {
        }
    }
}
