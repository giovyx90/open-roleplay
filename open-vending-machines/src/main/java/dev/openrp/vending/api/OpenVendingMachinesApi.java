package dev.openrp.vending.api;

import java.util.Collection;
import java.util.Optional;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import dev.openrp.vending.adapter.AdapterRegistry;
import dev.openrp.vending.core.PurchaseResult;
import dev.openrp.vending.core.RestockResult;
import dev.openrp.vending.core.WithdrawResult;
import dev.openrp.vending.hook.VendingHook;
import dev.openrp.vending.model.MachineType;
import dev.openrp.vending.model.ProductDefinition;
import dev.openrp.vending.model.VendingMachine;
import dev.openrp.vending.model.VendingMachineState;

/**
 * Public entry point, retrieved from Bukkit's services manager:
 * <pre>{@code
 * OpenVendingMachinesApi api = Bukkit.getServicesManager().load(OpenVendingMachinesApi.class);
 * api.adapters().setEconomy(new MyEconomyAdapter());
 * api.registerHook(new MyHook());
 * }</pre>
 *
 * <p>Through {@link #adapters()} you replace any integration; through {@link #registerHook} you
 * influence decisions; the transaction methods run the same validated, locked code paths the GUI and
 * commands use, so calling them programmatically is just as safe.</p>
 */
public interface OpenVendingMachinesApi {

    /** The live adapter set - swap any adapter here to integrate your own systems. */
    AdapterRegistry adapters();

    void registerHook(VendingHook hook);

    void unregisterHook(VendingHook hook);

    /** Read-only queries over existing machines. */
    VendingMachineService machines();

    Collection<MachineType> machineTypes();

    MachineType machineType(String id);

    Collection<ProductDefinition> products();

    ProductDefinition product(String id);

    /** Places a machine; empty if the type is unknown, the block is occupied or the company is at its limit. */
    Optional<VendingMachine> createMachine(Player creator, String typeId, Location location, String companyId);

    boolean removeMachine(Player remover, VendingMachine machine);

    boolean setState(VendingMachine machine, VendingMachineState state);

    PurchaseResult purchase(Player buyer, VendingMachine machine, String productId, int amount);

    RestockResult restock(Player staff, VendingMachine machine, String productId, int amount);

    boolean setPrice(Player staff, VendingMachine machine, String productId, double price);

    WithdrawResult withdraw(Player staff, VendingMachine machine);
}
