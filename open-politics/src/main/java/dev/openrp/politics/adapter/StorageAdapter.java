package dev.openrp.politics.adapter;

import java.util.Collection;
import java.util.Map;
import dev.openrp.politics.model.ChargeHolder;
import dev.openrp.politics.model.CollegiateVote;
import dev.openrp.politics.model.Election;
import dev.openrp.politics.model.Law;
import dev.openrp.politics.model.PoliticalAct;

/**
 * Persistence backend for every Open Politics record. CRUD-shaped so a relational backend can map each
 * {@code save}/{@code delete} to a single-row upsert, while the bundled YAML and in-memory adapters
 * rewrite their structures. The core calls the relevant {@code save} after each mutation, so durability
 * is the adapter's decision.
 */
public interface StorageAdapter {

    String id();

    /** Open files/connections and create schema if needed. Called once on enable. */
    void init();

    // --- charge holders ----------------------------------------------------------------------

    Collection<ChargeHolder> loadHolders();

    void saveHolder(ChargeHolder holder);

    void deleteHolder(String holderId);

    // --- elections ---------------------------------------------------------------------------

    Collection<Election> loadElections();

    void saveElection(Election election);

    void deleteElection(String electionId);

    // --- acts --------------------------------------------------------------------------------

    Collection<PoliticalAct> loadActs();

    void saveAct(PoliticalAct act);

    void deleteAct(String actId);

    // --- laws --------------------------------------------------------------------------------

    Collection<Law> loadLaws();

    void saveLaw(Law law);

    void deleteLaw(String lawId);

    // --- collegiate votes --------------------------------------------------------------------

    Collection<CollegiateVote> loadCollegiateVotes();

    void saveCollegiateVote(CollegiateVote vote);

    void deleteCollegiateVote(String voteId);

    // --- government activation overrides -----------------------------------------------------

    /** Active-state overrides keyed by government id; absence means "follow the config default". */
    Map<String, Boolean> loadGovernmentStates();

    void saveGovernmentState(String governmentId, boolean active);

    // --- counters ----------------------------------------------------------------------------

    Map<String, Long> loadCounters();

    void saveCounter(String key, long value);

    // --- lifecycle ---------------------------------------------------------------------------

    /** Forces any buffered writes to durable storage. */
    void flush();

    /** Releases resources. Called on disable. */
    void close();
}
