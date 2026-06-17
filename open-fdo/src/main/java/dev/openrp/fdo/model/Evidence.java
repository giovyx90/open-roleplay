package dev.openrp.fdo.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * A seized item tracked through its chain of custody. The core stores an opaque NBT blob (so the
 * physical item can be reconstructed) plus a human label, its current {@link EvidenceState} and the
 * ordered list of {@link CustodyEntry custody links}. The chain is append-only: links are added as
 * the item changes hands, never rewritten, which is exactly what lets the defence challenge a gap.
 *
 * <p>Not thread-safe on its own; callers mutate evidence only while holding the dossier lock.</p>
 */
public final class Evidence {

    private final UUID id;
    private String dossierId;
    private final String label;
    private final String source;
    private final String nbt;
    private final long createdAt;
    private EvidenceState state = EvidenceState.IN_HAND;
    private final List<CustodyEntry> chain = new ArrayList<>();

    public Evidence(UUID id, String dossierId, String label, String source, String nbt, long createdAt) {
        this.id = Objects.requireNonNull(id, "id");
        this.dossierId = dossierId;
        this.label = label == null ? "" : label;
        this.source = source == null ? "manual" : source;
        this.nbt = nbt == null ? "" : nbt;
        this.createdAt = createdAt;
    }

    public UUID id() {
        return id;
    }

    /** First 8 characters of the id, for compact display and command arguments. */
    public String shortId() {
        return id.toString().substring(0, 8);
    }

    public String dossierId() {
        return dossierId;
    }

    public void setDossierId(String dossierId) {
        this.dossierId = dossierId;
    }

    public String label() {
        return label;
    }

    /** Origin of the evidence: {@code "manual"}, or a tag from an {@code EvidenceSourceAdapter}. */
    public String source() {
        return source;
    }

    public String nbt() {
        return nbt;
    }

    public long createdAt() {
        return createdAt;
    }

    public EvidenceState state() {
        return state;
    }

    public void setState(EvidenceState state) {
        this.state = Objects.requireNonNull(state, "state");
    }

    /** Live, unmodifiable view of the chain of custody in chronological order. */
    public List<CustodyEntry> chain() {
        return Collections.unmodifiableList(chain);
    }

    public void addCustody(CustodyEntry entry) {
        if (entry != null) {
            chain.add(entry);
        }
    }

    /** The agent currently holding the item, derived from the last transfer/collection. */
    public UUID currentHolder() {
        for (int i = chain.size() - 1; i >= 0; i--) {
            CustodyEntry entry = chain.get(i);
            if (entry.toAgent() != null) {
                return entry.toAgent();
            }
        }
        return null;
    }
}
