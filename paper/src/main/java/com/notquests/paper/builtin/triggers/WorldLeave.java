package com.notquests.paper.builtin.triggers;

import org.bukkit.Bukkit;
import org.bukkit.World;
import com.notquests.paper.NotQuests;
import com.notquests.paper.triggers.TriggerCatalog;
import com.notquests.paper.registry.FieldTypes;

public final class WorldLeave {
    public static final String WORLD = "world to leave";

    private WorldLeave() {}

    public static void register(final NotQuests main, final TriggerCatalog triggers) {
        triggers.trigger("WORLDLEAVE")
                .displayName("World Leave")
                .description("Runs the selected action after the quest player leaves a specific world.")
                .field(
                        WORLD,
                        FieldTypes.text((context, input) -> Bukkit.getWorlds().stream()
                                .map(World::getName)
                                .collect(java.util.stream.Collectors.toCollection(() -> {
                                    final java.util.ArrayList<String> worlds = new java.util.ArrayList<>();
                                    worlds.add("ALL");
                                    return worlds;
                                }))).config("specifics.worldToLeave"),
                        "World name that must be left, or ALL for any world.")
                .field(
                        "amount",
                        FieldTypes.integer(1).config("amountNeeded"),
                        "Number of matching world-leave events required before this trigger runs.")
                .triggerDescription(trigger -> "World to leave: <WHITE>" + trigger.text(WORLD))
                .register();
    }
}
