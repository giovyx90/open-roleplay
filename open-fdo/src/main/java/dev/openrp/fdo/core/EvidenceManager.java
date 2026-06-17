package dev.openrp.fdo.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import dev.openrp.fdo.adapter.AdapterRegistry;
import dev.openrp.fdo.model.CustodyAction;
import dev.openrp.fdo.model.CustodyEntry;
import dev.openrp.fdo.model.Evidence;
import dev.openrp.fdo.model.EvidenceState;

/**
 * Tracks seized evidence and its append-only chain of custody. Collection, transfer and deposit each
 * add a link recording who, what, when and where - so a gap in the chain is exactly what lets the
 * defence contest a piece of evidence in roleplay. The core records the chain; it never decides
 * whether the evidence is admissible.
 */
public final class EvidenceManager {

    private final AdapterRegistry adapters;
    private final DossierManager dossiers;
    private final Map<UUID, Evidence> byId = new LinkedHashMap<>();

    public EvidenceManager(AdapterRegistry adapters, DossierManager dossiers) {
        this.adapters = adapters;
        this.dossiers = dossiers;
    }

    public synchronized void loadAll() {
        byId.clear();
        for (Evidence evidence : adapters.storage().loadEvidence()) {
            byId.put(evidence.id(), evidence);
        }
    }

    public Optional<Evidence> find(UUID id) {
        return Optional.ofNullable(id == null ? null : byId.get(id));
    }

    /** Lookup by the 8-character short id used on the command line. */
    public Optional<Evidence> findByShortId(String shortId) {
        if (shortId == null) {
            return Optional.empty();
        }
        return byId.values().stream().filter(e -> e.shortId().equalsIgnoreCase(shortId)).findFirst();
    }

    public Collection<Evidence> all() {
        return Collections.unmodifiableCollection(byId.values());
    }

    public List<Evidence> forDossier(String dossierId) {
        List<Evidence> result = new ArrayList<>();
        for (Evidence evidence : byId.values()) {
            if (dossierId != null && dossierId.equals(evidence.dossierId())) {
                result.add(evidence);
            }
        }
        return result;
    }

    /** Seizes a new item at the scene: opens the chain with a COLLECTED link held by the collector. */
    public synchronized FdoResult seize(String dossierId, String label, String source, String nbt,
                                        UUID byAgent, String world, double x, double y, double z) {
        Evidence evidence = new Evidence(UUID.randomUUID(), dossierId, label, source, nbt, System.currentTimeMillis());
        evidence.addCustody(new CustodyEntry(null, byAgent, CustodyAction.COLLECTED, System.currentTimeMillis(), world, x, y, z));
        byId.put(evidence.id(), evidence);
        adapters.storage().saveEvidence(evidence);
        if (dossierId != null) {
            dossiers.linkEvidence(dossierId, evidence.id());
        }
        return FdoResult.ok("evidence.seized", "id", evidence.shortId(), "label", evidence.label()).withPayload(evidence);
    }

    public synchronized FdoResult transfer(UUID evidenceId, UUID fromAgent, UUID toAgent,
                                           String world, double x, double y, double z) {
        Evidence evidence = byId.get(evidenceId);
        if (evidence == null) {
            return FdoResult.fail("evidence.not_found");
        }
        evidence.addCustody(new CustodyEntry(fromAgent, toAgent, CustodyAction.TRANSFERRED, System.currentTimeMillis(), world, x, y, z));
        adapters.storage().saveEvidence(evidence);
        return FdoResult.ok("evidence.transferred", "id", evidence.shortId()).withPayload(evidence);
    }

    public synchronized FdoResult deposit(UUID evidenceId, UUID byAgent, String world, double x, double y, double z) {
        Evidence evidence = byId.get(evidenceId);
        if (evidence == null) {
            return FdoResult.fail("evidence.not_found");
        }
        evidence.addCustody(new CustodyEntry(byAgent, null, CustodyAction.DEPOSITED, System.currentTimeMillis(), world, x, y, z));
        evidence.setState(EvidenceState.STORED);
        adapters.storage().saveEvidence(evidence);
        return FdoResult.ok("evidence.deposited", "id", evidence.shortId()).withPayload(evidence);
    }

    public synchronized FdoResult release(UUID evidenceId, UUID byAgent, String world, double x, double y, double z) {
        Evidence evidence = byId.get(evidenceId);
        if (evidence == null) {
            return FdoResult.fail("evidence.not_found");
        }
        evidence.addCustody(new CustodyEntry(byAgent, null, CustodyAction.RELEASED, System.currentTimeMillis(), world, x, y, z));
        evidence.setState(EvidenceState.RELEASED);
        adapters.storage().saveEvidence(evidence);
        return FdoResult.ok("evidence.released", "id", evidence.shortId()).withPayload(evidence);
    }
}
