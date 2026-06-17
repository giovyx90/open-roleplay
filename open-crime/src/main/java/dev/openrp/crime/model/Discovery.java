package dev.openrp.crime.model;

import java.util.Objects;
import java.util.UUID;

/**
 * The bridge between the underworld and the authorities. A {@link CrimeEvent} not linked to any
 * discovery does not exist for the authorities. A discovery is never a number - it is a concrete
 * moment when a piece of criminal information became accessible through a {@link DiscoveryType
 * specific RP action}, attributed to the player who made it and (optionally) attached to a dossier.
 */
public final class Discovery {

    private final String id;
    private final String crimeEventId;
    private final DiscoveryType type;
    private final UUID discoveredBy;
    private final long discoveredAt;
    private final String world;
    private final int x;
    private final int y;
    private final int z;
    private String dossierId;

    public Discovery(String id, String crimeEventId, DiscoveryType type, UUID discoveredBy,
                     long discoveredAt, String world, int x, int y, int z, String dossierId) {
        this.id = Objects.requireNonNull(id, "id");
        this.crimeEventId = Objects.requireNonNull(crimeEventId, "crimeEventId");
        this.type = type == null ? DiscoveryType.DENUNCIA : type;
        this.discoveredBy = discoveredBy;
        this.discoveredAt = discoveredAt;
        this.world = world == null ? "" : world;
        this.x = x;
        this.y = y;
        this.z = z;
        this.dossierId = emptyToNull(dossierId);
    }

    public String id() {
        return id;
    }

    public String crimeEventId() {
        return crimeEventId;
    }

    public DiscoveryType type() {
        return type;
    }

    public UUID discoveredBy() {
        return discoveredBy;
    }

    public long discoveredAt() {
        return discoveredAt;
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

    /** The dossier this discovery feeds, or {@code null} when no dossier has been opened yet. */
    public String dossierId() {
        return dossierId;
    }

    public void setDossierId(String dossierId) {
        this.dossierId = emptyToNull(dossierId);
    }

    private static String emptyToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
