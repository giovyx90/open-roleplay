package dev.openrp.crime.model;

import java.util.Objects;

/**
 * A running money-laundering process. It converts a dirty amount into a clean amount (minus the
 * method's loss) over real time. The clean amount is computed and credited on completion; an audit
 * can only catch it while it is still {@link LaunderingStatus#ACTIVE}.
 */
public final class LaunderingProcess {

    private final String id;
    private final String orgId;
    private final String methodId;
    private final long amountDirty;
    private long amountClean;
    private LaunderingStatus status = LaunderingStatus.ACTIVE;
    private final long startedAt;
    private long expectedAt;
    private long completedAt;

    public LaunderingProcess(String id, String orgId, String methodId, long amountDirty,
                             long startedAt, long expectedAt) {
        this.id = Objects.requireNonNull(id, "id");
        this.orgId = Objects.requireNonNull(orgId, "orgId");
        this.methodId = methodId == null ? "" : methodId;
        this.amountDirty = Math.max(0L, amountDirty);
        this.startedAt = startedAt;
        this.expectedAt = expectedAt;
    }

    public String id() {
        return id;
    }

    public String orgId() {
        return orgId;
    }

    public String methodId() {
        return methodId;
    }

    public long amountDirty() {
        return amountDirty;
    }

    public long amountClean() {
        return amountClean;
    }

    public void setAmountClean(long amountClean) {
        this.amountClean = Math.max(0L, amountClean);
    }

    public LaunderingStatus status() {
        return status;
    }

    public void setStatus(LaunderingStatus status) {
        this.status = Objects.requireNonNull(status, "status");
    }

    public long startedAt() {
        return startedAt;
    }

    public long expectedAt() {
        return expectedAt;
    }

    public long completedAt() {
        return completedAt;
    }

    public void setCompletedAt(long completedAt) {
        this.completedAt = completedAt;
    }

    public boolean isElapsed(long now) {
        return now >= expectedAt;
    }
}
