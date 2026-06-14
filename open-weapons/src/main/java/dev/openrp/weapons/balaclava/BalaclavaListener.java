package dev.openrp.weapons.balaclava;

import dev.openrp.weapons.module.WeaponsModule;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;

public class BalaclavaListener implements Listener {
    private final WeaponsModule module;
    private final BalaclavaManager manager;

    public BalaclavaListener(WeaponsModule module) {
        this.module = module;
        this.manager = module.getBalaclavaManager();
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        checkPlayerMask(event.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        manager.setMasked(event.getPlayer().getUniqueId(), false);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getWhoClicked() instanceof Player player) {
            if (event.getSlotType() == InventoryType.SlotType.ARMOR
                    || manager.isBalaclava(event.getCurrentItem())
                    || manager.isBalaclava(event.getCursor())) {
                Bukkit.getScheduler().runTaskLater(module.getCore(), () -> checkPlayerMask(player), 1L);
            }
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getWhoClicked() instanceof Player player && manager.isBalaclava(event.getOldCursor())) {
            Bukkit.getScheduler().runTaskLater(module.getCore(), () -> checkPlayerMask(player), 1L);
        }
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (event.getAction().isRightClick()) {
            ItemStack item = event.getItem();
            if (manager.isBalaclava(item)) {
                event.setCancelled(true);
                // Equip if helmet is empty
                ItemStack currentHelmet = player.getInventory().getHelmet();
                if (currentHelmet == null || currentHelmet.getType() == org.bukkit.Material.AIR) {
                    ItemStack helmet = item.clone();
                    helmet.setAmount(1);
                    player.getInventory().setHelmet(helmet);
                    item.setAmount(item.getAmount() - 1);
                    Bukkit.getScheduler().runTaskLater(module.getCore(), () -> checkPlayerMask(player), 1L);
                }
            }
        }
    }

    private void checkPlayerMask(Player player) {
        ItemStack helmet = player.getInventory().getHelmet();
        boolean isMaskedNow = manager.isBalaclava(helmet);
        boolean wasMasked = manager.isMasked(player.getUniqueId());

        if (isMaskedNow != wasMasked) {
            manager.setMasked(player.getUniqueId(), isMaskedNow);
            
            if (isMaskedNow) {
                module.getIdentityBridge().applyAnonymous(player);
            } else {
                module.getIdentityBridge().refreshPlayer(player);
            }
        }
    }
}
