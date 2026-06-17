package dev.openrp.crime.adapter;

import java.util.Collection;
import java.util.Map;
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
 * Persistence backend for every Open Crime record. CRUD-shaped so a relational backend can map each
 * {@code save}/{@code delete} to a single-row upsert, while the bundled YAML and in-memory adapters
 * rewrite their structures. The core calls the relevant {@code save} after each mutation, so
 * durability is the adapter's decision.
 */
public interface StorageAdapter {

    String id();

    /** Open files/connections and create schema if needed. Called once on enable. */
    void init();

    // --- organisations -----------------------------------------------------------------------

    Collection<IllegalOrg> loadOrgs();

    void saveOrg(IllegalOrg org);

    void deleteOrg(String orgId);

    // --- territory ---------------------------------------------------------------------------

    Collection<Territory> loadTerritories();

    void saveTerritory(Territory territory);

    void deleteTerritory(String regionId);

    // --- tracked goods -----------------------------------------------------------------------

    Collection<TrackedGood> loadTrackedGoods();

    void saveTrackedGood(TrackedGood good);

    void deleteTrackedGood(String itemUuid);

    // --- production --------------------------------------------------------------------------

    Collection<ProductionProcess> loadProduction();

    void saveProduction(ProductionProcess process);

    void deleteProduction(String id);

    // --- shipments ---------------------------------------------------------------------------

    Collection<Shipment> loadShipments();

    void saveShipment(Shipment shipment);

    void deleteShipment(String id);

    // --- laundering --------------------------------------------------------------------------

    Collection<LaunderingProcess> loadLaundering();

    void saveLaundering(LaunderingProcess process);

    void deleteLaundering(String id);

    // --- protections -------------------------------------------------------------------------

    Collection<Protection> loadProtections();

    void saveProtection(Protection protection);

    void deleteProtection(String id);

    // --- crime events (private by default) ---------------------------------------------------

    Collection<CrimeEvent> loadEvents();

    void saveEvent(CrimeEvent event);

    // --- discoveries -------------------------------------------------------------------------

    Collection<Discovery> loadDiscoveries();

    void saveDiscovery(Discovery discovery);

    // --- internal economy treasuries ---------------------------------------------------------

    /** Treasury balances keyed by treasury id: a two-element array {@code [clean, dirty]}. */
    Map<java.util.UUID, long[]> loadTreasuries();

    void saveTreasury(java.util.UUID treasury, long clean, long dirty);

    // --- counters ----------------------------------------------------------------------------

    Map<String, Long> loadCounters();

    void saveCounter(String key, long value);

    // --- lifecycle ---------------------------------------------------------------------------

    /** Forces any buffered writes to durable storage. */
    void flush();

    /** Releases resources. Called on disable. */
    void close();
}
