package dev.openrp.crime.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * The append-only record of one illegal action. It is <strong>private by default</strong>: it exists
 * in the database but is invisible to the authorities until a {@link Discovery} links it to a dossier.
 * The only mutable field is {@link #dossierId()}, set when the event is finally discovered.
 */
public final class CrimeEvent {

    private final String id;
    private final CrimeEventType type;
    private final String orgId;
    private final List<UUID> members;
    private final List<String> goodItemUuids;
    private final String world;
    private final int x;
    private final int y;
    private final int z;
    private final long timestamp;
    private String dossierId;

    public CrimeEvent(String id, CrimeEventType type, String orgId, List<UUID> members,
                      List<String> goodItemUuids, String world, int x, int y, int z, long timestamp,
                      String dossierId) {
        this.id = Objects.requireNonNull(id, "id");
        this.type = type == null ? CrimeEventType.PRODUCTION : type;
        this.orgId = orgId;
        this.members = members == null ? new ArrayList<>() : new ArrayList<>(members);
        this.goodItemUuids = goodItemUuids == null ? new ArrayList<>() : new ArrayList<>(goodItemUuids);
        this.world = world == null ? "" : world;
        this.x = x;
        this.y = y;
        this.z = z;
        this.timestamp = timestamp;
        this.dossierId = emptyToNull(dossierId);
    }

    public String id() {
        return id;
    }

    public CrimeEventType type() {
        return type;
    }

    /** The org involved, or {@code null} when it could not be determined. */
    public String orgId() {
        return orgId;
    }

    public List<UUID> members() {
        return Collections.unmodifiableList(members);
    }

    public List<String> goodItemUuids() {
        return Collections.unmodifiableList(goodItemUuids);
    }

    public String world() {
        return world;
    }

    public int x() {
        return x;
    }

    public int y() {
        return y;
    }

    public int z() {
        return z;
    }

    public long timestamp() {
        return timestamp;
    }

    /** The dossier this event has been linked to, or {@code null} while it is still undiscovered. */
    public String dossierId() {
        return dossierId;
    }

    public void setDossierId(String dossierId) {
        this.dossierId = emptyToNull(dossierId);
    }

    public boolean isDiscovered() {
        return dossierId != null;
    }

    private static String emptyToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
