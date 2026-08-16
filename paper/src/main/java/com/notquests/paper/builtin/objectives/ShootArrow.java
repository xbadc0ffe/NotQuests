package com.notquests.paper.builtin.objectives;

import java.util.Map;
import org.bukkit.Location;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.ProjectileHitEvent;
import com.notquests.paper.NotQuests;
import com.notquests.paper.commands.arguments.variables.NumberVariableArgument;
import com.notquests.paper.commands.framework.NQCommandBuilder;
import com.notquests.paper.commands.framework.NQDescription;
import com.notquests.paper.objectives.ObjectiveCatalog;
import com.notquests.paper.objectives.support.ObjectiveRegion;
import com.notquests.paper.objectives.support.ObjectiveRegionCommandPart;
import com.notquests.paper.registry.DefinedObjective;
import com.notquests.paper.registry.FieldTypes;
import com.notquests.paper.registry.ObjectiveDataContext;
import com.notquests.paper.registry.ObjectiveType;

public final class ShootArrow {
    private ShootArrow() {}

    private static final String TARGET_LOCATION = "targetLocation";
    private static final String TARGET_REGION_MIN = "targetRegionMin";
    private static final String TARGET_REGION_MAX = "targetRegionMax";
    private static final String RADIUS = "radius";

    public static void register(final NotQuests main, final ObjectiveCatalog objectives) {
        objectives.objective("ShootArrow")
                .displayName("Shoot Arrow")
                .description("Counts arrows the player lands inside a configured target region.")
                .field("amount", FieldTypes.numberExpression(false).progressNeeded(), "Number of arrows that must land inside the target region.")
                .field(TARGET_LOCATION, FieldTypes.storedLocation().config("specifics.targetLocation"), "Center of the target arrow region.")
                .field(TARGET_REGION_MIN, FieldTypes.storedLocation().config("specifics.targetRegionMin"), "Minimum corner of the target region when using a cuboid.")
                .field(TARGET_REGION_MAX, FieldTypes.storedLocation().config("specifics.targetRegionMax"), "Maximum corner of the target region when using a cuboid.")
                .field(RADIUS, FieldTypes.storedNumber(1).config("specifics.radius"), "Radius around the target center where arrows count.")
                .commands((type, builder, level) -> registerCommands(main, type, builder, level))
                .taskDescription((objective, questPlayer, activeObjective) -> taskDescription(main, objective, questPlayer, activeObjective))
                .on(ProjectileHitEvent.class, event -> {
                    if (!(event.getEntity() instanceof Arrow arrow) || !(arrow.getShooter() instanceof Player player)) {
                        return null;
                    }
                    return player;
                }, (event, objective) -> {
                    final Location arrowLocation = event.getEntity().getLocation();
                    if (countsArrowLocation(
                            objective.value(TARGET_LOCATION, Location.class),
                            objective.value(TARGET_REGION_MIN, Location.class),
                            objective.value(TARGET_REGION_MAX, Location.class),
                            objective.value(RADIUS, Double.class),
                            arrowLocation)) {
                        objective.addProgress(1);
                    }
                })
                .afterLoad((objective, context) -> objective.setLocation(objective.value(TARGET_LOCATION, Location.class), false))
                .register();
    }

    private static void registerCommands(
            final NotQuests main,
            final ObjectiveType type,
            final NQCommandBuilder builder,
            final int level) {
        final NQCommandBuilder amountBuilder = builder.required(
                "amount",
                NumberVariableArgument.numberVariableArgument("amount", null, false),
                NQDescription.of("Number of arrows the player must land inside the target region."));

        main.getCommandManager().getNQCommandManager().command(ObjectiveRegionCommandPart
                .centerRadius(
                        amountBuilder,
                        "target arrow region",
                        "Radius in blocks around the target center where arrows count.")
                .handler(context -> {
                    final ObjectiveRegionCommandPart.CenterRadius centerRadius =
                            ObjectiveRegionCommandPart.centerRadius(context);
                    addObjective(main, type, context, level, centerRadius.center(), centerRadius.radius(), null);
                }));

        if (main.getIntegrationsManager().isWorldEditEnabled()) {
            main.getCommandManager().getNQCommandManager().command(amountBuilder
                    .literal(
                            ObjectiveRegionCommandPart.WORLD_EDIT_SELECTION,
                            NQDescription.of("Uses your current WorldEdit selection as the arrow target region."))
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
                        addObjective(main, type, context, level, region.center(), region.enclosingRadius(), region);
                    }));
        }
    }

    private static void addObjective(
            final NotQuests main,
            final ObjectiveType type,
            final com.notquests.paper.commands.framework.NQCommandContext context,
            final int level,
            final Location center,
            final double radius,
            final ObjectiveRegion region) {
        final DefinedObjective objective = type.createObjective();
        objective.setProgressNeededExpression(context.get("amount"));
        objective.setValue(TARGET_LOCATION, center);
        objective.setValue(RADIUS, Math.max(0, radius));
        if (region != null) {
            objective.setValue(TARGET_REGION_MIN, region.min());
            objective.setValue(TARGET_REGION_MAX, region.max());
        }
        objective.setLocation(center, false);
        main.getObjectiveCatalog().addObjective(objective, context, level);
    }

    public static boolean countsArrowLocation(
            final Location targetLocation,
            final Location targetRegionMin,
            final Location targetRegionMax,
            final Double configuredRadius,
            final Location arrowLocation) {
        final ObjectiveRegion region = targetRegion(targetRegionMin, targetRegionMax);
        if (region != null) {
            return region.contains(arrowLocation);
        }
        if (targetLocation == null || arrowLocation == null) {
            return false;
        }
        if (targetLocation.getWorld() == null || arrowLocation.getWorld() == null) {
            return false;
        }
        if (!targetLocation.getWorld().equals(arrowLocation.getWorld())) {
            return false;
        }
        final double radius = configuredRadius == null ? 1 : Math.max(0, configuredRadius);
        return arrowLocation.distanceSquared(targetLocation) <= radius * radius;
    }

    public static ObjectiveRegion targetRegion(final Location targetRegionMin, final Location targetRegionMax) {
        if (targetRegionMin == null || targetRegionMax == null) {
            return null;
        }
        return new ObjectiveRegion(targetRegionMin, targetRegionMax);
    }

    private static String taskDescription(
            final NotQuests main,
            final ObjectiveDataContext objective,
            final com.notquests.paper.structs.QuestPlayer questPlayer,
            final com.notquests.paper.structs.ActiveObjective activeObjective) {
        final Location targetLocation = objective.value(TARGET_LOCATION, Location.class);
        final Double radius = objective.value(RADIUS, Double.class);
        final String worldName =
                targetLocation != null && targetLocation.getWorld() != null ? targetLocation.getWorld().getName() : "???";

        return main.getLanguageManager()
                .getString(
                        "chat.objectives.taskDescription.shootArrow.base",
                        questPlayer,
                        activeObjective,
                        Map.of(
                                "%COORDINATES%",
                                targetLocation == null
                                        ? "???"
                                        : "X: "
                                                + targetLocation.getX()
                                                + " Y: "
                                                + targetLocation.getY()
                                                + " Z: "
                                                + targetLocation.getZ(),
                                "%WORLDNAME%",
                                worldName,
                                "%RADIUS%",
                                String.valueOf(radius == null ? 1 : radius)));
    }
}
