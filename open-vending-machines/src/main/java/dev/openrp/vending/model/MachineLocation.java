package dev.openrp.vending.model;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

/**
 * Block-granular, world-decoupled position of a machine.
 *
 * <p>Stored as a world name plus block coordinates so persistence never needs a live {@link World}
 * reference. Conversion to and from Bukkit happens only at the edges (listeners, GUIs).</p>
 */
public record MachineLocation(String world, int x, int y, int z) {

    public MachineLocation {
        world = world == null || world.isBlank() ? "world" : world;
    }

    public static MachineLocation of(Location location) {
        return new MachineLocation(
                location.getWorld() == null ? "world" : location.getWorld().getName(),
                location.getBlockX(),
                location.getBlockY(),
                location.getBlockZ());
    }

    /** Centre of the block as a Bukkit {@link Location}, or {@code null} if the world is unloaded. */
    public Location toBukkitCenter() {
        World bukkitWorld = Bukkit.getWorld(world);
        return bukkitWorld == null ? null : new Location(bukkitWorld, x + 0.5, y + 0.5, z + 0.5);
    }

    /** True when the supplied Bukkit location refers to the same world and block. */
    public boolean matchesBlock(Location location) {
        return location != null
                && location.getWorld() != null
                && world.equals(location.getWorld().getName())
                && x == location.getBlockX()
                && y == location.getBlockY()
                && z == location.getBlockZ();
    }

    /**
     * Squared distance from the block centre to a Bukkit location, or {@link Double#MAX_VALUE} when
     * the locations are in different (or unloaded) worlds. Squared to avoid a needless sqrt on the
     * purchase hot-path.
     */
    public double distanceSquaredTo(Location location) {
        if (location == null || location.getWorld() == null || !world.equals(location.getWorld().getName())) {
            return Double.MAX_VALUE;
        }
        double dx = (x + 0.5) - location.getX();
        double dy = (y + 0.5) - location.getY();
        double dz = (z + 0.5) - location.getZ();
        return (dx * dx) + (dy * dy) + (dz * dz);
    }

    /** Stable key for maps and storage, e.g. {@code world:10:64:-20}. */
    public String toKey() {
        return world + ":" + x + ":" + y + ":" + z;
    }
}
