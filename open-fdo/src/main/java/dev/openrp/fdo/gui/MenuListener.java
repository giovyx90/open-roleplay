package dev.openrp.fdo.gui;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import dev.openrp.fdo.OpenFdoPlugin;

/** Handles clicks in the act-picker menu: cancels item movement and issues the chosen act's book. */
public final class MenuListener implements Listener {

    private final OpenFdoPlugin plugin;

    public MenuListener(OpenFdoPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof ActMenuHolder holder)) {
            return;
        }
        event.setCancelled(true);
        if (event.getClickedInventory() == null
                || event.getClickedInventory() != event.getView().getTopInventory()) {
            return;
        }
        String actId = holder.actAt(event.getRawSlot());
        if (actId == null || !(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        player.closeInventory();
        send(player, actId);
    }

    private void send(Player player, String actId) {
        var result = plugin.acts().beginAct(player, actId, null);
        if (result.success()) {
            plugin.messages().success(player, result.messageKey(), result.placeholders());
        } else {
            plugin.messages().warning(player, result.messageKey(), result.placeholders());
        }
    }
}
