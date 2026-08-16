package com.notquests.paper.builtin.conditions;

import com.notquests.paper.NotQuests;
import com.notquests.paper.conditions.ConditionCatalog;
import com.notquests.paper.registry.FieldTypes;

public final class WorldTime {
    private static final String MIN_TIME = "minTime";
    private static final String MAX_TIME = "maxTime";

    private WorldTime() {}

    public static void register(final NotQuests main, final ConditionCatalog conditions) {
        conditions.condition("WorldTime")
                .displayName("World Time")
                .description("Checks whether the player's current world time is inside a configured hour range.")
                .field(
                        MIN_TIME,
                        FieldTypes.integer(0).config("specifics.minTime"),
                        "Earliest allowed hour in the Minecraft day, using 0-24 style time.")
                .field(
                        MAX_TIME,
                        FieldTypes.integer(24).config("specifics.maxTime"),
                        "Latest allowed hour in the Minecraft day, using 0-24 style time.")
                .singleLine((condition, arguments) -> {
                    condition.setValue(MIN_TIME, Integer.parseInt(arguments.get(0)));
                    condition.setValue(MAX_TIME, Integer.parseInt(arguments.get(1)));
                })
                .check((condition, questPlayer) -> {
                    final int minTime = condition.integer(MIN_TIME);
                    final int maxTime = condition.integer(MAX_TIME, 24);
                    long currentTime = questPlayer.getPlayer().getWorld().getTime();
                    currentTime = currentTime >= 18000 ? currentTime / 1000 - 18 : currentTime / 1000 + 6;

                    if (maxTime >= minTime) {
                        if (currentTime <= maxTime && currentTime >= minTime) {
                            return "";
                        }
                    } else if (currentTime <= minTime) {
                        if (currentTime <= maxTime) {
                            return "";
                        }
                    } else if (currentTime >= minTime && currentTime <= 24) {
                        return "";
                    }
                    return "<YELLOW>Come back between <highlight>"
                            + minTime
                            + "</highlight> and <highlight>"
                            + maxTime
                            + "</highlight> (It's now "
                            + currentTime
                            + ")";
                })
                .conditionDescription((condition, questPlayer, objects) ->
                        "<GRAY>-- World time: "
                                + condition.integer(MIN_TIME)
                                + " - "
                                + condition.integer(MAX_TIME, 24))
                .register();
    }
}
