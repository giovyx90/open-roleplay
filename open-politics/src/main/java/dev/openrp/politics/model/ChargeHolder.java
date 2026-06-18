package dev.openrp.politics.model;

import java.util.Objects;
import java.util.UUID;

/**
 * The occupant of a charge for a span of time. The core knows who holds which charge, in which
 * government, since when and until when (null/0 for an unlimited mandate), who assigned them and an
 * optional designated successor for a hereditary charge. It carries no narrative weight - what the
 * charge <em>does</em> is RP. Bukkit-free, mutated under the manager lock.
 */
public final class ChargeHolder {

    private final String id;
    private final UUID playerUuid;
    private final String chargeId;
    private final String governmentId;
    private final long assignedAt;
    private long expiresAt;          // 0 = unlimited mandate
    private final String assignedBy; // a UUID string, or "system" for election/conquest
    private HolderStatus status = HolderStatus.ACTIVE;
    private UUID successor;          // designated heir for a hereditary charge, or null

    public ChargeHolder(String id, UUID playerUuid, String chargeId, String governmentId,
                        long assignedAt, long expiresAt, String assignedBy) {
        this.id = Objects.requireNonNull(id, "id");
        this.playerUuid = Objects.requireNonNull(playerUuid, "playerUuid");
        this.chargeId = Objects.requireNonNull(chargeId, "chargeId");
        this.governmentId = governmentId == null ? "" : governmentId;
        this.assignedAt = assignedAt;
        this.expiresAt = Math.max(0, expiresAt);
        this.assignedBy = assignedBy == null ? "system" : assignedBy;
    }

    public String id() {
        return id;
    }

    public UUID playerUuid() {
        return playerUuid;
    }

    public String chargeId() {
        return chargeId;
    }

    public String governmentId() {
        return governmentId;
    }

    public long assignedAt() {
        return assignedAt;
    }

    public long expiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(long expiresAt) {
        this.expiresAt = Math.max(0, expiresAt);
    }

    public boolean hasTerm() {
        return expiresAt > 0;
    }

    public boolean isExpired(long now) {
        return expiresAt > 0 && expiresAt <= now;
    }

    public String assignedBy() {
        return assignedBy;
    }

    public HolderStatus status() {
        return status;
    }

    public void setStatus(HolderStatus status) {
        this.status = Objects.requireNonNull(status, "status");
    }

    public boolean isActive() {
        return status == HolderStatus.ACTIVE;
    }

    public UUID successor() {
        return successor;
    }

    public void setSuccessor(UUID successor) {
        this.successor = successor;
    }
}
