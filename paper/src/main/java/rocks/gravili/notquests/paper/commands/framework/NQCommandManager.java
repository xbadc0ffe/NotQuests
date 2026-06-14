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

package rocks.gravili.notquests.paper.commands.framework;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.command.CommandSender;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.commands.framework.NQCommandBuilder.Kind;
import rocks.gravili.notquests.paper.commands.framework.NQCommandBuilder.Step;
import rocks.gravili.notquests.paper.commands.framework.NQCommandSchema.CommandInfo;
import rocks.gravili.notquests.paper.commands.framework.NQCommandSchema.CommandIndex;
import rocks.gravili.notquests.paper.commands.framework.NQCommandSchema.FlagInfo;
import rocks.gravili.notquests.paper.commands.framework.NQCommandSchema.SegmentInfo;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

/**
 * High-level command manager — our replacement for Cloud's {@code CommandManager}. Accepts
 * {@link NQCommandBuilder}s, merges them into one command tree (so commands sharing a prefix, e.g.
 * everything under {@code /qa edit}, end up under one root), and compiles that tree to native
 * Brigadier nodes registered through {@link NQCommands} when Paper fires its {@code COMMANDS} event.
 *
 * <p>Flags (Brigadier has no native concept) are modelled as a single optional trailing greedy
 * argument appended to a flag-bearing command; {@link #parseFlags} turns it into the flag map handed
 * to the handler via {@link NQCommandContext}.
 */
public final class NQCommandManager {
    private static final String FLAG_ARG = "nqFlags";
    private static final TextColor LITERAL_COLOR = TextColor.color(0xE8F1FF);
    private static final TextColor ARGUMENT_COLOR = TextColor.color(0x00FFFB);
    private static final TextColor FLAG_COLOR = TextColor.color(0xF9D66B);
    private static final TextColor DESCRIPTION_COLOR = TextColor.color(0xB7C2D5);

    private final NotQuests main;
    private final Map<String, Node> roots = new LinkedHashMap<>();
    private List<CommandInfo> cachedCommandSchema;

    public NQCommandManager(final NotQuests main, final NQCommands registrar) {
        this.main = main;
        registrar.register(this::registerAll);
        // Keep the action-bar command hint alive across typing pauses (action bars fade after a few
        // seconds). The hint itself is pushed from suggestionsWithHint() on every completion request.
        main.getUtilManager().startCommandHintRefreshTask();
    }

    /** Start a new root command. Mirrors Cloud's {@code commandManager.commandBuilder(...)}. */
    public NQCommandBuilder commandBuilder(final String name, final NQDescription description, final String... aliases) {
        return NQCommandBuilder.root(name, description, aliases);
    }

    /** Register a built command. Mirrors Cloud's {@code commandManager.command(builder)}. */
    public void command(final NQCommandBuilder builder) {
        final List<Step> steps = builder.steps();
        if (steps.isEmpty()) {
            return;
        }
        final Step rootStep = steps.get(0);
        final Node root = roots.computeIfAbsent(rootStep.name(), n -> new Node(Kind.LITERAL, rootStep.name()));
        addAliases(root, rootStep.aliases());
        if (root.description.isEmpty() && !rootStep.description().isEmpty()) {
            root.description = rootStep.description();
        }
        Node current = root;
        for (int i = 1; i < steps.size(); i++) {
            final Step step = steps.get(i);
            final Node child = current.children.computeIfAbsent(step.name(), n -> new Node(step.kind(), step.name()));
            addAliases(child, step.aliases());
            if (step.argument() != null) {
                child.argument = step.argument();
            }
            if (!step.description().isEmpty()) {
                child.description = step.description();
            }
            if (step.suggestionOverride() != null) {
                child.suggestionOverride = step.suggestionOverride();
            }
            current = child;
        }
        current.handler = builder.handler();
        current.permission = builder.permission();
        current.senderType = builder.senderType();
        current.flags = builder.flags();
        if (builder.commandDescription() != null && !builder.commandDescription().isEmpty()) {
            current.commandDescription = builder.commandDescription();
        }
        cachedCommandSchema = null;
    }

