package dev.openrp.companies.gui;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

/**
 * Base class for every Open Companies chest GUI - the on-screen surface of a physical device (POS,
 * company terminal, ATM). Being an {@link InventoryHolder}, the single {@link MenuListener} can detect
 * any of our menus from the open inventory and route clicks to {@link #onClick(InventoryClickEvent)}.
 * Subclasses build their inventory with {@code this} as the holder. Keeping all input on inventory
 * clicks is what makes the economy diegetic: the player operates a device, never types a command.
 */
public abstract class Menu implements InventoryHolder {

    protected Inventory inventory;

    /** Handles a click on a slot in this menu's inventory. The click is already cancelled. */
    public abstract void onClick(InventoryClickEvent event);

    public void open(Player player) {
        player.openInventory(getInventory());
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
