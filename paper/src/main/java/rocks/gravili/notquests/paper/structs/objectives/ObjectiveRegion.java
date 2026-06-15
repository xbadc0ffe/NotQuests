package rocks.gravili.notquests.paper.structs.objectives;

import java.util.Objects;
import org.bukkit.Location;
import org.bukkit.World;

public record ObjectiveRegion(Location min, Location max) {
    public ObjectiveRegion {
        Objects.requireNonNull(min, "min");
        Objects.requireNonNull(max, "max");

        final World world = min.getWorld();
        final double minX = Math.min(min.getX(), max.getX());
        final double minY = Math.min(min.getY(), max.getY());
        final double minZ = Math.min(min.getZ(), max.getZ());
        final double maxX = Math.max(min.getX(), max.getX());
        final double maxY = Math.max(min.getY(), max.getY());
        final double maxZ = Math.max(min.getZ(), max.getZ());

        min = new Location(world, minX, minY, minZ);
        max = new Location(world, maxX, maxY, maxZ);
    }

    public static ObjectiveRegion around(final Location center, final double radius) {
        final double safeRadius = Math.max(0, radius);
        return new ObjectiveRegion(
                center.clone().subtract(safeRadius, safeRadius, safeRadius),
                center.clone().add(safeRadius, safeRadius, safeRadius));
    }

    public Location center() {
        return new Location(
                min.getWorld(),
                (min.getX() + max.getX()) / 2.0,
                (min.getY() + max.getY()) / 2.0,
                (min.getZ() + max.getZ()) / 2.0);
    }

    public double enclosingRadius() {
        return center().distance(max);
    }

    public boolean contains(final Location location) {
        if (location == null || location.getWorld() == null || min.getWorld() == null) {
            return false;
        }
        if (!location.getWorld().equals(min.getWorld())) {
            return false;
        }
        return location.getX() >= min.getX()
                && location.getX() <= max.getX()
                && location.getY() >= min.getY()
                && location.getY() <= max.getY()
                && location.getZ() >= min.getZ()
                && location.getZ() <= max.getZ();
    }
}
