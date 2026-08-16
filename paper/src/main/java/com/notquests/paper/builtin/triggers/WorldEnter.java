package com.notquests.paper.builtin.triggers;

import org.bukkit.Bukkit;
import org.bukkit.World;
import com.notquests.paper.NotQuests;
import com.notquests.paper.triggers.TriggerCatalog;
import com.notquests.paper.registry.FieldTypes;

public final class WorldEnter {
    public static final String WORLD = "world to enter";

    private WorldEnter() {}

    public static void register(final NotQuests main, final TriggerCatalog triggers) {
        triggers.trigger("WORLDENTER")
                .displayName("World Enter")
                .description("Runs the selected action after the quest player enters a specific world.")
                .field(
                        WORLD,
                        FieldTypes.text((context, input) -> Bukkit.getWorlds().stream()
                                .map(World::getName)
                                .collect(java.util.stream.Collectors.toCollection(() -> {
                                    final java.util.ArrayList<String> worlds = new java.util.ArrayList<>();
                                    worlds.add("ALL");
                                    return worlds;
                                }))).config("specifics.worldToEnter"),
                        "World name that must be entered, or ALL for any world.")
                .field(
                        "amount",
                        FieldTypes.integer(1).config("amountNeeded"),
                        "Number of matching world-enter events required before this trigger runs.")
                .triggerDescription(trigger -> "World to enter: <WHITE>" + trigger.text(WORLD))
                .register();
    }
}
