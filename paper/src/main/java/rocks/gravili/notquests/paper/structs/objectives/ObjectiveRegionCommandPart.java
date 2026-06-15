package rocks.gravili.notquests.paper.structs.objectives;

import org.bukkit.Location;
import org.bukkit.World;
import rocks.gravili.notquests.paper.commands.framework.NQArguments;
import rocks.gravili.notquests.paper.commands.framework.NQCommandBuilder;
import rocks.gravili.notquests.paper.commands.framework.NQCommandContext;
import rocks.gravili.notquests.paper.commands.framework.NQDescription;

final class ObjectiveRegionCommandPart {
    static final String WORLD_EDIT_SELECTION = "worldeditselection";

    private ObjectiveRegionCommandPart() {}

    static NQCommandBuilder centerRadius(
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

    static CenterRadius centerRadius(final NQCommandContext context) {
        final World world = context.get("world");
        final double x = context.get("x");
        final double y = context.get("y");
        final double z = context.get("z");
        final double radius = context.get("radius");
        return new CenterRadius(new Location(world, x, y, z), Math.max(0, radius));
    }

    record CenterRadius(Location center, double radius) {
        ObjectiveRegion asRegion() {
            return ObjectiveRegion.around(center, radius);
        }
    }
}
