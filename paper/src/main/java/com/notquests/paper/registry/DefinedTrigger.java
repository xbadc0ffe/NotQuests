package com.notquests.paper.registry;

import java.util.HashMap;
import java.util.Map;
import org.bukkit.configuration.file.FileConfiguration;
import com.notquests.paper.NotQuests;
import com.notquests.paper.triggers.Trigger;

public final class DefinedTrigger extends Trigger {
    private final TriggerType definition;
    private final Map<String, Object> values = new HashMap<>();

    public DefinedTrigger(final NotQuests main, final TriggerType definition) {
        super(main);
        this.definition = definition;
    }

    public TriggerType definition() {
        return definition;
    }

    <V> void setRawValue(final TriggerTypeField<V> field, final Object value) {
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
    public void save(final FileConfiguration configuration, final String initialPath) {
        definition.save(this, configuration, initialPath);
    }

    @Override
    public void load(final FileConfiguration configuration, final String initialPath) {
        definition.load(this, configuration, initialPath);
    }

    @Override
    public String getTriggerDescription() {
        return definition.triggerDescription(this);
    }
}
