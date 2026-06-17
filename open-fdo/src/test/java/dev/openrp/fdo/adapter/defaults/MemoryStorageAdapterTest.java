package dev.openrp.fdo.adapter.defaults;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.UUID;
import org.junit.Test;
import dev.openrp.fdo.model.Agent;
import dev.openrp.fdo.model.Dossier;

public class MemoryStorageAdapterTest {

    @Test
    public void agentsRoundTrip() {
        MemoryStorageAdapter storage = new MemoryStorageAdapter();
        UUID uuid = UUID.randomUUID();
        storage.saveAgent(new Agent(uuid, "Mario", "polizia", "agente", "PS-0001", 1L));
        assertEquals(1, storage.loadAgents().size());
        Agent loaded = storage.loadAgents().iterator().next();
        assertEquals("PS-0001", loaded.matricola());
        storage.deleteAgent(uuid);
        assertTrue(storage.loadAgents().isEmpty());
    }

    @Test
    public void dossiersRoundTripByConfiguredId() {
        MemoryStorageAdapter storage = new MemoryStorageAdapter();
        Dossier dossier = new Dossier("2026/1/PS", UUID.randomUUID(), "Mario", "polizia", UUID.randomUUID(), 5L);
        storage.saveDossier(dossier);
        assertEquals(1, storage.loadDossiers().size());
        storage.deleteDossier("2026/1/PS");
        assertTrue(storage.loadDossiers().isEmpty());
    }

    @Test
    public void countersPersistAndReload() {
        MemoryStorageAdapter storage = new MemoryStorageAdapter();
        storage.saveCounter("2026/polizia", 4L);
        assertEquals(Long.valueOf(4L), storage.loadCounters().get("2026/polizia"));
    }
}
