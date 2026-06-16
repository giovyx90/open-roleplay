package dev.openrp.companies.model;

import java.util.Objects;
import java.util.UUID;

/**
 * A recurring salary a director configured for one member: a fixed amount paid from the company
 * treasury into the member's bank account every {@code intervalSeconds}. It stays fully discretionary -
 * the director chooses the amount and cadence and can change or cancel it at any time from the terminal;
 * the payroll task only automates the repetition. Pure data so it is storage-agnostic and testable.
 */
public final class RecurringPayment {

    private final String companyId;
    private final UUID memberUuid;
    private double amount;
    private long intervalSeconds;
    private long nextDueAt;

    public RecurringPayment(String companyId, UUID memberUuid, double amount, long intervalSeconds, long nextDueAt) {
        this.companyId = Objects.requireNonNull(companyId, "companyId");
        this.memberUuid = Objects.requireNonNull(memberUuid, "memberUuid");
        this.amount = Math.max(0.0, amount);
        this.intervalSeconds = Math.max(1L, intervalSeconds);
        this.nextDueAt = nextDueAt;
    }

    /** Stable storage key for one company/member pairing. */
    public static String key(String companyId, UUID memberUuid) {
        return companyId + ":" + memberUuid;
    }

    public String key() {
        return key(companyId, memberUuid);
    }

    public String companyId() {
        return companyId;
    }

    public UUID memberUuid() {
        return memberUuid;
    }

    public double amount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = Math.max(0.0, amount);
    }

    public long intervalSeconds() {
        return intervalSeconds;
    }

    public void setIntervalSeconds(long intervalSeconds) {
        this.intervalSeconds = Math.max(1L, intervalSeconds);
    }

    public long nextDueAt() {
        return nextDueAt;
    }

    public void setNextDueAt(long nextDueAt) {
        this.nextDueAt = nextDueAt;
    }

    /** Whether this payment is due to run at the given wall-clock time. */
    public boolean isDue(long now) {
        return amount > 0.0 && now >= nextDueAt;
    }

    /** Advances {@link #nextDueAt} by one interval from {@code now}. */
    public void scheduleNext(long now) {
        this.nextDueAt = now + intervalSeconds * 1000L;
    }
}
