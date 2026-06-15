/*
 * NotQuests - A Questing plugin for Minecraft Servers
 * Copyright (C) 2021-2022 Alessio Gravili
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package rocks.gravili.notquests.paper.metadata;

import java.util.List;
import rocks.gravili.notquests.paper.commands.framework.NQCommandSchema.CommandIndex;

/**
 * Runtime-generated metadata exported by NotQuests.
 *
 * <p>This is intentionally built from the enabled server's runtime managers, not from a docs-only
 * hand-written list. That keeps the plugin, docs, E2E sweep, and future editor integration on one
 * source of truth, including API-added types and integration-gated types.
 */
public final class NQMetadataSchema {
    private NQMetadataSchema() {}

    public record MetadataIndex(
            String pluginVersion,
            String minecraftVersion,
            CommandIndex commands,
            RegistryIndex registry) {
        public String toJson() {
            final StringBuilder json = new StringBuilder(96_000);
            json.append("{\n");
            appendField(json, 1, "pluginVersion", pluginVersion).append(",\n");
            appendField(json, 1, "minecraftVersion", minecraftVersion).append(",\n");
            indent(json, 1).append("\"commands\": ");
            json.append(commands.toJson().trim()).append(",\n");
            indent(json, 1).append("\"registry\": ");
            registry.appendJson(json, 1);
            json.append('\n').append("}\n");
            return json.toString();
        }
    }

    public record RegistryIndex(
            List<TypeInfo> objectives,
            List<TypeInfo> actions,
            List<TypeInfo> conditions,
            List<TypeInfo> triggers,
            List<VariableInfo> variables) {
        void appendJson(final StringBuilder json, final int level) {
            json.append("{\n");
            appendTypeArray(json, level + 1, "objectives", objectives).append(",\n");
            appendTypeArray(json, level + 1, "actions", actions).append(",\n");
            appendTypeArray(json, level + 1, "conditions", conditions).append(",\n");
            appendTypeArray(json, level + 1, "triggers", triggers).append(",\n");
            appendVariableArray(json, level + 1, "variables", variables).append('\n');
            indent(json, level).append('}');
        }
    }

    public record TypeInfo(
            String id,
            String className,
            String description,
            String source,
            boolean integrationOnly) {
        void appendJson(final StringBuilder json, final int level) {
            indent(json, level).append("{\n");
            appendField(json, level + 1, "id", id).append(",\n");
            appendField(json, level + 1, "className", className).append(",\n");
            appendField(json, level + 1, "description", description).append(",\n");
            appendField(json, level + 1, "source", source).append(",\n");
            appendField(json, level + 1, "integrationOnly", integrationOnly).append('\n');
            indent(json, level).append('}');
        }
    }

    public record VariableInfo(
            String id,
            String className,
            String description,
            String valueType,
            boolean settable,
            List<String> stringArguments,
            List<String> numberArguments,
            List<String> booleanArguments,
            List<String> booleanFlags) {
        void appendJson(final StringBuilder json, final int level) {
            indent(json, level).append("{\n");
            appendField(json, level + 1, "id", id).append(",\n");
            appendField(json, level + 1, "className", className).append(",\n");
            appendField(json, level + 1, "description", description).append(",\n");
            appendField(json, level + 1, "valueType", valueType).append(",\n");
            appendField(json, level + 1, "settable", settable).append(",\n");
            appendStringArrayField(json, level + 1, "stringArguments", stringArguments).append(",\n");
            appendStringArrayField(json, level + 1, "numberArguments", numberArguments).append(",\n");
            appendStringArrayField(json, level + 1, "booleanArguments", booleanArguments).append(",\n");
            appendStringArrayField(json, level + 1, "booleanFlags", booleanFlags).append('\n');
            indent(json, level).append('}');
        }
    }

    private static StringBuilder appendTypeArray(
            final StringBuilder json, final int level, final String name, final List<TypeInfo> values) {
        indent(json, level).append('"').append(escape(name)).append("\": [\n");
        for (int i = 0; i < values.size(); i++) {
            values.get(i).appendJson(json, level + 1);
            if (i + 1 < values.size()) {
                json.append(',');
            }
            json.append('\n');
        }
        return indent(json, level).append(']');
    }

    private static StringBuilder appendVariableArray(
            final StringBuilder json, final int level, final String name, final List<VariableInfo> values) {
        indent(json, level).append('"').append(escape(name)).append("\": [\n");
        for (int i = 0; i < values.size(); i++) {
            values.get(i).appendJson(json, level + 1);
            if (i + 1 < values.size()) {
                json.append(',');
            }
            json.append('\n');
        }
        return indent(json, level).append(']');
    }

    private static StringBuilder appendField(
            final StringBuilder json, final int level, final String name, final String value) {
        indent(json, level).append('"').append(escape(name)).append("\": ");
        if (value == null) {
            return json.append("null");
        }
        return json.append('"').append(escape(value)).append('"');
    }

    private static StringBuilder appendField(
            final StringBuilder json, final int level, final String name, final boolean value) {
        return indent(json, level).append('"').append(escape(name)).append("\": ").append(value);
    }

    private static StringBuilder appendStringArrayField(
            final StringBuilder json, final int level, final String name, final List<String> values) {
        indent(json, level).append('"').append(escape(name)).append("\": [");
        for (int i = 0; i < values.size(); i++) {
            if (i > 0) {
                json.append(", ");
            }
            json.append('"').append(escape(values.get(i))).append('"');
        }
        return json.append(']');
    }

    private static StringBuilder indent(final StringBuilder json, final int level) {
        return json.append("  ".repeat(Math.max(0, level)));
    }

    private static String escape(final String value) {
        final StringBuilder escaped = new StringBuilder(value.length() + 16);
        for (int i = 0; i < value.length(); i++) {
            final char ch = value.charAt(i);
            switch (ch) {
                case '\\' -> escaped.append("\\\\");
                case '"' -> escaped.append("\\\"");
                case '\b' -> escaped.append("\\b");
                case '\f' -> escaped.append("\\f");
                case '\n' -> escaped.append("\\n");
                case '\r' -> escaped.append("\\r");
                case '\t' -> escaped.append("\\t");
                default -> {
                    if (ch < 0x20) {
                        escaped.append(String.format("\\u%04x", (int) ch));
                    } else {
                        escaped.append(ch);
                    }
                }
            }
        }
        return escaped.toString();
    }
}
