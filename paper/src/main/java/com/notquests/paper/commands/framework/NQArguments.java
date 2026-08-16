package com.notquests.paper.commands.framework;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.World;
import com.notquests.paper.NotQuests;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Factories for primitive/common {@link NQArgumentType}s (numbers, strings, durations, worlds,
 * components). Used mostly for flag values, which Cloud backed with its standard parsers. Domain
 * arguments (quests, objectives, …) live next to their Cloud counterparts in
 * {@code commands.arguments} instead.
 */
public final class NQArguments {
    private static final Pattern DURATION = Pattern.compile(
            "(\\d+)\\s*("
                    + "ms|msec|msecs|millis|millisecond|milliseconds|milisecond|miliseconds|"
                    + "s|sec|secs|second|seconds|"
                    + "m|min|mins|minute|minutes|"
                    + "h|hr|hrs|hour|hours|"
                    + "d|day|days"
                    + ")?",
            Pattern.CASE_INSENSITIVE);

    private NQArguments() {}

    public static NQArgumentType<Integer> integerArgument() {
        return new NQArgumentType<>() {
            @Override
            public Integer convert(final String input) throws CommandSyntaxException {
                try {
                    return Integer.parseInt(input.trim());
                } catch (final NumberFormatException e) {
                    throw fail("'" + input + "' is not a whole number");
                }
            }

            @Override
            protected List<String> suggest(final CommandContext<?> context, final String remaining) {
                return List.of("1", "2", "3", "4", "5", "10", "16", "32", "64");
            }

            @Override
            public String valueTypeName() {
                return "whole number";
            }
        };
    }

    public static NQArgumentType<Double> doubleArgument() {
        return new NQArgumentType<>() {
            @Override
            public Double convert(final String input) throws CommandSyntaxException {
                try {
                    return Double.parseDouble(input.trim());
                } catch (final NumberFormatException e) {
                    throw fail("'" + input + "' is not a number");
                }
            }

            @Override
            protected List<String> suggest(final CommandContext<?> context, final String remaining) {
                return List.of("0", "1", "2", "3", "4", "5", "10", "16", "32", "64", "100");
            }

            @Override
            public String valueTypeName() {
                return "number";
            }
        };
    }

    public static NQArgumentType<Long> longArgument() {
        return new NQArgumentType<>() {
            @Override
            public Long convert(final String input) throws CommandSyntaxException {
                try {
                    return Long.parseLong(input.trim());
                } catch (final NumberFormatException e) {
                    throw fail("'" + input + "' is not a whole number");
                }
            }

            @Override
            public String valueTypeName() {
                return "whole number";
            }
        };
    }

    public static NQArgumentType<Boolean> booleanArgument() {
        return new NQArgumentType<>() {
            @Override
            public Boolean convert(final String input) throws CommandSyntaxException {
                final String value = input.trim().toLowerCase(java.util.Locale.ROOT);
                return switch (value) {
                    case "true", "yes", "y", "on" -> Boolean.TRUE;
                    case "false", "no", "n", "off" -> Boolean.FALSE;
                    default -> throw fail("'" + input + "' is not a boolean (true/false)");
                };
            }

            @Override
            protected List<String> suggest(final CommandContext<?> context, final String remaining) {
                return List.of("true", "false");
            }

            @Override
            public String valueTypeName() {
                return "true or false";
            }
        };
    }

    public static NQArgumentType<String> stringArgument() {
        return new NQArgumentType<>() {
            @Override
            public String convert(final String input) {
                return input;
            }

            @Override
            public String valueTypeName() {
                return "text";
            }
        };
    }

    /** A string that consumes the rest of the line (Cloud's {@code greedyStringParser}). */
    public static NQArgumentType<String> greedyStringArgument() {
        return new NQArgumentType<>() {
            @Override
            public String convert(final String input) {
                return input;
            }

            @Override
            public com.mojang.brigadier.arguments.ArgumentType<String> getNativeType() {
                return com.mojang.brigadier.arguments.StringArgumentType.greedyString();
            }

            @Override
            public String valueTypeName() {
                return "text";
            }
        };
    }

    public static NQArgumentType<String[]> stringArrayArgument() {
        return new NQArgumentType<>() {
            @Override
            public String[] convert(final String input) {
                return input.trim().isEmpty() ? new String[0] : input.trim().split("\\s+");
            }

            @Override
            public com.mojang.brigadier.arguments.ArgumentType<String> getNativeType() {
                return com.mojang.brigadier.arguments.StringArgumentType.greedyString();
            }

            @Override
            public String valueTypeName() {
                return "text";
            }
        };
    }

