package dev.openrp.crime.config;

/**
 * A kind of production location. Bound to a region tag (resolved by the {@code RegionAdapter}), with
 * a discovery radius the authorities can stumble into and an optional item that must be present in
 * the region for it to count.
 *
 * @param id             config key (e.g. {@code lab})
 * @param displayName    shown to players
 * @param regionTag      region tag the location must carry
 * @param maxConcurrent  active processes allowed at once in one region
 * @param discoverable   whether the authorities can physically discover it
 * @param discoveryRadius blocks within which a nearby agent discovers it
 * @param requiresItem   Bukkit material that must be present, or empty for none
 */
public record LocationType(String id, String displayName, String regionTag, int maxConcurrent,
                           boolean discoverable, int discoveryRadius, String requiresItem) {

    public LocationType {
        regionTag = regionTag == null ? "" : regionTag;
        requiresItem = requiresItem == null ? "" : requiresItem;
        maxConcurrent = Math.max(1, maxConcurrent);
        discoveryRadius = Math.max(0, discoveryRadius);
    }
}
