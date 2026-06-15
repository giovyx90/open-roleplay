package dev.openrp.vending.hook;

import org.bukkit.entity.Player;
import dev.openrp.vending.model.MachineProduct;
import dev.openrp.vending.model.ProductDefinition;
import dev.openrp.vending.model.VendingMachine;

/**
 * Immutable snapshot of an in-flight purchase, handed to {@link VendingHook#beforePayment} and
 * {@link VendingHook#afterPayment}. The price is the already-resolved unit price (after
 * {@link VendingHook#resolveProductPrice}); {@link #totalPrice()} is {@code unitPrice * amount}.
 */
public record PurchaseContext(
        Player player,
        VendingMachine machine,
        MachineProduct product,
        ProductDefinition definition,
        int amount,
        double unitPrice) {

    public double totalPrice() {
        return unitPrice * amount;
    }
}
