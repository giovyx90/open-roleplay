package dev.openrp.weapons.listeners;

import dev.openrp.weapons.armor.HelmetManager;
import dev.openrp.weapons.model.HelmetDefinition;
import dev.openrp.weapons.model.WeaponCategory;
import dev.openrp.weapons.model.WeaponDefinition;
import dev.openrp.weapons.module.WeaponsModule;
import dev.openrp.weapons.shield.ShieldManager;
import it.meridian.core.staffboard.StaffBoardMetadata;
import it.meridian.core.staffboard.model.StaffBoardCategory;
import it.meridian.core.staffboard.model.StaffBoardLogEvent;
import it.meridian.core.staffboard.model.StaffBoardSensitivity;
import it.meridian.core.staffboard.model.StaffBoardSeverity;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

public class MeleeListener implements Listener {
    private final WeaponsModule module;
    private final NamespacedKey usesKey;

    public MeleeListener(WeaponsModule module) {
        this.module = module;
        this.usesKey = new NamespacedKey(module.getCore(), "weapon_uses");
    }

    @EventHandler(ignoreCancelled = false)
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player)) return;

        // ── Check if target has a Riot Shield (blocks ALL melee/physical damage) ──
        if (event.getEntity() instanceof Player targetPlayer) {
            ShieldManager shieldManager = module.getShieldManager();
            if (shieldManager.isRiotShieldBlocking(targetPlayer, player.getLocation())) {
                // Riot shield blocks melee damage
                event.setCancelled(true);
                player.playSound(player.getLocation(), Sound.ITEM_SHIELD_BLOCK, 1.0f, 1.0f);
                player.sendActionBar(Component.text("Blocked by riot shield!", NamedTextColor.RED));
                return;
            }
        }

        ItemStack item = player.getInventory().getItemInMainHand();
        WeaponDefinition weapon = module.getWeaponRegistry().getWeapon(item);

        if (weapon != null && weapon.getCategory() == WeaponCategory.MELEE) {
            event.setDamage(weapon.getDamage());

            // Remove knockback for all melee weapons
            if (event.getEntity() instanceof LivingEntity target) {
                Bukkit.getScheduler().runTaskLater(module.getCore(), () -> {
                    if (!target.isDead()) {
                        target.setVelocity(new Vector(0, 0, 0));
                    }
                }, 1L);
            }

            // ── Check if target has Riot Helmet (prevents stun) ───────
            boolean targetHasRiotHelmet = false;
            if (event.getEntity() instanceof Player targetPlayer) {
                HelmetManager helmetManager = module.getHelmetManager();
                ItemStack helmet = targetPlayer.getInventory().getHelmet();
                HelmetDefinition helmetDef = helmetManager.getHelmet(helmet);
                if (helmetDef != null && helmetDef.preventsMeleeStun()) {
                    targetHasRiotHelmet = true;
                }
            }

            // ── Special melee effects ──────────────────────────────
            String weaponId = weapon.getId();
            if (event.getEntity() instanceof LivingEntity target) {
                switch (weaponId) {
                    // Baseball bat: stun (Slowness II + Blindness I for 3 seconds)
                    case "bat" -> {
                        if (!targetHasRiotHelmet) {
                            applyStun(target, 60, 1, true); // 3 seconds
                        } else {
                            notifyStunBlocked(player);
                        }
                    }
                    // Spiked bat: stun (Slowness II + Blindness I for 3 seconds)
                    case "spiked_bat" -> {
                        if (!targetHasRiotHelmet) {
                            applyStun(target, 60, 1, true); // 3 seconds
                        } else {
                            notifyStunBlocked(player);
                        }
                    }
                    // Metal pipe: stun (Slowness II + Blindness I for 3 seconds)
                    case "metal_pipe" -> {
                        if (!targetHasRiotHelmet) {
                            applyStun(target, 60, 1, true); // 3 seconds
                        } else {
                            notifyStunBlocked(player);
                        }
                    }
                    // Machete: apply Poison I for 4 seconds on hit
                    case "machete" -> {
                        target.addPotionEffect(new PotionEffect(
                                PotionEffectType.POISON,
                                80,  // 4 seconds
                                0,   // Level I (amplifier 0)
                                false, false, true
                        ));
                    }
                    // Knife: apply Poison I for 3 seconds on hit
                    case "knife" -> {
                        target.addPotionEffect(new PotionEffect(
                                PotionEffectType.POISON,
                                60,  // 3 seconds
                                0,   // Level I (amplifier 0)
                                false, false, true
                        ));
                    }
                    // Baton (Manganello): stun (Slowness II + Blindness I for 3 seconds), 100 uses
                    case "baton", "manganello" -> {
                        if (!targetHasRiotHelmet) {
                            applyStun(target, 60, 1, true); // 3 seconds
                        } else {
                            notifyStunBlocked(player);
                        }
                        consumeUse(player, item, 100);
                    }
                }
            }

            try {
                Sound hitSound = Sound.valueOf(weapon.getSoundHit().toUpperCase().replace(".", "_"));
                player.getWorld().playSound(event.getEntity().getLocation(), hitSound, 1.0f, 1.0f);
            } catch (Exception ignored) {}
            emitMeleeHit(player, event.getEntity() instanceof LivingEntity target ? target : null, weapon, event.getDamage());
            
            // Note: attack speed cooldown is complex to override in pure Bukkit without NBT attribute modifiers.
            // For a complete implementation, the WeaponRegistry should add the generic.attack_speed attribute 
            // modifier to the ItemStack when it's created.
        }
    }

    private void emitMeleeHit(Player attacker, LivingEntity target, WeaponDefinition weapon, double damage) {
        if (target == null) {
            return;
        }
        StaffBoardMetadata metadata = StaffBoardMetadata.create()
                .put("shooter_uuid", attacker.getUniqueId())
                .put("shooter_name", attacker.getName())
                .put("weapon_id", weapon.getId())
                .put("weapon_name", weapon.getDisplayName())
                .put("weapon_category", weapon.getCategory().name())
                .put("damage", damage)
                .put("source_system", "OpenWeapons")
                .putLocation(target.getLocation());

        StaffBoardLogEvent.Builder hitBuilder = StaffBoardLogEvent.builder("combat.weapon.hit", "OpenWeapons")
                .category(StaffBoardCategory.COMBAT)
                .severity(StaffBoardSeverity.NOTICE)
                .sensitivity(StaffBoardSensitivity.DEPARTMENT_ONLY)
                .actor(attacker)
                .location(target.getLocation())
                .message(attacker.getName() + " hit a target with " + weapon.getDisplayName() + ".")
                .metadataJson(metadata.toJson());
        if (target instanceof Player targetPlayer) {
            metadata.put("victim_uuid", targetPlayer.getUniqueId())
                    .put("victim_name", targetPlayer.getName());
            hitBuilder.target(targetPlayer)
                    .message(attacker.getName() + " hit " + targetPlayer.getName() + " with " + weapon.getDisplayName() + ".");
        }
        module.getCore().getStaffBoardPublisher().emit(hitBuilder.metadataJson(metadata.toJson()).build());

        if (target instanceof Player targetPlayer) {
            module.getCore().getStaffBoardPublisher().emit(StaffBoardLogEvent.builder("combat.player.damaged", "OpenWeapons")
                    .category(StaffBoardCategory.COMBAT)
                    .severity(StaffBoardSeverity.NOTICE)
                    .sensitivity(StaffBoardSensitivity.DEPARTMENT_ONLY)
                    .actor(attacker)
                    .target(targetPlayer)
                    .location(target.getLocation())
                    .message(targetPlayer.getName() + " damaged by " + attacker.getName() + ".")
                    .metadataJson(metadata.toJson())
                    .build());
        }
    }

    /**
     * Applies stun effect: Slowness + Blindness + Nausea.
     */
    private void applyStun(LivingEntity target, int durationTicks, int slownessAmplifier, boolean withBlindness) {
        target.addPotionEffect(new PotionEffect(
                PotionEffectType.SLOWNESS,
                durationTicks,
                slownessAmplifier, // Slowness II (amplifier 1)
                false, false, true
        ));
        if (withBlindness) {
            target.addPotionEffect(new PotionEffect(
                    PotionEffectType.BLINDNESS,
                    durationTicks,
                    0, // Level I
                    false, false, true
            ));
            target.addPotionEffect(new PotionEffect(
                    PotionEffectType.NAUSEA,
                    durationTicks,
                    0,
                    false, false, true
            ));
        }

        if (target instanceof Player targetPlayer) {
            targetPlayer.sendActionBar(Component.text("⚡ You have been stunned!", NamedTextColor.RED));
        }
    }

    private void notifyStunBlocked(Player attacker) {
        attacker.sendActionBar(Component.text("Stun blocked by riot helmet!", NamedTextColor.YELLOW));
    }

    /**
     * Tracks and consumes uses for limited-use melee weapons via PDC.
     */
    private void consumeUse(Player player, ItemStack item, int maxUses) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;

        int currentUses = meta.getPersistentDataContainer().getOrDefault(usesKey, PersistentDataType.INTEGER, 0);
        currentUses++;

        if (currentUses >= maxUses) {
            player.getInventory().remove(item);
            player.sendMessage(Component.text("Your baton broke!", NamedTextColor.RED));
            player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 1.0f, 1.0f);
        } else {
            meta.getPersistentDataContainer().set(usesKey, PersistentDataType.INTEGER, currentUses);
            int remaining = maxUses - currentUses;
            meta.lore(java.util.List.of(
                    Component.text("Uses: " + remaining + "/" + maxUses, NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)
            ));
            item.setItemMeta(meta);
        }
    }
}
