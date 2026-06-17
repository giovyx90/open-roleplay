package dev.openrp.crime.model;

import java.util.Objects;

/**
 * A protection / extortion contract over a company. The core records the contract and times the
 * periodic payment; escalation level 3 carries no automatic effect - it is a narrative signal that
 * "from here on it is unassisted RP". The plugin never destroys a company or punishes anyone.
 */
public final class Protection {

    private final String id;
    private final String orgId;
    private final String companyId;
    private long amount;
    private int periodDays;
    private ProtectionStatus status = ProtectionStatus.PENDING;
    private int coercionLevel;
    private long lastPayment;
    private long nextDue;

    public Protection(String id, String orgId, String companyId, long amount, int periodDays) {
        this.id = Objects.requireNonNull(id, "id");
        this.orgId = Objects.requireNonNull(orgId, "orgId");
        this.companyId = Objects.requireNonNull(companyId, "companyId");
        this.amount = Math.max(0L, amount);
        this.periodDays = Math.max(1, periodDays);
    }

    public String id() {
        return id;
    }

    public String orgId() {
        return orgId;
    }

    public String companyId() {
        return companyId;
    }

    public long amount() {
        return amount;
    }

    public void setAmount(long amount) {
        this.amount = Math.max(0L, amount);
    }

    public int periodDays() {
        return periodDays;
    }

    public void setPeriodDays(int periodDays) {
        this.periodDays = Math.max(1, periodDays);
    }

    public ProtectionStatus status() {
        return status;
    }

    public void setStatus(ProtectionStatus status) {
        this.status = Objects.requireNonNull(status, "status");
    }

    public int coercionLevel() {
        return coercionLevel;
    }

    public void setCoercionLevel(int coercionLevel) {
        this.coercionLevel = Math.max(0, Math.min(3, coercionLevel));
    }

    public long lastPayment() {
        return lastPayment;
    }

    public void setLastPayment(long lastPayment) {
        this.lastPayment = lastPayment;
    }

    public long nextDue() {
        return nextDue;
    }

    public void setNextDue(long nextDue) {
        this.nextDue = nextDue;
    }

    public boolean isDue(long now) {
        return status == ProtectionStatus.ACTIVE && nextDue > 0L && now >= nextDue;
    }
}
