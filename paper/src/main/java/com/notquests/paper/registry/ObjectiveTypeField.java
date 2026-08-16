package com.notquests.paper.registry;

import java.util.Objects;
import com.notquests.paper.NotQuests;
import com.notquests.paper.commands.framework.NQArgumentType;
import com.notquests.paper.commands.framework.NQCommandBuilder;
import com.notquests.paper.commands.framework.NQCommandContext;
import com.notquests.paper.commands.framework.NQDescription;
import com.notquests.paper.commands.framework.NQFlag;
import com.notquests.paper.metadata.NQMetadataSchema.TypeFieldInfo;

final class ObjectiveTypeField<V> {
    private final String name;
    private final FieldType<V> type;
    private final NQDescription description;
    private final boolean flag;

    ObjectiveTypeField(
            final String name, final FieldType<V> type, final String description, final boolean flag) {
        this.name = FieldType.requireText(name, "field name");
        this.type = Objects.requireNonNull(type, "type");
        this.description = requireDescription(description, (flag ? "flag " : "field ") + name);
        this.flag = flag;
    }

    String name() {
        return name;
    }

    FieldType<V> type() {
        return type;
    }

    boolean flag() {
        return flag;
    }

    NQCommandBuilder appendTo(final NotQuests main, final NQCommandBuilder builder) {
        return builder.required(name, type.argument(main, name), description);
    }

    NQFlag commandFlag(final NotQuests main) {
        final NQArgumentType<V> argument = type.argument(main, name);
        if (argument == null || type.isPresenceFlag()) {
            return NQFlag.presence(name, description);
        }
        return NQFlag.builder(name, description).withArgument(argument).build();
    }

    @SuppressWarnings("unchecked")
    V readFromCommand(final NQCommandContext context) {
        if (!flag) {
            return context.get(name);
        }
        if (type.isPresenceFlag()) {
            return (V) Boolean.valueOf(context.flags().isPresent(name));
        }
        return context.flags().getValue(name, type.fallback());
    }

    TypeFieldInfo metadata(final NotQuests main) {
        final NQArgumentType<V> argument = type.argument(main, name);
        return new TypeFieldInfo(
                name,
                description.textDescription(),
                argument == null ? "PresenceFlag" : argumentTypeName(argument),
                type.valueTypeName(),
                !flag,
                flag);
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
