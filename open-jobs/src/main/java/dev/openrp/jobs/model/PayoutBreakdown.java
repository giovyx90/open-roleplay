package dev.openrp.jobs.model;

/**
 * The fully itemised result of a payout computation: the base pay before modifiers, then every
 * multiplier that applied (progression seniority, cooperative work, tools, shift, season) and the
 * final total. Pure data so the payment logic is unit-testable without a running server, and so the
 * end-of-session summary can show the worker exactly how the figure was reached.
 */
public final class PayoutBreakdown {

    private final double base;
    private final double progressionMultiplier;
    private final double cooperativeMultiplier;
    private final double toolMultiplier;
    private final double shiftMultiplier;
    private final double seasonalMultiplier;

    public PayoutBreakdown(double base, double progressionMultiplier, double cooperativeMultiplier,
                           double toolMultiplier, double shiftMultiplier, double seasonalMultiplier) {
        this.base = Math.max(0.0, base);
        this.progressionMultiplier = positive(progressionMultiplier);
        this.cooperativeMultiplier = positive(cooperativeMultiplier);
        this.toolMultiplier = positive(toolMultiplier);
        this.shiftMultiplier = positive(shiftMultiplier);
        this.seasonalMultiplier = positive(seasonalMultiplier);
    }

    public static PayoutBreakdown none() {
        return new PayoutBreakdown(0.0, 1.0, 1.0, 1.0, 1.0, 1.0);
    }

    private static double positive(double value) {
        return value <= 0.0 ? 1.0 : value;
    }

    public double base() {
        return base;
    }

    public double progressionMultiplier() {
        return progressionMultiplier;
    }

    public double cooperativeMultiplier() {
        return cooperativeMultiplier;
    }

    public double toolMultiplier() {
        return toolMultiplier;
    }

    public double shiftMultiplier() {
        return shiftMultiplier;
    }

    public double seasonalMultiplier() {
        return seasonalMultiplier;
    }

    /** The combined multiplier applied on top of the base pay. */
    public double totalMultiplier() {
        return progressionMultiplier * cooperativeMultiplier * toolMultiplier * shiftMultiplier * seasonalMultiplier;
    }

    public double total() {
        return base * totalMultiplier();
    }

    /** The portion of the total attributable to seniority alone (for the payout audit log). */
    public double progressionBonus() {
        return base * (progressionMultiplier - 1.0);
    }

    public double cooperativeBonus() {
        return base * (cooperativeMultiplier - 1.0);
    }
}
