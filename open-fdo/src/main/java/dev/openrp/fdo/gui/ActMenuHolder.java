package dev.openrp.fdo.gui;

import java.util.HashMap;
import java.util.Map;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

/** Marker holder identifying the {@code /atto nuovo} act-picker inventory and mapping slots to act ids. */
public final class ActMenuHolder implements InventoryHolder {

    private final Map<Integer, String> slotToAct = new HashMap<>();
    private Inventory inventory;

    void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    void bind(int slot, String actId) {
        slotToAct.put(slot, actId);
    }

    public String actAt(int slot) {
        return slotToAct.get(slot);
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
