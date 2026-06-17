package dev.openrp.jobs.api;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import dev.openrp.jobs.adapter.AdapterRegistry;
import dev.openrp.jobs.config.Job;
import dev.openrp.jobs.core.LocationManager;
import dev.openrp.jobs.core.ProgressionService;
import dev.openrp.jobs.model.WorkLicense;
import dev.openrp.jobs.model.WorkRecord;
import dev.openrp.jobs.model.WorkSession;

/**
 * Public API, registered with the Bukkit ServicesManager. It exposes the job catalogue, a worker's
 * active session, their lifetime records and licences, and the live progression tier - the data the
 * Open Gestionale widgets and an Open Identity licence item read. Register your economy / company /
 * identity / region adapter through {@link #adapters()}. Retrieve it with
 * {@code Bukkit.getServicesManager().load(OpenJobsApi.class)}.
 */
public interface OpenJobsApi {

    /** The live adapter set; register your economy/company/identity/region adapter here. */
    AdapterRegistry adapters();

    // --- jobs --------------------------------------------------------------------------------

    Collection<Job> jobs();

    Optional<Job> getJob(String jobId);

    // --- sessions ----------------------------------------------------------------------------

    /** The worker's current (active or paused) session, if any. */
    Optional<WorkSession> getActiveSession(UUID player);

    /** Every session currently in flight on the server. */
    List<WorkSession> activeSessions();

    // --- records & progression ---------------------------------------------------------------

    Optional<WorkRecord> getRecord(UUID player, String jobId);

    List<WorkRecord> getRecords(UUID player);

    /** The worker's live seniority tier id for a job (decay-adjusted), or empty if they have no record. */
    Optional<String> getTier(UUID player, String jobId);

    // --- licences ----------------------------------------------------------------------------

    boolean hasLicense(UUID player, String jobId);

    Optional<WorkLicense> getLicense(UUID player, String jobId);

    List<WorkLicense> getLicenses(UUID player);

    // --- manager access for richer integrations ----------------------------------------------

    LocationManager locations();

    ProgressionService progression();
}
