/*
 * NotQuests - A Questing plugin for Minecraft Servers
 * Copyright (C) 2021-2022 Alessio Gravili
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package rocks.gravili.notquests.paper.registry;

import rocks.gravili.notquests.paper.commands.arguments.wrappers.ItemStackSelection;
import rocks.gravili.notquests.paper.structs.ActiveObjective;
import rocks.gravili.notquests.paper.structs.QuestPlayer;

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

    public ItemStackSelection itemSelection(final String name) {
        return objective.value(name, ItemStackSelection.class);
    }
}
