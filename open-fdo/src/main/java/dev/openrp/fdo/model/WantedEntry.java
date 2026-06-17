package dev.openrp.fdo.model;

import java.util.Objects;
import java.util.UUID;

/**
 * An entry in the wanted register. The {@code level} is an abstract integer; what each level means
 * and how many exist is configured in {@code wanted.yml}, so the core assumes no fixed scale.
 *
 * <p>Not thread-safe on its own; mutated under the wanted lock.</p>
 */
public final class WantedEntry {

    private final UUID subjectUuid;
    private final String subjectName;
    private int level;
    private String reason;
    private final UUID issuedBy;
    private final long issuedAt;
    private boolean active = true;

    public WantedEntry(UUID subjectUuid, String subjectName, int level, String reason, UUID issuedBy, long issuedAt) {
        this.subjectUuid = Objects.requireNonNull(subjectUuid, "subjectUuid");
        this.subjectName = subjectName == null ? "" : subjectName;
        this.level = level;
        this.reason = reason == null ? "" : reason;
        this.issuedBy = issuedBy;
        this.issuedAt = issuedAt;
    }

    public UUID subjectUuid() {
        return subjectUuid;
    }

    public String subjectName() {
        return subjectName;
    }

    public int level() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public String reason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason == null ? "" : reason;
    }

    public UUID issuedBy() {
        return issuedBy;
    }

    public long issuedAt() {
        return issuedAt;
    }

    public boolean active() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
