package dev.openrp.weapons.grenades;

import dev.openrp.weapons.module.WeaponsModule;
import dev.openrp.weapons.utility.UtilityItemType;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Snowball;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class GrenadeListener implements Listener {
    private static final long GRENADE_COOLDOWN_MILLIS = 3000L;

    private final WeaponsModule module;
    private final GrenadeManager manager;
    private final Set<UUID> handledProjectiles = ConcurrentHashMap.newKeySet();
    private final java.util.Map<UUID, Long> grenadeCooldowns = new ConcurrentHashMap<>();

    public GrenadeListener(WeaponsModule module) {
        this.module = module;
        this.manager = module.getGrenadeManager();
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        ItemStack item = event.getItem();
        GrenadeDefinition def = manager.getGrenade(item);
        if (def == null) return;

        event.setCancelled(true);
        if (module.getC4Manager().isC4(def)) {
            module.getC4Manager().openTimerGui(player, def, event.getHand(), event.getClickedBlock(), event.getBlockFace());
            return;
        }

        long now = System.currentTimeMillis();
        long cooldownUntil = grenadeCooldowns.getOrDefault(player.getUniqueId(), 0L);
        if (cooldownUntil > now) {
            long remainingMillis = cooldownUntil - now;
            player.sendActionBar(net.kyori.adventure.text.Component.text(
                    "Grenade cooldown: " + String.format("%.1f", remainingMillis / 1000.0) + "s",
                    net.kyori.adventure.text.format.NamedTextColor.RED));
            return;
        }
        grenadeCooldowns.put(player.getUniqueId(), now + GRENADE_COOLDOWN_MILLIS);

        consumeOne(player, event.getHand());

        // Throw snowball
        Snowball snowball = player.launchProjectile(Snowball.class);
        ItemStack projectileItem = manager.createItemStack(def.getId());
        if (projectileItem == null) {
            projectileItem = new ItemStack(def.getMaterial());
        }
        projectileItem.setAmount(1);
        snowball.setItem(projectileItem);
        
        // Mark the projectile so we don't do vanilla things
        snowball.getPersistentDataContainer().set(manager.getGrenadeKey(), PersistentDataType.STRING, def.getId());

        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_EGG_THROW, 1.0f, 1.0f);

    }

    @EventHandler
    public void onProjectileHit(ProjectileHitEvent event) {
        if (!(event.getEntity() instanceof Snowball snowball)) {
            return;
        }
        String grenadeId = snowball.getPersistentDataContainer().get(manager.getGrenadeKey(), PersistentDataType.STRING);
        GrenadeDefinition def = manager.getGrenade(grenadeId);
        if (def == null) {
            return;
        }
        if (!handledProjectiles.add(snowball.getUniqueId())) {
            return;
        }
        Location impact = snowball.getLocation();
        if (event.getHitBlock() != null) {
            impact = event.getHitBlock().getLocation().add(0.5, 0.2, 0.5);
        }
        snowball.remove();
        if (def.getType() == GrenadeType.IMPACT) {
            detonateAt(impact, def);
        } else {
            primeGroundMarker(impact, def);
        }
    }

    private void consumeOne(Player player, EquipmentSlot hand) {
        EquipmentSlot slot = hand == null ? EquipmentSlot.HAND : hand;
        ItemStack item = player.getInventory().getItem(slot);
        if (item == null) {
            return;
        }
        if (item.getAmount() <= 1) {
            player.getInventory().setItem(slot, null);
        } else {
            item.setAmount(item.getAmount() - 1);
        }
    }

    private void primeGroundMarker(Location loc, GrenadeDefinition def) {
        if (loc.getWorld() == null) {
            return;
        }
        ItemStack markerItem = manager.createItemStack(def.getId());
        if (markerItem == null) {
            markerItem = new ItemStack(def.getMaterial());
        }
        markerItem.setAmount(1);
        ItemStack markerStack = markerItem;
        Item marker = loc.getWorld().spawn(loc, Item.class, item -> {
            item.setItemStack(markerStack);
            item.setPickupDelay(Integer.MAX_VALUE);
            item.setCanMobPickup(false);
            item.setCanPlayerPickup(false);
            item.setSilent(true);
            item.setVelocity(new org.bukkit.util.Vector(0, 0, 0));
            item.setInvulnerable(true);
            item.addScoreboardTag("next_grenade_marker");
        });
        loc.getWorld().playSound(loc, Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 0.7f);
        loc.getWorld().spawnParticle(Particle.CRIT, loc, 12, 0.25, 0.12, 0.25, 0.01);
        Bukkit.getScheduler().runTaskLater(module.getCore(), () -> {
            Location detonation = marker.isValid() ? marker.getLocation() : loc;
            marker.remove();
            detonateAt(detonation, def);
        }, Math.max(1L, def.getFuseTimeTicks()));
    }

    private void detonateAt(Location loc, GrenadeDefinition def) {
        switch (def.getType()) {
            case SMOKE -> detonateSmoke(loc, def);
            case FLASHBANG -> detonateFlashbang(loc, def);
            case FRAG, IMPACT -> detonateFrag(loc, def);
        }
    }

    private void detonateSmoke(Location loc, GrenadeDefinition def) {
        loc.getWorld().playSound(loc, Sound.BLOCK_FIRE_EXTINGUISH, 2.0f, 0.5f);

        // Spawn particles over time
        int durationTicks = def.getEffectDurationTicks();
        double radius = def.getRadius();

        for (int i = 0; i < durationTicks; i += 10) {
            Bukkit.getScheduler().runTaskLater(module.getCore(), () -> {
                loc.getWorld().spawnParticle(Particle.CAMPFIRE_SIGNAL_SMOKE, loc, (int)(radius * 20), radius, radius/2, radius, 0.05);
                
                // Apply blindness to players inside
                for (Player p : loc.getWorld().getPlayers()) {
                    if (p.getLocation().distance(loc) <= radius) {
                        if (module.getUtilityItemManager().isWearing(p, UtilityItemType.GAS_MASK)) {
                            p.removePotionEffect(PotionEffectType.BLINDNESS);
                            continue;
                        }
                        p.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 100, 0, false, false, false));
                    }
                }
            }, i);
        }
    }

    private void detonateFlashbang(Location loc, GrenadeDefinition def) {
        loc.getWorld().playSound(loc, Sound.ENTITY_FIREWORK_ROCKET_LARGE_BLAST, 2.0f, 1.5f);
        loc.getWorld().spawnParticle(Particle.FLASH, loc, 5, 0.5, 0.5, 0.5, 0);

        double radius = def.getRadius();
        for (Player p : loc.getWorld().getPlayers()) {
            if (p.getLocation().distance(loc) <= radius) {
                // Flashbang effect: Blindness + Slowness + fake damage flash
                p.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, def.getEffectDurationTicks(), 1, false, false, false));
                p.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 60, 0, false, false, false));
                p.damage(0.01); // Just to flash screen
            }
        }
    }

    private void detonateFrag(Location loc, GrenadeDefinition def) {
        loc.getWorld().playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 2.0f, 1.0f);
        loc.getWorld().spawnParticle(Particle.EXPLOSION_EMITTER, loc, 1);

        double radius = def.getRadius();
        double maxDamage = def.getDamage();

        for (LivingEntity entity : loc.getWorld().getLivingEntities()) {
            double distance = entity.getLocation().distance(loc);
            if (distance <= radius) {
                // Calculate damage falloff
                double damageMult = 1.0 - (distance / radius);
                if (damageMult < 0.1) damageMult = 0.1;
                
                entity.damage(maxDamage * damageMult);
            }
        }
    }
}
