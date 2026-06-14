package dev.openrp.access;

import dev.openrp.access.model.AccessAction;
import dev.openrp.access.model.AccessCheckResult;
import dev.openrp.access.model.AccessDecision;
import dev.openrp.access.model.AccessProfile;
import dev.openrp.access.util.AccessMessages;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;

import java.util.EnumSet;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

public class AccessListener implements Listener {

    private final AccessService service;
    private final Set<Material> configuredMaterials;

    public AccessListener(AccessService service, Set<Material> configuredMaterials) {
        this.service = service;
        this.configuredMaterials = configuredMaterials == null ? Set.of() : Set.copyOf(configuredMaterials);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() != null && event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        Block block = event.getClickedBlock();
        if (block == null || block.getWorld() == null) {
            return;
        }
        Player player = event.getPlayer();
        Optional<AccessProfile> profile = service.findProfileAt(block.getLocation());
        if (profile.isEmpty()) {
            return;
        }
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK && player.isSneaking()) {
            if (service.canManage(player, profile.get())) {
                event.setCancelled(true);
                service.openEditor(player, profile.get(), block.getLocation());
                return;
            }
        }

        AccessAction action = actionFor(event.getAction(), block.getType());
        if (action == null) {
            return;
        }
        AccessCheckResult result = service.resolve(player, block.getLocation(), action);
        if (result.decision() == AccessDecision.DENY) {
            event.setCancelled(true);
            player.sendMessage(AccessMessages.error("Access", result.reason()));
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onInventoryOpen(InventoryOpenEvent event) {
        if (!(event.getPlayer() instanceof Player player)) {
            return;
        }
        Location location = event.getInventory().getLocation();
        if (location == null || location.getWorld() == null) {
            return;
        }
        AccessCheckResult result = service.resolve(player, location, AccessAction.CONTAINER);
        if (result.decision() == AccessDecision.DENY) {
            event.setCancelled(true);
            player.sendMessage(AccessMessages.error("Access", result.reason()));
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onBlockPlace(BlockPlaceEvent event) {
        AccessCheckResult result = service.resolve(event.getPlayer(), event.getBlockPlaced().getLocation(), AccessAction.PLACE);
        if (result.decision() == AccessDecision.DENY) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(AccessMessages.error("Access", result.reason()));
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onBlockBreak(BlockBreakEvent event) {
        AccessCheckResult result = service.resolve(event.getPlayer(), event.getBlock().getLocation(), AccessAction.BREAK);
        if (result.decision() == AccessDecision.DENY) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(AccessMessages.error("Access", result.reason()));
        }
    }

    private AccessAction actionFor(Action action, Material material) {
        if (action == Action.PHYSICAL && isSignal(material)) {
            return AccessAction.SIGNAL;
        }
        if (action != Action.RIGHT_CLICK_BLOCK && action != Action.LEFT_CLICK_BLOCK) {
            return null;
        }
        if (isContainer(material)) {
            return AccessAction.CONTAINER;
        }
        if (isOpenable(material)) {
            return AccessAction.OPEN;
        }
        if (isSignal(material)) {
            return AccessAction.SIGNAL;
        }
        if (configuredMaterials.contains(material) || isLikelyCustomInteractive(material)) {
            return AccessAction.MACHINE;
        }
        return null;
    }

    private boolean isContainer(Material material) {
        if (material == null) {
            return false;
        }
        return material == Material.CHEST
                || material == Material.TRAPPED_CHEST
                || material == Material.BARREL
                || material == Material.FURNACE
                || material == Material.BLAST_FURNACE
                || material == Material.SMOKER
                || material == Material.HOPPER
                || material == Material.DISPENSER
                || material == Material.DROPPER
                || Tag.SHULKER_BOXES.isTagged(material);
    }

    private boolean isOpenable(Material material) {
        return material != null
                && (Tag.DOORS.isTagged(material)
                || Tag.TRAPDOORS.isTagged(material)
                || Tag.FENCE_GATES.isTagged(material));
    }

    private boolean isSignal(Material material) {
        if (material == null) {
            return false;
        }
        String name = material.name();
        return Tag.BUTTONS.isTagged(material)
                || name.endsWith("_PRESSURE_PLATE")
                || name.endsWith("_BUTTON")
                || material == Material.LEVER;
    }

    private boolean isLikelyCustomInteractive(Material material) {
        if (material == null) {
            return false;
        }
        return EnumSet.of(Material.NOTE_BLOCK, Material.BARRIER, Material.LIGHT, Material.STRUCTURE_VOID)
                .contains(material);
    }

    public static Set<Material> parseConfiguredMaterials(Iterable<String> rawValues) {
        Set<Material> materials = EnumSet.noneOf(Material.class);
        if (rawValues == null) {
            return materials;
        }
        for (String raw : rawValues) {
            if (raw == null || raw.isBlank()) {
                continue;
            }
            try {
                materials.add(Material.valueOf(raw.trim().toUpperCase(Locale.ROOT)));
            } catch (IllegalArgumentException ignored) {
            }
        }
        return materials;
    }
}
