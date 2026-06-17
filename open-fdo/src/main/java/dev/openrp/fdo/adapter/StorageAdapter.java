package dev.openrp.fdo.adapter;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import dev.openrp.fdo.model.ActRecord;
import dev.openrp.fdo.model.Agent;
import dev.openrp.fdo.model.AlertState;
import dev.openrp.fdo.model.DetentionOrder;
import dev.openrp.fdo.model.Dossier;
import dev.openrp.fdo.model.DutySession;
import dev.openrp.fdo.model.Evidence;
import dev.openrp.fdo.model.WantedEntry;

/**
 * Persistence backend for every core record. CRUD-shaped so a relational backend can map each
 * {@code save}/{@code delete} to a single-row upsert, while the bundled YAML and in-memory adapters
 * rewrite their structures. The append-only logs ({@link #appendAct}, {@link #appendDutySession})
 * are never updated in place. The core calls the relevant {@code save} after each mutation, so
 * durability is the adapter's decision.
 */
public interface StorageAdapter {

    String id();

    /** Open files/connections and create schema if needed. Called once on enable. */
    void init();

    // --- agents ------------------------------------------------------------------------------

    Collection<Agent> loadAgents();

    void saveAgent(Agent agent);

    void deleteAgent(UUID agentUuid);

    // --- dossiers ----------------------------------------------------------------------------

    Collection<Dossier> loadDossiers();

    void saveDossier(Dossier dossier);

    void deleteDossier(String dossierId);

    // --- evidence ----------------------------------------------------------------------------

    Collection<Evidence> loadEvidence();

    void saveEvidence(Evidence evidence);

    void deleteEvidence(UUID evidenceId);

    // --- wanted register ---------------------------------------------------------------------

    Collection<WantedEntry> loadWanted();

    void saveWanted(WantedEntry entry);

    void deleteWanted(UUID subjectUuid);

    // --- detention orders --------------------------------------------------------------------

    Collection<DetentionOrder> loadDetentions();

    void saveDetention(DetentionOrder order);

    void deleteDetention(UUID inmate);

    // --- append-only logs --------------------------------------------------------------------

    Collection<ActRecord> loadActs();

    void appendAct(ActRecord act);

    Collection<DutySession> loadDutySessions();

    void appendDutySession(DutySession session);

    // --- dossier id counters -----------------------------------------------------------------

    /** Persisted sequence counters keyed by "{year}/{corpsId}", so dossier numbers survive restarts. */
    Map<String, Long> loadCounters();

    void saveCounter(String key, long value);

    // --- alert state -------------------------------------------------------------------------

    Optional<AlertState> loadAlert();

    void saveAlert(AlertState state);

    // --- lifecycle ---------------------------------------------------------------------------

    /** Forces any buffered writes to durable storage. */
    void flush();

    /** Releases resources. Called on disable. */
    void close();
}