    /**
     * One-level-deep usage lines for the root command {@code rootName} (looked up by name in the
     * internal {@code roots} map), used to render the {@code /qa help} / {@code /nq help} menus.
     * For each direct child of the root, produces {@code "/" + rootName + " " + childName} (argument
     * children are shown as {@code <name>}), appending {@code " ..."} when that child has its own
     * children. Returns an empty list if no root with that name is registered.
     */
    public List<String> rootUsage(final String rootName) {
        final Node root = roots.get(rootName);
        if (root == null) {
            return List.of();
        }
        return usageLines(root, rootName);
    }

    /** Send hoverable/clickable one-level help for a root command. */
    public void sendRootHelp(final CommandSender sender, final String rootName, final String title) {
        main.sendMessage(sender, title);
        final Node root = roots.get(rootName);
        if (root == null) {
            return;
        }
        sendUsageLines(sender, root, List.of(root));
    }

    /** Full executable-command schema, generated from the merged command tree and cached. */
    public List<CommandInfo> commandSchema() {
        if (cachedCommandSchema == null) {
            cachedCommandSchema = List.copyOf(buildCommandSchema());
        }
        return cachedCommandSchema;
    }

    public CommandIndex commandIndex() {
        return new CommandIndex(main.getMain().getDescription().getVersion(), commandSchema());
    }

    /** Write the current command schema to {@code plugins/NotQuests/generated/commands.json}. */
    public Path exportCommandSchema() throws IOException {
        final Path output = main.getMain().getDataFolder().toPath().resolve("generated").resolve("commands.json");
        Files.createDirectories(output.getParent());
        Files.writeString(output, commandIndex().toJson(), StandardCharsets.UTF_8);
        return output;
    }

    /**
     * Usage lines for a node's direct children, each rendered as {@code /<path> <child>}. Literal
     * children show their name; argument children show {@code <name>}; a child with its own children
     * is suffixed with {@code " ..."} to signal that more input follows. Sorted for stable output.
     */
    private List<String> usageLines(final Node node, final String path) {
        final List<String> usages = new ArrayList<>();
        for (final Node child : node.children.values()) {
            final String label = child.kind == Kind.LITERAL ? child.name : "<" + child.name + ">";
            final String more = child.children.isEmpty() ? "" : " ...";
            usages.add("/" + path + " " + label + more);
        }
        usages.sort(null);
        return usages;
    }

    private void registerAll(final Commands commands) {
        for (final Node root : roots.values()) {
            try {
                final LiteralCommandNode<CommandSourceStack> node =
                        (LiteralCommandNode<CommandSourceStack>) compile(root, List.of(root));
                commands.register(node, root.description.textDescription(), new ArrayList<>(root.aliases));
            } catch (final Throwable t) {
                main.getLogManager().warn("Failed to register native command /" + root.name + ": " + t.getMessage());
            }
        }
        try {
            exportCommandSchema();
        } catch (final IOException e) {
            main.getLogManager().warn("Failed to export command schema: " + e.getMessage());
        }
    }

    private CommandNode<CommandSourceStack> compile(final Node node, final List<Node> path) {
        if (node.kind == Kind.LITERAL) {
            final LiteralArgumentBuilder<CommandSourceStack> builder = Commands.literal(node.name);
            populate(builder, node, path);
            return builder.build();
        }
        final ArgumentType<?> type = node.argument;
        final RequiredArgumentBuilder<CommandSourceStack, ?> builder = Commands.argument(node.name, type);
        // Always route arg suggestions through suggestionsWithHint: it pushes the action-bar hint for
        // this argument, then delegates to the override (if any) or the argument type's own suggestions.
        builder.suggests(suggestionsWithHint(node));
        populate(builder, node, path);
        return builder.build();
    }

