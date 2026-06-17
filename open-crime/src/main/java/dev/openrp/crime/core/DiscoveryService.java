package dev.openrp.crime.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import dev.openrp.crime.adapter.AdapterRegistry;
import dev.openrp.crime.model.CrimeEvent;
import dev.openrp.crime.model.Discovery;
import dev.openrp.crime.model.DiscoveryType;

/**
 * The only path by which the authorities learn anything. A {@link Discovery} is created exclusively
 * through a concrete RP action (denuncia, physical discovery, arrest, informant, investigation), it
 * names who made it, and it links one {@link CrimeEvent} to a dossier. Nothing here ever happens on a
 * timer or by itself - if no player does anything, no discovery is ever born.
 */
public final class DiscoveryService {

    private final AdapterRegistry adapters;
    private final CrimeEventLog events;
    private final Map<String, Discovery> byId = new LinkedHashMap<>();

    public DiscoveryService(AdapterRegistry adapters, CrimeEventLog events) {
        this.adapters = adapters;
        this.events = events;
    }

    public synchronized void loadAll() {
        byId.clear();
        for (Discovery discovery : adapters.storage().loadDiscoveries()) {
            byId.put(discovery.id(), discovery);
        }
    }

    /** Records a single discovery as-is (public API entry point). */
    public synchronized Discovery register(Discovery discovery) {
        byId.put(discovery.id(), discovery);
        adapters.storage().saveDiscovery(discovery);
        return discovery;
    }

    /**
     * Opens discoveries of the given type for a set of crime events, all attached to the same dossier,
     * attributing them to {@code discoveredBy} at the given location. Each event is linked to the
     * dossier - the moment it stops being private. Returns the discoveries created.
     */
    public synchronized List<Discovery> open(DiscoveryType type, UUID discoveredBy, String world,
                                             int x, int y, int z, String dossierId, List<CrimeEvent> linked) {
        List<Discovery> created = new ArrayList<>();
        if (linked == null) {
            return created;
        }
        for (CrimeEvent event : linked) {
            Discovery discovery = new Discovery(Ids.prefixed("disc"), event.id(), type, discoveredBy,
                    System.currentTimeMillis(), world, x, y, z, dossierId);
            byId.put(discovery.id(), discovery);
            adapters.storage().saveDiscovery(discovery);
            if (dossierId != null) {
                events.linkToDossier(event, dossierId);
            }
            created.add(discovery);
        }
        return created;
    }

    public Optional<Discovery> find(String id) {
        return Optional.ofNullable(id == null ? null : byId.get(id));
    }

    public synchronized List<Discovery> byDossier(String dossierId) {
        List<Discovery> result = new ArrayList<>();
        if (dossierId == null) {
            return result;
        }
        for (Discovery discovery : byId.values()) {
            if (dossierId.equals(discovery.dossierId())) {
                result.add(discovery);
            }
        }
        return result;
    }

    public synchronized List<Discovery> byEvent(String eventId) {
        List<Discovery> result = new ArrayList<>();
        if (eventId == null) {
            return result;
        }
        for (Discovery discovery : byId.values()) {
            if (eventId.equals(discovery.crimeEventId())) {
                result.add(discovery);
            }
        }
        return result;
    }

    public Collection<Discovery> all() {
        return Collections.unmodifiableCollection(byId.values());
    }
}
