package dev.openrp.companies.gui;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;

/**
 * Single dispatcher for every {@link Menu}. While one of our menus is the open top inventory, all
 * clicks and drags are cancelled (so items can never be taken from or dropped into a device screen),
 * and clicks landing on the menu's own slots are forwarded to {@link Menu#onClick}.
 */
public final class MenuListener implements Listener {

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getView().getTopInventory().getHolder() instanceof Menu menu)) {
            return;
        }
        // Cancel everything (including shift-clicks from the player's own inventory) while a device
        // screen is open; only forward genuine clicks on the device's slots.
        event.setCancelled(true);
        if (event.getClickedInventory() == event.getView().getTopInventory()
                && event.getWhoClicked() instanceof Player) {
            menu.onClick(event);
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof Menu) {
            event.setCancelled(true);
        }
    }
}
