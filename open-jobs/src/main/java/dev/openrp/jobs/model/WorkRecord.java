package dev.openrp.jobs.model;

import java.util.UUID;

/**
 * The lifetime record of one player in one job - the heart of progression. It does not track XP; it
 * tracks <strong>real sessions over time</strong>. A worker with a hundred completed sessions in three
 * months is experienced because they were present and consistent, not because they ground a number.
 * Tier is derived from {@link #effectiveSessions()} so that decay can erode an absent veteran.
 */
public final class WorkRecord {

    private final UUID player;
    private final String jobId;

    private int totalSessions;
    private long totalProduced;
    private double totalPayout;
    private String currentTier = "";
    private long firstSessionAt;
    private long lastSessionAt;
    /** Sessions equivalent removed by inactivity decay; subtracted from totals to get the live tier. */
    private double decayedSessions;

    public WorkRecord(UUID player, String jobId) {
        this.player = player;
        this.jobId = jobId;
    }

    public UUID player() {
        return player;
    }

    public String jobId() {
        return jobId;
    }

    public int totalSessions() {
        return totalSessions;
    }

    public void setTotalSessions(int totalSessions) {
        this.totalSessions = Math.max(0, totalSessions);
    }

    public long totalProduced() {
        return totalProduced;
    }

    public void setTotalProduced(long totalProduced) {
        this.totalProduced = Math.max(0, totalProduced);
    }

    public void addProduced(long produced) {
        this.totalProduced += Math.max(0, produced);
    }

    public double totalPayout() {
        return totalPayout;
    }

    public void setTotalPayout(double totalPayout) {
        this.totalPayout = Math.max(0.0, totalPayout);
    }

    public void addPayout(double payout) {
        this.totalPayout += Math.max(0.0, payout);
    }

    public String currentTier() {
        return currentTier;
    }

    public void setCurrentTier(String currentTier) {
        this.currentTier = currentTier == null ? "" : currentTier;
    }

    public long firstSessionAt() {
        return firstSessionAt;
    }

    public void setFirstSessionAt(long firstSessionAt) {
        this.firstSessionAt = firstSessionAt;
    }

    public long lastSessionAt() {
        return lastSessionAt;
    }

    public void setLastSessionAt(long lastSessionAt) {
        this.lastSessionAt = lastSessionAt;
    }

    public double decayedSessions() {
        return decayedSessions;
    }

    public void setDecayedSessions(double decayedSessions) {
        this.decayedSessions = Math.max(0.0, decayedSessions);
    }

    /** Completed sessions minus decayed equivalents, floored at zero - the number that drives the tier. */
    public double effectiveSessions() {
        return Math.max(0.0, totalSessions - decayedSessions);
    }

    /** Registers a freshly completed session. */
    public void recordSession(long produced, double payout, long when) {
        this.totalSessions++;
        addProduced(produced);
        addPayout(payout);
        if (firstSessionAt == 0L) {
            firstSessionAt = when;
        }
        this.lastSessionAt = when;
    }
}
