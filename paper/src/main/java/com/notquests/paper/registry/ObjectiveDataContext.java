package com.notquests.paper.registry;

import com.notquests.paper.commands.arguments.wrappers.ItemStackSelection;

public final class ObjectiveDataContext {
    private final DefinedObjective objective;

    ObjectiveDataContext(final DefinedObjective objective) {
        this.objective = objective;
    }

    public ItemStackSelection itemSelection(final String name) {
        return objective.value(name, ItemStackSelection.class);
    }

    public <V> V value(final String name, final Class<V> type) {
        return objective.value(name, type);
    }

    public boolean flag(final String name) {
        return Boolean.TRUE.equals(objective.value(name, Boolean.class));
    }

    public String text(final String name) {
        final String value = objective.value(name, String.class);
        return value == null ? "" : value;
    }
}
