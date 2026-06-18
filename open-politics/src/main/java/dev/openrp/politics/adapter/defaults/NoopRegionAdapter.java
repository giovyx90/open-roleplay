package dev.openrp.politics.adapter.defaults;

import java.util.Optional;
import java.util.UUID;
import dev.openrp.politics.adapter.RegionAdapter;

/** No-op region adapter: reports unavailable; a conquest charge stays vacant until a backend registers. */
public final class NoopRegionAdapter implements RegionAdapter {

    @Override
    public String id() {
        return "none";
    }

    @Override
    public boolean available() {
        return false;
    }

    @Override
    public boolean regionExists(String regionId) {
        return false;
    }

    @Override
    public Optional<UUID> controller(String regionId) {
        return Optional.empty();
    }
}
