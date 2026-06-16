package dev.openrp.companies.adapter.defaults;

import java.util.Optional;
import org.bukkit.OfflinePlayer;
import dev.openrp.companies.adapter.RegionAdapter;

/**
 * Default region adapter: there is no region system, so a headquarters is just a saved location. Any
 * position is allowed and no region name is reported. Swap this for a WorldGuard-backed adapter to
 * require HQs to sit inside a controlled region (see the README).
 */
public final class NoopRegionAdapter implements RegionAdapter {

    @Override
    public String id() {
        return "noop";
    }

    @Override
    public boolean canSetHeadquarters(OfflinePlayer player, String world, double x, double y, double z) {
        return true;
    }

    @Override
    public Optional<String> regionNameAt(String world, double x, double y, double z) {
        return Optional.empty();
    }
}
