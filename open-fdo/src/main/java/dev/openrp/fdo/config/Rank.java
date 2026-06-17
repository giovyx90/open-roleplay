package dev.openrp.fdo.config;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;
import dev.openrp.fdo.capability.Capability;

/**
 * A configured rank within a corps. {@code order} places it in the chain (0 = lowest);
 * {@code capabilities} are the ones granted directly at this rank. Higher ranks inherit the
 * capabilities of lower ones (see {@code CapabilityResolver}). {@code apical} ranks may promote and
 * enrol within their own corps.
 */
public record Rank(String id, String displayName, int order, boolean apical, Set<Capability> capabilities) {

    public Rank {
        capabilities = capabilities == null || capabilities.isEmpty()
                ? Collections.unmodifiableSet(EnumSet.noneOf(Capability.class))
                : Collections.unmodifiableSet(EnumSet.copyOf(capabilities));
    }
}
