package dev.openrp.jobs.adapter;

import java.util.Optional;
import java.util.UUID;

/**
 * Bridge to a company system (Open Companies). A company can employ basic-job workers; when an employee
 * does their basic job on company time, the pay may be redirected wholly or partly to the employer (the
 * employer pays them a fixed wage instead). The bundled default is a no-op reporting no employer, so
 * payouts always go straight to the worker when no company plugin is present.
 */
public interface CompanyEmploymentAdapter {

    String id();

    /** Whether a real company backend is present. */
    boolean available();

    /** The id of the company employing the player for this job, if any. */
    Optional<String> getEmployer(UUID player, String jobId);

    /** Whether this worker's payout should be redirected to the employer rather than paid directly. */
    boolean shouldRedirectPayout(UUID player, String jobId);

    /** Notifies the company that the worker did {@code amount} worth of work (for wage/accounting). */
    void notifyPayout(UUID player, String jobId, double amount);
}
