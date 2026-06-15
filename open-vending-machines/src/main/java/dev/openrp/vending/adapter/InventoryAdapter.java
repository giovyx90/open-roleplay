package dev.openrp.vending.adapter;

import org.bukkit.entity.Player;
import dev.openrp.vending.model.ProductDefinition;

/**
 * Bridge to however a server stores items. The full {@link ProductDefinition} is passed so a custom
 * implementation can build / match items its own way (custom model data, a custom item plugin,
 * weightless "virtual" inventories, ...) while still keying off {@link ProductDefinition#id()}.
 *
 * <p>{@code give}/{@code take} must be all-or-nothing: if the full {@code amount} cannot be moved,
 * return {@code false} and leave the player's inventory unchanged.</p>
 */
public interface InventoryAdapter {

    String id();

    /** Whether the player has room to receive {@code amount} units (no items moved). */
    boolean canReceive(Player player, ProductDefinition product, int amount);

    /** Delivers the product to the player; returns {@code false} (no change) if it could not. */
    boolean give(Player player, ProductDefinition product, int amount);

    /** Whether the player currently holds at least {@code amount} units (no items moved). */
    boolean has(Player player, ProductDefinition product, int amount);

    /** Consumes {@code amount} units from the player; returns {@code false} (no change) if it could not. */
    boolean take(Player player, ProductDefinition product, int amount);
}
