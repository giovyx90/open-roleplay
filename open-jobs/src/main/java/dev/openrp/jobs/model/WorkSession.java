package dev.openrp.jobs.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * A single work session: the fundamental unit of tracking. Everything the worker physically does
 * between {@code /lavoro inizia} and {@code /lavoro fine} is recorded here. Time spent merely standing
 * in the region is not work - only valid activity counts. A session that loses its region is
 * {@link SessionStatus#PAUSED} (clock stopped) and resumes on return; staying out too long ends it as
 * {@link SessionStatus#ABANDONED}.
 */
public final class WorkSession {

    private final String id;
    private final UUID player;
    private final String jobId;
    private final String locationId;
    private final long startedAt;

    private SessionStatus status = SessionStatus.ACTIVE;
    private long endedAt;
    private String progressionTier = "";
    private double payout;
    /** Pay accrued from completed transformations (workshop jobs), summed across the session. */
    private double transformationEarnings;

    /** Wall-clock millis actually spent ACTIVE (pauses excluded), banked at each pause. */
    private long activeMillisBanked;
    /** When the current ACTIVE stretch started; 0 while paused. */
    private long resumedAt;
    /** When the session left its region; 0 while inside. Drives abandon detection. */
    private long leftRegionAt;

    private final Map<String, Integer> producedByMaterial = new LinkedHashMap<>();
    private final List<ActivityEntry> log = new ArrayList<>();
    /** Active-minute indices in which at least one valid action happened (drives the a_sessione floor). */
    private final java.util.Set<Long> activeMinutes = new java.util.HashSet<>();
    /** Last time a transformation completed, keyed by transformation index (for craft_time gating). */
    private final Map<Integer, Long> lastTransformAt = new java.util.HashMap<>();

    public WorkSession(String id, UUID player, String jobId, String locationId, long startedAt) {
        this.id = id;
        this.player = player;
        this.jobId = jobId;
        this.locationId = locationId;
        this.startedAt = startedAt;
        this.resumedAt = startedAt;
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

    public String locationId() {
        return locationId;
    }

    public long startedAt() {
        return startedAt;
    }

    public SessionStatus status() {
        return status;
    }

    public void setStatus(SessionStatus status) {
        this.status = status;
    }

    public boolean isActive() {
        return status == SessionStatus.ACTIVE;
    }

    public long endedAt() {
        return endedAt;
    }

    public void setEndedAt(long endedAt) {
        this.endedAt = endedAt;
    }

    public String progressionTier() {
        return progressionTier;
    }

    public void setProgressionTier(String progressionTier) {
        this.progressionTier = progressionTier == null ? "" : progressionTier;
    }

    public double payout() {
        return payout;
    }

    public void setPayout(double payout) {
        this.payout = payout;
    }

    public long leftRegionAt() {
        return leftRegionAt;
    }

    public void setLeftRegionAt(long leftRegionAt) {
        this.leftRegionAt = leftRegionAt;
    }

    // --- active-time bookkeeping -------------------------------------------------------------

    public long activeMillisBanked() {
        return activeMillisBanked;
    }

    public void setActiveMillisBanked(long activeMillisBanked) {
        this.activeMillisBanked = Math.max(0, activeMillisBanked);
    }

    /** Total ACTIVE millis as of {@code now}, including the open stretch if currently active. */
    public long activeMillis(long now) {
        long total = activeMillisBanked;
        if (status == SessionStatus.ACTIVE && resumedAt > 0) {
            total += Math.max(0, now - resumedAt);
        }
        return total;
    }

    /** Banks the open ACTIVE stretch and stops the clock (transition to paused/ended). */
    public void bankActiveTime(long now) {
        if (resumedAt > 0) {
            activeMillisBanked += Math.max(0, now - resumedAt);
        }
        resumedAt = 0;
    }

    /** Restarts the ACTIVE clock (transition back to active). */
    public void resumeClock(long now) {
        this.resumedAt = now;
    }

    // --- production ---------------------------------------------------------------------------

    /** Records a valid action: aggregates the produced count and stores the granular entry. */
    public void record(ActivityEntry entry, long now) {
        if (entry.material() != null) {
            producedByMaterial.merge(entry.material(), entry.quantity(), Integer::sum);
        }
        log.add(entry);
        activeMinutes.add(activeMillis(now) / 60_000L);
    }

    public int produced(String material) {
        return producedByMaterial.getOrDefault(material, 0);
    }

    public int totalProduced() {
        int total = 0;
        for (int amount : producedByMaterial.values()) {
            total += amount;
        }
        return total;
    }

    public Map<String, Integer> producedByMaterial() {
        return Collections.unmodifiableMap(producedByMaterial);
    }

    public void putProduced(String material, int amount) {
        if (material != null && amount > 0) {
            producedByMaterial.put(material, amount);
        }
    }

    public List<ActivityEntry> log() {
        return Collections.unmodifiableList(log);
    }

    /** Active minutes with at least one action over total elapsed active minutes (0..1). */
    public double activeRatio(long now) {
        long minutes = Math.max(1, activeMillis(now) / 60_000L);
        return Math.min(1.0, activeMinutes.size() / (double) minutes);
    }

    public long lastTransformAt(int index) {
        return lastTransformAt.getOrDefault(index, 0L);
    }

    public void markTransform(int index, long now) {
        lastTransformAt.put(index, now);
    }

    public double transformationEarnings() {
        return transformationEarnings;
    }

    public void addTransformationEarnings(double amount) {
        if (amount > 0) {
            this.transformationEarnings += amount;
        }
    }
}
