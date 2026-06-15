package dev.openrp.vending.event;

/**
 * Why a purchase did not complete. Shared by the core purchase result and
 * {@link VendingMachinePurchaseFailEvent} so observers can react to a stable, typed reason rather
 * than parsing a message.
 */
public enum PurchaseFailReason {
    /** Buyer was outside the machine's interaction distance. */
    DISTANCE,
    /** The requested product was out of stock. */
    STOCK,
    /** Buyer could not afford the price. */
    FUNDS,
    /** Machine was empty, broken or disabled. */
    STATE,
    /** Buyer is on the anti-spam cooldown. */
    COOLDOWN,
    /** Buyer's inventory had no room for the item. */
    INVENTORY_FULL,
    /** A Bukkit event was cancelled or a hook denied the purchase. */
    DENIED,
    /** The product id is no longer present on the machine. */
    UNKNOWN_PRODUCT,
    /** Catch-all for unexpected failures. */
    OTHER
}
