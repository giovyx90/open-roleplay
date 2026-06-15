package dev.openrp.vending.gui;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

/**
 * {@link InventoryHolder} that tags an open GUI as belonging to Open Vending Machines, remembers
 * which machine and view it is, and maps clicked slots back to product ids. Using a holder (rather
 * than matching titles) is the robust way to recognise our own inventories in the click listener.
 */
public final class VendingGuiHolder implements InventoryHolder {

    public enum View {
        PURCHASE,
        MANAGE
    }

    private final UUID machineId;
    private final View view;
    private final Map<Integer, String> productBySlot = new HashMap<>();
    private Inventory inventory;

    public VendingGuiHolder(UUID machineId, View view) {
        this.machineId = machineId;
        this.view = view;
    }

    public UUID machineId() {
        return machineId;
    }

    public View view() {
        return view;
    }

    public void mapProduct(int slot, String productId) {
        productBySlot.put(slot, productId);
    }

    /** Product id at the slot, or {@code null} if the slot is not a product. */
    public String productAt(int slot) {
        return productBySlot.get(slot);
    }

    void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
