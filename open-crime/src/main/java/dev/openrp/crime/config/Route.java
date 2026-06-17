package dev.openrp.crime.config;

import java.util.List;

/**
 * A traffic route: an origin/destination region pair with optional waypoints. Not a hardcoded path -
 * the carrier physically travels it. {@code distanceKm} is narrative; {@code durationMinutes} drives
 * the expected-arrival estimate.
 *
 * @param id                config key
 * @param name              shown to players
 * @param originRegion      region the shipment must start in
 * @param destinationRegion region the shipment must be delivered in
 * @param waypoints         optional intermediate region ids
 * @param distanceKm        narrative distance
 * @param durationMinutes   expected travel time (pre time-scale)
 */
public record Route(String id, String name, String originRegion, String destinationRegion,
                    List<String> waypoints, double distanceKm, int durationMinutes) {

    public Route {
        name = name == null ? id : name;
        originRegion = originRegion == null ? "" : originRegion;
        destinationRegion = destinationRegion == null ? "" : destinationRegion;
        waypoints = waypoints == null ? List.of() : List.copyOf(waypoints);
        durationMinutes = Math.max(0, durationMinutes);
    }
}
