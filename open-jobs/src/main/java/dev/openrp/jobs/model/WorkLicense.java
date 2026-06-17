package dev.openrp.jobs.model;

import java.util.UUID;

/**
 * A professional licence: not bureaucracy but an identity - the document that recognises a player as a
 * practitioner of a trade. With an Identity adapter it is also a physical NBT item; without one it is
 * purely narrative. The record in the database is authoritative: a revoked licence is useless even if
 * the item is still in the inventory, and a lost item can be reissued because the licence still stands.
 */
public final class WorkLicense {

    private final String id;
    private final UUID player;
    private final String jobId;
    private final long issuedAt;
    private final String issuedBy;

    private LicenseStatus status = LicenseStatus.ACTIVE;
    private String itemUuid;

    public WorkLicense(String id, UUID player, String jobId, long issuedAt, String issuedBy) {
        this.id = id;
        this.player = player;
        this.jobId = jobId;
        this.issuedAt = issuedAt;
        this.issuedBy = issuedBy == null ? "system" : issuedBy;
    }

    public String id() {
        return id;
    }

    public UUID player() {
        return player;
    }

    public String jobId() {
        return jobId;
    }

    public long issuedAt() {
        return issuedAt;
    }

    public String issuedBy() {
        return issuedBy;
    }

    public LicenseStatus status() {
        return status;
    }

    public void setStatus(LicenseStatus status) {
        this.status = status == null ? LicenseStatus.ACTIVE : status;
    }

    public boolean isActive() {
        return status == LicenseStatus.ACTIVE;
    }

    public String itemUuid() {
        return itemUuid;
    }

    public void setItemUuid(String itemUuid) {
        this.itemUuid = itemUuid;
    }
}
