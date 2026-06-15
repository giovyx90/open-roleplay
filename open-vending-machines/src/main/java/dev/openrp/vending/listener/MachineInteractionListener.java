package dev.openrp.vending.listener;

import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import dev.openrp.vending.OpenVendingMachinesPlugin;
import dev.openrp.vending.core.Authorization;
import dev.openrp.vending.model.VendingMachine;

/**
 * Connects the world to the GUI: right-clicking a machine block opens the buy view (or, when
 * sneaking and authorized, the management view), and machine blocks are protected from being broken
 * so they cannot be orphaned - removal goes through {@code /ovm remove}.
 */
public final class MachineInteractionListener implements Listener {

    private final OpenVendingMachinesPlugin plugin;

    public MachineInteractionListener(OpenVendingMachinesPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        Block block = event.getClickedBlock();
        if (block == null) {
            return;
        }
        VendingMachine machine = plugin.machines().getAt(block.getLocation());
        if (machine == null) {
            return;
        }
        event.setCancelled(true);
        openMachine(plugin, event.getPlayer(), machine);
    }

    /**
     * Shared entry point used by every interaction front-end (block click, furniture entity click,
     * ...): enforces the use permission, then opens the management view for sneaking authorized staff
     * or the buy view for everyone else.
     */
    public static void openMachine(OpenVendingMachinesPlugin plugin, Player player, VendingMachine machine) {
        if (!Authorization.canUse(plugin, player)) {
            plugin.messages().warning(player, "general.no_permission");
            return;
        }
        boolean management = player.isSneaking()
                && (Authorization.canRestock(plugin, player, machine)
                || Authorization.canWithdraw(plugin, player, machine)
                || Authorization.canEditPrice(plugin, player, machine));
        if (management) {
            plugin.userInterface().openManagement(player, machine);
        } else {
            plugin.userInterface().openPurchase(player, machine);
        }
    }

    @EventHandler
    public void onBreak(BlockBreakEvent event) {
        VendingMachine machine = plugin.machines().getAt(event.getBlock().getLocation());
        if (machine == null) {
            return;
        }
        event.setCancelled(true);
        plugin.messages().warning(event.getPlayer(), "machine.protected");
    }
}
