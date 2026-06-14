/*
 * NotQuests - A Questing plugin for Minecraft Servers
 * Copyright (C) 2022 Alessio Gravili
 *
 * Licensed under the GNU General Public License v3. See the LICENSE file.
 */

package rocks.gravili.notquests.paper.structs.actions;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import org.bukkit.configuration.file.FileConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.managers.data.Category;
import rocks.gravili.notquests.paper.structs.QuestPlayer;
import rocks.gravili.notquests.paper.structs.conditions.Condition;

class ActionActionTest {
    private NotQuests main;

    @BeforeEach
    void setUp() {
        MockBukkit.mock();
        main = mock(NotQuests.class, RETURNS_DEEP_STUBS);
        main.allActions = new ArrayList<>();
        main.allConditions = new ArrayList<>();
        when(main.getDataManager().getDefaultCategory()).thenReturn(mock(Category.class));
        when(main.getDataManager().isDisabled()).thenReturn(false);
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

        final ActionAction actionAction = new ActionAction(main);
        actionAction.setActions(new ArrayList<>(java.util.List.of(blockedAction, eligibleAction)));
        actionAction.setAmount(1);
        actionAction.setMinRandom(1);
        actionAction.setMaxRandom(1);
        actionAction.setOnlyCountForRandomIfConditionsFulfilled(true);

        actionAction.executeInternally(null);

        assertFalse(blockedAction.executed, "action with failed conditions must be skipped");
        assertTrue(eligibleAction.executed, "random quota should continue to the next eligible action");
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
