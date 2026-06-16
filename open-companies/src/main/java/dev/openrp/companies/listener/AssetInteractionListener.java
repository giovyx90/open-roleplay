package dev.openrp.companies.listener;

import java.util.Optional;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import dev.openrp.companies.OpenCompaniesPlugin;
import dev.openrp.companies.model.CompanyAsset;

/**
 * Turns a right-click on a registered company asset block into its on-screen menu. Mirrors the vending
 * module's machine listener: it resolves the clicked block to a {@link CompanyAsset} via the asset
 * registry's position index and, if one is there, cancels the interaction and opens the matching
 * screen. Blocks that are not registered assets are left entirely alone.
 */
public final class AssetInteractionListener implements Listener {

    private final OpenCompaniesPlugin plugin;

    public AssetInteractionListener(OpenCompaniesPlugin plugin) {
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
        Optional<CompanyAsset> asset = plugin.assets().assetAt(
                block.getWorld().getName(), block.getX(), block.getY(), block.getZ());
        if (asset.isEmpty()) {
            return;
        }
        event.setCancelled(true);
        plugin.menus().open(event.getPlayer(), asset.get());
    }
}
