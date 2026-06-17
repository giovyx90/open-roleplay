package dev.openrp.jobs.core;

import java.util.Optional;
import dev.openrp.jobs.config.Job;
import dev.openrp.jobs.config.ProgressionLadder;
import dev.openrp.jobs.config.ProgressionTier;
import dev.openrp.jobs.model.WorkRecord;

/**
 * Computes seniority from real sessions over time. Progression is not XP: the tier is derived from the
 * number of completed sessions, optionally eroded by inactivity decay. Pure logic over a record and the
 * configured ladder - fully unit-testable without a running server. Decay is derived from how long a
 * worker has been inactive past the threshold, so it erodes a vanished veteran but is repaid by simply
 * returning to work.
 */
public final class ProgressionService {

    private static final long DAY_MILLIS = 86_400_000L;

    private final ProgressionLadder ladder;

    public ProgressionService(ProgressionLadder ladder) {
        this.ladder = ladder;
    }

    /** Sessions-equivalent removed by inactivity at time {@code now}; 0 when decay is off or recent. */
    public double decayFor(WorkRecord record, long now) {
        if (record == null || !ladder.decayEnabled() || record.lastSessionAt() <= 0L) {
            return 0.0;
        }
        double daysInactive = (now - record.lastSessionAt()) / (double) DAY_MILLIS;
        double over = daysInactive - ladder.inactiveDaysThreshold();
        if (over <= 0) {
            return 0.0;
        }
        return Math.min(record.totalSessions(), over * ladder.decaySessionsPerDay());
    }

    public double effectiveSessions(WorkRecord record, long now) {
        if (record == null) {
            return 0.0;
        }
        return Math.max(0.0, record.totalSessions() - decayFor(record, now));
    }

    public Optional<ProgressionTier> currentTier(WorkRecord record, long now) {
        return ladder.tierFor(effectiveSessions(record, now));
    }

    /** Pay multiplier from the worker's live tier; 1.0 when progression is off for the job or no tier matches. */
    public double payMultiplier(WorkRecord record, Job job, long now) {
        if (record == null || job == null || !job.progressionEnabled()) {
            return 1.0;
        }
        return currentTier(record, now).map(ProgressionTier::payMultiplier).orElse(1.0);
    }

    /**
     * Stores the live tier and decay on the record. Returns {@code true} if the tier id changed - the
     * caller can then notify the worker and re-stamp the licence item.
     */
    public boolean refreshTier(WorkRecord record, long now) {
        if (record == null) {
            return false;
        }
        record.setDecayedSessions(decayFor(record, now));
        String previous = record.currentTier();
        String next = currentTier(record, now).map(ProgressionTier::id).orElse("");
        record.setCurrentTier(next);
        return !next.equals(previous);
    }

    /** The number of sessions still needed to reach the next tier, or empty when already at the top. */
    public Optional<Integer> sessionsToNextTier(WorkRecord record, long now) {
        double effective = effectiveSessions(record, now);
        ProgressionTier next = null;
        for (ProgressionTier tier : ladder.tiers()) {
            if (tier.sessionsRequired() > effective && (next == null || tier.sessionsRequired() < next.sessionsRequired())) {
                next = tier;
            }
        }
        return next == null ? Optional.empty() : Optional.of((int) Math.ceil(next.sessionsRequired() - effective));
    }
}
