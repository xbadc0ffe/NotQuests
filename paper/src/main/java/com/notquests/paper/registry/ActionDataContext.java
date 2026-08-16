package com.notquests.paper.registry;

import java.time.Duration;
import com.notquests.paper.commands.arguments.ActionList;
import com.notquests.paper.commands.arguments.wrappers.ItemStackSelection;
import com.notquests.paper.structs.Quest;

public record ActionDataContext(DefinedAction action) {
    public String text(final String name) {
        return action.text(name);
    }

    public int integer(final String name) {
        return action.value(name, 0);
    }

    public int integer(final String name, final int fallback) {
        return action.value(name, fallback);
    }

    public boolean flag(final String name) {
        return Boolean.TRUE.equals(action.value(name, Boolean.class));
    }

    public Duration duration(final String name, final Duration fallback) {
        final Duration value = action.value(name, Duration.class);
        return value == null ? fallback : value;
    }

    public Quest quest(final String name) {
        return action.value(name, Quest.class);
    }

    public ItemStackSelection itemSelection(final String name) {
        return action.value(name, ItemStackSelection.class);
    }

    public ActionList actionList(final String name) {
        return action.value(name, ActionList.class);
    }
}