    private void populate(final ArgumentBuilder<CommandSourceStack, ?> builder, final Node node, final List<Node> path) {
        if (node.permission != null) {
            final String permission = node.permission;
            builder.requires(source -> source.getSender().hasPermission(permission));
        }
        for (final Node child : node.children.values()) {
            final List<Node> childPath = appendPath(path, child);
            final CommandNode<CommandSourceStack> built = compile(child, childPath);
            builder.then(built);
            // NOTE: short sub-command aliases (e.g. "o" for "objectives") are intentionally NOT
            // registered as tree nodes. In Brigadier the client builds literal suggestions locally
            // from the command graph it is sent, and a node is in that graph iff it is usable for
            // execution — so a working alias literal cannot be hidden from tab-completion. Rather
            // than clutter every suggestion list with single-letter aliases, we only expose the
            // canonical names. Root-command aliases (e.g. /qa) are unaffected; they are registered
            // separately via commands.register(node, desc, aliases).
        }
        if (node.handler != null) {
            builder.executes(ctx -> execute(node, ctx, ""));
            if (!node.flags.isEmpty()) {
                final RequiredArgumentBuilder<CommandSourceStack, String> flagsArg =
                        Commands.argument(FLAG_ARG, StringArgumentType.greedyString());
                flagsArg.suggests(flagSuggestions(node));
                flagsArg.executes(ctx -> execute(node, ctx, StringArgumentType.getString(ctx, FLAG_ARG)));
                builder.then(flagsArg.build());
            }
        } else if (!node.children.isEmpty()) {
            // Branch node with no handler of its own: when the user stops here (e.g. `/qa actions`),
            // print the valid continuations instead of Brigadier's raw "Unknown or incomplete
            // command". This restores the Cloud-style contextual help the user expects. Nodes gated
            // by a permission are already filtered by requires() above, so help only shows reachable
            // subcommands.
            builder.executes(ctx -> printBranchHelp(node, path, ctx));
        }
    }

    /** Default executor for handler-less branch nodes: lists the node's valid continuations. */
    private int printBranchHelp(
            final Node node, final List<Node> path, final CommandContext<CommandSourceStack> ctx) {
        final CommandSender sender = ctx.getSource().getSender();
        main.sendMessage(sender, "<main>/" + pathSyntax(path) + " <unimportant>— available subcommands:");
        sendUsageLines(sender, node, path);
        return Command.SINGLE_SUCCESS;
    }

    private void sendUsageLines(final CommandSender sender, final Node node, final List<Node> path) {
        final List<Node> children = new ArrayList<>(node.children.values());
        children.sort(Comparator.comparing(NQCommandManager::displayToken));
        for (final Node child : children) {
            final List<Node> childPath = appendPath(path, child);
            main.sendMessage(sender, usageLineComponent(childPath, !child.children.isEmpty()));
        }
    }

    private Component usageLineComponent(final List<Node> path, final boolean hasMore) {
        final String displaySyntax = "/" + pathSyntax(path) + (hasMore ? " ..." : flagsSyntax(path.get(path.size() - 1)));
        final String clickSyntax = "/" + pathSyntax(path) + (hasMore ? " " : flagsSyntax(path.get(path.size() - 1)));
        Component line = Component.text("/", NamedTextColor.DARK_GRAY)
                .clickEvent(ClickEvent.suggestCommand(clickSyntax))
                .hoverEvent(HoverEvent.showText(Component.text("Click to insert this command shape", DESCRIPTION_COLOR)));

        for (int i = 0; i < path.size(); i++) {
            if (i > 0) {
                line = line.append(Component.space());
            }
            line = line.append(segmentComponent(path.get(i), path.subList(0, i + 1)));
        }
        if (hasMore) {
            line = line.append(Component.text(" ...", NamedTextColor.DARK_GRAY)
                    .hoverEvent(HoverEvent.showText(Component.text("More arguments or subcommands follow", DESCRIPTION_COLOR))));
        } else {
            for (final NQFlag flag : path.get(path.size() - 1).flags) {
                line = line.append(Component.space()).append(flagComponent(flag, displaySyntax));
            }
        }
        return line;
    }

    private Component segmentComponent(final Node node, final List<Node> pathPrefix) {
        final boolean argument = node.kind != Kind.LITERAL;
        final String token = displayToken(node);
        final TextColor color = argument ? ARGUMENT_COLOR : LITERAL_COLOR;
        final String suggestedPrefix = "/" + pathSyntax(pathPrefix) + (node.children.isEmpty() ? "" : " ");
        return Component.text(token, color)
                .clickEvent(ClickEvent.suggestCommand(suggestedPrefix))
                .hoverEvent(HoverEvent.showText(segmentHover(node, pathPrefix)));
    }

