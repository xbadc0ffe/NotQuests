package com.notquests.paper.registry;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.bukkit.configuration.file.FileConfiguration;
import com.notquests.paper.NotQuests;
import com.notquests.paper.commands.framework.NQCommandBuilder;
import com.notquests.paper.commands.framework.NQCommandContext;
import com.notquests.paper.triggers.TriggerCatalog;
import com.notquests.paper.metadata.NQMetadataSchema.TypeInfo;

public final class TriggerType {
    private final NotQuests main;
    private final TriggerCatalog manager;
    private final String id;
    private final String displayName;
    private final String description;
    private final List<TriggerTypeField<?>> fields;
    private final TriggerDescriptionRenderer descriptionRenderer;

    TriggerType(final Builder builder) {
        this.main = builder.main;
        this.manager = builder.manager;
        this.id = FieldType.requireText(builder.id, "trigger id");
        this.displayName = FieldType.requireText(builder.displayName, "trigger display name");
        this.description = FieldType.requireText(builder.description, "trigger description");
        this.fields = List.copyOf(builder.fields);
        this.descriptionRenderer = Objects.requireNonNull(builder.descriptionRenderer, "trigger description renderer");
    }

    public String id() {
        return id;
    }

    public String displayName() {
        return displayName;
    }

    public String description() {
        return description;
    }

    public DefinedTrigger createTrigger() {
        return new DefinedTrigger(main, this);
    }

    public void registerCommands(final NQCommandBuilder builder) {
        NQCommandBuilder command = builder;
        for (final TriggerTypeField<?> field : fields) {
            command = field.appendTo(main, command);
        }
        command = command
                .flag(main.getCommandManager().applyOn)
                .flag(main.getCommandManager().triggerWorldString);
        main.getCommandManager().getNQCommandManager().command(command.handler(context -> {
            final DefinedTrigger trigger = createTrigger();
            applyCommandValues(trigger, context);
            main.getTriggerCatalog().addTrigger(trigger, context);
        }));
    }

    private void applyCommandValues(final DefinedTrigger trigger, final NQCommandContext context) {
        for (final TriggerTypeField<?> field : fields) {
            trigger.setRawValue(field, field.readFromCommand(context));
        }
    }

    void save(final DefinedTrigger trigger, final FileConfiguration configuration, final String path) {
        for (final TriggerTypeField<?> field : fields) {
            saveField(trigger, configuration, path, field);
        }
    }

    private <V> void saveField(
            final DefinedTrigger trigger,
            final FileConfiguration configuration,
            final String path,
            final TriggerTypeField<V> field) {
        field.type().save(main, configuration, path, trigger.value(field.name(), field.type().fallback()));
    }

    void load(final DefinedTrigger trigger, final FileConfiguration configuration, final String path) {
        for (final TriggerTypeField<?> field : fields) {
            loadField(trigger, configuration, path, field);
        }
    }

    private <V> void loadField(
            final DefinedTrigger trigger,
            final FileConfiguration configuration,
            final String path,
            final TriggerTypeField<V> field) {
        trigger.setRawValue(field, field.type().load(main, configuration, path));
    }

    String triggerDescription(final DefinedTrigger trigger) {
        return descriptionRenderer.render(new TriggerDataContext(trigger));
    }

    public TypeInfo metadata(final String source, final boolean integrationOnly) {
        return new TypeInfo(
                id,
                displayName,
                DefinedTrigger.class.getName(),
                description,
                source,
                integrationOnly,
                fields.stream().map(field -> field.metadata(main)).toList(),
                List.of());
    }

    @FunctionalInterface
    public interface TriggerDescriptionRenderer {
        String render(TriggerDataContext trigger);
    }

    public static final class Builder {
        private final NotQuests main;
        private final TriggerCatalog manager;
        private final String id;
        private String displayName;
        private String description;
        private final List<TriggerTypeField<?>> fields = new ArrayList<>();
        private TriggerDescriptionRenderer descriptionRenderer = trigger -> "";

        public Builder(final NotQuests main, final TriggerCatalog manager, final String id) {
            this.main = main;
            this.manager = manager;
            this.id = id;
        }

        public Builder displayName(final String displayName) {
            this.displayName = displayName;
            return this;
        }

        public Builder description(final String description) {
            this.description = description;
            return this;
        }

        public <V> Builder field(final String name, final FieldType<V> type, final String description) {
            fields.add(new TriggerTypeField<>(name, type, description));
            return this;
        }

        public Builder triggerDescription(final TriggerDescriptionRenderer renderer) {
            this.descriptionRenderer = Objects.requireNonNull(renderer, "renderer");
            return this;
        }

        public TriggerType register() {
            final TriggerType type = new TriggerType(this);
            manager.registerTrigger(type);
            return type;
        }
    }
}
