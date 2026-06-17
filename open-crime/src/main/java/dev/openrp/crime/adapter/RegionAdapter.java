package dev.openrp.crime.adapter;

import java.util.Optional;
import org.bukkit.Location;

/**
 * Abstracts "which region am I in" and "does this region carry this tag" so the core never assumes
 * WorldGuard. A WorldGuard bridge registers a real implementation; the bundled default derives a
 * synthetic per-chunk region id and treats every tag as satisfied, so the plugin stays usable on a
 * server with no region plugin (when {@code territory.require_worldguard} is off).
 */
public interface RegionAdapter {

    String id();

    /** Whether a real region backend (e.g. WorldGuard) is present and authoritative. */
    boolean available();

    /** The primary region id at the location, or empty when the location has none. */
    Optional<String> regionAt(Location location);

    /** Whether the region carries the given tag/flag (e.g. {@code production_lab}). */
    boolean hasTag(String regionId, String tag);
}
