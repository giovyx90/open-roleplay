package dev.openrp.weapons.handcuffs;

import dev.openrp.weapons.module.WeaponsModule;
import dev.openrp.weapons.util.JumpRestrictionManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import com.destroystokyo.paper.event.player.PlayerJumpEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class HandcuffListener implements Listener {
    private final WeaponsModule module;
    private final HandcuffManager manager;
    private final Map<UUID, Long> actionTask = new HashMap<>();

    public HandcuffListener(WeaponsModule module) {
        this.module = module;
        this.manager = module.getHandcuffManager();
    }

    @EventHandler
    public void onInteractEntity(PlayerInteractEntityEvent event) {
        if (manager.isRestrained(event.getPlayer())) {
            manager.ensureBoundRestraintItem(event.getPlayer());
            event.setCancelled(true);
            return;
        }
        
        if (!(event.getRightClicked() instanceof Player target)) return;
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();

        if (manager.isHandcuffItem(item)) {
            event.setCancelled(true);
            if (manager.isRestrained(target)) {
                if (manager.getRestraintType(target) == RestraintType.ROPE) {
                    player.sendActionBar(Component.text("Use scissors to cut rope.", NamedTextColor.RED));
                    return;
                }
                startUncuffing(player, target);
                return;
            }

            if (actionTask.containsKey(player.getUniqueId())) return;

            player.sendMessage(Component.text("Applying handcuffs...", NamedTextColor.YELLOW));
            target.sendActionBar(Component.text("Someone is handcuffing you!", NamedTextColor.RED));
            
            long startTime = System.currentTimeMillis();
            long endTime = startTime + module.getUtilitySettings().handcuffDurationMillis();
            actionTask.put(player.getUniqueId(), endTime);

            new BukkitRunnable() {
                int ticks = 0;
                @Override
                public void run() {
                    if (!player.isOnline() || !target.isOnline() || player.getLocation().distance(target.getLocation()) > 4) {
                        actionTask.remove(player.getUniqueId());
                        player.sendActionBar(Component.text("Handcuffing cancelled.", NamedTextColor.RED));
                        cancel();
                        return;
                    }
                    
                    if (ticks % 10 == 0) {
                        player.playSound(player.getLocation(), Sound.BLOCK_IRON_TRAPDOOR_OPEN, 0.5f, 0.5f);
                        player.sendActionBar(progress("Handcuffing", startTime, endTime));
                        target.sendActionBar(progress("Being handcuffed", startTime, endTime));
                    }

                    if (System.currentTimeMillis() >= endTime) {
                        actionTask.remove(player.getUniqueId());
                        
                        // Handcuffs are unlimited use — no consumption
                        manager.handcuff(target, player);
                        player.playSound(player.getLocation(), Sound.BLOCK_IRON_TRAPDOOR_CLOSE, 1.0f, 1.5f);
                        target.playSound(target.getLocation(), Sound.BLOCK_IRON_TRAPDOOR_CLOSE, 1.0f, 1.5f);
                        
                        player.sendMessage(Component.text("You handcuffed " + target.getName(), NamedTextColor.GREEN));
                        target.sendMessage(Component.text("You have been handcuffed!", NamedTextColor.RED));
                        cancel();
                    }
                    ticks++;
                }
            }.runTaskTimer(module.getCore(), 0L, 1L);
            
        } else if (manager.isBoltCutterItem(item)) {
            event.setCancelled(true);
            if (!manager.isHandcuffed(target)) return;
            startUncuffing(player, target);
        }
    }

    private void startUncuffing(Player player, Player target) {
        if (actionTask.containsKey(player.getUniqueId())) return;

        player.sendMessage(Component.text("Removing handcuffs...", NamedTextColor.YELLOW));

        long startTime = System.currentTimeMillis();
        long endTime = startTime + module.getUtilitySettings().handcuffDurationMillis();
        actionTask.put(player.getUniqueId(), endTime);

        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (!player.isOnline() || !target.isOnline() || player.getLocation().distance(target.getLocation()) > 4) {
                    actionTask.remove(player.getUniqueId());
                    player.sendActionBar(Component.text("Uncuffing cancelled.", NamedTextColor.RED));
                    cancel();
                    return;
                }

                if (ticks % 10 == 0) {
                    player.playSound(player.getLocation(), Sound.ENTITY_SHEEP_SHEAR, 1.0f, 0.5f);
                    player.sendActionBar(progress("Uncuffing", startTime, endTime));
                    target.sendActionBar(progress("Being uncuffed", startTime, endTime));
                }

                if (System.currentTimeMillis() >= endTime) {
                    manager.uncuff(target);
                    actionTask.remove(player.getUniqueId());

                    player.playSound(player.getLocation(), Sound.BLOCK_CHAIN_BREAK, 1.0f, 1.0f);
                    target.playSound(target.getLocation(), Sound.BLOCK_CHAIN_BREAK, 1.0f, 1.0f);

                    player.sendMessage(Component.text("You freed " + target.getName(), NamedTextColor.GREEN));
                    target.sendMessage(Component.text("You have been freed!", NamedTextColor.GREEN));
                    cancel();
                }
                ticks++;
            }
        }.runTaskTimer(module.getCore(), 0L, 1L);
    }

    private Component progress(String label, long startTime, long endTime) {
        long now = System.currentTimeMillis();
        int filled = (int) Math.round(10.0D * (now - startTime) / Math.max(1L, endTime - startTime));
        filled = Math.max(0, Math.min(10, filled));
        long seconds = Math.max(0L, endTime - now + 999L) / 1000L;
        return Component.text(label + " [" + "|".repeat(filled) + ".".repeat(10 - filled) + "] " + seconds + "s",
                NamedTextColor.YELLOW);
    }

    // Restrict handcuffed players
    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (manager.isRestrained(event.getPlayer())) {
            manager.ensureBoundRestraintItem(event.getPlayer());
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryOpen(InventoryOpenEvent event) {
        if (event.getPlayer() instanceof Player player && manager.isRestrained(player)) {
            manager.ensureBoundRestraintItem(player);
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getWhoClicked() instanceof Player player && manager.isRestrained(player)) {
            manager.ensureBoundRestraintItem(player);
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getWhoClicked() instanceof Player player && manager.isRestrained(player)) {
            manager.ensureBoundRestraintItem(player);
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onDropItem(PlayerDropItemEvent event) {
        if (manager.isRestrained(event.getPlayer())) {
            manager.ensureBoundRestraintItem(event.getPlayer());
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onSwapHand(PlayerSwapHandItemsEvent event) {
        if (manager.isRestrained(event.getPlayer())) {
            manager.ensureBoundRestraintItem(event.getPlayer());
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onItemHeldChange(PlayerItemHeldEvent event) {
        if (manager.isRestrained(event.getPlayer())) {
            manager.ensureBoundRestraintItem(event.getPlayer());
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onJump(PlayerJumpEvent event) {
        if (manager.isRestrained(event.getPlayer())) {
            event.setCancelled(true);
        }
    }
    
    @EventHandler
    public void onDamage(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player player) {
            if (manager.isRestrained(player)) {
                manager.ensureBoundRestraintItem(player);
                event.setCancelled(true); // Cannot attack while cuffed
            }
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        if (manager.isRestrained(event.getPlayer())) {
            manager.applyEffects(event.getPlayer());
            manager.ensureBoundRestraintItem(event.getPlayer());
        } else {
            JumpRestrictionManager.repairStale(event.getPlayer());
        }
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        if (manager.isRestrained(player)) {
            manager.uncuff(player);
        }
        JumpRestrictionManager.clearAll(player);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        JumpRestrictionManager.clearAll(event.getPlayer());
    }

    @EventHandler
    public void onSprint(org.bukkit.event.player.PlayerToggleSprintEvent event) {
        if (event.isSprinting() && manager.isRestrained(event.getPlayer())) {
            manager.ensureBoundRestraintItem(event.getPlayer());
            event.setCancelled(true);
            event.getPlayer().sendActionBar(Component.text("You cannot sprint while restrained!", NamedTextColor.RED));
        }
    }

    @EventHandler
    public void onPickup(org.bukkit.event.entity.EntityPickupItemEvent event) {
        if (event.getEntity() instanceof Player player && manager.isRestrained(player)) {
            manager.ensureBoundRestraintItem(player);
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onCommand(org.bukkit.event.player.PlayerCommandPreprocessEvent event) {
        if (manager.isRestrained(event.getPlayer())) {
            manager.ensureBoundRestraintItem(event.getPlayer());
            String msg = event.getMessage().toLowerCase();
            // Allow only OOC commands if needed
            if (!msg.startsWith("/ooc")
                    && !msg.startsWith("/me")
                    && !msg.startsWith("/do")
                    && !msg.startsWith("/shout")
                    && !msg.startsWith("/whisper")
                    && !msg.startsWith("/w")
                    && !msg.startsWith("/action")) {
                event.setCancelled(true);
                event.getPlayer().sendMessage(Component.text("You cannot use commands while restrained!", NamedTextColor.RED));
            }
        }
    }
}
