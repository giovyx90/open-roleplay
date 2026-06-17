package dev.openrp.fdo.adapter.defaults;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import dev.openrp.fdo.adapter.StorageAdapter;
import dev.openrp.fdo.model.ActRecord;
import dev.openrp.fdo.model.Agent;
import dev.openrp.fdo.model.AlertState;
import dev.openrp.fdo.model.DetentionOrder;
import dev.openrp.fdo.model.Dossier;
import dev.openrp.fdo.model.DutySession;
import dev.openrp.fdo.model.Evidence;
import dev.openrp.fdo.model.WantedEntry;

/**
 * Volatile storage backend ({@code adapters.storage: memory}). Useful for tests and ephemeral
 * servers; nothing survives a restart. Holds the canonical object references, so the managers and
 * this adapter share the same mutable aggregates.
 */
public final class MemoryStorageAdapter implements StorageAdapter {

    private final Map<UUID, Agent> agents = new ConcurrentHashMap<>();
    private final Map<String, Dossier> dossiers = new LinkedHashMap<>();
    private final Map<UUID, Evidence> evidence = new ConcurrentHashMap<>();
    private final Map<UUID, WantedEntry> wanted = new ConcurrentHashMap<>();
    private final Map<UUID, DetentionOrder> detentions = new ConcurrentHashMap<>();
    private final List<ActRecord> acts = new ArrayList<>();
    private final List<DutySession> dutySessions = new ArrayList<>();
    private final Map<String, Long> counters = new ConcurrentHashMap<>();
    private AlertState alert;

    @Override
    public String id() {
        return "memory";
    }

    @Override
    public void init() {
        // nothing to open
    }

    @Override
    public Collection<Agent> loadAgents() {
        return new ArrayList<>(agents.values());
    }

    @Override
    public void saveAgent(Agent agent) {
        agents.put(agent.uuid(), agent);
    }

    @Override
    public void deleteAgent(UUID agentUuid) {
        agents.remove(agentUuid);
    }

    @Override
    public Collection<Dossier> loadDossiers() {
        return new ArrayList<>(dossiers.values());
    }

    @Override
    public void saveDossier(Dossier dossier) {
        dossiers.put(dossier.id(), dossier);
    }

    @Override
    public void deleteDossier(String dossierId) {
        dossiers.remove(dossierId);
    }

    @Override
    public Collection<Evidence> loadEvidence() {
        return new ArrayList<>(evidence.values());
    }

    @Override
    public void saveEvidence(Evidence item) {
        evidence.put(item.id(), item);
    }

    @Override
    public void deleteEvidence(UUID evidenceId) {
        evidence.remove(evidenceId);
    }

    @Override
    public Collection<WantedEntry> loadWanted() {
        return new ArrayList<>(wanted.values());
    }

    @Override
    public void saveWanted(WantedEntry entry) {
        wanted.put(entry.subjectUuid(), entry);
    }

    @Override
    public void deleteWanted(UUID subjectUuid) {
        wanted.remove(subjectUuid);
    }

    @Override
    public Collection<DetentionOrder> loadDetentions() {
        return new ArrayList<>(detentions.values());
    }

    @Override
    public void saveDetention(DetentionOrder order) {
        detentions.put(order.inmate(), order);
    }

    @Override
    public void deleteDetention(UUID inmate) {
        detentions.remove(inmate);
    }

    @Override
    public Collection<ActRecord> loadActs() {
        return new ArrayList<>(acts);
    }

    @Override
    public void appendAct(ActRecord act) {
        acts.add(act);
    }

    @Override
    public Collection<DutySession> loadDutySessions() {
        return new ArrayList<>(dutySessions);
    }

    @Override
    public void appendDutySession(DutySession session) {
        dutySessions.add(session);
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
    public Optional<AlertState> loadAlert() {
        return Optional.ofNullable(alert);
    }

    @Override
    public void saveAlert(AlertState state) {
        this.alert = state;
    }

    @Override
    public void flush() {
        // nothing to flush
    }

    @Override
    public void close() {
        // nothing to release
    }
}
