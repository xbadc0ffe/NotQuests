package com.notquests.paper.registry;

import com.notquests.paper.commands.arguments.wrappers.ItemStackSelection;
import com.notquests.paper.structs.ActiveObjective;
import com.notquests.paper.structs.QuestPlayer;

public final class ObjectiveEventContext {
    private final DefinedObjective objective;
    private final ActiveObjective activeObjective;

    ObjectiveEventContext(final DefinedObjective objective, final ActiveObjective activeObjective) {
        this.objective = objective;
        this.activeObjective = activeObjective;
    }

    public QuestPlayer questPlayer() {
        return activeObjective.getQuestPlayer();
    }

    public ActiveObjective activeObjective() {
        return activeObjective;
    }

    public void addProgress(final double amount) {
        activeObjective.addProgress(amount);
    }

    public void removeProgress(final double amount, final boolean capAtZero) {
        activeObjective.removeProgress(amount, capAtZero);
    }

    public boolean flag(final String name) {
        return Boolean.TRUE.equals(objective.value(name, Boolean.class));
    }

    public String text(final String name) {
        final String value = objective.value(name, String.class);
        return value == null ? "" : value;
    }

    public <V> V value(final String name, final Class<V> type) {
        return objective.value(name, type);
    }

    public ItemStackSelection itemSelection(final String name) {
        return objective.value(name, ItemStackSelection.class);
    }
}
