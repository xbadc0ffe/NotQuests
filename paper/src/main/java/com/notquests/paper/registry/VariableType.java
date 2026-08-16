package com.notquests.paper.registry;

import java.util.Objects;
import com.notquests.paper.NotQuests;
import com.notquests.paper.metadata.NQMetadataSchema.VariableInfo;
import com.notquests.paper.variables.Variable;

public final class VariableType {
    private final NotQuests main;
    private final String id;
    private final String displayName;
    private final String description;
    private final VariableFactory factory;
    private Class<?> runtimeClass;

    public VariableType(
            final NotQuests main,
            final String id,
            final String displayName,
            final String description,
            final VariableFactory factory) {
        this.main = Objects.requireNonNull(main, "main");
        this.id = FieldType.requireText(id, "variable id");
        this.displayName = FieldType.requireText(displayName, "variable display name");
        this.description = FieldType.requireText(description, "variable description");
        this.factory = Objects.requireNonNull(factory, "variable factory");
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

    public Variable<?> createVariable() {
        final Variable<?> variable = Objects.requireNonNull(factory.create(main), "variable factory result");
        if (runtimeClass == null) {
            runtimeClass = variable.getClass();
        }
        return variable;
    }

    public boolean matches(final Variable<?> variable) {
        if (variable == null) {
            return false;
        }
        if (runtimeClass == null) {
            createVariable();
        }
        return variable.getClass().equals(runtimeClass);
    }

    public VariableInfo metadata() {
        final Variable<?> variable = createVariable();
        return new VariableInfo(
                id,
                displayName,
                variable.getClass().getName(),
                description,
                integrationSource(variable.getClass()),
                integrationSource(variable.getClass()) != null,
                variable.getVariableDataType().name(),
                variable.isCanSetValue(),
                variable.getRequiredStrings().stream().map(parser -> parser.getIdentifier()).toList(),
                variable.getRequiredNumbers().stream().map(parser -> parser.getIdentifier()).toList(),
                variable.getRequiredBooleans().stream().map(parser -> parser.getIdentifier()).toList(),
                variable.getRequiredBooleanFlags().stream().map(flag -> flag.name()).toList());
    }

    private static String integrationSource(final Class<?> type) {
        final String packageName = type.getPackageName();
        final String marker = ".hooks.";
        final int markerIndex = packageName.indexOf(marker);
        if (markerIndex < 0) {
            return null;
        }
        final String afterHooks = packageName.substring(markerIndex + marker.length());
        final int nextDot = afterHooks.indexOf('.');
        return nextDot < 0 ? afterHooks : afterHooks.substring(0, nextDot);
    }

    @FunctionalInterface
    public interface VariableFactory {
        Variable<?> create(NotQuests main);
    }
}
