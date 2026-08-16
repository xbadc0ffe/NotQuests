package com.notquests.paper.registry;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.bukkit.configuration.file.FileConfiguration;
import com.notquests.paper.NotQuests;
import com.notquests.paper.commands.framework.NQCommandBuilder;
import com.notquests.paper.commands.framework.NQCommandContext;
import com.notquests.paper.conditions.ConditionCatalog;
import com.notquests.paper.metadata.NQMetadataSchema.TypeInfo;
import com.notquests.paper.structs.QuestPlayer;
import com.notquests.paper.conditions.ConditionFor;

public final class ConditionType {
    private final NotQuests main;
    private final ConditionCatalog manager;
    private final String id;
    private final String displayName;
    private final String description;
    private final List<ConditionTypeField<?>> fields;
    private final List<ConditionTypeField<?>> flags;
    private final ConditionDescriptionRenderer conditionDescriptionRenderer;
    private final ConditionChecker checker;
    private final SingleLineParser singleLineParser;
    private final CommandRegistrar commandRegistrar;
    private final boolean typeLiteral;

    ConditionType(final Builder builder) {
        this.main = builder.main;
        this.manager = builder.manager;
        this.id = FieldType.requireText(builder.id, "condition id");
        this.displayName = FieldType.requireText(builder.displayName, "condition display name");
        this.description = FieldType.requireText(builder.description, "condition description");
        this.fields = List.copyOf(builder.fields);
        this.flags = List.copyOf(builder.flags);
        this.conditionDescriptionRenderer = Objects.requireNonNull(
                builder.conditionDescriptionRenderer, "condition description renderer");
        this.checker = Objects.requireNonNull(builder.checker, "condition checker");
        this.singleLineParser = builder.singleLineParser;
        this.commandRegistrar = builder.commandRegistrar;
        this.typeLiteral = builder.typeLiteral;
    }

    public String id() {
        return id;
    }

    public NotQuests main() {
        return main;
    }

    public String displayName() {
        return displayName;
    }

    public String description() {
        return description;
    }

    public boolean usesTypeLiteral() {
        return typeLiteral;
    }

    public DefinedCondition createCondition() {
        return new DefinedCondition(main, this);
    }

    public void registerCommands(final NQCommandBuilder builder, final ConditionFor conditionFor) {
        if (commandRegistrar != null) {
            commandRegistrar.register(this, builder, conditionFor);
            return;
        }
        NQCommandBuilder command = builder;
        for (final ConditionTypeField<?> field : fields) {
            command = field.appendTo(main, command);
        }
        for (final ConditionTypeField<?> flag : flags) {
            command = command.flag(flag.commandFlag(main));
        }
        main.getCommandManager().getNQCommandManager().command(command.handler(context -> {
            final DefinedCondition condition = createCondition();
            applyCommandValues(condition, context);
            main.getConditionCatalog().addCondition(condition, context, conditionFor);
        }));
    }

    private void applyCommandValues(final DefinedCondition condition, final NQCommandContext context) {
        for (final ConditionTypeField<?> field : fields) {
            condition.setRawValue(field, field.readFromCommand(context));
        }
        for (final ConditionTypeField<?> flag : flags) {
            condition.setRawValue(flag, flag.readFromCommand(context));
        }
    }

    void save(final DefinedCondition condition, final FileConfiguration configuration, final String path) {
        for (final ConditionTypeField<?> field : fields) {
            saveField(condition, configuration, path, field);
        }
        for (final ConditionTypeField<?> flag : flags) {
            saveField(condition, configuration, path, flag);
        }
    }

    private <V> void saveField(
            final DefinedCondition condition,
            final FileConfiguration configuration,
            final String path,
            final ConditionTypeField<V> field) {
        field.type().save(main, configuration, path, condition.value(field.name(), field.type().fallback()));
    }

    void load(final DefinedCondition condition, final FileConfiguration configuration, final String path) {
        for (final ConditionTypeField<?> field : fields) {
            loadField(condition, configuration, path, field);
        }
        for (final ConditionTypeField<?> flag : flags) {
            loadField(condition, configuration, path, flag);
        }
    }

    private <V> void loadField(
            final DefinedCondition condition,
            final FileConfiguration configuration,
            final String path,
            final ConditionTypeField<V> field) {
        condition.setRawValue(field, field.type().load(main, configuration, path));
    }

    String check(final DefinedCondition condition, final QuestPlayer questPlayer) {
        return checker.check(new ConditionDataContext(condition), questPlayer);
    }

    String conditionDescription(
            final DefinedCondition condition, final QuestPlayer questPlayer, final Object... objects) {
        return conditionDescriptionRenderer.render(new ConditionDataContext(condition), questPlayer, objects);
    }

    void deserialize(final DefinedCondition condition, final ArrayList<String> arguments) {
        if (singleLineParser == null) {
            throw new IllegalStateException("Condition " + id + " cannot be used as an inline condition.");
        }
        singleLineParser.parse(condition, arguments);
    }

    public TypeInfo metadata(final String source, final boolean integrationOnly) {
        return new TypeInfo(
                id,
                displayName,
                DefinedCondition.class.getName(),
                description,
                source,
                integrationOnly,
                fields.stream().map(field -> field.metadata(main)).toList(),
                flags.stream().map(flag -> flag.metadata(main)).toList());
    }

    @FunctionalInterface
    public interface ConditionDescriptionRenderer {
        String render(ConditionDataContext condition, QuestPlayer questPlayer, Object... objects);
    }

    @FunctionalInterface
    public interface ConditionChecker {
        String check(ConditionDataContext condition, QuestPlayer questPlayer);
    }

    @FunctionalInterface
    public interface SingleLineParser {
        void parse(DefinedCondition condition, ArrayList<String> arguments);
    }

    @FunctionalInterface
    public interface CommandRegistrar {
        void register(ConditionType type, NQCommandBuilder builder, ConditionFor conditionFor);
    }

    public static final class Builder {
        private final NotQuests main;
        private final ConditionCatalog manager;
        private final String id;
        private String displayName;
        private String description;
        private final List<ConditionTypeField<?>> fields = new ArrayList<>();
        private final List<ConditionTypeField<?>> flags = new ArrayList<>();
        private ConditionDescriptionRenderer conditionDescriptionRenderer;
        private ConditionChecker checker;
        private SingleLineParser singleLineParser;
        private CommandRegistrar commandRegistrar;
        private boolean typeLiteral = true;

        public Builder(final NotQuests main, final ConditionCatalog manager, final String id) {
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
            fields.add(new ConditionTypeField<>(name, type, description, false));
            return this;
        }

        public <V> Builder flag(final String name, final FieldType<V> type, final String description) {
            flags.add(new ConditionTypeField<>(name, type, description, true));
            return this;
        }

        public Builder withoutTypeLiteral() {
            this.typeLiteral = false;
            return this;
        }

        public Builder conditionDescription(final ConditionDescriptionRenderer renderer) {
            this.conditionDescriptionRenderer = Objects.requireNonNull(renderer, "renderer");
            return this;
        }

        public Builder check(final ConditionChecker checker) {
            this.checker = Objects.requireNonNull(checker, "checker");
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

        public ConditionType register() {
            final ConditionType type = new ConditionType(this);
            manager.registerCondition(type);
            return type;
        }
    }
}
