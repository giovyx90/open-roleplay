package dev.openrp.vending.core;

import java.util.concurrent.locks.ReentrantLock;
import org.bukkit.entity.Player;
import dev.openrp.vending.OpenVendingMachinesPlugin;
import dev.openrp.vending.config.RestockMode;
import dev.openrp.vending.event.VendingMachineRestockEvent;
import dev.openrp.vending.hook.VendingDecision;
import dev.openrp.vending.model.MachineProduct;
import dev.openrp.vending.model.ProductDefinition;
import dev.openrp.vending.model.VendingMachine;

/**
 * Refills a machine and edits prices under the machine lock. Restock quantity is clamped to the
 * configured per-action maximum and to remaining capacity. In any mode other than {@code FREE} the
 * items are sourced through the {@code InventoryAdapter} (the default consumes the staff member's
 * inventory; a custom warehouse-backed adapter pulls from the company stock). Items are only taken
 * after the cancellable {@link VendingMachineRestockEvent} passes, so a vetoed refill costs nothing.
 */
public final class RestockService {

    private final OpenVendingMachinesPlugin plugin;

    public RestockService(OpenVendingMachinesPlugin plugin) {
        this.plugin = plugin;
    }

    public RestockResult restock(Player staff, VendingMachine machine, String productId, int amount) {
        int requested = Math.min(Math.max(1, amount), plugin.settings().restockMaxPerAction());
        ReentrantLock lock = plugin.locks().get(machine.id());
        lock.lock();
        try {
            if (!Authorization.canRestock(plugin, staff, machine) || plugin.hooks().canRestock(staff, machine).denied()) {
                plugin.messages().warning(staff, "restock.denied");
                return RestockResult.fail(productId);
            }
            MachineProduct product = machine.product(productId);
            ProductDefinition definition = plugin.products().get(productId);
            if (product == null || definition == null) {
                plugin.messages().warning(staff, "restock.unknown_product", "product", productId);
                return RestockResult.fail(productId);
            }
            String productName = definition.plainName();
            int free = product.freeSpace();
            if (free <= 0) {
                plugin.messages().warning(staff, "restock.full", "product", productName);
                return RestockResult.fail(productId);
            }
            int addable = Math.min(requested, free);

            VendingMachineRestockEvent event = new VendingMachineRestockEvent(staff, machine, productId, addable);
            plugin.getServer().getPluginManager().callEvent(event);
            if (event.isCancelled()) {
                return RestockResult.fail(productId);
            }

            if (plugin.settings().restockMode() != RestockMode.FREE
                    && !plugin.adapters().inventory().take(staff, definition, addable)) {
                plugin.messages().warning(staff, "restock.no_items", "amount", addable, "product", productName);
                return RestockResult.fail(productId);
            }

            int added = product.addStock(addable);
            plugin.machines().save(machine);
            plugin.adapters().logging().log("RESTOCK",
                    staff.getName() + " restocked " + added + "x " + productId + " into machine " + machine.id());
            plugin.messages().success(staff, "restock.success",
                    "amount", added, "product", productName, "stock", product.stock(), "capacity", product.capacity());
            plugin.machines().refreshState(machine);
            return RestockResult.ok(productId, added, product.stock(), product.capacity());
        } finally {
            lock.unlock();
        }
    }

    public boolean setPrice(Player staff, VendingMachine machine, String productId, double price) {
        if (!plugin.settings().allowPriceEditing()) {
            plugin.messages().warning(staff, "price.disabled");
            return false;
        }
        ReentrantLock lock = plugin.locks().get(machine.id());
        lock.lock();
        try {
            if (!Authorization.canEditPrice(plugin, staff, machine)) {
                plugin.messages().warning(staff, "price.denied");
                return false;
            }
            MachineProduct product = machine.product(productId);
            if (product == null) {
                plugin.messages().warning(staff, "restock.unknown_product", "product", productId);
                return false;
            }
            product.setPrice(Math.max(0.0, price));
            plugin.machines().save(machine);
            plugin.adapters().logging().log("PRICE",
                    staff.getName() + " set price of " + productId + " to " + Money.format(product.price())
                            + " on machine " + machine.id());
            ProductDefinition definition = plugin.products().get(productId);
            String productName = definition == null ? productId : definition.plainName();
            plugin.messages().success(staff, "price.updated",
                    "product", productName, "symbol", plugin.settings().currencySymbol(), "price", Money.format(product.price()));
            return true;
        } finally {
            lock.unlock();
        }
    }
}
