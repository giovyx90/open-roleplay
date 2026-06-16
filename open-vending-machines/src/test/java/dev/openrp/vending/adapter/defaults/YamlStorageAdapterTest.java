package dev.openrp.vending.adapter.defaults;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.UUID;
import java.util.logging.Logger;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import dev.openrp.vending.model.MachineLocation;
import dev.openrp.vending.model.MachineProduct;
import dev.openrp.vending.model.VendingMachine;

public class YamlStorageAdapterTest {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    private static final Logger LOGGER = Logger.getLogger(YamlStorageAdapterTest.class.getName());

    private YamlStorageAdapter open(File file) {
        YamlStorageAdapter adapter = new YamlStorageAdapter(file, LOGGER);
        adapter.init();
        return adapter;
    }

    private VendingMachine sampleMachine() {
        VendingMachine machine = new VendingMachine(
                UUID.randomUUID(), "snack", new MachineLocation("world", 1, 2, 3), null);
        machine.setCashBalance(42.0);
        machine.putProduct(new MachineProduct("a", 1.5, 7, 10));
        return machine;
    }

    @Test
    public void machineRoundTripsAcrossReopenWithCash() {
        File file = new File(folder.getRoot(), "machines.yml");
        VendingMachine machine = sampleMachine();
        open(file).save(machine);

        YamlStorageAdapter reopened = open(file);
        assertEquals(1, reopened.loadAll().size());
        VendingMachine loaded = reopened.loadAll().iterator().next();
        assertEquals(machine.id(), loaded.id());
        assertEquals(42.0, loaded.cashBalance(), 1e-9);
    }

    @Test
    public void writeIsAtomicAndKeepsBackup() {
        File file = new File(folder.getRoot(), "machines.yml");
        YamlStorageAdapter adapter = open(file);
        adapter.save(sampleMachine());

        assertTrue(file.isFile());
        assertFalse(new File(folder.getRoot(), "machines.yml.tmp").exists());

        adapter.save(sampleMachine());
        assertTrue(new File(folder.getRoot(), "machines.yml.bak").isFile());
    }

    @Test
    public void recoversFromCorruptPrimaryUsingBackup() throws Exception {
        File file = new File(folder.getRoot(), "machines.yml");
        YamlStorageAdapter adapter = open(file);
        VendingMachine first = sampleMachine();
        adapter.save(first);
        // Second write rotates the good first copy into the backup.
        adapter.save(sampleMachine());

        // Simulate a crash that left the live file truncated/garbage.
        Files.write(file.toPath(), "{{{ not: valid: yaml".getBytes(StandardCharsets.UTF_8));

        YamlStorageAdapter recovered = open(file);
        // Recovery must not silently return an empty set (which would let the next save wipe the data).
        assertFalse(recovered.loadAll().isEmpty());
    }
}
