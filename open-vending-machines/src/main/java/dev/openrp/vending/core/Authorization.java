package dev.openrp.vending.core;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import dev.openrp.vending.OpenVendingMachinesPlugin;
import dev.openrp.vending.adapter.BusinessCapability;
import dev.openrp.vending.model.VendingMachine;

/**
 * Capability checks for machine actions, combining the {@code PermissionAdapter} (node-level access)
 * with the {@code BusinessAdapter} (company role capabilities). {@code openvending.admin} bypasses
 * company checks. For machines without an owner, company capabilities do not apply and only the
 * permission node (or admin) is required.
 */
public final class Authorization {

    private Authorization() {
    }

    public static boolean isAdmin(OpenVendingMachinesPlugin plugin, CommandSender sender) {
        return plugin.adapters().permission().has(sender, "openvending.admin");
    }

    public static boolean canUse(OpenVendingMachinesPlugin plugin, Player player) {
        return plugin.adapters().permission().has(player, "openvending.use");
    }

    public static boolean canRestock(OpenVendingMachinesPlugin plugin, Player player, VendingMachine machine) {
        return canManage(plugin, player, machine, "openvending.restock", BusinessCapability.RESTOCK);
    }

    public static boolean canWithdraw(OpenVendingMachinesPlugin plugin, Player player, VendingMachine machine) {
        return canManage(plugin, player, machine, "openvending.withdraw", BusinessCapability.WITHDRAW);
    }

    public static boolean canRemove(OpenVendingMachinesPlugin plugin, Player player, VendingMachine machine) {
        if (isAdmin(plugin, player)) {
            return true;
        }
        // Removing an owned machine needs the company MANAGE capability; unowned machines are admin-only.
        return machine.hasOwner()
                && plugin.adapters().permission().has(player, "openvending.remove")
                && plugin.adapters().business().hasCapability(player, machine.ownerCompanyId().orElseThrow(), BusinessCapability.MANAGE);
    }

    public static boolean canEditPrice(OpenVendingMachinesPlugin plugin, Player player, VendingMachine machine) {
        if (isAdmin(plugin, player)) {
            return true;
        }
        // Price edits on unowned (server) machines are admin-only.
        return machine.hasOwner()
                && plugin.adapters().permission().has(player, "openvending.restock")
                && plugin.adapters().business().hasCapability(player, machine.ownerCompanyId().orElseThrow(), BusinessCapability.EDIT_PRICE);
    }

    private static boolean canManage(OpenVendingMachinesPlugin plugin, Player player, VendingMachine machine,
                                     String permission, BusinessCapability capability) {
        if (!plugin.adapters().permission().has(player, permission)) {
            return false;
        }
        if (isAdmin(plugin, player)) {
            return true;
        }
        if (machine.hasOwner()) {
            return plugin.adapters().business().hasCapability(player, machine.ownerCompanyId().orElseThrow(), capability);
        }
        // Unowned/server machine: the permission node alone is enough.
        return true;
    }
}
