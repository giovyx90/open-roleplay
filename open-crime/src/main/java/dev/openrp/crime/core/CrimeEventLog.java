package dev.openrp.crime.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import dev.openrp.crime.adapter.AdapterRegistry;
import dev.openrp.crime.model.CrimeEvent;

/**
 * The append-only ledger of illegal actions. Every event is <strong>private by default</strong>: it
 * lives here but is invisible to the authorities until a {@link DiscoveryService discovery} links it
 * to a dossier. The authorities can never query this log freely - only through a dossier id that has
 * been tied to real discoveries.
 */
public final class CrimeEventLog {

    private final AdapterRegistry adapters;
    private final Map<String, CrimeEvent> byId = new LinkedHashMap<>();

    public CrimeEventLog(AdapterRegistry adapters) {
        this.adapters = adapters;
    }

    public synchronized void loadAll() {
        byId.clear();
        for (CrimeEvent event : adapters.storage().loadEvents()) {
            byId.put(event.id(), event);
        }
    }

    public synchronized CrimeEvent register(CrimeEvent event) {
        byId.put(event.id(), event);
        adapters.storage().saveEvent(event);
        return event;
    }

    public Optional<CrimeEvent> find(String id) {
        return Optional.ofNullable(id == null ? null : byId.get(id));
    }

    public Collection<CrimeEvent> all() {
        return Collections.unmodifiableCollection(byId.values());
    }

    /** Events attributed to the org no older than {@code sinceMillis}. Used when linking an arrest. */
    public synchronized List<CrimeEvent> recentByOrg(String orgId, long sinceMillis) {
        List<CrimeEvent> result = new ArrayList<>();
        if (orgId == null) {
            return result;
        }
        for (CrimeEvent event : byId.values()) {
            if (orgId.equals(event.orgId()) && event.timestamp() >= sinceMillis) {
                result.add(event);
            }
        }
        return result;
    }

    /**
     * Recent events within {@code radius} blocks of a point, in the same world, no older than
     * {@code sinceMillis}. Used to verify a {@code /denuncia} actually corresponds to a crime that
     * happened nearby and recently - you cannot report into the void.
     */
    public synchronized List<CrimeEvent> nearbyRecent(String world, int x, int y, int z, int radius,
                                                      long sinceMillis) {
        List<CrimeEvent> result = new ArrayList<>();
        long radiusSq = (long) radius * radius;
        for (CrimeEvent event : byId.values()) {
            if (!event.world().equals(world) || event.timestamp() < sinceMillis) {
                continue;
            }
            long dx = event.x() - x;
            long dy = event.y() - y;
            long dz = event.z() - z;
            if (dx * dx + dy * dy + dz * dz <= radiusSq) {
                result.add(event);
            }
        }
        return result;
    }

    /** Events the org carried out that are already linked to the given dossier. */
    public synchronized List<CrimeEvent> discoveredByOrg(String orgId, String dossierId) {
        List<CrimeEvent> result = new ArrayList<>();
        if (orgId == null || dossierId == null) {
            return result;
        }
        for (CrimeEvent event : byId.values()) {
            if (orgId.equals(event.orgId()) && dossierId.equals(event.dossierId())) {
                result.add(event);
            }
        }
        return result;
    }

    /** Links an event to a dossier - the moment it becomes visible to the authorities. */
    public synchronized void linkToDossier(CrimeEvent event, String dossierId) {
        if (event == null || dossierId == null) {
            return;
        }
        event.setDossierId(dossierId);
        adapters.storage().saveEvent(event);
    }
}
