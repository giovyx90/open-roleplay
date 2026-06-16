package dev.openrp.vending.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.locks.ReentrantLock;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import dev.openrp.vending.OpenVendingMachinesPlugin;
import dev.openrp.vending.event.VendingMachineCreateEvent;
import dev.openrp.vending.event.VendingMachineRemoveEvent;
import dev.openrp.vending.event.VendingMachineStateChangeEvent;
import dev.openrp.vending.model.MachineLocation;
import dev.openrp.vending.model.MachineProduct;
import dev.openrp.vending.model.MachineType;
import dev.openrp.vending.model.ProductDefinition;
import dev.openrp.vending.model.VendingMachine;
import dev.openrp.vending.model.VendingMachineState;

/**
 * Registry and lifecycle owner for vending machines: loading from storage, creating, removing,
 * lookup by id/location, company-limit enforcement and event-firing state transitions. Holds the
 * authoritative in-memory set; storage is updated through the {@code StorageAdapter}.
 */
public final class VendingMachineManager {

    private final OpenVendingMachinesPlugin plugin;
    private final Map<UUID, VendingMachine> machines = new LinkedHashMap<>();
    private final Map<String, UUID> byLocation = new LinkedHashMap<>();

    public VendingMachineManager(OpenVendingMachinesPlugin plugin) {
        this.plugin = plugin;
    }

    /** Loads all machines from storage into memory. Called on enable and reload. */
    public void loadAll() {
        machines.clear();
        byLocation.clear();
        for (VendingMachine machine : plugin.adapters().storage().loadAll()) {
            index(machine);
        }
        plugin.getLogger().info("[OpenVendingMachines] Loaded " + machines.size() + " machine(s).");
    }

    public Collection<VendingMachine> all() {
        return new ArrayList<>(machines.values());
    }

    public VendingMachine get(UUID id) {
        return machines.get(id);
    }

    public VendingMachine getAt(Location location) {
        if (location == null) {
            return null;
        }
        UUID id = byLocation.get(MachineLocation.of(location).toKey());
        return id == null ? null : machines.get(id);
    }

    public boolean existsAt(Location location) {
        return getAt(location) != null;
    }

    public int count() {
        return machines.size();
    }

    public long countOwnedBy(String companyId) {
        if (companyId == null) {
            return 0;
        }
        return machines.values().stream()
                .filter(machine -> machine.ownerCompanyId().map(companyId::equalsIgnoreCase).orElse(false))
                .count();
    }

    /**
     * Effective machine limit for a company: the {@code BusinessAdapter} value, then any
     * {@code VendingHook} adjustment. {@code -1} means unlimited.
     */
    public int effectiveLimit(String companyId) {
        int base = plugin.adapters().business().machineLimit(companyId);
        return plugin.hooks().resolveCompanyLimit(companyId, base);
    }

    public boolean isAtLimit(String companyId) {
        if (companyId == null) {
            return false;
        }
        int limit = effectiveLimit(companyId);
        return limit >= 0 && countOwnedBy(companyId) >= limit;
    }

