package dev.openrp.fdo.core;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import dev.openrp.fdo.adapter.AdapterRegistry;
import dev.openrp.fdo.config.Corps;
import dev.openrp.fdo.config.FdoConfig;
import dev.openrp.fdo.model.Charge;
import dev.openrp.fdo.model.Dossier;
import dev.openrp.fdo.model.DossierStatus;
import dev.openrp.fdo.model.Verdict;
import dev.openrp.fdo.model.VerdictOutcome;

/**
 * Owns the case files. Opens dossiers with config-formatted ids, manages the mutable body (charges,
 * evidence links, notes, custody deadline) and signs the immutable outcome. The core only registers
 * and times - it never decides charges or sentences, it records them.
 */
public final class DossierManager {

    private final FdoConfig config;
    private final AdapterRegistry adapters;
    private final Counters counters;
    private final Map<String, Dossier> byId = new LinkedHashMap<>();

    public DossierManager(FdoConfig config, AdapterRegistry adapters, Counters counters) {
        this.config = config;
        this.adapters = adapters;
        this.counters = counters;
    }

    public synchronized void loadAll() {
        byId.clear();
        for (Dossier dossier : adapters.storage().loadDossiers()) {
            byId.put(dossier.id(), dossier);
        }
    }

    public Optional<Dossier> find(String id) {
        return Optional.ofNullable(id == null ? null : byId.get(id));
    }

    public Collection<Dossier> all() {
        return Collections.unmodifiableCollection(byId.values());
    }

    public List<Dossier> forSubject(UUID subject) {
        List<Dossier> result = new ArrayList<>();
        for (Dossier dossier : byId.values()) {
            if (subject != null && subject.equals(dossier.subjectUuid())) {
                result.add(dossier);
            }
        }
        return result;
    }

    /**
     * Opens a new dossier. {@code custodyIgHours <= 0} opens it without custody; a positive value
     * sets a cautionary custody deadline scaled by the configured time scale.
     */
    public synchronized FdoResult open(UUID subject, String subjectName, String corpsId, UUID openedBy,
                                       long custodyIgHours) {
        Optional<Corps> corps = config.corps().get(corpsId);
        if (corps.isEmpty()) {
            return FdoResult.fail("dossier.unknown_corps", "corps", String.valueOf(corpsId));
        }
        int year = LocalDate.now().getYear();
        long number = counters.next(DossierIds.counterKey(year, corpsId));
        String id = DossierIds.format(config.settings().dossierIdPattern(), year, number, corps.get());
        Dossier dossier = new Dossier(id, subject, subjectName, corpsId, openedBy, System.currentTimeMillis());
        if (custodyIgHours > 0) {
            dossier.setCustodyDeadline(System.currentTimeMillis() + realMillis(custodyIgHours));
        }
        byId.put(id, dossier);
        adapters.storage().saveDossier(dossier);
        return FdoResult.ok("dossier.opened", "id", id).withPayload(dossier);
    }

    public synchronized FdoResult addCharge(String dossierId, String crimeId, UUID by) {
        Dossier dossier = byId.get(dossierId);
        if (dossier == null) {
            return FdoResult.fail("dossier.not_found", "id", String.valueOf(dossierId));
        }
        if (dossier.isClosed()) {
            return FdoResult.fail("dossier.closed", "id", dossierId);
        }
        if (!config.crimes().exists(crimeId)) {
            return FdoResult.fail("dossier.unknown_crime", "crime", String.valueOf(crimeId));
        }
        dossier.addCharge(new Charge(crimeId, by, System.currentTimeMillis()));
        if (dossier.status() == DossierStatus.OPEN) {
            dossier.setStatus(DossierStatus.INVESTIGATION);
        }
        adapters.storage().saveDossier(dossier);
        return FdoResult.ok("dossier.charge_added", "crime", crimeId, "id", dossierId).withPayload(dossier);
    }

    public synchronized void linkEvidence(String dossierId, UUID evidenceId) {
        Dossier dossier = byId.get(dossierId);
        if (dossier != null) {
            dossier.linkEvidence(evidenceId);
            adapters.storage().saveDossier(dossier);
        }
    }

    public synchronized FdoResult addNote(String dossierId, String note) {
        Dossier dossier = byId.get(dossierId);
        if (dossier == null) {
            return FdoResult.fail("dossier.not_found", "id", String.valueOf(dossierId));
        }
        dossier.addNote(note);
        adapters.storage().saveDossier(dossier);
        return FdoResult.ok("dossier.note_added", "id", dossierId).withPayload(dossier);
    }

    public synchronized FdoResult extendCustody(String dossierId, long igHours) {
        Dossier dossier = byId.get(dossierId);
        if (dossier == null) {
            return FdoResult.fail("dossier.not_found", "id", String.valueOf(dossierId));
        }
        if (dossier.isClosed()) {
            return FdoResult.fail("dossier.closed", "id", dossierId);
        }
        long base = dossier.hasActiveCustody() ? dossier.custodyDeadline() : System.currentTimeMillis();
        dossier.setCustodyDeadline(base + realMillis(igHours));
        adapters.storage().saveDossier(dossier);
        return FdoResult.ok("dossier.custody_extended", "id", dossierId).withPayload(dossier);
    }

    public synchronized FdoResult setStatus(String dossierId, DossierStatus status) {
        Dossier dossier = byId.get(dossierId);
        if (dossier == null) {
            return FdoResult.fail("dossier.not_found", "id", String.valueOf(dossierId));
        }
        dossier.setStatus(status);
        adapters.storage().saveDossier(dossier);
        return FdoResult.ok("dossier.status_set", "id", dossierId).withPayload(dossier);
    }

    /**
     * Signs Section C. For a guilty verdict {@code sentenceIgHours} is converted to real seconds for
     * the detention timer; other outcomes carry no sentence. Returns a failure if the dossier is
     * missing or already closed (the outcome is immutable once signed).
     */
    public synchronized FdoResult signVerdict(String dossierId, VerdictOutcome outcome, long sentenceIgHours,
                                              int securityLevel, UUID judge, String note) {
        Dossier dossier = byId.get(dossierId);
        if (dossier == null) {
            return FdoResult.fail("dossier.not_found", "id", String.valueOf(dossierId));
        }
        if (outcome == null) {
            return FdoResult.fail("verdict.unknown_outcome");
        }
        if (dossier.isClosed()) {
            return FdoResult.fail("dossier.closed", "id", dossierId);
        }
        long sentenceSeconds = outcome.carriesSentence() ? Math.max(0L, sentenceIgHours) * config.settings().timeScaleSecondsPerHour() : 0L;
        Verdict verdict = new Verdict(outcome, sentenceSeconds, Math.max(1, securityLevel),
                judge, System.currentTimeMillis(), note == null ? "" : note);
        if (!dossier.signVerdict(verdict)) {
            return FdoResult.fail("dossier.closed", "id", dossierId);
        }
        adapters.storage().saveDossier(dossier);
        return FdoResult.ok("verdict.signed", "id", dossierId,
                "outcome", outcome.name().toLowerCase()).withPayload(dossier);
    }

    /** Real milliseconds for an in-game-hour quantity, via the configured time scale. */
    private long realMillis(long igHours) {
        return igHours * config.settings().timeScaleSecondsPerHour() * 1000L;
    }
}
