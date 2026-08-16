package com.notquests.paper.commands.framework;

import java.util.List;

/**
 * Structured representation of the command tree. It is generated from the same internal tree that is
 * compiled into Brigadier, so in-game help, exported docs, and tests can use one source of truth.
 */
public final class NQCommandSchema {
    private NQCommandSchema() {}

    public record CommandIndex(String pluginVersion, List<CommandInfo> commands) {
        public String toJson() {
            final StringBuilder json = new StringBuilder(64_000);
            json.append("{\n");
            appendField(json, 1, "pluginVersion", pluginVersion).append(",\n");
            indent(json, 1).append("\"commands\": [\n");
            for (int i = 0; i < commands.size(); i++) {
                commands.get(i).appendJson(json, 2);
                if (i + 1 < commands.size()) {
                    json.append(',');
                }
                json.append('\n');
            }
            indent(json, 1).append("]\n");
            json.append("}\n");
            return json.toString();
        }
    }

    public record CommandInfo(
            String root,
            List<String> rootAliases,
            String syntax,
            String description,
            String permission,
            String senderType,
            List<SegmentInfo> segments,
            List<FlagInfo> flags) {
        private void appendJson(final StringBuilder json, final int level) {
            indent(json, level).append("{\n");
            appendField(json, level + 1, "root", root).append(",\n");
            appendStringArrayField(json, level + 1, "rootAliases", rootAliases).append(",\n");
            appendField(json, level + 1, "syntax", syntax).append(",\n");
            appendField(json, level + 1, "description", description).append(",\n");
            appendField(json, level + 1, "permission", permission).append(",\n");
            appendField(json, level + 1, "senderType", senderType).append(",\n");
            indent(json, level + 1).append("\"segments\": [\n");
            for (int i = 0; i < segments.size(); i++) {
                segments.get(i).appendJson(json, level + 2);
                if (i + 1 < segments.size()) {
                    json.append(',');
                }
                json.append('\n');
            }
            indent(json, level + 1).append("],\n");
            indent(json, level + 1).append("\"flags\": [\n");
            for (int i = 0; i < flags.size(); i++) {
                flags.get(i).appendJson(json, level + 2);
                if (i + 1 < flags.size()) {
                    json.append(',');
                }
                json.append('\n');
            }
            indent(json, level + 1).append("]\n");
            indent(json, level).append('}');
        }
    }

    public record SegmentInfo(
            String kind,
            String name,
            String token,
            String description,
            String argumentType,
            String valueType,
            boolean required) {
        private void appendJson(final StringBuilder json, final int level) {
            indent(json, level).append("{\n");
            appendField(json, level + 1, "kind", kind).append(",\n");
            appendField(json, level + 1, "name", name).append(",\n");
            appendField(json, level + 1, "token", token).append(",\n");
            appendField(json, level + 1, "description", description).append(",\n");
            appendField(json, level + 1, "argumentType", argumentType).append(",\n");
            appendField(json, level + 1, "valueType", valueType).append(",\n");
            appendField(json, level + 1, "required", required).append('\n');
            indent(json, level).append('}');
        }
    }

    public record FlagInfo(
            String name,
            String token,
            String description,
            String argumentType,
            String valueType,
            boolean presenceOnly) {
        private void appendJson(final StringBuilder json, final int level) {
            indent(json, level).append("{\n");
            appendField(json, level + 1, "name", name).append(",\n");
            appendField(json, level + 1, "token", token).append(",\n");
            appendField(json, level + 1, "description", description).append(",\n");
            appendField(json, level + 1, "argumentType", argumentType).append(",\n");
            appendField(json, level + 1, "valueType", valueType).append(",\n");
            appendField(json, level + 1, "presenceOnly", presenceOnly).append('\n');
            indent(json, level).append('}');
        }
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
            final StringBuilder json, final int level, final String name, final int value) {
        return indent(json, level).append('"').append(escape(name)).append("\": ").append(value);
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