    /** Parses durations like {@code 500ms}, {@code 30s}, {@code 5m}, {@code 2h}, {@code 1d}. */
    public static NQArgumentType<Duration> durationArgument() {
        return new NQArgumentType<>() {
            @Override
            public Duration convert(final String input) throws CommandSyntaxException {
                final Matcher matcher = DURATION.matcher(input.trim());
                if (!matcher.matches()) {
                    throw fail("'" + input + "' is not a valid duration (e.g. 500ms, 30s, 5m, 2h, 1d)");
                }
                final long amount = Long.parseLong(matcher.group(1));
                final String unit = matcher.group(2) == null
                        ? "ms"
                        : matcher.group(2).toLowerCase(Locale.ROOT);
                return switch (unit) {
                    case "s", "sec", "secs", "second", "seconds" -> Duration.ofSeconds(amount);
                    case "m", "min", "mins", "minute", "minutes" -> Duration.ofMinutes(amount);
                    case "h", "hr", "hrs", "hour", "hours" -> Duration.ofHours(amount);
                    case "d", "day", "days" -> Duration.ofDays(amount);
                    default -> Duration.ofMillis(amount);
                };
            }

            @Override
            protected List<String> suggest(final CommandContext<?> context, final String remaining) {
                return List.of("250ms", "500ms", "1s", "5s", "10s", "30s", "1m", "5m", "1h");
            }

            @Override
            public String valueTypeName() {
                return "duration such as 500ms, 1s, 5m, or 2h";
            }
        };
    }

    public static NQArgumentType<World> worldArgument() {
        return new NQArgumentType<>() {
            @Override
            public World convert(final String input) throws CommandSyntaxException {
                final World world = Bukkit.getWorld(input);
                if (world == null) {
                    throw fail("World '" + input + "' does not exist");
                }
                return world;
            }

            @Override
            protected List<String> suggest(final CommandContext<?> context, final String remaining) {
                final List<String> names = new ArrayList<>();
                for (final World world : Bukkit.getWorlds()) {
                    names.add(world.getName());
                }
                return names;
            }

            @Override
            public String valueTypeName() {
                return "world name";
            }
        };
    }

    public static NQArgumentType<org.bukkit.entity.Player> playerArgument() {
        return new NQArgumentType<>() {
            @Override
            public org.bukkit.entity.Player convert(final String input) throws CommandSyntaxException {
                final org.bukkit.entity.Player player = Bukkit.getPlayerExact(input.trim());
                if (player == null) {
                    throw fail("Player '" + input + "' is not online");
                }
                return player;
            }

            @Override
            protected List<String> suggest(final CommandContext<?> context, final String remaining) {
                final List<String> names = new ArrayList<>();
                for (final org.bukkit.entity.Player online : Bukkit.getOnlinePlayers()) {
                    names.add(online.getName());
                }
                return names;
            }

            @Override
            public String valueTypeName() {
                return "online player name";
            }
        };
    }

    public static <E extends Enum<E>> NQArgumentType<E> enumArgument(final Class<E> type) {
        return new NQArgumentType<>() {
            @Override
            public E convert(final String input) throws CommandSyntaxException {
                for (final E constant : type.getEnumConstants()) {
                    if (constant.name().equalsIgnoreCase(input.trim())) {
                        return constant;
                    }
                }
                throw fail("'" + input + "' is not a valid " + type.getSimpleName());
            }

            @Override
            protected List<String> suggest(final CommandContext<?> context, final String remaining) {
                final List<String> names = new ArrayList<>();
                for (final E constant : type.getEnumConstants()) {
                    names.add(constant.name());
                }
                return names;
            }

            @Override
            public String valueTypeName() {
                return type.getSimpleName() + " value";
            }
        };
    }

    /** A MiniMessage component (e.g. a custom task description). */
    public static NQArgumentType<Component> componentArgument(final NotQuests main) {
        return new NQArgumentType<>() {
            @Override
            public Component convert(final String input) {
                return main.parse(input);
            }

            @Override
            public com.mojang.brigadier.arguments.ArgumentType<String> getNativeType() {
                return com.mojang.brigadier.arguments.StringArgumentType.greedyString();
            }

            @Override
            public String valueTypeName() {
                return "MiniMessage text";
            }
        };
    }
}
