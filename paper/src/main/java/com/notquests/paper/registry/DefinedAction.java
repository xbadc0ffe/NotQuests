package com.notquests.paper.registry;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import org.bukkit.configuration.file.FileConfiguration;
import com.notquests.paper.NotQuests;
import com.notquests.paper.structs.QuestPlayer;
import com.notquests.paper.actions.Action;

public final class DefinedAction extends Action {
    private final ActionType definition;
    private final Map<String, Object> values = new HashMap<>();

    public DefinedAction(final NotQuests main, final ActionType definition) {
        super(main);
        this.definition = definition;
    }

    public ActionType definition() {
        return definition;
    }

    public boolean isType(final String id) {
        return definition.id().equals(id);
    }

    <V> void setRawValue(final ActionTypeField<V> field, final Object value) {
        setValue(field.name(), value);
    }

    public void setValue(final String name, final Object value) {
        values.put(name, value);
    }

    @SuppressWarnings("unchecked")
    public <V> V value(final String name, final V fallback) {
        return values.containsKey(name) ? (V) values.get(name) : fallback;
    }

    public <V> V value(final String name, final Class<V> type) {
        final Object value = values.get(name);
        return value == null ? null : type.cast(value);
    }

    public String text(final String name) {
        final String value = value(name, String.class);
        return value == null ? "" : value;
    }

    @Override
    public String getActionDescription(final QuestPlayer questPlayer, final Object... objects) {
        return definition.actionDescription(this, questPlayer, objects);
    }

    @Override
    protected void executeInternally(final QuestPlayer questPlayer, final Object... objects) {
        definition.execute(this, questPlayer, objects);
    }

    @Override
    public void save(final FileConfiguration configuration, final String initialPath) {
        definition.save(this, configuration, initialPath);
    }

    @Override
    public void load(final FileConfiguration configuration, final String initialPath) {
        definition.load(this, configuration, initialPath);
    }

    @Override
    public void deserializeFromSingleLineString(final ArrayList<String> arguments) {
        definition.deserialize(this, arguments);
    }
}
