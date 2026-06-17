package dev.openrp.fdo.adapter.defaults;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.nio.file.Files;
import java.util.UUID;
import java.util.logging.Logger;
import org.junit.Test;
import dev.openrp.fdo.model.CustodyAction;
import dev.openrp.fdo.model.CustodyEntry;
import dev.openrp.fdo.model.Evidence;

/** Round-trips evidence + its chain of custody through the durable YAML adapter. */
public class YamlStorageEvidenceTest {

    @Test
    public void custodyChainSurvivesWorldNameContainingSemicolon() throws Exception {
        File dir = Files.createTempDirectory("fdo-storage").toFile();
        File file = new File(dir, "fdo-data.yml");
        Logger logger = Logger.getLogger("test");

        YamlStorageAdapter storage = new YamlStorageAdapter(file, logger);
        storage.init();
        Evidence evidence = new Evidence(UUID.randomUUID(), "2026/1/PS", "Coltello", "manual", "", 1000L);
        // A semicolon in the world name must not corrupt the coordinates: world is encoded last.
        evidence.addCustody(new CustodyEntry(null, UUID.randomUUID(), CustodyAction.COLLECTED, 2000L,
                "weird;world", 10.5, 64.0, -3.25));
        storage.saveEvidence(evidence);

        YamlStorageAdapter reopened = new YamlStorageAdapter(file, logger);
        reopened.init();
        Evidence loaded = reopened.loadEvidence().iterator().next();
        assertNotNull(loaded);
        assertEquals("2026/1/PS", loaded.dossierId());
        assertEquals(1, loaded.chain().size());
        CustodyEntry entry = loaded.chain().get(0);
        assertEquals("weird;world", entry.world());
        assertEquals(10.5, entry.x(), 0.0001);
        assertEquals(64.0, entry.y(), 0.0001);
        assertEquals(-3.25, entry.z(), 0.0001);
        assertEquals(CustodyAction.COLLECTED, entry.action());
    }
}
