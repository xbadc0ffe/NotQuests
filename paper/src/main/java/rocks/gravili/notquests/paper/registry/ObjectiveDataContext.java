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

public final class ObjectiveDataContext {
    private final DefinedObjective objective;

    ObjectiveDataContext(final DefinedObjective objective) {
        this.objective = objective;
    }

    public ItemStackSelection itemSelection(final String name) {
        return objective.value(name, ItemStackSelection.class);
    }

    public boolean flag(final String name) {
        return Boolean.TRUE.equals(objective.value(name, Boolean.class));
    }
}
