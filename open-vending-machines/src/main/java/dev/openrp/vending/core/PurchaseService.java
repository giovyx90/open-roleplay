package dev.openrp.vending.core;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.locks.ReentrantLock;
import org.bukkit.entity.Player;
import dev.openrp.vending.OpenVendingMachinesPlugin;
import dev.openrp.vending.event.PurchaseFailReason;
import dev.openrp.vending.event.VendingMachinePurchaseAttemptEvent;
import dev.openrp.vending.event.VendingMachinePurchaseFailEvent;
import dev.openrp.vending.event.VendingMachinePurchaseSuccessEvent;
import dev.openrp.vending.hook.PurchaseContext;
import dev.openrp.vending.hook.VendingDecision;
import dev.openrp.vending.model.MachineProduct;
import dev.openrp.vending.model.ProductDefinition;
import dev.openrp.vending.model.VendingMachine;
import dev.openrp.vending.model.VendingMachineState;

/**
 * Executes a purchase under the machine lock with full server-side validation. Nothing here trusts
 * client input: distance, state, cooldown, stock, price, inventory space, funds, hooks and a
 * cancellable event are all checked, in that order, before any money or item moves. Money is
 * withdrawn before the item is given, and refunded if delivery fails - so a sale can never duplicate
 * or lose value.
 */
public final class PurchaseService {

    private final OpenVendingMachinesPlugin plugin;

    public PurchaseService(OpenVendingMachinesPlugin plugin) {
        this.plugin = plugin;
    }

    public PurchaseResult purchase(Player buyer, VendingMachine machine, String productId, int amount) {
        int qty = Math.max(1, amount);
        String symbol = plugin.settings().currencySymbol();
        ReentrantLock lock = plugin.locks().get(machine.id());
        lock.lock();
        try {
            if (!machine.state().canSell()) {
                String stateLabel = plugin.messages().text(buyer, machine.state().messageKey());
                return deny(buyer, machine, productId, PurchaseFailReason.STATE, "purchase.fail_state", "state", stateLabel);
            }
            if (plugin.cooldowns().isOnCooldown(buyer.getUniqueId(), plugin.settings().cooldownMillis())) {
                return deny(buyer, machine, productId, PurchaseFailReason.COOLDOWN, "purchase.fail_cooldown");
            }
            plugin.cooldowns().mark(buyer.getUniqueId());

            if (machine.location().distanceSquaredTo(buyer.getLocation()) > plugin.settings().maxInteractionDistanceSquared()) {
                return deny(buyer, machine, productId, PurchaseFailReason.DISTANCE, "purchase.fail_distance");
            }

            VendingDecision use = plugin.hooks().canUse(buyer, machine);
            if (use.denied()) {
                return deny(buyer, machine, productId, PurchaseFailReason.DENIED, "purchase.fail_denied", "reason", use.reason());
            }

            MachineProduct product = machine.product(productId);
            ProductDefinition definition = plugin.products().get(productId);
            if (product == null || definition == null) {
                return deny(buyer, machine, productId, PurchaseFailReason.UNKNOWN_PRODUCT, "purchase.fail_unknown_product");
            }
            String productName = definition.plainName();

            if (product.stock() < qty) {
                return deny(buyer, machine, productId, PurchaseFailReason.STOCK, "purchase.fail_stock", "product", productName);
            }

            double unitPrice = Money.round(plugin.hooks().resolvePrice(buyer, machine, product, product.price()));
            double total = Money.round(unitPrice * qty);

            if (plugin.settings().failIfInventoryFull() && !plugin.adapters().inventory().canReceive(buyer, definition, qty)) {
                return deny(buyer, machine, productId, PurchaseFailReason.INVENTORY_FULL, "purchase.fail_inventory_full");
            }

            String account = plugin.settings().paymentAccount();
            if (!plugin.adapters().economy().has(buyer, account, total)) {
                return deny(buyer, machine, productId, PurchaseFailReason.FUNDS, "purchase.fail_funds", "symbol", symbol, "price", Money.format(total));
            }

            PurchaseContext context = new PurchaseContext(buyer, machine, product, definition, qty, unitPrice);
            VendingDecision before = plugin.hooks().beforePayment(context);
            if (before.denied()) {
                return deny(buyer, machine, productId, PurchaseFailReason.DENIED, "purchase.fail_denied", "reason", before.reason());
            }

            VendingMachinePurchaseAttemptEvent attempt =
                    new VendingMachinePurchaseAttemptEvent(buyer, machine, productId, qty, unitPrice);
            plugin.getServer().getPluginManager().callEvent(attempt);
            if (attempt.isCancelled()) {
                return deny(buyer, machine, productId, PurchaseFailReason.DENIED, "purchase.fail_denied", "reason", "cancelled");
            }

            // ---- critical section: money out, item in, stock down, cash box up ----
            if (!plugin.adapters().economy().withdraw(buyer, account, total)) {
                return deny(buyer, machine, productId, PurchaseFailReason.FUNDS, "purchase.fail_funds", "symbol", symbol, "price", Money.format(total));
            }
            if (!plugin.adapters().inventory().give(buyer, definition, qty)) {
                plugin.adapters().economy().deposit(buyer, account, total); // delivery failed: refund
                return deny(buyer, machine, productId, PurchaseFailReason.INVENTORY_FULL, "purchase.fail_inventory_full");
            }
            product.removeStock(qty);
            machine.depositCash(total);
            plugin.machines().save(machine);

            plugin.adapters().logging().log("PURCHASE", buyer.getName() + " bought " + qty + "x " + productId
                    + " for " + symbol + Money.format(total) + " from machine " + machine.id());
            plugin.messages().success(buyer, "purchase.success",
                    "amount", qty, "product", productName, "symbol", symbol, "price", Money.format(total));

            plugin.getServer().getPluginManager().callEvent(
                    new VendingMachinePurchaseSuccessEvent(buyer, machine, productId, qty, total));
            plugin.hooks().afterPayment(context);

            maybeBreakdown(machine);
            plugin.machines().refreshState(machine);
            return PurchaseResult.ok(productId, qty, total);
        } finally {
            lock.unlock();
        }
    }

    private void maybeBreakdown(VendingMachine machine) {
        double chance = plugin.settings().randomBreakdownChance();
        if (chance > 0 && ThreadLocalRandom.current().nextDouble() < chance) {
            plugin.machines().changeState(machine, VendingMachineState.BROKEN);
        }
    }

    private PurchaseResult deny(Player buyer, VendingMachine machine, String productId, PurchaseFailReason reason,
                                String messageKey, Object... placeholders) {
        plugin.getServer().getPluginManager().callEvent(
                new VendingMachinePurchaseFailEvent(buyer, machine, productId, reason));
        if (buyer != null) {
            plugin.messages().warning(buyer, messageKey, placeholders);
        }
        return PurchaseResult.fail(reason, productId);
    }
}
