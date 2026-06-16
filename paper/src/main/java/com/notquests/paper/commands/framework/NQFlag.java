package com.notquests.paper.commands.framework;

import java.util.Objects;

/**
 * A command flag — our replacement for Cloud's {@code CommandFlag}. Brigadier has no native flag
 * concept, so {@link NQCommandManager} models a command's flags as a single optional trailing greedy
 * argument that it parses into {@code --name value} (value flag) or {@code --name} (presence flag).
 *
 * <p>A presence flag (no value argument) is read via {@code context.flags().isPresent("name")}; a
 * value flag's parsed value via {@code context.flags().getValue("name")}.
 */
public final class NQFlag {
    private final String name;
    private final NQArgumentType<?> valueArgument; // null => presence-only flag
    private final NQDescription description;
    private final NQSuggestionProvider valueSuggestions;

    private NQFlag(
            final String name,
            final NQArgumentType<?> valueArgument,
            final NQDescription description,
            final NQSuggestionProvider valueSuggestions) {
        this.name = name;
        this.valueArgument = valueArgument;
        this.description = description == null ? NQDescription.EMPTY : description;
        this.valueSuggestions = valueSuggestions;
    }

    /** A flag that is either present or not (no value). */
    public static NQFlag presence(final String name, final NQDescription description) {
        return new NQFlag(name, null, description, null);
    }

    public static Builder builder(final String name, final NQDescription description) {
        return new Builder(name, description);
    }

    public String name() {
        return name;
    }

    public NQArgumentType<?> valueArgument() {
        return valueArgument;
    }

    public boolean isPresence() {
        return valueArgument == null;
    }

    public NQDescription description() {
        return description;
    }

    public NQSuggestionProvider valueSuggestions() {
        return valueSuggestions;
    }

    public static final class Builder {
        private final String name;
        private NQArgumentType<?> valueArgument;
        private final NQDescription description;
        private NQSuggestionProvider valueSuggestions;

        private Builder(final String name, final NQDescription description) {
            this.name = name;
            this.description = Objects.requireNonNull(description, "description");
        }

        /** Make this a value flag backed by the given argument type. */
        public Builder withArgument(final NQArgumentType<?> valueArgument) {
            this.valueArgument = valueArgument;
            return this;
        }

        public Builder withSuggestions(final NQSuggestionProvider valueSuggestions) {
            this.valueSuggestions = valueSuggestions;
            return this;
        }

        public NQFlag build() {
            return new NQFlag(name, valueArgument, description, valueSuggestions);
        }
    }
}
