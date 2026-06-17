package dev.openrp.jobs.config;

/**
 * One seniority tier: a threshold of completed sessions that unlocks a pay multiplier. Tiers are not
 * XP - they reward presence and consistency over time. The tier is per job: a master woodcutter is a
 * novice miner if they have never mined.
 */
public record ProgressionTier(String id, int order, int sessionsRequired, double payMultiplier, String displayName) {

    public ProgressionTier {
        sessionsRequired = Math.max(0, sessionsRequired);
        payMultiplier = payMultiplier <= 0 ? 1.0 : payMultiplier;
        displayName = displayName == null || displayName.isBlank() ? id : displayName;
    }
}