    private Component flagComponent(final NQFlag flag, final String syntax) {
        final String token = flag.isPresence() ? "[--" + flag.name() + "]" : "[--" + flag.name() + " <value>]";
        return Component.text(token, FLAG_COLOR)
                .clickEvent(ClickEvent.suggestCommand(syntax))
                .hoverEvent(HoverEvent.showText(flagHover(flag)));
    }

    private Component segmentHover(final Node node, final List<Node> pathPrefix) {
        final boolean argument = node.kind != Kind.LITERAL;
        Component hover = Component.text(argument ? "Argument " + displayToken(node) : "Command " + node.name,
                argument ? ARGUMENT_COLOR : LITERAL_COLOR);
        final String description = descriptionText(node.description);
        if (!description.isBlank()) {
            hover = hover.append(Component.newline()).append(Component.text(description, DESCRIPTION_COLOR));
        }
        if (argument && node.argument != null) {
            hover = hover.append(Component.newline())
                    .append(Component.text("Accepts: " + node.argument.valueTypeName(), NamedTextColor.GRAY));
        }
        hover = hover.append(Component.newline())
                .append(Component.text("Syntax: /" + pathSyntax(pathPrefix), NamedTextColor.GRAY));
        return hover;
    }

    private Component flagHover(final NQFlag flag) {
        Component hover = Component.text("Flag --" + flag.name(), FLAG_COLOR);
        final String description = descriptionText(flag.description());
        if (!description.isBlank()) {
            hover = hover.append(Component.newline()).append(Component.text(description, DESCRIPTION_COLOR));
        }
        if (!flag.isPresence()) {
            hover = hover.append(Component.newline())
                    .append(Component.text("Accepts: " + flag.valueArgument().valueTypeName(), NamedTextColor.GRAY));
        }
        return hover;
    }

    private List<CommandInfo> buildCommandSchema() {
        final List<CommandInfo> commands = new ArrayList<>();
        for (final Node root : roots.values()) {
            collectCommandSchema(root, List.of(root), commands);
        }
        commands.sort(Comparator.comparing(CommandInfo::syntax));
        return commands;
    }

    private void collectCommandSchema(final Node node, final List<Node> path, final List<CommandInfo> commands) {
        if (node.handler != null) {
            commands.add(commandInfo(path, node));
        }
        for (final Node child : node.children.values()) {
            collectCommandSchema(child, appendPath(path, child), commands);
        }
    }

    private CommandInfo commandInfo(final List<Node> path, final Node executable) {
        final List<SegmentInfo> segments = new ArrayList<>();
        for (final Node node : path) {
            segments.add(new SegmentInfo(
                    node.kind.name().toLowerCase(java.util.Locale.ROOT),
                    node.name,
                    displayToken(node),
                    descriptionText(node.description),
                    node.argument == null ? null : argumentTypeName(node.argument),
                    node.argument == null ? null : node.argument.valueTypeName(),
                    node.kind != Kind.OPTIONAL));
        }

        final List<FlagInfo> flags = new ArrayList<>();
        for (final NQFlag flag : executable.flags) {
            flags.add(new FlagInfo(
                    flag.name(),
                    flag.isPresence() ? "--" + flag.name() : "--" + flag.name() + " <value>",
                    descriptionText(flag.description()),
                    flag.isPresence() ? null : argumentTypeName(flag.valueArgument()),
                    flag.isPresence() ? null : flag.valueArgument().valueTypeName(),
                    flag.isPresence()));
        }

        final Node root = path.get(0);
        return new CommandInfo(
                root.name,
                List.copyOf(root.aliases),
                "/" + pathSyntax(path) + flagsSyntax(executable),
                descriptionText(executable.commandDescription.isEmpty() ? executable.description : executable.commandDescription),
                executable.permission,
                executable.senderType == null ? "any" : executable.senderType.getSimpleName(),
                List.copyOf(segments),
                List.copyOf(flags));
    }

    private static List<Node> appendPath(final List<Node> path, final Node child) {
        final List<Node> childPath = new ArrayList<>(path.size() + 1);
        childPath.addAll(path);
        childPath.add(child);
        return List.copyOf(childPath);
    }

    private static String pathSyntax(final List<Node> path) {
        final StringBuilder syntax = new StringBuilder();
        for (int i = 0; i < path.size(); i++) {
            if (i > 0) {
                syntax.append(' ');
            }
            syntax.append(displayToken(path.get(i)));
        }
        return syntax.toString();
    }

