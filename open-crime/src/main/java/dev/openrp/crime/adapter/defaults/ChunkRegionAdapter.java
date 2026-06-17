package dev.openrp.crime.adapter.defaults;

import java.util.Optional;
import org.bukkit.Location;
import dev.openrp.crime.adapter.RegionAdapter;

/**
 * Default region adapter for servers with no region plugin. It derives a stable synthetic region id
 * from the chunk a location sits in ({@code world@chunkX,chunkZ}) and treats every tag as satisfied,
 * so territory and production are usable out of the box. It reports {@link #available()} as
 * {@code false}: features gated behind {@code territory.require_worldguard} stay disabled until a
 * real {@code RegionAdapter} (e.g. a WorldGuard bridge) is registered.
 */
public final class ChunkRegionAdapter implements RegionAdapter {

    @Override
    public String id() {
        return "chunk";
    }

    @Override
    public boolean available() {
        return false;
    }

    @Override
    public Optional<String> regionAt(Location location) {
        if (location == null || location.getWorld() == null) {
            return Optional.empty();
        }
        int chunkX = location.getBlockX() >> 4;
        int chunkZ = location.getBlockZ() >> 4;
        return Optional.of(location.getWorld().getName() + "@" + chunkX + "," + chunkZ);
    }

    @Override
    public boolean hasTag(String regionId, String tag) {
        // No real region backend: every place qualifies so the plugin stays usable without WorldGuard.
        return true;
    }
}
