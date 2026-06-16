package dev.openrp.companies.adapter;

import java.util.Optional;
import org.bukkit.OfflinePlayer;

/**
 * Optional bridge to a region/claim system (WorldGuard, GriefPrevention, a custom plugin, ...) used
 * when a company sets its headquarters. The default {@code NoopRegionAdapter} treats headquarters as
 * a plain saved location: it allows any position and reports no region. Swap this adapter to require
 * that a HQ sits inside a region the player controls, or to expose the region's name.
 */
public interface RegionAdapter {

    String id();

    /** Whether {@code player} may set a company headquarters at the given coordinates. */
    boolean canSetHeadquarters(OfflinePlayer player, String world, double x, double y, double z);

    /** Name of the region covering the coordinates, if any (for display/audit). */
    Optional<String> regionNameAt(String world, double x, double y, double z);
}
