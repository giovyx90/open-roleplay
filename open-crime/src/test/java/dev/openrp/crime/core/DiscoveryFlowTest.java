package dev.openrp.crime.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.UUID;
import org.junit.Before;
import org.junit.Test;
import dev.openrp.crime.adapter.AdapterRegistry;
import dev.openrp.crime.adapter.defaults.MemoryStorageAdapter;
import dev.openrp.crime.model.CrimeEvent;
import dev.openrp.crime.model.CrimeEventType;
import dev.openrp.crime.model.DiscoveryType;

public class DiscoveryFlowTest {

    private CrimeEventLog events;
    private DiscoveryService discoveries;

    @Before
    public void setUp() {
        AdapterRegistry adapters = new AdapterRegistry();
        adapters.setStorage(new MemoryStorageAdapter());
        events = new CrimeEventLog(adapters);
        events.loadAll();
        discoveries = new DiscoveryService(adapters, events);
        discoveries.loadAll();
    }

    private CrimeEvent registerEvent(String org, long timestamp) {
        return events.register(new CrimeEvent("evt-" + UUID.randomUUID(), CrimeEventType.PRODUCTION, org,
                List.of(UUID.randomUUID()), List.of(), "world", 100, 64, 100, timestamp, null));
    }

    @Test
    public void eventsArePrivateUntilDiscovered() {
        long now = System.currentTimeMillis();
        registerEvent("clan", now);
        // No dossier yet: the authorities cannot see anything for the org.
        assertTrue(events.discoveredByOrg("clan", "DOSS-1").isEmpty());
    }

    @Test
    public void denunciaLinksNearbyRecentEvents() {
        long now = System.currentTimeMillis();
        CrimeEvent event = registerEvent("clan", now);

        List<CrimeEvent> nearby = events.nearbyRecent("world", 100, 64, 100, 16, now - 60_000);
        assertEquals(1, nearby.size());

        discoveries.open(DiscoveryType.DENUNCIA, UUID.randomUUID(), "world", 100, 64, 100, "DOSS-1", nearby);
        assertEquals(1, discoveries.byDossier("DOSS-1").size());
        // Now the event is linked to the dossier and visible to the authorities.
        assertEquals(1, events.discoveredByOrg("clan", "DOSS-1").size());
        assertTrue(event.isDiscovered());
    }

    @Test
    public void nearbyRecentRespectsWindowAndRadius() {
        long now = System.currentTimeMillis();
        registerEvent("clan", now - 10_000_000L); // too old
        registerEvent("clan", now);               // recent but far
        events.register(new CrimeEvent("evt-far", CrimeEventType.TRAFFIC, "clan",
                List.of(), List.of(), "world", 5000, 64, 5000, now, null));

        List<CrimeEvent> nearby = events.nearbyRecent("world", 100, 64, 100, 16, now - 60_000);
        assertEquals(1, nearby.size());
        assertFalse(nearby.get(0).id().equals("evt-far"));
    }
}
