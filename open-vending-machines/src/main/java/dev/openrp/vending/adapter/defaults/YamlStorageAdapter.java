package dev.openrp.vending.adapter.defaults;

import java.io.File;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import dev.openrp.vending.adapter.StorageAdapter;
import dev.openrp.vending.model.MachineLocation;
import dev.openrp.vending.model.MachineProduct;
import dev.openrp.vending.model.VendingMachine;
import dev.openrp.vending.model.VendingMachineState;

/**
 * Default storage adapter persisting machines to a single YAML file. The schema mirrors the model
 * one-to-one, so it doubles as human-readable documentation of what a machine "is". A relational
 * adapter would implement the same interface with per-row writes - see the README. Each rewrite is
 * durable: the file is written to a temporary sibling and atomically renamed over the live file, and
 * the previous contents are retained as a {@code .bak}, so a crash mid-write can never corrupt or
 * lose the whole machine set (which carries each machine's cash balance).
 */
public final class YamlStorageAdapter implements StorageAdapter {

    private static final String ROOT = "machines";

    private final File file;
    private final File tempFile;
    private final File backupFile;
    private final Logger logger;
    private YamlConfiguration yaml = new YamlConfiguration();

    public YamlStorageAdapter(File file, Logger logger) {
        this.file = file;
        this.tempFile = new File(file.getParentFile(), file.getName() + ".tmp");
        this.backupFile = new File(file.getParentFile(), file.getName() + ".bak");
        this.logger = logger;
    }

    @Override
    public String id() {
        return "yaml";
    }

    @Override
    public void init() {
        File parent = file.getParentFile();
        if (parent != null) {
            parent.mkdirs();
        }
        // Recover a write interrupted after the temp file was written but before the atomic rename:
        // prefer the live file, fall back to .tmp, then .bak.
        YamlConfiguration loaded = tryLoad(file);
        if (loaded == null) {
            loaded = tryLoad(tempFile);
            if (loaded != null) {
                logger.warning("[OpenVendingMachines] Recovered machine data from interrupted write (" + tempFile.getName() + ").");
            }
        }
        if (loaded == null) {
            loaded = tryLoad(backupFile);
            if (loaded != null) {
                logger.warning("[OpenVendingMachines] Primary machine data unreadable; recovered from backup (" + backupFile.getName() + ").");
            }
        }
        yaml = loaded != null ? loaded : new YamlConfiguration();
    }

    /**
     * Loads a YAML file strictly, returning {@code null} when it is missing or corrupt so the caller
     * can fall back to a backup. Unlike {@link YamlConfiguration#loadConfiguration(File)} this does
     * not silently swallow a parse failure into an empty configuration, which would look like "no
     * machines" and let the next save overwrite the only good copy.
     */
    private YamlConfiguration tryLoad(File source) {
        if (source == null || !source.isFile()) {
            return null;
        }
        YamlConfiguration config = new YamlConfiguration();
        try {
            config.load(source);
            return config;
        } catch (IOException | org.bukkit.configuration.InvalidConfigurationException exception) {
            logger.severe("[OpenVendingMachines] Failed to read '" + source.getName() + "': " + exception.getMessage());
            return null;
        }
    }

    @Override
    public Collection<VendingMachine> loadAll() {
        List<VendingMachine> machines = new ArrayList<>();
        ConfigurationSection root = yaml.getConfigurationSection(ROOT);
        if (root == null) {
            return machines;
        }
        for (String key : root.getKeys(false)) {
            ConfigurationSection section = root.getConfigurationSection(key);
            if (section == null) {
                continue;
            }
            try {
                machines.add(read(UUID.fromString(key), section));
            } catch (RuntimeException exception) {
                logger.warning("[OpenVendingMachines] Skipping malformed machine '" + key + "': " + exception.getMessage());
            }
        }
        return machines;
    }

    @Override
    public void save(VendingMachine machine) {
        write(machine);
        persist();
    }

    @Override
    public void saveAll(Collection<VendingMachine> machines) {
        yaml.set(ROOT, null);
        for (VendingMachine machine : machines) {
            write(machine);
        }
        persist();
    }

    @Override
    public void delete(UUID machineId) {
        yaml.set(ROOT + "." + machineId, null);
        persist();
    }

    @Override
    public void flush() {
        persist();
    }

    @Override
    public void close() {
        persist();
    }

    private VendingMachine read(UUID id, ConfigurationSection section) {
        MachineLocation location = new MachineLocation(
                section.getString("world", "world"),
                section.getInt("x"),
                section.getInt("y"),
                section.getInt("z"));
        VendingMachine machine = new VendingMachine(id, section.getString("type", ""), location, section.getString("owner"));
        machine.setCashBalance(section.getDouble("cash", 0.0));
        machine.setState(VendingMachineState.fromString(section.getString("state")));
        ConfigurationSection products = section.getConfigurationSection("products");
        if (products != null) {
            for (String productId : products.getKeys(false)) {
                ConfigurationSection productSection = products.getConfigurationSection(productId);
                if (productSection == null) {
                    continue;
                }
                machine.putProduct(new MachineProduct(
                        productId,
                        productSection.getDouble("price", 0.0),
                        productSection.getInt("stock", 0),
                        productSection.getInt("capacity", 0)));
            }
        }
        return machine;
    }

    private void write(VendingMachine machine) {
        String base = ROOT + "." + machine.id();
        yaml.set(base + ".type", machine.typeId());
        yaml.set(base + ".world", machine.location().world());
        yaml.set(base + ".x", machine.location().x());
        yaml.set(base + ".y", machine.location().y());
        yaml.set(base + ".z", machine.location().z());
        yaml.set(base + ".owner", machine.ownerCompanyId().orElse(null));
        yaml.set(base + ".cash", machine.cashBalance());
        yaml.set(base + ".state", machine.state().name());
        yaml.set(base + ".products", null);
        for (MachineProduct product : machine.products()) {
            String productBase = base + ".products." + product.productId();
            yaml.set(productBase + ".price", product.price());
            yaml.set(productBase + ".stock", product.stock());
            yaml.set(productBase + ".capacity", product.capacity());
        }
    }

    /**
     * Durably writes the current state: new contents go to a temp file, the live file is copied aside
     * as {@code .bak}, then the temp file is atomically renamed over the live file. A crash at any
     * point leaves either the previous complete file or a recoverable {@code .tmp}/{@code .bak} -
     * never a half-written machines file.
     */
    private void persist() {
        try {
            yaml.save(tempFile);
        } catch (IOException exception) {
            logger.severe("[OpenVendingMachines] Failed to write machine data: " + exception.getMessage());
            return;
        }
        if (file.exists()) {
            try {
                Files.copy(file.toPath(), backupFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException exception) {
                logger.warning("[OpenVendingMachines] Failed to refresh machine data backup: " + exception.getMessage());
            }
        }
        try {
            Files.move(tempFile.toPath(), file.toPath(),
                    StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException atomicUnsupported) {
            try {
                Files.move(tempFile.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException exception) {
                logger.severe("[OpenVendingMachines] Failed to commit machine data: " + exception.getMessage());
            }
        } catch (IOException exception) {
            logger.severe("[OpenVendingMachines] Failed to commit machine data: " + exception.getMessage());
        }
    }
}
