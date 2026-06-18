package dev.openrp.politics.adapter.defaults;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import dev.openrp.politics.adapter.StorageAdapter;
import dev.openrp.politics.model.ChargeHolder;
import dev.openrp.politics.model.CollegiateVote;
import dev.openrp.politics.model.Election;
import dev.openrp.politics.model.Law;
import dev.openrp.politics.model.PoliticalAct;

/**
 * Non-persistent storage adapter: everything lives in memory and is lost on restart. Useful for tests
 * and for a transient server. It returns the live objects, so a mutation by the core is immediately
 * visible - exactly what an in-process backend should do.
 */
public final class MemoryStorageAdapter implements StorageAdapter {

    private final Map<String, ChargeHolder> holders = new ConcurrentHashMap<>();
    private final Map<String, Election> elections = new ConcurrentHashMap<>();
    private final Map<String, PoliticalAct> acts = new ConcurrentHashMap<>();
    private final Map<String, Law> laws = new ConcurrentHashMap<>();
    private final Map<String, CollegiateVote> collegiateVotes = new ConcurrentHashMap<>();
    private final Map<String, Boolean> governmentStates = new ConcurrentHashMap<>();
    private final Map<String, Long> counters = new ConcurrentHashMap<>();

    @Override
    public String id() {
        return "memory";
    }

    @Override
    public void init() {
        // Nothing to open.
    }

    @Override
    public Collection<ChargeHolder> loadHolders() {
        return new ArrayList<>(holders.values());
    }

    @Override
    public void saveHolder(ChargeHolder holder) {
        holders.put(holder.id(), holder);
    }

    @Override
    public void deleteHolder(String holderId) {
        holders.remove(holderId);
    }

    @Override
    public Collection<Election> loadElections() {
        return new ArrayList<>(elections.values());
    }

    @Override
    public void saveElection(Election election) {
        elections.put(election.id(), election);
    }

    @Override
    public void deleteElection(String electionId) {
        elections.remove(electionId);
    }

    @Override
    public Collection<PoliticalAct> loadActs() {
        return new ArrayList<>(acts.values());
    }

    @Override
    public void saveAct(PoliticalAct act) {
        acts.put(act.id(), act);
    }

    @Override
    public void deleteAct(String actId) {
        acts.remove(actId);
    }

    @Override
    public Collection<Law> loadLaws() {
        return new ArrayList<>(laws.values());
    }

    @Override
    public void saveLaw(Law law) {
        laws.put(law.id(), law);
    }

    @Override
    public void deleteLaw(String lawId) {
        laws.remove(lawId);
    }

    @Override
    public Collection<CollegiateVote> loadCollegiateVotes() {
        return new ArrayList<>(collegiateVotes.values());
    }

    @Override
    public void saveCollegiateVote(CollegiateVote vote) {
        collegiateVotes.put(vote.id(), vote);
    }

    @Override
    public void deleteCollegiateVote(String voteId) {
        collegiateVotes.remove(voteId);
    }

    @Override
    public Map<String, Boolean> loadGovernmentStates() {
        return new LinkedHashMap<>(governmentStates);
    }

    @Override
    public void saveGovernmentState(String governmentId, boolean active) {
        governmentStates.put(governmentId, active);
    }

    @Override
    public Map<String, Long> loadCounters() {
        return new LinkedHashMap<>(counters);
    }

    @Override
    public void saveCounter(String key, long value) {
        counters.put(key, value);
    }

    @Override
    public void flush() {
        // Nothing to flush.
    }

    @Override
    public void close() {
        // Nothing to close.
    }
}
