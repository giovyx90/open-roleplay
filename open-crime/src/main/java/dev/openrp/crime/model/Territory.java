package dev.openrp.crime.model;

import java.util.Objects;

/**
 * A controlled region. The core records who controls it and whether it is contested; it never
 * arbitrates a territory war - the players resolve that physically in RP.
 */
public final class Territory {

    private final String regionId;
    private String orgId;
    private boolean contested;
    private long controlSince;

    public Territory(String regionId, String orgId, boolean contested, long controlSince) {
        this.regionId = Objects.requireNonNull(regionId, "regionId");
        this.orgId = orgId;
        this.contested = contested;
        this.controlSince = controlSince;
    }

    public String regionId() {
        return regionId;
    }

    /** The controlling org id, or {@code null} when nobody controls the region. */
    public String orgId() {
        return orgId;
    }

    public void setOrgId(String orgId) {
        this.orgId = orgId;
    }

    public boolean contested() {
        return contested;
    }

    public void setContested(boolean contested) {
        this.contested = contested;
    }

    public long controlSince() {
        return controlSince;
    }

    public void setControlSince(long controlSince) {
        this.controlSince = controlSince;
    }
}
