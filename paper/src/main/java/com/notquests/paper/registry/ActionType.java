package com.notquests.paper.registry;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.bukkit.configuration.file.FileConfiguration;
import com.notquests.paper.NotQuests;
import com.notquests.paper.commands.framework.NQCommandBuilder;
import com.notquests.paper.commands.framework.NQCommandContext;
import com.notquests.paper.actions.ActionCatalog;
import com.notquests.paper.metadata.NQMetadataSchema.TypeInfo;
import com.notquests.paper.structs.QuestPlayer;
import com.notquests.paper.actions.ActionFor;

public final class ActionType {
    private final NotQuests main;
    private final ActionCatalog manager;
    private final String id;
    private final String displayName;
    private final String description;
    private final List<ActionTypeField<?>> fields;
    private final List<ActionTypeField<?>> flags;
    private final ActionDescriptionRenderer actionDescriptionRenderer;
    private final ActionExecutor executor;
    private final SingleLineParser singleLineParser;
    private final CommandRegistrar commandRegistrar;
    private final boolean typeLiteral;

    ActionType(final Builder builder) {
        this.main = builder.main;
        this.manager = builder.manager;
        this.id = FieldType.requireText(builder.id, "action id");
        this.displayName = FieldType.requireText(builder.displayName, "action display name");
        this.description = FieldType.requireText(builder.description, "action description");
        this.fields = List.copyOf(builder.fields);
        this.flags = List.copyOf(builder.flags);
        this.actionDescriptionRenderer = Objects.requireNonNull(
                builder.actionDescriptionRenderer, "action description renderer");
        this.executor = Objects.requireNonNull(builder.executor, "action executor");
        this.singleLineParser = builder.singleLineParser;
        this.commandRegistrar = builder.commandRegistrar;
        this.typeLiteral = builder.typeLiteral;
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

    public List<ActionTypeField<?>> fields() {
        return fields;
    }

    public List<ActionTypeField<?>> flags() {
        return flags;
    }

    public boolean usesTypeLiteral() {
        return typeLiteral;
    }

    public DefinedAction createAction() {
        return new DefinedAction(main, this);
    }

    public void registerCommands(final NQCommandBuilder builder, final ActionFor actionFor) {
        if (commandRegistrar != null) {
            commandRegistrar.register(this, builder, actionFor);
            return;
        }
        NQCommandBuilder command = builder;
        for (final ActionTypeField<?> field : fields) {
            command = field.appendTo(main, command);
        }
        for (final ActionTypeField<?> flag : flags) {
            command = command.flag(flag.commandFlag(main));
        }
        main.getCommandManager().getNQCommandManager().command(command.handler(context -> {
            final DefinedAction action = createAction();
            applyCommandValues(action, context);
            main.getActionCatalog().addAction(action, context, actionFor);
        }));
    }

    private void applyCommandValues(final DefinedAction action, final NQCommandContext context) {
        for (final ActionTypeField<?> field : fields) {
            action.setRawValue(field, field.readFromCommand(context));
        }
        for (final ActionTypeField<?> flag : flags) {
            action.setRawValue(flag, flag.readFromCommand(context));
        }
    }

    void save(final DefinedAction action, final FileConfiguration configuration, final String path) {
        for (final ActionTypeField<?> field : fields) {
            saveField(action, configuration, path, field);
        }
        for (final ActionTypeField<?> flag : flags) {
            saveField(action, configuration, path, flag);
        }
    }

    private <V> void saveField(
            final DefinedAction action,
            final FileConfiguration configuration,
            final String path,
            final ActionTypeField<V> field) {
        field.type().save(main, configuration, path, action.value(field.name(), field.type().fallback()));
    }

    void load(final DefinedAction action, final FileConfiguration configuration, final String path) {
        for (final ActionTypeField<?> field : fields) {
            loadField(action, configuration, path, field);
        }
        for (final ActionTypeField<?> flag : flags) {
            loadField(action, configuration, path, flag);
        }
    }

    private <V> void loadField(
            final DefinedAction action,
            final FileConfiguration configuration,
            final String path,
            final ActionTypeField<V> field) {
        action.setRawValue(field, field.type().load(main, configuration, path));
    }

    String actionDescription(
            final DefinedAction action, final QuestPlayer questPlayer, final Object... objects) {
        return actionDescriptionRenderer.render(new ActionDataContext(action), questPlayer, objects);
    }

    void execute(final DefinedAction action, final QuestPlayer questPlayer, final Object... objects) {
        executor.execute(new ActionDataContext(action), questPlayer, objects);
    }

    void deserialize(final DefinedAction action, final ArrayList<String> arguments) {
        if (singleLineParser == null) {
            throw new IllegalStateException("Action " + id + " cannot be used as a conversation inline action.");
        }
        singleLineParser.parse(action, arguments);
    }

    public TypeInfo metadata(final String source, final boolean integrationOnly) {
        return new TypeInfo(
                id,
                displayName,
                DefinedAction.class.getName(),
                description,
                source,
                integrationOnly,
                fields.stream().map(field -> field.metadata(main)).toList(),
                flags.stream().map(flag -> flag.metadata(main)).toList());
    }

    @FunctionalInterface
    public interface ActionDescriptionRenderer {
        String render(ActionDataContext action, QuestPlayer questPlayer, Object... objects);
    }

    @FunctionalInterface
    public interface ActionExecutor {
        void execute(ActionDataContext action, QuestPlayer questPlayer, Object... objects);
    }

    @FunctionalInterface
    public interface SingleLineParser {
        void parse(DefinedAction action, ArrayList<String> arguments);
    }

    @FunctionalInterface
    public interface CommandRegistrar {
        void register(ActionType type, NQCommandBuilder builder, ActionFor actionFor);
    }

    public static final class Builder {
        private final NotQuests main;
        private final ActionCatalog manager;
        private final String id;
        private String displayName;
        private String description;
        private final List<ActionTypeField<?>> fields = new ArrayList<>();
        private final List<ActionTypeField<?>> flags = new ArrayList<>();
        private ActionDescriptionRenderer actionDescriptionRenderer;
        private ActionExecutor executor;
        private SingleLineParser singleLineParser;
        private CommandRegistrar commandRegistrar;
        private boolean typeLiteral = true;

        public Builder(final NotQuests main, final ActionCatalog manager, final String id) {
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
            fields.add(new ActionTypeField<>(name, type, description, false));
            return this;
        }

        public <V> Builder flag(final String name, final FieldType<V> type, final String description) {
            flags.add(new ActionTypeField<>(name, type, description, true));
            return this;
        }

        public Builder actionDescription(final ActionDescriptionRenderer renderer) {
            this.actionDescriptionRenderer = Objects.requireNonNull(renderer, "renderer");
            return this;
        }

        public Builder execute(final ActionExecutor executor) {
            this.executor = Objects.requireNonNull(executor, "executor");
            return this;
        }

        public Builder singleLine(final SingleLineParser parser) {
            this.singleLineParser = Objects.requireNonNull(parser, "parser");
            return this;
        }

        public Builder commands(final CommandRegistrar registrar) {
            this.commandRegistrar = Objects.requireNonNull(registrar, "registrar");
            return this;
        }

        public Builder withoutTypeLiteral() {
            this.typeLiteral = false;
            return this;
        }

        public ActionType register() {
            final ActionType type = new ActionType(this);
            manager.registerAction(type);
            return type;
        }
    }
}
