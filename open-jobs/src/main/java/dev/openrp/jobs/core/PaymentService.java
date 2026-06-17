package dev.openrp.jobs.core;

import java.time.LocalTime;
import java.util.Map;
import dev.openrp.jobs.config.Job;
import dev.openrp.jobs.config.PaymentSpec;
import dev.openrp.jobs.model.PaymentModel;
import dev.openrp.jobs.model.PayoutBreakdown;
import dev.openrp.jobs.model.Season;
import dev.openrp.jobs.model.WorkSession;

/**
 * Computes a session's payout, fully itemised. RP First: it pays the work actually done, never the time
 * merely spent. Production pays per produced unit; delivery pays per unit on arrival at the delivery
 * point; session pay multiplies effective active time by an hourly rate, with a malus below the
 * activity floor. On top of the base it layers the seniority, cooperative, tool, shift and seasonal
 * multipliers. Pure logic over the session, the job config and pre-resolved context - unit-testable
 * without a running server.
 */
public final class PaymentService {

    /** The context resolved at payout time: who else is working here, what tool is held, when and what season. */
    public static final class Context {
        private final double progressionMultiplier;
        private final int cooperativeParticipants;
        private final String heldToolMaterial;
        private final Season season;
        private final LocalTime timeOfDay;
        private final boolean cooperativeGloballyEnabled;
        private final boolean seasonalGloballyEnabled;

        public Context(double progressionMultiplier, int cooperativeParticipants, String heldToolMaterial,
                       Season season, LocalTime timeOfDay, boolean cooperativeGloballyEnabled,
                       boolean seasonalGloballyEnabled) {
            this.progressionMultiplier = progressionMultiplier <= 0 ? 1.0 : progressionMultiplier;
            this.cooperativeParticipants = Math.max(1, cooperativeParticipants);
            this.heldToolMaterial = heldToolMaterial;
            this.season = season;
            this.timeOfDay = timeOfDay;
            this.cooperativeGloballyEnabled = cooperativeGloballyEnabled;
            this.seasonalGloballyEnabled = seasonalGloballyEnabled;
        }
    }

    /** Base pay before any multiplier, by payment model. Below {@code minimum_payout}, production and delivery pay nothing. */
    public double basePay(Job job, WorkSession session, long now) {
        PaymentSpec payment = job.payment();
        // Transformative jobs (carpenter, smith, baker) pay per completed transformation, not per rate.
        if (job.isTransformative()) {
            return applyMinimum(session.transformationEarnings(), payment);
        }
        return switch (job.paymentModel()) {
            case A_PRODUZIONE -> applyMinimum(producedValue(session, payment, false), payment);
            case A_CONSEGNA -> applyMinimum(producedValue(session, payment, true), payment);
            case A_SESSIONE -> sessionPay(session, payment, now);
        };
    }

    private double producedValue(WorkSession session, PaymentSpec payment, boolean delivery) {
        double total = 0.0;
        for (Map.Entry<String, Integer> entry : session.producedByMaterial().entrySet()) {
            double rate = delivery ? payment.deliveryRate(entry.getKey()) : payment.rate(entry.getKey());
            total += rate * entry.getValue();
        }
        return total;
    }

    private double sessionPay(WorkSession session, PaymentSpec payment, long now) {
        double activeHours = session.activeMillis(now) / 3_600_000.0;
        double base = activeHours * payment.ratePerHour();
        if (session.activeRatio(now) < payment.activityThreshold()) {
            base *= payment.inactivityPenalty();
        }
        return base;
    }

    private double applyMinimum(double base, PaymentSpec payment) {
        return base < payment.minimumPayout() ? 0.0 : base;
    }

    /** The full, itemised breakdown for a session given the resolved context. */
    public PayoutBreakdown compute(Job job, WorkSession session, long now, Context context) {
        double base = basePay(job, session, now);

        double cooperative = (context.cooperativeGloballyEnabled && job.cooperative().enabled())
                ? job.cooperative().multiplierFor(context.cooperativeParticipants)
                : 1.0;
        double tool = job.tool().enabled() ? job.tool().bonusFor(context.heldToolMaterial) : 1.0;
        double shift = job.shift().multiplierAt(context.timeOfDay);
        double seasonal = (context.seasonalGloballyEnabled && job.seasonal().enabled())
                ? job.seasonal().multiplierFor(context.season)
                : 1.0;

        return new PayoutBreakdown(base, context.progressionMultiplier, cooperative, tool, shift, seasonal);
    }

    /** Whether this job pays only once the worker reaches its delivery point. */
    public boolean isDelivery(Job job) {
        return job.paymentModel() == PaymentModel.A_CONSEGNA;
    }
}
