package dev.openrp.crime.config;

/**
 * A configured way to launder money. The plugin provides the framework; the server's config decides
 * which methods exist. A method may declare {@code requiresAdapter} (e.g. {@code COMPANIES}); when
 * that adapter is absent the method is hidden, exactly like an FDO act whose adapter is missing.
 *
 * @param id             config key
 * @param displayName    shown to players
 * @param requiresAdapter optional adapter id the method needs, or empty for none
 * @param lossPercentage percentage of the dirty amount lost in the wash (0..100)
 * @param maxPerDay      per-day cap on the dirty amount
 * @param detectionRisk  percentage chance to leave an auditable trail (narrative)
 * @param durationHours  real hours the process runs (pre time-scale)
 */
public record LaunderingMethod(String id, String displayName, String requiresAdapter,
                               int lossPercentage, long maxPerDay, int detectionRisk, int durationHours) {

    public LaunderingMethod {
        displayName = displayName == null ? id : displayName;
        requiresAdapter = requiresAdapter == null ? "" : requiresAdapter;
        lossPercentage = Math.max(0, Math.min(100, lossPercentage));
        maxPerDay = Math.max(0L, maxPerDay);
        detectionRisk = Math.max(0, Math.min(100, detectionRisk));
        durationHours = Math.max(0, durationHours);
    }

    /** Clean amount yielded for a dirty input, applying the loss. */
    public long cleanFrom(long dirty) {
        long kept = Math.max(0L, dirty) * (100L - lossPercentage) / 100L;
        return Math.max(0L, kept);
    }
}
