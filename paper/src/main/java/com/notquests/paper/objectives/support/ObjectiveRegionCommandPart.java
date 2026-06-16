package com.notquests.paper.objectives.support;

import org.bukkit.Location;
import org.bukkit.World;
import com.notquests.paper.commands.framework.NQArguments;
import com.notquests.paper.commands.framework.NQCommandBuilder;
import com.notquests.paper.commands.framework.NQCommandContext;
import com.notquests.paper.commands.framework.NQDescription;

public final class ObjectiveRegionCommandPart {
    public static final String WORLD_EDIT_SELECTION = "worldeditselection";

    private ObjectiveRegionCommandPart() {}

    public static NQCommandBuilder centerRadius(
            final NQCommandBuilder builder,
            final String targetDescription,
            final String radiusDescription) {
        return builder
                .required(
                        "world",
                        NQArguments.worldArgument(),
                        NQDescription.of("World containing the center of the " + targetDescription + "."))
                .required(
                        "x",
                        NQArguments.doubleArgument(),
                        NQDescription.of("Center X coordinate of the " + targetDescription + "."))
                .required(
                        "y",
                        NQArguments.doubleArgument(),
                        NQDescription.of("Center Y coordinate of the " + targetDescription + "."))
                .required(
                        "z",
                        NQArguments.doubleArgument(),
                        NQDescription.of("Center Z coordinate of the " + targetDescription + "."))
                .required("radius", NQArguments.doubleArgument(), NQDescription.of(radiusDescription));
    }

    public static CenterRadius centerRadius(final NQCommandContext context) {
        final World world = context.get("world");
        final double x = context.get("x");
        final double y = context.get("y");
        final double z = context.get("z");
        final double radius = context.get("radius");
        return new CenterRadius(new Location(world, x, y, z), Math.max(0, radius));
    }

    public record CenterRadius(Location center, double radius) {
        public ObjectiveRegion asRegion() {
            return ObjectiveRegion.around(center, radius);
        }
    }
}
