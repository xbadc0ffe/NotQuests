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

import java.util.HashMap;
import java.util.Map;
import org.bukkit.configuration.file.FileConfiguration;
import org.checkerframework.checker.nullness.qual.Nullable;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.structs.ActiveObjective;
import rocks.gravili.notquests.paper.structs.QuestPlayer;
import rocks.gravili.notquests.paper.structs.objectives.Objective;

public final class DefinedObjective extends Objective {
    private final ObjectiveType definition;
    private final Map<String, Object> values = new HashMap<>();

    public DefinedObjective(final NotQuests main, final ObjectiveType definition) {
        super(main);
        this.definition = definition;
    }

    public ObjectiveType definition() {
        return definition;
    }

    <V> void setRawValue(final ObjectiveTypeField<V> field, final Object value) {
        setValue(field.name(), value);
        if (field.type().isProgressNeeded() && value instanceof String expression) {
            setProgressNeededExpression(expression);
        }
    }

    public void setValue(final String name, final Object value) {
        values.put(name, value);
    }

    @SuppressWarnings("unchecked")
    <V> V value(final String name, final V fallback) {
        return values.containsKey(name) ? (V) values.get(name) : fallback;
    }

    public <V> V value(final String name, final Class<V> type) {
        final Object value = values.get(name);
        if (value == null) {
            return null;
        }
        return type.cast(value);
    }

    @Override
    public String getTaskDescriptionInternal(
            final QuestPlayer questPlayer, final @Nullable ActiveObjective activeObjective) {
        return definition.taskDescription(this, questPlayer, activeObjective);
    }

    @Override
    public void onObjectiveUnlock(
            final ActiveObjective activeObjective,
            final boolean unlockedDuringPluginStartupQuestLoadingProcess) {}

    @Override
    public void onObjectiveCompleteOrLock(
            final ActiveObjective activeObjective,
            final boolean lockedOrCompletedDuringPluginStartupQuestLoadingProcess,
            final boolean completed) {}

    @Override
    public void save(final FileConfiguration configuration, final String initialPath) {
        definition.save(this, configuration, initialPath);
    }

    @Override
    public void load(final FileConfiguration configuration, final String initialPath) {
        definition.load(this, configuration, initialPath);
    }
}
