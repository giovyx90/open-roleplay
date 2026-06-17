package dev.openrp.crime.model;

import java.util.Objects;
import java.util.UUID;

/**
 * One running (or finished) production stage at a physical location. The core only times it and
 * records who/where; the worker must be physically present and supply the real ingredients - the
 * plugin never produces anything by itself.
 */
public final class ProductionProcess {

    private final String id;
    private final String orgId;
    private final String recipeId;
    private String stageId;
    private final String locationRegion;
    private final UUID worker;
    private final String world;
    private final int x;
    private final int y;
    private final int z;
    private final long startedAt;
    private long expectedAt;
    private ProductionStatus status = ProductionStatus.ACTIVE;
    private int quality;
    private String eventId;

    public ProductionProcess(String id, String orgId, String recipeId, String stageId,
                             String locationRegion, UUID worker, String world, int x, int y, int z,
                             long startedAt, long expectedAt) {
        this.id = Objects.requireNonNull(id, "id");
        this.orgId = Objects.requireNonNull(orgId, "orgId");
        this.recipeId = recipeId == null ? "" : recipeId;
        this.stageId = stageId == null ? "" : stageId;
        this.locationRegion = locationRegion == null ? "" : locationRegion;
        this.worker = worker;
        this.world = world == null ? "" : world;
        this.x = x;
        this.y = y;
        this.z = z;
        this.startedAt = startedAt;
        this.expectedAt = expectedAt;
    }

    public String id() {
        return id;
    }

    public String orgId() {
        return orgId;
    }

    public String recipeId() {
        return recipeId;
    }

    public String stageId() {
        return stageId;
    }

    public void setStageId(String stageId) {
        this.stageId = stageId == null ? "" : stageId;
    }

    public String locationRegion() {
        return locationRegion;
    }

    public UUID worker() {
        return worker;
    }

    public String world() {
        return world;
    }

    public int x() {
        return x;
    }

    public int y() {
        return y;
    }

    public int z() {
        return z;
    }

    public long startedAt() {
        return startedAt;
    }

    public long expectedAt() {
        return expectedAt;
    }

    public void setExpectedAt(long expectedAt) {
        this.expectedAt = expectedAt;
    }

    public ProductionStatus status() {
        return status;
    }

    public void setStatus(ProductionStatus status) {
        this.status = Objects.requireNonNull(status, "status");
    }

    public int quality() {
        return quality;
    }

    public void setQuality(int quality) {
        this.quality = Math.max(1, Math.min(5, quality));
    }

    /** The crime event this production generated, linked when discovered. */
    public String eventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public boolean isElapsed(long now) {
        return now >= expectedAt;
    }
}
