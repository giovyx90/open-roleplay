package dev.openrp.crime.adapter.defaults;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import dev.openrp.crime.adapter.StorageAdapter;
import dev.openrp.crime.model.CrimeEvent;
import dev.openrp.crime.model.Discovery;
import dev.openrp.crime.model.IllegalOrg;
import dev.openrp.crime.model.LaunderingProcess;
import dev.openrp.crime.model.ProductionProcess;
import dev.openrp.crime.model.Protection;
import dev.openrp.crime.model.Shipment;
import dev.openrp.crime.model.Territory;
import dev.openrp.crime.model.TrackedGood;

/**
 * Volatile storage backend keeping every record in memory. Useful for tests and for servers that
 * deliberately want no persistence (data is gone on restart). It holds the live model objects, so it
 * never serialises - mutations made by the managers are reflected immediately.
 */
public final class MemoryStorageAdapter implements StorageAdapter {

    private final Map<String, IllegalOrg> orgs = new LinkedHashMap<>();
    private final Map<String, Territory> territories = new LinkedHashMap<>();
    private final Map<String, TrackedGood> goods = new LinkedHashMap<>();
    private final Map<String, ProductionProcess> production = new LinkedHashMap<>();
    private final Map<String, Shipment> shipments = new LinkedHashMap<>();
    private final Map<String, LaunderingProcess> laundering = new LinkedHashMap<>();
    private final Map<String, Protection> protections = new LinkedHashMap<>();
    private final Map<String, CrimeEvent> events = new LinkedHashMap<>();
    private final Map<String, Discovery> discoveries = new LinkedHashMap<>();
    private final Map<UUID, long[]> treasuries = new LinkedHashMap<>();
    private final Map<String, Long> counters = new LinkedHashMap<>();

    @Override
    public String id() {
        return "memory";
    }

    @Override
    public void init() {
        // nothing to open
    }

    @Override
    public Collection<IllegalOrg> loadOrgs() {
        return new ArrayList<>(orgs.values());
    }

    @Override
    public void saveOrg(IllegalOrg org) {
        orgs.put(org.id(), org);
    }

    @Override
    public void deleteOrg(String orgId) {
        orgs.remove(orgId);
    }

    @Override
    public Collection<Territory> loadTerritories() {
        return new ArrayList<>(territories.values());
    }

    @Override
    public void saveTerritory(Territory territory) {
        territories.put(territory.regionId(), territory);
    }

    @Override
    public void deleteTerritory(String regionId) {
        territories.remove(regionId);
    }

    @Override
    public Collection<TrackedGood> loadTrackedGoods() {
        return new ArrayList<>(goods.values());
    }

    @Override
    public void saveTrackedGood(TrackedGood good) {
        goods.put(good.itemUuid(), good);
    }

    @Override
    public void deleteTrackedGood(String itemUuid) {
        goods.remove(itemUuid);
    }

    @Override
    public Collection<ProductionProcess> loadProduction() {
        return new ArrayList<>(production.values());
    }

    @Override
    public void saveProduction(ProductionProcess process) {
        production.put(process.id(), process);
    }

    @Override
    public void deleteProduction(String id) {
        production.remove(id);
    }

    @Override
    public Collection<Shipment> loadShipments() {
        return new ArrayList<>(shipments.values());
    }

    @Override
    public void saveShipment(Shipment shipment) {
        shipments.put(shipment.id(), shipment);
    }

    @Override
    public void deleteShipment(String id) {
        shipments.remove(id);
    }

    @Override
    public Collection<LaunderingProcess> loadLaundering() {
        return new ArrayList<>(laundering.values());
    }

    @Override
    public void saveLaundering(LaunderingProcess process) {
        laundering.put(process.id(), process);
    }

    @Override
    public void deleteLaundering(String id) {
        laundering.remove(id);
    }

    @Override
    public Collection<Protection> loadProtections() {
        return new ArrayList<>(protections.values());
    }

    @Override
    public void saveProtection(Protection protection) {
        protections.put(protection.id(), protection);
    }

    @Override
    public void deleteProtection(String id) {
        protections.remove(id);
    }

    @Override
    public Collection<CrimeEvent> loadEvents() {
        return new ArrayList<>(events.values());
    }

    @Override
    public void saveEvent(CrimeEvent event) {
        events.put(event.id(), event);
    }

    @Override
    public Collection<Discovery> loadDiscoveries() {
        return new ArrayList<>(discoveries.values());
    }

    @Override
    public void saveDiscovery(Discovery discovery) {
        discoveries.put(discovery.id(), discovery);
    }

    @Override
    public Map<UUID, long[]> loadTreasuries() {
        Map<UUID, long[]> copy = new LinkedHashMap<>();
        treasuries.forEach((key, value) -> copy.put(key, new long[]{value[0], value[1]}));
        return copy;
    }

    @Override
    public void saveTreasury(UUID treasury, long clean, long dirty) {
        treasuries.put(treasury, new long[]{clean, dirty});
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
        // nothing to flush
    }

    @Override
    public void close() {
        // nothing to close
    }
}
