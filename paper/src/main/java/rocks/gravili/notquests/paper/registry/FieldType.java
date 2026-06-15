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

import java.util.Objects;
import java.util.function.BiFunction;
import org.bukkit.configuration.file.FileConfiguration;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.commands.framework.NQArgumentType;

public final class FieldType<V> {
    private final BiFunction<NotQuests, String, NQArgumentType<V>> argumentFactory;
    private final ConfigCodec<V> codec;
    private final String valueTypeName;
    private final V fallback;
    private String configPath;
    private boolean progressNeeded;
    private boolean presenceFlag;
    private boolean invertedBooleanConfig;

    FieldType(
            final BiFunction<NotQuests, String, NQArgumentType<V>> argumentFactory,
            final ConfigCodec<V> codec,
            final String valueTypeName,
            final V fallback) {
        this.argumentFactory = argumentFactory;
        this.codec = Objects.requireNonNull(codec, "codec");
        this.valueTypeName = Objects.requireNonNull(valueTypeName, "valueTypeName");
        this.fallback = fallback;
    }

    public FieldType<V> config(final String configPath) {
        this.configPath = requireText(configPath, "config path");
        return this;
    }

    public FieldType<V> progressNeeded() {
        this.progressNeeded = true;
        this.configPath = "progressNeededExpression";
        return this;
    }

    public FieldType<V> presenceFlag() {
        this.presenceFlag = true;
        return this;
    }

    public FieldType<V> invertedBooleanConfig(final String configPath) {
        this.configPath = requireText(configPath, "config path");
        this.invertedBooleanConfig = true;
        return this;
    }

    NQArgumentType<V> argument(final NotQuests main, final String name) {
        return argumentFactory == null ? null : argumentFactory.apply(main, name);
    }

    boolean hasArgument() {
        return argumentFactory != null;
    }

    boolean isPresenceFlag() {
        return presenceFlag;
    }

    boolean isProgressNeeded() {
        return progressNeeded;
    }

    V fallback() {
        return fallback;
    }

    String valueTypeName() {
        return valueTypeName;
    }

    void save(final NotQuests main, final FileConfiguration configuration, final String basePath, final V value) {
        if (configPath == null) {
            return;
        }
        final Object valueToSave = invertedBooleanConfig && value instanceof Boolean bool ? !bool : value;
        codec.save(main, configuration, basePath + "." + configPath, valueToSave);
    }

    @SuppressWarnings("unchecked")
    V load(final NotQuests main, final FileConfiguration configuration, final String basePath) {
        if (configPath == null) {
            return fallback;
        }
        final V loaded = codec.load(main, configuration, basePath + "." + configPath, fallback);
        if (invertedBooleanConfig && loaded instanceof Boolean bool) {
            return (V) Boolean.valueOf(!bool);
        }
        return loaded;
    }

    static String requireText(final String value, final String label) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(label + " is required");
        }
        return value;
    }

    @FunctionalInterface
    interface ConfigCodec<V> {
        V load(NotQuests main, FileConfiguration configuration, String path, V fallback);

        default void save(
                final NotQuests main,
                final FileConfiguration configuration,
                final String path,
                final Object value) {
            configuration.set(path, value);
        }
    }
}