    /**
     * Builds, validates and registers a new machine. Returns {@code null} if the
     * {@link VendingMachineCreateEvent} is cancelled. Callers should check {@link #existsAt} and
     * {@link #isAtLimit} first to give the user a specific reason.
     */
    public VendingMachine create(Player creator, MachineType type, MachineLocation location, String companyId) {
        VendingMachine machine = new VendingMachine(UUID.randomUUID(), type.id(), location, companyId);
        seedProducts(machine, type);
        machine.setState(machine.isOutOfStock() ? VendingMachineState.EMPTY : VendingMachineState.ACTIVE);

        VendingMachineCreateEvent event = new VendingMachineCreateEvent(creator, machine);
        plugin.getServer().getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            return null;
        }
        index(machine);
        plugin.adapters().storage().save(machine);
        // Optionally materialise the type's "icon" block in the world (the machine's visual).
        if (plugin.settings().placeIconBlock()) {
            Location loc = machine.location().toBukkitCenter();
            if (loc != null) {
                loc.getBlock().setType(type.icon());
            }
        }
        plugin.adapters().logging().log("CREATE", describe(creator) + " created " + type.id() + " machine " + machine.id()
                + (companyId == null ? "" : " for company " + companyId));
        return machine;
    }

    /** Removes a machine. Returns {@code false} if the {@link VendingMachineRemoveEvent} is cancelled. */
    public boolean remove(Player remover, VendingMachine machine) {
        ReentrantLock lock = plugin.locks().get(machine.id());
        lock.lock();
        try {
            VendingMachineRemoveEvent event = new VendingMachineRemoveEvent(remover, machine);
            plugin.getServer().getPluginManager().callEvent(event);
            if (event.isCancelled()) {
                return false;
            }
            machines.remove(machine.id());
            byLocation.remove(machine.location().toKey());
            plugin.adapters().storage().delete(machine.id());
            // Clear the materialised icon block back to air (only if we placed it).
            if (plugin.settings().placeIconBlock()) {
                Location loc = machine.location().toBukkitCenter();
                if (loc != null) {
                    loc.getBlock().setType(Material.AIR);
                }
            }
            plugin.adapters().logging().log("REMOVE", describe(remover) + " removed machine " + machine.id());
        } finally {
            lock.unlock();
        }
        // The machine's lock is intentionally retained (see MachineLocks): evicting it could let a
        // concurrent operation on the same id acquire a different lock instance and race the delete.
        return true;
    }

    /**
     * Changes a machine's state through the cancellable {@link VendingMachineStateChangeEvent}.
     * Returns {@code false} if the change was vetoed or was a no-op. Persists and logs on success.
     */
    public boolean changeState(VendingMachine machine, VendingMachineState newState) {
        VendingMachineState current = machine.state();
        if (current == newState) {
            return false;
        }
        ReentrantLock lock = plugin.locks().get(machine.id());
        lock.lock();
        try {
            VendingMachineStateChangeEvent event = new VendingMachineStateChangeEvent(machine, current, newState);
            plugin.getServer().getPluginManager().callEvent(event);
            if (event.isCancelled()) {
                return false;
            }
            machine.setState(newState);
            plugin.adapters().storage().save(machine);
            plugin.adapters().logging().log("STATE", "Machine " + machine.id() + " " + current + " -> " + newState);
            return true;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Recomputes the automatic ACTIVE/EMPTY transition (preserving manual BROKEN/DISABLED) and fires
     * a state change event if it differs. Call after stock changes.
     */
    public void refreshState(VendingMachine machine) {
        VendingMachineState current = machine.state();
        if (current == VendingMachineState.BROKEN || current == VendingMachineState.DISABLED) {
            return;
        }
        VendingMachineState target = machine.isOutOfStock() ? VendingMachineState.EMPTY : VendingMachineState.ACTIVE;
        if (target != current) {
            changeState(machine, target);
        }
    }

    /** Persists a machine after an in-place mutation by a transaction service. */
    public void save(VendingMachine machine) {
        plugin.adapters().storage().save(machine);
    }

    /** Persists every machine (used on disable / bulk operations). */
    public void saveAll() {
        plugin.adapters().storage().saveAll(all());
    }

    private void seedProducts(VendingMachine machine, MachineType type) {
        int slots = type.slots();
        int used = 0;
        boolean seedFull = plugin.settings().seedFullOnCreate();
        for (String productId : type.defaultProducts()) {
            if (used >= slots) {
                break;
            }
            ProductDefinition definition = plugin.products().get(productId);
            if (definition == null) {
                continue;
            }
            int capacity = definition.defaultMaxStock();
            int initialStock = seedFull ? capacity : 0;
            machine.putProduct(new MachineProduct(definition.id(), definition.defaultPrice(), initialStock, capacity));
            used++;
        }
    }

    private void index(VendingMachine machine) {
        machines.put(machine.id(), machine);
        byLocation.put(machine.location().toKey(), machine.id());
    }

    private static String describe(Player player) {
        return player == null ? "API" : player.getName();
    }

    public List<VendingMachine> nearby(Location location, double radius) {
        double radiusSquared = radius * radius;
        List<VendingMachine> result = new ArrayList<>();
        for (VendingMachine machine : machines.values()) {
            if (machine.location().distanceSquaredTo(location) <= radiusSquared) {
                result.add(machine);
            }
        }
        return result;
    }
}
