package dev.openrp.fdo.adapter.defaults;

import java.util.Set;
import java.util.UUID;
import dev.openrp.fdo.adapter.RegionAdapter;

/**
 * Default region adapter for servers without WorldGuard: every region is unknown and nobody is ever
 * "inside". The evidence-locker and intercept-zone features degrade gracefully - they simply never
 * report containment - while the rest of the plugin is unaffected.
 */
public final class NoopRegionAdapter implements RegionAdapter {

    @Override
    public String id() {
        return "noop";
    }

    @Override
    public boolean isInside(UUID player, String regionId) {
        return false;
    }

    @Override
    public Set<UUID> playersInside(String regionId) {
        return Set.of();
    }

    @Override
    public boolean exists(String regionId) {
        return false;
    }
}
