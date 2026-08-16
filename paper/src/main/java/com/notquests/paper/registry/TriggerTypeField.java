package com.notquests.paper.registry;

import java.util.Objects;
import com.notquests.paper.NotQuests;
import com.notquests.paper.commands.framework.NQArgumentType;
import com.notquests.paper.commands.framework.NQCommandBuilder;
import com.notquests.paper.commands.framework.NQCommandContext;
import com.notquests.paper.commands.framework.NQDescription;
import com.notquests.paper.metadata.NQMetadataSchema.TypeFieldInfo;

final class TriggerTypeField<V> {
    private final String name;
    private final FieldType<V> type;
    private final NQDescription description;

    TriggerTypeField(final String name, final FieldType<V> type, final String description) {
        this.name = FieldType.requireText(name, "trigger field name");
        this.type = Objects.requireNonNull(type, "type");
        this.description = requireDescription(description, "trigger field " + name);
    }

    String name() {
        return name;
    }

    FieldType<V> type() {
        return type;
    }

    NQCommandBuilder appendTo(final NotQuests main, final NQCommandBuilder builder) {
        return builder.required(name, type.argument(main, name), description);
    }

    V readFromCommand(final NQCommandContext context) {
        return context.get(name);
    }

    TypeFieldInfo metadata(final NotQuests main) {
        final NQArgumentType<V> argument = type.argument(main, name);
        return new TypeFieldInfo(
                name,
                description.textDescription(),
                argument == null ? "stored" : argumentTypeName(argument),
                type.valueTypeName(),
                true,
                false);
    }

    private static String argumentTypeName(final NQArgumentType<?> argument) {
        final String simpleName = argument.getClass().getSimpleName();
        if (!simpleName.isBlank()) {
            return simpleName;
        }
        final String nativeName = argument.getNativeType().getClass().getSimpleName();
        return nativeName.isBlank() ? argument.getClass().getName() : nativeName;
    }

    private static NQDescription requireDescription(final String description, final String owner) {
        final NQDescription checked = NQDescription.of(Objects.requireNonNull(description, owner + " description"));
        if (checked.textDescription().isBlank()) {
            throw new IllegalArgumentException(owner + " description is required");
        }
        return checked;
    }
}
