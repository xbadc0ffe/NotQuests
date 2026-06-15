package rocks.gravili.notquests.paper.registry;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.plugin.EventExecutor;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.commands.framework.NQCommandBuilder;
import rocks.gravili.notquests.paper.commands.framework.NQCommandContext;
import rocks.gravili.notquests.paper.managers.registering.ObjectiveManager;
import rocks.gravili.notquests.paper.metadata.NQMetadataSchema.TypeInfo;
import rocks.gravili.notquests.paper.structs.ActiveObjective;
import rocks.gravili.notquests.paper.structs.QuestPlayer;
import rocks.gravili.notquests.paper.structs.objectives.Objective;

public final class ObjectiveType {
    private final NotQuests main;
    private final ObjectiveManager manager;
    private final String id;
    private final String displayName;
    private final String description;
    private final List<ObjectiveTypeField<?>> fields;
    private final List<ObjectiveTypeField<?>> flags;
    private final List<EventBinding<?>> eventBindings;
    private final TaskDescriptionRenderer taskDescriptionRenderer;
    private final BiConsumer<DefinedObjective, LoadContext> afterLoad;

    ObjectiveType(final Builder builder) {
        this.main = builder.main;
        this.manager = builder.manager;
        this.id = FieldType.requireText(builder.id, "objective id");
        this.displayName = FieldType.requireText(builder.displayName, "objective display name");
        this.description = FieldType.requireText(builder.description, "objective description");
        this.fields = List.copyOf(builder.fields);
        this.flags = List.copyOf(builder.flags);
        this.eventBindings = List.copyOf(builder.eventBindings);
        this.taskDescriptionRenderer = builder.taskDescriptionRenderer;
        this.afterLoad = builder.afterLoad;
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

    public List<ObjectiveTypeField<?>> fields() {
        return fields;
    }

    public List<ObjectiveTypeField<?>> flags() {
        return flags;
    }

    public DefinedObjective createObjective() {
        return new DefinedObjective(main, this);
    }

    public void registerCommands(final NQCommandBuilder builder, final int level) {
        NQCommandBuilder command = builder;
        for (final ObjectiveTypeField<?> field : fields) {
            command = field.appendTo(main, command);
        }
        for (final ObjectiveTypeField<?> flag : flags) {
            command = command.flag(flag.commandFlag(main));
        }
        main.getCommandManager().getNQCommandManager().command(command.handler(context -> {
            final DefinedObjective objective = createObjective();
            applyCommandValues(objective, context);
            main.getObjectiveManager().addObjective(objective, context, level);
        }));
    }

    private void applyCommandValues(final DefinedObjective objective, final NQCommandContext context) {
        for (final ObjectiveTypeField<?> field : fields) {
            objective.setRawValue(field, field.readFromCommand(context));
        }
        for (final ObjectiveTypeField<?> flag : flags) {
            objective.setRawValue(flag, flag.readFromCommand(context));
        }
    }

    void save(final DefinedObjective objective, final FileConfiguration configuration, final String path) {
        for (final ObjectiveTypeField<?> field : fields) {
            saveField(objective, configuration, path, field);
        }
        for (final ObjectiveTypeField<?> flag : flags) {
            saveField(objective, configuration, path, flag);
        }
    }

    private <V> void saveField(
            final DefinedObjective objective,
            final FileConfiguration configuration,
            final String path,
            final ObjectiveTypeField<V> field) {
        field.type().save(main, configuration, path, objective.value(field.name(), field.type().fallback()));
    }

    void load(final DefinedObjective objective, final FileConfiguration configuration, final String path) {
        for (final ObjectiveTypeField<?> field : fields) {
            loadField(objective, configuration, path, field);
        }
        for (final ObjectiveTypeField<?> flag : flags) {
            loadField(objective, configuration, path, flag);
        }
        if (afterLoad != null) {
            afterLoad.accept(objective, new LoadContext(main, configuration, path));
        }
    }

    private <V> void loadField(
            final DefinedObjective objective,
            final FileConfiguration configuration,
            final String path,
            final ObjectiveTypeField<V> field) {
        objective.setRawValue(field, field.type().load(main, configuration, path));
    }

    String taskDescription(final DefinedObjective objective, final QuestPlayer questPlayer, final ActiveObjective activeObjective) {
        if (taskDescriptionRenderer == null) {
            return description;
        }
        return taskDescriptionRenderer.render(new ObjectiveDataContext(objective), questPlayer, activeObjective);
    }

    public void registerEventListeners() {
        if (eventBindings.isEmpty()) {
            return;
        }
        final Listener listener = new Listener() {};
        for (final EventBinding<?> binding : eventBindings) {
            Bukkit.getPluginManager().registerEvent(
                    binding.eventType(),
                    listener,
                    EventPriority.HIGHEST,
                    eventExecutor(binding),
                    main.getMain(),
                    true);
        }
    }

    private <E extends Event> EventExecutor eventExecutor(final EventBinding<E> binding) {
        return (listener, event) -> handleEvent(binding, event);
    }

    private <E extends Event> void handleEvent(final EventBinding<E> binding, final Event event) {
        final Player player = playerFromEvent(event);
        if (player == null) {
            return;
        }
        final QuestPlayer questPlayer = main.getQuestPlayerManager().getActiveQuestPlayer(player.getUniqueId());
        if (questPlayer == null || questPlayer.getActiveQuests().isEmpty()) {
            return;
        }
        questPlayer.queueObjectiveCheck(activeObjective -> {
            if (activeObjective.getObjective() instanceof final DefinedObjective objective
                    && objective.definition() == this) {
                binding.handler().accept(binding.eventType().cast(event), new ObjectiveEventContext(objective, activeObjective));
            }
        });
        questPlayer.checkQueuedObjectives();
    }

    private static Player playerFromEvent(final Event event) {
        try {
            final Method method = event.getClass().getMethod("getPlayer");
            final Object player = method.invoke(event);
            return player instanceof Player bukkitPlayer ? bukkitPlayer : null;
        } catch (final ReflectiveOperationException ignored) {
            return null;
        }
    }

    public TypeInfo metadata(final String source, final boolean integrationOnly) {
        return new TypeInfo(
                id,
                displayName,
                DefinedObjective.class.getName(),
                description,
                source,
                integrationOnly,
                fields.stream().map(field -> field.metadata(main)).toList(),
                flags.stream().map(flag -> flag.metadata(main)).toList());
    }

    private record EventBinding<E extends Event>(Class<E> eventType, BiConsumer<E, ObjectiveEventContext> handler) {}

    @FunctionalInterface
    public interface TaskDescriptionRenderer {
        String render(ObjectiveDataContext objective, QuestPlayer questPlayer, ActiveObjective activeObjective);
    }

    public record LoadContext(NotQuests main, FileConfiguration configuration, String path) {}

    public static final class Builder {
        private final NotQuests main;
        private final ObjectiveManager manager;
        private final String id;
        private String displayName;
        private String description;
        private final List<ObjectiveTypeField<?>> fields = new ArrayList<>();
        private final List<ObjectiveTypeField<?>> flags = new ArrayList<>();
        private final List<EventBinding<?>> eventBindings = new ArrayList<>();
        private TaskDescriptionRenderer taskDescriptionRenderer;
        private BiConsumer<DefinedObjective, LoadContext> afterLoad;

        public Builder(final NotQuests main, final ObjectiveManager manager, final String id) {
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
            fields.add(new ObjectiveTypeField<>(name, type, description, false));
            return this;
        }

        public <V> Builder flag(final String name, final FieldType<V> type, final String description) {
            flags.add(new ObjectiveTypeField<>(name, type, description, true));
            return this;
        }

        public Builder taskDescription(final TaskDescriptionRenderer renderer) {
            this.taskDescriptionRenderer = Objects.requireNonNull(renderer, "renderer");
            return this;
        }

        public <E extends Event> Builder on(final Class<E> eventType, final BiConsumer<E, ObjectiveEventContext> handler) {
            eventBindings.add(new EventBinding<>(Objects.requireNonNull(eventType, "eventType"), Objects.requireNonNull(handler, "handler")));
            return this;
        }

        public Builder afterLoad(final BiConsumer<DefinedObjective, LoadContext> afterLoad) {
            this.afterLoad = Objects.requireNonNull(afterLoad, "afterLoad");
            return this;
        }

        public ObjectiveType register() {
            final ObjectiveType type = new ObjectiveType(this);
            manager.registerObjective(type);
            return type;
        }
    }
}
