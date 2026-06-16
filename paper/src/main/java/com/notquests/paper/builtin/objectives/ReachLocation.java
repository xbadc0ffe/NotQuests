package com.notquests.paper.builtin.objectives;

import java.util.Map;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerMoveEvent;
import com.notquests.paper.NotQuests;
import com.notquests.paper.commands.framework.NQArguments;
import com.notquests.paper.commands.framework.NQCommandBuilder;
import com.notquests.paper.commands.framework.NQDescription;
import com.notquests.paper.objectives.ObjectiveCatalog;
import com.notquests.paper.objectives.support.ObjectiveRegion;
import com.notquests.paper.objectives.support.ObjectiveRegionCommandPart;
import com.notquests.paper.registry.DefinedObjective;
import com.notquests.paper.registry.FieldTypes;
import com.notquests.paper.registry.ObjectiveType;

public final class ReachLocation {
    private ReachLocation() {}

    private static final String MIN_LOCATION = "minLocation";
    private static final String MAX_LOCATION = "maxLocation";
    private static final String LOCATION_NAME = "locationName";

    public static void register(final NotQuests main, final ObjectiveCatalog objectives) {
        objectives.objective("ReachLocation")
                .displayName("Reach Location")
                .description("Completes when the player enters a configured location region.")
                .field(
                        MIN_LOCATION,
                        FieldTypes.storedLocation().config("specifics.minLocation"),
                        "Minimum corner of the target region.")
                .field(
                        MAX_LOCATION,
                        FieldTypes.storedLocation().config("specifics.maxLocation"),
                        "Maximum corner of the target region.")
                .field(
                        LOCATION_NAME,
                        FieldTypes.greedyText().config("specifics.locationName"),
                        "Location name shown to players in objective task text.")
                .commands((type, builder, level) -> registerCommands(main, objectives, type, builder, level))
                .taskDescription((objective, questPlayer, activeObjective) -> main.getLanguageManager()
                        .getString(
                                "chat.objectives.taskDescription.reachLocation.base",
                                questPlayer,
                                activeObjective,
                                Map.of("%LOCATIONNAME%", objective.text(LOCATION_NAME))))
                .on(PlayerMoveEvent.class, PlayerMoveEvent::getPlayer, (event, objective) -> {
                    if (!main.getConfiguration().isMoveEventEnabled()
                            || event.getTo() == null
                            || sameBlock(event.getFrom(), event.getTo())) {
                        return;
                    }
                    if (contains(objective.value(MIN_LOCATION, Location.class), objective.value(MAX_LOCATION, Location.class), event.getTo())) {
                        objective.addProgress(1);
                    }
                })
                .afterLoad((objective, context) -> objective.setLocation(objective.value(MIN_LOCATION, Location.class), false))
                .register();
    }

    private static void registerCommands(
            final NotQuests main,
            final ObjectiveCatalog objectives,
            final ObjectiveType type,
            final NQCommandBuilder builder,
            final int level) {
        main.getCommandManager().getNQCommandManager().command(ObjectiveRegionCommandPart
                .centerRadius(
                        builder,
                        "reach-location region",
                        "Radius in blocks around the center that counts as reaching this location.")
                .required(
                        LOCATION_NAME,
                        NQArguments.greedyStringArgument(),
                        NQDescription.of("Name shown to players for this location in objective task text."))
                .handler(context -> {
                    final ObjectiveRegion region = ObjectiveRegionCommandPart.centerRadius(context).asRegion();
                    addObjective(main, type, context, level, context.get(LOCATION_NAME), region);
                }));

        if (main.getIntegrationsManager().isWorldEditEnabled()) {
            main.getCommandManager().getNQCommandManager().command(builder
                    .literal(
                            ObjectiveRegionCommandPart.WORLD_EDIT_SELECTION,
                            NQDescription.of("Uses your current WorldEdit selection as the target region."))
                    .required(
                            LOCATION_NAME,
                            NQArguments.greedyStringArgument(),
                            NQDescription.of("Name shown to players for this location in objective task text."))
                    .handler(context -> {
                        if (!(context.sender() instanceof final Player player)) {
                            context.sender().sendMessage(main.parse(
                                    "<error>This shortcut can only be used by a player. Use the coordinate form from console."));
                            return;
                        }
                        final ObjectiveRegion region =
                                main.getIntegrationsManager().getWorldEditManager().getSelectionRegionOrNull(player);
                        if (region == null) {
                            context.sender().sendMessage(
                                    main.parse("<error>Please make a region selection using WorldEdit first."));
                            return;
                        }
                        addObjective(main, type, context, level, context.get(LOCATION_NAME), region);
                    }));
        }
    }

    private static void addObjective(
            final NotQuests main,
            final ObjectiveType type,
            final com.notquests.paper.commands.framework.NQCommandContext context,
            final int level,
            final String locationName,
            final ObjectiveRegion region) {
        final DefinedObjective objective = type.createObjective();
        objective.setValue(MIN_LOCATION, region.min());
        objective.setValue(MAX_LOCATION, region.max());
        objective.setValue(LOCATION_NAME, locationName);
        objective.setLocation(region.min(), false);
        main.getObjectiveCatalog().addObjective(objective, context, level);
    }

    private static boolean sameBlock(final Location from, final Location to) {
        return from.getBlockX() == to.getBlockX()
                && from.getBlockY() == to.getBlockY()
                && from.getBlockZ() == to.getBlockZ();
    }

    public static boolean contains(final Location minLocation, final Location maxLocation, final Location currentLocation) {
        if (minLocation == null || maxLocation == null || currentLocation == null) {
            return false;
        }
        if (minLocation.getWorld() != null
                && currentLocation.getWorld() != null
                && !currentLocation.getWorld().equals(minLocation.getWorld())) {
            return false;
        }
        return currentLocation.getX() >= minLocation.getX()
                && currentLocation.getX() <= maxLocation.getX()
                && currentLocation.getZ() >= minLocation.getZ()
                && currentLocation.getZ() <= maxLocation.getZ()
                && currentLocation.getY() >= minLocation.getY()
                && currentLocation.getY() <= maxLocation.getY();
    }
}
