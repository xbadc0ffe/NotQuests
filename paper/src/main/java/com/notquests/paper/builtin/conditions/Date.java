package com.notquests.paper.builtin.conditions;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Locale;
import java.util.TimeZone;
import com.notquests.paper.NotQuests;
import com.notquests.paper.commands.framework.NQArguments;
import com.notquests.paper.commands.framework.NQDescription;
import com.notquests.paper.commands.framework.NQFlag;
import com.notquests.paper.conditions.ConditionCatalog;
import com.notquests.paper.registry.ConditionDataContext;
import com.notquests.paper.registry.ConditionType;
import com.notquests.paper.registry.FieldTypes;

public final class Date {
    private static final String OPERATION = "Date operation";
    private static final String YEAR = "year";
    private static final String MONTH = "month";
    private static final String DAY = "day";
    private static final String HOURS = "hours";
    private static final String MINUTES = "minutes";
    private static final String SECONDS = "seconds";
    private static final String TIME_ZONE = "timeZone";

    private Date() {}

    public static void register(final NotQuests main, final ConditionCatalog conditions) {
        conditions.condition("Date")
                .displayName("Date")
                .description("Checks whether the current real-world date or time is before or after configured values.")
                .field(
                        OPERATION,
                        FieldTypes.text().config("specifics.operation"),
                        "Whether the current date/time must be before or after the configured values.")
                .flag(YEAR, FieldTypes.integer(-1).config("specifics.year"), "Year that must match for this date condition.")
                .flag(MONTH, FieldTypes.integer(-1).config("specifics.month"), "Month that must match for this date condition.")
                .flag(DAY, FieldTypes.integer(-1).config("specifics.day"), "Day of month that must match for this date condition.")
                .flag(HOURS, FieldTypes.integer(-1).config("specifics.hours"), "Hour that must match for this date condition.")
                .flag(MINUTES, FieldTypes.integer(-1).config("specifics.minutes"), "Minute that must match for this date condition.")
                .flag(SECONDS, FieldTypes.integer(-1).config("specifics.seconds"), "Second that must match for this date condition.")
                .flag(TIME_ZONE, FieldTypes.text().config("specifics.timeZone"), "Time zone used when evaluating the date condition.")
                .commands(Date::registerCommands)
                .singleLine((condition, arguments) -> {
                    condition.setValue(OPERATION, arguments.get(0).toLowerCase(Locale.ROOT));
                    condition.setValue(YEAR, Integer.parseInt(arguments.get(1)));
                    condition.setValue(MONTH, Integer.parseInt(arguments.get(2)));
                    condition.setValue(DAY, Integer.parseInt(arguments.get(3)));
                    condition.setValue(HOURS, Integer.parseInt(arguments.get(4)));
                    condition.setValue(MINUTES, Integer.parseInt(arguments.get(5)));
                    condition.setValue(SECONDS, Integer.parseInt(arguments.get(6)));
                    condition.setValue(TIME_ZONE, arguments.get(7));
                })
                .check(Date::check)
                .conditionDescription(Date::description)
                .register();
    }

