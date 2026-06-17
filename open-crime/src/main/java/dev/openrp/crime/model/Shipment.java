package dev.openrp.crime.model;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * A consignment of illegal goods moving along a {@link dev.openrp.crime.config.Route}. The carrier
 * physically transports the marked items; an interception is the authorities physically stopping and
 * searching that carrier - the core never auto-notifies anyone about a shipment.
 */
public final class Shipment {

    private final String id;
    private final String orgId;
    private final String routeId;
    private final UUID carrier;
    private ShipmentStatus status = ShipmentStatus.IN_TRANSIT;
    private final long startedAt;
    private long expectedAt;
    private long deliveredAt;
    private final Map<String, Integer> goods = new LinkedHashMap<>();
    private final Set<UUID> escorts = new LinkedHashSet<>();

    public Shipment(String id, String orgId, String routeId, UUID carrier, long startedAt, long expectedAt) {
        this.id = Objects.requireNonNull(id, "id");
        this.orgId = Objects.requireNonNull(orgId, "orgId");
        this.routeId = routeId == null ? "" : routeId;
        this.carrier = carrier;
        this.startedAt = startedAt;
        this.expectedAt = expectedAt;
    }

    public String id() {
        return id;
    }

    public String orgId() {
        return orgId;
    }

    public String routeId() {
        return routeId;
    }

    public UUID carrier() {
        return carrier;
    }

    public ShipmentStatus status() {
        return status;
    }

    public void setStatus(ShipmentStatus status) {
        this.status = Objects.requireNonNull(status, "status");
    }

    public long startedAt() {
        return startedAt;
    }

    public long expectedAt() {
        return expectedAt;
    }

    public long deliveredAt() {
        return deliveredAt;
    }

    public void setDeliveredAt(long deliveredAt) {
        this.deliveredAt = deliveredAt;
    }

    public Map<String, Integer> goods() {
        return Collections.unmodifiableMap(goods);
    }

    public void addGood(String goodId, int amount) {
        if (goodId == null || goodId.isBlank() || amount <= 0) {
            return;
        }
        goods.merge(goodId, amount, Integer::sum);
    }

    public Set<UUID> escorts() {
        return Collections.unmodifiableSet(escorts);
    }

    public void addEscort(UUID escort) {
        if (escort != null) {
            escorts.add(escort);
        }
    }
}
