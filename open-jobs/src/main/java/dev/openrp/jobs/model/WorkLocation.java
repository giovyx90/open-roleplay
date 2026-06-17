package dev.openrp.jobs.model;

/**
 * A place where a job physically happens, bound to a region id (resolved through the region adapter).
 * There can be one or many per job. {@code capacity} caps simultaneous sessions ({@code 0} = unlimited),
 * which creates natural competition for the richest spots.
 */
public final class WorkLocation {

    private final String id;
    private final String jobId;
    private final String displayName;
    private final String regionId;
    private final int capacity;
    private final boolean seasonal;

    private boolean active = true;

    public WorkLocation(String id, String jobId, String displayName, String regionId, int capacity, boolean seasonal) {
        this.id = id;
        this.jobId = jobId;
        this.displayName = displayName == null || displayName.isBlank() ? id : displayName;
        this.regionId = regionId;
        this.capacity = Math.max(0, capacity);
        this.seasonal = seasonal;
    }

    public String id() {
        return id;
    }

    public String jobId() {
        return jobId;
    }

    public String displayName() {
        return displayName;
    }

    public String regionId() {
        return regionId;
    }

    public int capacity() {
        return capacity;
    }

    public boolean unlimited() {
        return capacity <= 0;
    }

    public boolean seasonal() {
        return seasonal;
    }

    public boolean active() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