    private static String flagsSyntax(final Node node) {
        if (node.flags.isEmpty()) {
            return "";
        }
        final StringBuilder syntax = new StringBuilder();
        for (final NQFlag flag : node.flags) {
            syntax.append('[').append("--").append(flag.name());
            if (!flag.isPresence()) {
                syntax.append(" <value>");
            }
            syntax.append("] ");
        }
        return " " + syntax.toString().trim();
    }

    private static String displayToken(final Node node) {
        return switch (node.kind) {
            case LITERAL -> node.name;
            case REQUIRED -> "<" + node.name + ">";
            case OPTIONAL -> "[<" + node.name + ">]";
        };
    }

    private static String descriptionText(final NQDescription description) {
        return description == null ? "" : description.textDescription();
    }

    private static String argumentTypeName(final NQArgumentType<?> argument) {
        if (argument == null) {
            return "";
        }
        final String simpleName = argument.getClass().getSimpleName();
        if (!simpleName.isBlank()) {
            return simpleName;
        }
        final String nativeName = argument.getNativeType().getClass().getSimpleName();
        return nativeName.isBlank() ? argument.getClass().getName() : nativeName;
    }

    private int execute(final Node node, final CommandContext<CommandSourceStack> ctx, final String flagString) {
        final CommandSender sender = ctx.getSource().getSender();
        if (node.senderType != null && !node.senderType.isInstance(sender)) {
            main.sendMessage(sender, "<error>This command can only be used by a " + node.senderType.getSimpleName() + ".");
            return Command.SINGLE_SUCCESS;
        }
        final Map<String, Object> flagValues = new HashMap<>();
        final Set<String> presentFlags = new HashSet<>();
        if (!node.flags.isEmpty() && flagString != null && !flagString.isBlank()) {
            parseFlags(node, flagString, flagValues, presentFlags);
        }
        try {
            node.handler.accept(new NQCommandContext(ctx, flagValues, presentFlags, ctx.getInput()));
        } catch (final Throwable t) {
            final String message = t.getMessage();
            main.sendMessage(sender, "<error>" + (message != null ? message : t.getClass().getSimpleName()));
            if (main.getConfiguration().debug) {
                t.printStackTrace();
            }
        }
        return Command.SINGLE_SUCCESS;
    }

    private void parseFlags(
            final Node node, final String flagString, final Map<String, Object> values, final Set<String> present) {
        final String[] tokens = flagString.trim().split("\\s+");
        for (int i = 0; i < tokens.length; i++) {
            final String token = tokens[i];
            if (!token.startsWith("--")) {
                continue;
            }
            final String flagName = token.substring(2);
            final NQFlag flag = findFlag(node, flagName);
            if (flag == null) {
                continue;
            }
            present.add(flag.name());
            // Only consume the next token as this flag's value if it isn't itself a flag: with
            // "--min --max 3", a missing value for --min must not swallow --max (which would
            // silently drop the second flag entirely).
            if (!flag.isPresence() && i + 1 < tokens.length && !tokens[i + 1].startsWith("--")) {
                final String raw = tokens[++i];
                try {
                    values.put(flag.name(), flag.valueArgument().convert(raw));
                } catch (final Exception ignored) {
                    // bad flag value -> leave unset; getValue(flag, fallback) then returns the fallback
                }
            }
        }
    }

    /**
     * Wraps an argument node's suggestions so the action-bar command hint (e.g. {@code [Quest Name]})
     * is pushed to the player on every completion request, then delegates to the node's override
     * suggestions or the argument type's own suggestions.
     */
    private SuggestionProvider<CommandSourceStack> suggestionsWithHint(final Node node) {
        final SuggestionProvider<CommandSourceStack> delegate =
                node.suggestionOverride != null
                        ? overrideSuggestions(node)
                        : (ctx, suggestionsBuilder) -> node.argument.listSuggestions(ctx, suggestionsBuilder);
        return (ctx, suggestionsBuilder) -> {
            pushHint(node, ctx);
            return delegate.getSuggestions(ctx, suggestionsBuilder);
        };
    }

