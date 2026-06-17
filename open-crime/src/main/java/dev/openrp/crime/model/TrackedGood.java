package dev.openrp.crime.model;

import java.util.Objects;
import java.util.UUID;

/**
 * Tracking record for one illegal good item, keyed by the per-item UUID written into the item's
 * persistent data. It lets the core follow an item from production through transit to seizure or
 * sale without trusting the item stack itself.
 */
public final class TrackedGood {

    private final String itemUuid;
    private final String goodId;
    private final UUID producer;
    private final String orgId;
    private final long producedAt;
    private TrackedGoodStatus status = TrackedGoodStatus.FREE;
    private int quality;

    public TrackedGood(String itemUuid, String goodId, UUID producer, String orgId, long producedAt) {
        this.itemUuid = Objects.requireNonNull(itemUuid, "itemUuid");
        this.goodId = goodId == null ? "" : goodId;
        this.producer = producer;
        this.orgId = orgId;
        this.producedAt = producedAt;
    }

    public String itemUuid() {
        return itemUuid;
    }

    public String goodId() {
        return goodId;
    }

    public UUID producer() {
        return producer;
    }

    public String orgId() {
        return orgId;
    }

    public long producedAt() {
        return producedAt;
    }

    public TrackedGoodStatus status() {
        return status;
    }

    public void setStatus(TrackedGoodStatus status) {
        this.status = Objects.requireNonNull(status, "status");
    }

    public int quality() {
        return quality;
    }

    public void setQuality(int quality) {
        this.quality = Math.max(1, Math.min(5, quality));
    }
}
