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
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.checkerframework.checker.nullness.qual.Nullable;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.commands.framework.NQArguments;
import rocks.gravili.notquests.paper.commands.framework.NQCommandBuilder;
import rocks.gravili.notquests.paper.commands.framework.NQCommandManager;
import rocks.gravili.notquests.paper.commands.framework.NQDescription;
import rocks.gravili.notquests.paper.structs.ActiveObjective;
import rocks.gravili.notquests.paper.structs.QuestPlayer;

public class ShootArrowObjective extends Objective {
    private Location targetLocation;
    private double radius = 1;

    public ShootArrowObjective(final NotQuests main) {
        super(main);
    }

    public static void handleCommands(
            final NotQuests main,
            final NQCommandManager manager,
            final NQCommandBuilder addObjectiveBuilder,
            final int level) {
        manager.command(addObjectiveBuilder
                .required(
                        "amount",
                        numberVariableArgument("amount", null, false),
                        NQDescription.of("Number of arrows the player must land inside the target region."))
                .required("world", NQArguments.worldArgument(), NQDescription.of("World containing the target arrow region."))
                .required("x", NQArguments.doubleArgument(), NQDescription.of("Center X coordinate of the target arrow region."))
                .required("y", NQArguments.doubleArgument(), NQDescription.of("Center Y coordinate of the target arrow region."))
                .required("z", NQArguments.doubleArgument(), NQDescription.of("Center Z coordinate of the target arrow region."))
                .required(
                        "radius",
                        NQArguments.doubleArgument(),
                        NQDescription.of("Radius in blocks around the target center where arrows count."))
                .handler(context -> {
                    final String amountExpression = context.get("amount");
                    final World world = context.get("world");
                    final double x = context.get("x");
                    final double y = context.get("y");
                    final double z = context.get("z");
                    final double radius = context.get("radius");

                    final ShootArrowObjective shootArrowObjective = new ShootArrowObjective(main);
                    shootArrowObjective.setTargetLocation(new Location(world, x, y, z));
                    shootArrowObjective.setRadius(radius);
                    shootArrowObjective.setProgressNeededExpression(amountExpression);

                    main.getObjectiveManager().addObjective(shootArrowObjective, context, level);
                }));
    }

    public boolean countsArrowLocation(final Location arrowLocation) {
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
        configuration.set(initialPath + ".specifics.radius", radius);
    }

    @Override
    public void load(final FileConfiguration configuration, final String initialPath) {
        setTargetLocation(configuration.getLocation(initialPath + ".specifics.targetLocation"));
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
        setLocation(targetLocation, false);
    }

    public double getRadius() {
        return radius;
    }

    public void setRadius(final double radius) {
        this.radius = Math.max(0, radius);
    }
}
