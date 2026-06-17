package dev.openrp.crime.config;

import java.util.Set;
import dev.openrp.crime.capability.Capability;

/**
 * One rank in an organisation's configured hierarchy. {@code order} sorts ranks low-to-high; the
 * apical rank ({@code apical = true}) may dissolve the org and manage every member. A rank holding
 * {@link Capability#ALL} passes every capability check.
 *
 * @param id           config key (e.g. {@code soldato})
 * @param displayName  shown to players
 * @param order        rank order; higher means more senior
 * @param apical       whether this is the top rank
 * @param capabilities the capabilities this rank grants
 */
public record OrgRank(String id, String displayName, int order, boolean apical, Set<Capability> capabilities) {

    public OrgRank {
        capabilities = capabilities == null ? Set.of() : Set.copyOf(capabilities);
    }

    /** Whether this rank grants the capability, honouring the {@link Capability#ALL} wildcard. */
    public boolean has(Capability capability) {
        return capabilities.contains(Capability.ALL) || capabilities.contains(capability);
    }
}
