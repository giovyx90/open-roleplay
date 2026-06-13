package dev.openrp.weapons.armor;

import dev.openrp.weapons.model.ArmorDefinition;
import dev.openrp.weapons.module.WeaponsModule;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Handles:
 * - Applying/removing Slowness when a bulletproof vest is equipped/unequipped.
 *   Light vest = no slowness, Heavy vest = Slowness I.
 * - Reducing incoming damage based on the vest's protection level.
 * - Removing vanilla knockback from all gun damage.
 * - Ceramic plate application (right-click with plate while wearing heavy vest, 3s timer).
 */
public class ArmorListener implements Listener {
    private final WeaponsModule module;
    private final ArmorManager armorManager;
    // Players currently applying a ceramic plate
    private final Set<UUID> applyingPlate = new HashSet<>();

    public ArmorListener(WeaponsModule module) {
        this.module = module;
        this.armorManager = module.getArmorManager();
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        checkAndApplySlowness(event.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        applyingPlate.remove(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getWhoClicked() instanceof Player player) {
            if (event.getSlotType() == InventoryType.SlotType.ARMOR) {
                // Delay by 1 tick so the inventory state updates
                Bukkit.getScheduler().runTaskLater(module.getCore(), () -> checkAndApplySlowness(player), 1L);
            }
        }
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (!event.getAction().isRightClick()) return;

        ItemStack item = event.getItem();
        if (item == null) return;

        // ── Ceramic Plate Application ───────────────────────────────────────────
        if (armorManager.isCeramicPlate(item)) {
            event.setCancelled(true);

            // Check if wearing a heavy vest
            ItemStack chestplate = player.getInventory().getChestplate();
            String armorId = armorManager.getArmorId(chestplate);
            if (!"vest_heavy".equals(armorId)) {
                player.sendActionBar(Component.text("Devi indossare un giubbotto pesante per applicare una piastra ceramica!", NamedTextColor.RED));
                return;
            }

            if (applyingPlate.contains(player.getUniqueId())) {
                player.sendActionBar(Component.text("Stai gia' applicando una piastra...", NamedTextColor.YELLOW));
                return;
            }

            // Start 3-second application
            applyingPlate.add(player.getUniqueId());
            player.sendActionBar(Component.text("Applicazione piastra ceramica... (3 secondi)", NamedTextColor.YELLOW));
            player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_USE, 0.5f, 1.5f);

            new BukkitRunnable() {
                int ticks = 0;
                @Override
                public void run() {
                    ticks++;
                    if (!player.isOnline() || player.isDead()) {
                        applyingPlate.remove(player.getUniqueId());
                        cancel();
                        return;
                    }

                    // Check the player is still holding the plate
                    ItemStack currentHand = player.getInventory().getItemInMainHand();
                    if (!armorManager.isCeramicPlate(currentHand)) {
                        applyingPlate.remove(player.getUniqueId());
                        player.sendActionBar(Component.text("Applicazione piastra annullata!", NamedTextColor.RED));
                        cancel();
                        return;
                    }

                    // Check still wearing heavy vest
                    ItemStack currentChest = player.getInventory().getChestplate();
                    String currentArmorId = armorManager.getArmorId(currentChest);
                    if (!"vest_heavy".equals(currentArmorId)) {
                        applyingPlate.remove(player.getUniqueId());
                        player.sendActionBar(Component.text("Applicazione piastra annullata: giubbotto rimosso!", NamedTextColor.RED));
                        cancel();
                        return;
                    }

                    // Progress indicators
                    if (ticks == 20) {
                        player.sendActionBar(Component.text("Applicazione piastra ceramica... ▓░░", NamedTextColor.YELLOW));
                    } else if (ticks == 40) {
                        player.sendActionBar(Component.text("Applicazione piastra ceramica... ▓▓░", NamedTextColor.YELLOW));
                    }

                    // Complete after 3 seconds (60 ticks)
                    if (ticks >= 60) {
                        applyingPlate.remove(player.getUniqueId());

                        // Consume the plate
                        currentHand.setAmount(currentHand.getAmount() - 1);

                        // Convert vest to plated
                        armorManager.convertHeavyToPlated(currentChest);

                        player.sendActionBar(Component.text("✦ Piastra ceramica installata!", NamedTextColor.GREEN));
                        player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_LAND, 0.8f, 1.2f);
                        cancel();
                    }
                }
            }.runTaskTimer(module.getCore(), 0L, 1L);
            return;
        }

        // ── Normal vest equip via right-click ───────────────────────────────────
        if (armorManager.getArmor(item) != null) {
            Bukkit.getScheduler().runTaskLater(module.getCore(), () -> checkAndApplySlowness(player), 1L);
        }
    }

    /**
     * Reduce damage when wearing a vest.
     * Also removes ALL knockback from incoming damage.
     */
    @EventHandler
    public void onDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;

        ItemStack chestplate = player.getInventory().getChestplate();
        ArmorDefinition armor = armorManager.getArmor(chestplate);
        boolean gunArmorAlreadyHandled = armorManager.consumeNextDamageArmorHandled(player.getUniqueId());

        if (armor != null && !gunArmorAlreadyHandled) {
            double originalDamage = event.getDamage();
            double reducedDamage = originalDamage * (1.0 - armor.getDamageReduction());
            event.setDamage(reducedDamage);
        }

        // Remove knockback from all damage to the player (gun hits shouldn't push)
        Bukkit.getScheduler().runTaskLater(module.getCore(), () -> {
            if (player.isOnline() && !player.isDead()) {
                player.setVelocity(player.getVelocity().setX(0).setZ(0));
            }
        }, 1L);
    }

    private void checkAndApplySlowness(Player player) {
        ItemStack chestplate = player.getInventory().getChestplate();
        ArmorDefinition armor = armorManager.getArmor(chestplate);

        // Remove any existing vest-related slowness first
        player.removePotionEffect(PotionEffectType.SLOWNESS);

        if (armor != null && armor.hasSlowness()) {
            player.addPotionEffect(new PotionEffect(
                    PotionEffectType.SLOWNESS,
                    999999,                    // Effectively permanent while wearing
                    armor.getSlownessLevel(),  // 0 = Slowness I
                    false,                     // No ambient particles
                    false,                     // No particle effects
                    true                       // Show icon
            ));
        }
        // Light vest: no slowness applied (hasSlowness() returns false)
    }
}