    private void pushHint(final Node node, final CommandContext<CommandSourceStack> ctx) {
        try {
            if (!(ctx.getSource().getSender() instanceof org.bukkit.entity.Player player)) {
                return;
            }
            final String description = node.description == null ? null : node.description.textDescription();
            final String hint = "[" + (description != null && !description.isBlank() ? description : node.name) + "]";
            main.getUtilManager().sendCommandHint(player, ctx.getInput(), hint);
        } catch (final Throwable ignored) {
            // a hint must never break suggestions
        }
    }

    private SuggestionProvider<CommandSourceStack> overrideSuggestions(final Node node) {
        final NQSuggestionProvider override = node.suggestionOverride;
        return (ctx, suggestionsBuilder) -> {
            try {
                final NQCommandContext context = new NQCommandContext(ctx, Map.of(), Set.of(), ctx.getInput());
                final String remaining = suggestionsBuilder.getRemaining();
                for (final String suggestion : override.suggest(context, remaining)) {
                    if (suggestion != null && suggestion.regionMatches(true, 0, remaining, 0, remaining.length())) {
                        suggestionsBuilder.suggest(suggestion);
                    }
                }
            } catch (final Throwable ignored) {
                // suggestions must never break the command
            }
            return suggestionsBuilder.buildFuture();
        };
    }

    private SuggestionProvider<CommandSourceStack> flagSuggestions(final Node node) {
        return (ctx, suggestionsBuilder) -> {
            try {
                final String remaining = suggestionsBuilder.getRemaining();
                final int lastSpace = remaining.lastIndexOf(' ');
                final String token = remaining.substring(lastSpace + 1);
                final SuggestionsBuilder offset = suggestionsBuilder.createOffset(suggestionsBuilder.getStart() + lastSpace + 1);
                final String before = remaining.substring(0, lastSpace + 1).trim();
                NQFlag awaitingValue = null;
                if (!before.isEmpty()) {
                    final String[] previous = before.split("\\s+");
                    final String prev = previous[previous.length - 1];
                    if (prev.startsWith("--")) {
                        final NQFlag flag = findFlag(node, prev.substring(2));
                        if (flag != null && !flag.isPresence()) {
                            awaitingValue = flag;
                        }
                    }
                }
                if (awaitingValue != null) {
                    if (awaitingValue.valueSuggestions() != null) {
                        final NQCommandContext context = new NQCommandContext(ctx, Map.of(), Set.of(), ctx.getInput());
                        for (final String suggestion : awaitingValue.valueSuggestions().suggest(context, token)) {
                            if (suggestion != null && suggestion.regionMatches(true, 0, token, 0, token.length())) {
                                offset.suggest(suggestion);
                            }
                        }
                        return offset.buildFuture();
                    }
                    return awaitingValue.valueArgument().listSuggestions(ctx, offset);
                } else {
                    for (final NQFlag flag : node.flags) {
                        final String option = "--" + flag.name();
                        if (option.regionMatches(true, 0, token, 0, token.length())) {
                            offset.suggest(option);
                        }
                    }
                }
                return offset.buildFuture();
            } catch (final Throwable ignored) {
                return suggestionsBuilder.buildFuture();
            }
        };
    }

    private static NQFlag findFlag(final Node node, final String name) {
        for (final NQFlag flag : node.flags) {
            if (flag.name().equalsIgnoreCase(name)) {
                return flag;
            }
        }
        return null;
    }

    private static void addAliases(final Node node, final List<String> aliases) {
        for (final String alias : aliases) {
            if (!node.aliases.contains(alias)) {
                node.aliases.add(alias);
            }
        }
    }

    /** A merged node in the command tree. */
    private static final class Node {
        private final Kind kind;
        private final String name;
        private final List<String> aliases = new ArrayList<>();
        private final Map<String, Node> children = new LinkedHashMap<>();
        private NQArgumentType<?> argument;
        private NQDescription description = NQDescription.EMPTY;
        private NQSuggestionProvider suggestionOverride;
        private NQDescription commandDescription = NQDescription.EMPTY;
        private Consumer<NQCommandContext> handler;
        private String permission;
        private Class<?> senderType;
        private List<NQFlag> flags = List.of();

        private Node(final Kind kind, final String name) {
            this.kind = kind;
            this.name = name;
        }
    }
}
