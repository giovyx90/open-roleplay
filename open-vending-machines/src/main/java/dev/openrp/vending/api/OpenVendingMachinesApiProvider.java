package dev.openrp.vending.api;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import dev.openrp.vending.OpenVendingMachinesPlugin;
import dev.openrp.vending.adapter.AdapterRegistry;
import dev.openrp.vending.core.PurchaseResult;
import dev.openrp.vending.core.RestockResult;
import dev.openrp.vending.core.WithdrawResult;
import dev.openrp.vending.hook.VendingHook;
import dev.openrp.vending.model.MachineLocation;
import dev.openrp.vending.model.MachineType;
import dev.openrp.vending.model.ProductDefinition;
import dev.openrp.vending.model.VendingMachine;
import dev.openrp.vending.model.VendingMachineState;

/** Default API implementation; delegates to the manager, services, hooks and adapter registry. */
public final class OpenVendingMachinesApiProvider implements OpenVendingMachinesApi, VendingMachineService {

    private final OpenVendingMachinesPlugin plugin;

    public OpenVendingMachinesApiProvider(OpenVendingMachinesPlugin plugin) {
        this.plugin = plugin;
    }

    // --- OpenVendingMachinesApi -------------------------------------------------------------

    @Override
    public AdapterRegistry adapters() {
        return plugin.adapters();
    }

    @Override
    public void registerHook(VendingHook hook) {
        plugin.hooks().register(hook);
    }

    @Override
    public void unregisterHook(VendingHook hook) {
        plugin.hooks().unregister(hook);
    }

    @Override
    public VendingMachineService machines() {
        return this;
    }

    @Override
    public Collection<MachineType> machineTypes() {
        return plugin.machineTypes().all();
    }

    @Override
    public MachineType machineType(String id) {
        return plugin.machineTypes().get(id);
    }

    @Override
    public Collection<ProductDefinition> products() {
        return plugin.products().all();
    }

    @Override
    public ProductDefinition product(String id) {
        return plugin.products().get(id);
    }

    @Override
    public Optional<VendingMachine> createMachine(Player creator, String typeId, Location location, String companyId) {
        MachineType type = plugin.machineTypes().get(typeId);
        if (type == null || location == null) {
            return Optional.empty();
        }
        if (plugin.machines().existsAt(location)) {
            return Optional.empty();
        }
        if (companyId != null && plugin.machines().isAtLimit(companyId)) {
            return Optional.empty();
        }
        return Optional.ofNullable(plugin.machines().create(creator, type, MachineLocation.of(location), companyId));
    }

    @Override
    public boolean removeMachine(Player remover, VendingMachine machine) {
        return machine != null && plugin.machines().remove(remover, machine);
    }

    @Override
    public boolean setState(VendingMachine machine, VendingMachineState state) {
        return machine != null && plugin.machines().changeState(machine, state);
    }

    @Override
    public PurchaseResult purchase(Player buyer, VendingMachine machine, String productId, int amount) {
        return plugin.purchases().purchase(buyer, machine, productId, amount);
    }

    @Override
    public RestockResult restock(Player staff, VendingMachine machine, String productId, int amount) {
        return plugin.restocks().restock(staff, machine, productId, amount);
    }

    @Override
    public boolean setPrice(Player staff, VendingMachine machine, String productId, double price) {
        return plugin.restocks().setPrice(staff, machine, productId, price);
    }

    @Override
    public WithdrawResult withdraw(Player staff, VendingMachine machine) {
        return plugin.cash().withdraw(staff, machine);
    }

    // --- VendingMachineService --------------------------------------------------------------

    @Override
    public Collection<VendingMachine> all() {
        return plugin.machines().all();
    }

    @Override
    public VendingMachine byId(UUID id) {
        return plugin.machines().get(id);
    }

    @Override
    public VendingMachine at(Location location) {
        return plugin.machines().getAt(location);
    }

    @Override
    public List<VendingMachine> nearby(Location location, double radius) {
        return plugin.machines().nearby(location, radius);
    }

    @Override
    public int count() {
        return plugin.machines().count();
    }

    @Override
    public long countOwnedBy(String companyId) {
        return plugin.machines().countOwnedBy(companyId);
    }
}
