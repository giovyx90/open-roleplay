package dev.openrp.vending.hook;

import org.bukkit.entity.Player;
import dev.openrp.vending.model.MachineProduct;
import dev.openrp.vending.model.VendingMachine;

/**
 * Synchronous callback surface for tight integrations - a lighter-weight alternative to listening
 * for {@code event.*} Bukkit events when you need to <em>influence</em> (not just observe) an action
 * and want a return value.
 *
 * <p>Every method has a permissive default, so implement only the ones you care about and register
 * with {@code OpenVendingMachinesApi#registerHook}. Gate methods returning a denied
 * {@link VendingDecision} stop the action immediately. Resolver methods are folded in registration
 * order, each receiving the previous hook's result.</p>
 */
public interface VendingHook {

    default VendingDecision canPlayerUseMachine(Player player, VendingMachine machine) {
        return VendingDecision.allow();
    }

    default VendingDecision canPlayerRestockMachine(Player player, VendingMachine machine) {
        return VendingDecision.allow();
    }

    default VendingDecision canPlayerWithdrawCash(Player player, VendingMachine machine) {
        return VendingDecision.allow();
    }

    /** Adjust the price a specific player pays for a product; return {@code currentPrice} to leave it. */
    default double resolveProductPrice(Player player, VendingMachine machine, MachineProduct product, double currentPrice) {
        return currentPrice;
    }

    /** Adjust how many machines a company may own; return {@code currentLimit} to leave it. */
    default int resolveCompanyLimit(String companyId, int currentLimit) {
        return currentLimit;
    }

    /** Last gate before money/items move. Returning a deny aborts the purchase cleanly. */
    default VendingDecision beforePayment(PurchaseContext context) {
        return VendingDecision.allow();
    }

    /** Fired after money has moved, the item has been delivered and stock reduced. */
    default void afterPayment(PurchaseContext context) {
    }
}
