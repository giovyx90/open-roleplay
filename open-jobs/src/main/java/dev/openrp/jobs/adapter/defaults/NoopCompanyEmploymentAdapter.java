package dev.openrp.jobs.adapter.defaults;

import java.util.Optional;
import java.util.UUID;
import dev.openrp.jobs.adapter.CompanyEmploymentAdapter;

/**
 * Default company adapter when no company plugin is present: reports no employer and never redirects.
 * Every payout therefore goes straight to the worker.
 */
public final class NoopCompanyEmploymentAdapter implements CompanyEmploymentAdapter {

    @Override
    public String id() {
        return "none";
    }

    @Override
    public boolean available() {
        return false;
    }

    @Override
    public Optional<String> getEmployer(UUID player, String jobId) {
        return Optional.empty();
    }

    @Override
    public boolean shouldRedirectPayout(UUID player, String jobId) {
        return false;
    }

    @Override
    public void notifyPayout(UUID player, String jobId, double amount) {
        // no-op
    }
}
