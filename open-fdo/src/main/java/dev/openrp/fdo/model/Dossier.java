package dev.openrp.fdo.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * A case file. Mirrors the three-section structure of the design:
 * <ul>
 *   <li><b>Section A - heading</b> ({@code subject}, {@code corpsId}, {@code openedBy},
 *       {@code openedAt}): immutable once created, signed by whoever opened the dossier.</li>
 *   <li><b>Section B - body</b> ({@link #charges()}, {@link #evidenceIds()}, {@link #notes()},
 *       {@link #custodyDeadline()}): mutable while the proceeding is open.</li>
 *   <li><b>Section C - outcome</b> ({@link #verdict()}): immutable once signed by the judge; signing
 *       it closes the dossier.</li>
 * </ul>
 *
 * <p>The id is produced from the configured pattern (default {@code {anno}/{numero}/{sigla_corpo}});
 * the core never embeds an authority name in it, only the configured short code. Bukkit-free and
 * mutated only under the dossier lock.</p>
 */
public final class Dossier {

    private final String id;
    private final UUID subjectUuid;
    private final String subjectName;
    private final String corpsId;
    private final UUID openedBy;
    private final long openedAt;
    private DossierStatus status = DossierStatus.OPEN;
    private long custodyDeadline;
    private final List<Charge> charges = new ArrayList<>();
    private final List<UUID> evidenceIds = new ArrayList<>();
    private final List<String> notes = new ArrayList<>();
    private Verdict verdict;

    public Dossier(String id, UUID subjectUuid, String subjectName, String corpsId, UUID openedBy, long openedAt) {
        this.id = Objects.requireNonNull(id, "id");
        this.subjectUuid = subjectUuid;
        this.subjectName = subjectName == null ? "" : subjectName;
        this.corpsId = corpsId == null ? "" : corpsId;
        this.openedBy = openedBy;
        this.openedAt = openedAt;
    }

    // --- Section A (immutable) ---------------------------------------------------------------

    public String id() {
        return id;
    }

    public UUID subjectUuid() {
        return subjectUuid;
    }

    public String subjectName() {
        return subjectName;
    }

    public String corpsId() {
        return corpsId;
    }

    public UUID openedBy() {
        return openedBy;
    }

    public long openedAt() {
        return openedAt;
    }

    // --- Section B (mutable while open) ------------------------------------------------------

    public DossierStatus status() {
        return status;
    }

    public void setStatus(DossierStatus status) {
        this.status = Objects.requireNonNull(status, "status");
    }

    /** Custody deadline as an epoch millisecond, or {@code 0} when there is no active custody. */
    public long custodyDeadline() {
        return custodyDeadline;
    }

    public void setCustodyDeadline(long custodyDeadline) {
        this.custodyDeadline = Math.max(0L, custodyDeadline);
    }

    public boolean hasActiveCustody() {
        return custodyDeadline > 0L;
    }

    public List<Charge> charges() {
        return Collections.unmodifiableList(charges);
    }

    /** Adds a charge. Refused once the verdict is signed (Section C locks the file). */
    public boolean addCharge(Charge charge) {
        if (charge == null || isClosed()) {
            return false;
        }
        return charges.add(charge);
    }

    public List<UUID> evidenceIds() {
        return Collections.unmodifiableList(evidenceIds);
    }

    public void linkEvidence(UUID evidenceId) {
        if (evidenceId != null && !evidenceIds.contains(evidenceId)) {
            evidenceIds.add(evidenceId);
        }
    }

    public List<String> notes() {
        return Collections.unmodifiableList(notes);
    }

    public void addNote(String note) {
        if (note != null && !note.isBlank()) {
            notes.add(note);
        }
    }

    // --- Section C (immutable once signed) ---------------------------------------------------

    public Optional<Verdict> verdict() {
        return Optional.ofNullable(verdict);
    }

    public boolean isClosed() {
        return verdict != null;
    }

    /**
     * Signs Section C exactly once. Returns {@code false} (changing nothing) if a verdict already
     * exists, guaranteeing the outcome is immutable after the judge signs.
     */
    public boolean signVerdict(Verdict verdict) {
        if (this.verdict != null || verdict == null) {
            return false;
        }
        this.verdict = verdict;
        this.custodyDeadline = 0L;
        this.status = DossierStatus.CLOSED;
        return true;
    }
}