    private static void registerCommands(
            final ConditionType type,
            final com.notquests.paper.commands.framework.NQCommandBuilder builder,
            final com.notquests.paper.conditions.ConditionFor conditionFor) {
        final NQFlag year = NQFlag.builder(YEAR, NQDescription.of("Year that must match for this date condition."))
                .withArgument(NQArguments.integerArgument())
                .build();
        final NQFlag month = NQFlag.builder(MONTH, NQDescription.of("Month that must match for this date condition."))
                .withArgument(NQArguments.integerArgument())
                .build();
        final NQFlag day = NQFlag.builder(DAY, NQDescription.of("Day of month that must match for this date condition."))
                .withArgument(NQArguments.integerArgument())
                .build();
        final NQFlag hours = NQFlag.builder(HOURS, NQDescription.of("Hour that must match for this date condition."))
                .withArgument(NQArguments.integerArgument())
                .build();
        final NQFlag minutes = NQFlag.builder(MINUTES, NQDescription.of("Minute that must match for this date condition."))
                .withArgument(NQArguments.integerArgument())
                .build();
        final NQFlag seconds = NQFlag.builder(SECONDS, NQDescription.of("Second that must match for this date condition."))
                .withArgument(NQArguments.integerArgument())
                .build();
        final NQFlag timeZone = NQFlag.builder(TIME_ZONE, NQDescription.of("Time zone used when evaluating the date condition."))
                .withArgument(NQArguments.stringArgument())
                .withSuggestions((context, input) -> Arrays.stream(TimeZone.getAvailableIDs()).toList())
                .build();

        type.main().getCommandManager().getNQCommandManager().command(builder
                .required(
                        OPERATION,
                        NQArguments.stringArgument(),
                        NQDescription.of("Whether the current date/time must be before or after the configured values."),
                        (context, input) -> java.util.List.of("after", "before"))
                .flag(year)
                .flag(month)
                .flag(day)
                .flag(hours)
                .flag(minutes)
                .flag(seconds)
                .flag(timeZone)
                .handler(context -> {
                    final String operation = context.<String>get(OPERATION).toLowerCase(Locale.ROOT);
                    if (!operation.equals("after") && !operation.equals("before")) {
                        context.sender().sendMessage(type.main().parse("<error>Error: The date operation can only be <highlight>after</highlight> or <highlight>before</highlight>."));
                        return;
                    }
                    final var condition = type.createCondition();
                    condition.setValue(OPERATION, operation);
                    condition.setValue(YEAR, context.flags().getValue(year.name(), -1));
                    condition.setValue(MONTH, context.flags().getValue(month.name(), -1));
                    condition.setValue(DAY, context.flags().getValue(day.name(), -1));
                    condition.setValue(HOURS, context.flags().getValue(hours.name(), -1));
                    condition.setValue(MINUTES, context.flags().getValue(minutes.name(), -1));
                    condition.setValue(SECONDS, context.flags().getValue(seconds.name(), -1));
                    condition.setValue(TIME_ZONE, context.flags().getValue(timeZone.name(), ""));
                    type.main().getConditionCatalog().addCondition(condition, context, conditionFor);
                }));
    }

    private static String check(final ConditionDataContext condition, final com.notquests.paper.structs.QuestPlayer questPlayer) {
        final LocalDateTime currentTime = currentTime(condition);
        final LocalDateTime timeToCompare = configuredTime(condition, currentTime);
        final String operation = condition.text(OPERATION);
        if (operation.equals("before")) {
            if (!currentTime.isBefore(timeToCompare)) {
                return "<YELLOW>The current date needs to be before the " + timeToCompare;
            }
        } else if (operation.equals("after")) {
            if (!currentTime.isAfter(timeToCompare)) {
                return "<YELLOW>The current date needs to be after the " + timeToCompare;
            }
        } else {
            return "<error>Invalid date operator: <highlight>" + operation + "</highlight>.";
        }
        return "";
    }

    private static String description(
            final ConditionDataContext condition,
            final com.notquests.paper.structs.QuestPlayer questPlayer,
            final Object... objects) {
        final LocalDateTime currentTime = currentTime(condition);
        final LocalDateTime timeToCompare = configuredTime(condition, currentTime);
        final String operation = condition.text(OPERATION);
        if (operation.equals("before")) {
            return "<GRAY>- Current date before " + timeToCompare;
        }
        if (operation.equals("after")) {
            return "<GRAY>- Current date after " + timeToCompare;
        }
        return "<error>Invalid date operator: <highlight>" + operation + "</highlight>.";
    }

    private static LocalDateTime currentTime(final ConditionDataContext condition) {
        final String zone = condition.text(TIME_ZONE);
        return zone.isBlank() ? LocalDateTime.now() : LocalDateTime.now(TimeZone.getTimeZone(zone).toZoneId());
    }

    private static LocalDateTime configuredTime(
            final ConditionDataContext condition, final LocalDateTime currentTime) {
        return LocalDateTime.of(
                condition.integer(YEAR, -1) > -1 ? condition.integer(YEAR, -1) : currentTime.getYear(),
                condition.integer(MONTH, -1) > -1 ? condition.integer(MONTH, -1) : currentTime.getMonthValue(),
                condition.integer(DAY, -1) > -1 ? condition.integer(DAY, -1) : currentTime.getDayOfMonth(),
                condition.integer(HOURS, -1) > -1 ? condition.integer(HOURS, -1) : currentTime.getHour(),
                condition.integer(MINUTES, -1) > -1 ? condition.integer(MINUTES, -1) : currentTime.getMinute(),
                condition.integer(SECONDS, -1) > -1 ? condition.integer(SECONDS, -1) : currentTime.getSecond());
    }
}
