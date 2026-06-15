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

package rocks.gravili.notquests.paper.structs.objectives;

import static rocks.gravili.notquests.paper.commands.arguments.variables.NumberVariableArgument.numberVariableArgument;

import java.util.Map;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.checkerframework.checker.nullness.qual.Nullable;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.commands.framework.NQCommandBuilder;
import rocks.gravili.notquests.paper.commands.framework.NQCommandContext;
import rocks.gravili.notquests.paper.commands.framework.NQCommandManager;
import rocks.gravili.notquests.paper.commands.framework.NQDescription;
import rocks.gravili.notquests.paper.structs.ActiveObjective;
import rocks.gravili.notquests.paper.structs.QuestPlayer;

public class ShootArrowObjective extends Objective {
    private Location targetLocation;
    private Location targetRegionMin;
    private Location targetRegionMax;
    private double radius = 1;

    public ShootArrowObjective(final NotQuests main) {
        super(main);
    }

    public static void handleCommands(
            final NotQuests main,
            final NQCommandManager manager,
            final NQCommandBuilder addObjectiveBuilder,
            final int level) {
        final NQCommandBuilder amountBuilder = addObjectiveBuilder
                .required(
                        "amount",
                        numberVariableArgument("amount", null, false),
                        NQDescription.of("Number of arrows the player must land inside the target region."));

        manager.command(ObjectiveRegionCommandPart
                .centerRadius(
                        amountBuilder,
                        "target arrow region",
                        "Radius in blocks around the target center where arrows count.")
                .handler(context -> {
                    final ObjectiveRegionCommandPart.CenterRadius centerRadius =
                            ObjectiveRegionCommandPart.centerRadius(context);
                    addShootArrowObjective(main, context, level, centerRadius.center(), centerRadius.radius(), null);
                }));

        if (main.getIntegrationsManager().isWorldEditEnabled()) {
            manager.command(amountBuilder
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
                        addShootArrowObjective(main, context, level, region.center(), region.enclosingRadius(), region);
                    }));
        }
    }

    private static void addShootArrowObjective(
            final NotQuests main,
            final NQCommandContext context,
            final int level,
            final Location center,
            final double radius,
            final ObjectiveRegion region) {
        final String amountExpression = context.get("amount");

        final ShootArrowObjective shootArrowObjective = new ShootArrowObjective(main);
        shootArrowObjective.setTargetLocation(center);
        shootArrowObjective.setRadius(radius);
        if (region != null) {
            shootArrowObjective.setTargetRegion(region);
        }
        shootArrowObjective.setProgressNeededExpression(amountExpression);

        main.getObjectiveManager().addObjective(shootArrowObjective, context, level);
    }

    private ObjectiveRegion targetRegion() {
        if (targetRegionMin == null || targetRegionMax == null) {
            return null;
        }
        return new ObjectiveRegion(targetRegionMin, targetRegionMax);
    }

    public void setTargetRegion(final ObjectiveRegion region) {
        targetRegionMin = region.min();
        targetRegionMax = region.max();
        targetLocation = region.center();
        radius = region.enclosingRadius();
        setLocation(targetLocation, false);
    }

    public boolean hasTargetRegion() {
        return targetRegion() != null;
    }

    public ObjectiveRegion getTargetRegion() {
        return targetRegion();
    }

    public boolean countsArrowLocation(final Location arrowLocation) {
        final ObjectiveRegion region = targetRegion();
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
        return arrowLocation.distanceSquared(targetLocation) <= radius * radius;
    }

    @Override
    public String getTaskDescriptionInternal(
            final QuestPlayer questPlayer, final @Nullable ActiveObjective activeObjective) {
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
                                String.valueOf(radius)));
    }

    @Override
    public void save(final FileConfiguration configuration, final String initialPath) {
        configuration.set(initialPath + ".specifics.targetLocation", targetLocation);
        configuration.set(initialPath + ".specifics.targetRegionMin", targetRegionMin);
        configuration.set(initialPath + ".specifics.targetRegionMax", targetRegionMax);
        configuration.set(initialPath + ".specifics.radius", radius);
    }

    @Override
    public void load(final FileConfiguration configuration, final String initialPath) {
        setTargetLocation(configuration.getLocation(initialPath + ".specifics.targetLocation"));
        targetRegionMin = configuration.getLocation(initialPath + ".specifics.targetRegionMin");
        targetRegionMax = configuration.getLocation(initialPath + ".specifics.targetRegionMax");
        radius = configuration.getDouble(initialPath + ".specifics.radius", 1);
    }

    @Override
    public void onObjectiveUnlock(
            final ActiveObjective activeObjective,
            final boolean unlockedDuringPluginStartupQuestLoadingProcess) {
    }

    @Override
    public void onObjectiveCompleteOrLock(
            final ActiveObjective activeObjective,
            final boolean lockedOrCompletedDuringPluginStartupQuestLoadingProcess,
            final boolean completed) {
    }

    public Location getTargetLocation() {
        return targetLocation;
    }

    public void setTargetLocation(final Location targetLocation) {
        this.targetLocation = targetLocation;
        targetRegionMin = null;
        targetRegionMax = null;
        setLocation(targetLocation, false);
    }

    public double getRadius() {
        return radius;
    }

    public void setRadius(final double radius) {
        this.radius = Math.max(0, radius);
    }
}
