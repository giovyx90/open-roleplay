package dev.openrp.weapons.mechanics;

import dev.openrp.weapons.armor.ArmorManager;
import dev.openrp.weapons.armor.HelmetManager;
import dev.openrp.weapons.attachments.AttachmentManager;
import dev.openrp.weapons.cosmetics.WeaponCosmeticManager;
import it.meridian.core.hospital.GunshotSeverity;
import it.meridian.core.hospital.HospitalModule;
import dev.openrp.weapons.api.WeaponCombatDecision;
import dev.openrp.weapons.model.AmmoDefinition;
import dev.openrp.weapons.model.ArmorDefinition;
import dev.openrp.weapons.model.HelmetDefinition;
import dev.openrp.weapons.model.WeaponDefinition;
import dev.openrp.weapons.module.WeaponsModule;
import dev.openrp.weapons.shield.ShieldManager;
import dev.openrp.weapons.util.JumpRestrictionManager;
import it.meridian.core.staffboard.StaffBoardMetadata;
import it.meridian.core.staffboard.StaffBoardModuleRegistry;
import it.meridian.core.staffboard.model.StaffBoardCategory;
import it.meridian.core.staffboard.model.StaffBoardLogEvent;
import it.meridian.core.staffboard.model.StaffBoardSensitivity;
import it.meridian.core.staffboard.model.StaffBoardSeverity;
import org.bukkit.Bukkit;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.damage.DamageSource;
import org.bukkit.damage.DamageType;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

public class ShootingMechanics {

    private static final String RIOT_SHIELD_HITS_KEY = "riot_shield_hits_taken";
    private static final long HIT_JUMP_LOCK_TICKS = 140L;
    private static final Map<UUID, BukkitTask> hitJumpRestrictionTasks = new ConcurrentHashMap<>();

    /**
     * Fires a shot from the player using raycasting.
     * Applies recoil to the shooter's pitch based on the weapon's recoil value.
     * Removes all knockback from the hit target.
     * Uses gray bullet trail particles.
     * Handles: helmet headshot negation, vest durability, ballistic shield blocking.
     * Supports buckshot (multiple pellets with spread) for shotguns.
     */
    public static void shoot(Player shooter, WeaponDefinition weapon) {
        shoot(shooter, weapon, null);
    }

    public static void shoot(Player shooter, WeaponDefinition weapon, ItemStack weaponItem) {
        Location eyeLoc = shooter.getEyeLocation();
        Vector direction = eyeLoc.getDirection();
        WeaponsModule module = getModule();
        AttachmentManager attachmentManager = module != null ? module.getAttachmentManager() : null;
        double recoilMultiplier = attachmentManager != null ? attachmentManager.getRecoilMultiplier(weaponItem) : 1.0D;
        double soundMultiplier = attachmentManager != null ? attachmentManager.getSoundMultiplier(weaponItem) : 1.0D;
        emitWeaponShot(shooter, weapon, weaponItem);
        if (module != null) {
            module.notifyWeaponShot(shooter, weapon, weaponItem);
        }

        // Muzzle flash — gray smoke instead of flash
        Particle.DustOptions smokeDust = new Particle.DustOptions(org.bukkit.Color.fromRGB(150, 150, 150), 1.0f);
        shooter.getWorld().spawnParticle(Particle.DUST, eyeLoc.clone().add(direction.clone().multiply(0.5)), 3, 0.05, 0.05, 0.05, 0, smokeDust);

        playShootSound(shooter, weapon, weaponItem, eyeLoc, soundMultiplier, module);

        ejectSpentCasing(shooter, weapon);

        // ── Recoil ──────────────────────────────────────────
        double recoil = weapon.getRecoil() * recoilMultiplier;
        if (recoil > 0) {
            float pitchKick = (float) -(recoil * 100.0);
            float yawDeviation = (float) ((Math.random() - 0.5) * recoil * 40.0);

            Location loc = shooter.getLocation();
            float newPitch = Math.max(-90.0f, loc.getPitch() + pitchKick);
            float newYaw = loc.getYaw() + yawDeviation;
            shooter.teleport(new Location(loc.getWorld(), loc.getX(), loc.getY(), loc.getZ(), newYaw, newPitch));
        }

        // ── Fire pellets (buckshot or single slug) ──────────
        int pelletCount = weapon.getPelletCount();
        if (pelletCount > 1) {
            for (int i = 0; i < pelletCount; i++) {
                Vector pelletDir = applySpread(shooter, weapon, weaponItem, direction, attachmentManager);
                double pelletDamage = weapon.getDamage() / pelletCount;
                fireSinglePellet(shooter, weapon, weaponItem, eyeLoc.clone(), pelletDir, pelletDamage);
            }
        } else {
            Vector bulletDir = applySpread(shooter, weapon, weaponItem, direction, attachmentManager);
            fireSinglePellet(shooter, weapon, weaponItem, eyeLoc, bulletDir, weapon.getDamage());
        }
    }

    /**
     * Fires a single pellet/bullet from the shooter.
     */
    private static void fireSinglePellet(Player shooter, WeaponDefinition weapon, ItemStack weaponItem,
                                         Location eyeLoc, Vector direction, double baseDamage) {
        // ── Raycast ─────────────────────────────────────────
        WeaponsModule module = getModule();
        World world = shooter.getWorld();
        double maxDistance = effectiveMaxDistance(weapon, weaponItem);
        RayTraceResult blockResult = firstBlockingRayTrace(world, eyeLoc, direction, maxDistance, module, shooter);
        double blockDistance = blockResult != null && blockResult.getHitPosition() != null
                ? blockResult.getHitPosition().distance(eyeLoc.toVector())
                : maxDistance;
        RayTraceResult result = world.rayTraceEntities(eyeLoc, direction, maxDistance, 0.45,
                entity -> entity instanceof LivingEntity living
                        && !entity.equals(shooter)
                        && entity.isValid()
                        && !living.isDead()
                        && canTarget(module, shooter, weapon, weaponItem, living));
        if (result != null && result.getHitEntity() != null && result.getHitPosition() != null) {
            double entityDistance = result.getHitPosition().distance(eyeLoc.toVector());
            if (entityDistance > blockDistance + 0.05) {
                result = null;
            }
        }

        Location hitLoc;
        if (result != null && result.getHitEntity() != null) {
            hitLoc = result.getHitPosition().toLocation(shooter.getWorld());
            LivingEntity target = (LivingEntity) result.getHitEntity();

            AmmoDefinition ammo = module != null ? module.getAmmoRegistry().getAmmo(weapon.getAmmoType()) : null;
            double hitDistance = result.getHitPosition().distance(eyeLoc.toVector());
            double previewDamage = applyDamageFalloff(weapon, baseDamage, hitDistance) * fleshDamageMultiplier(ammo);
            boolean previewHeadshot = Math.abs(result.getHitPosition().getY() - target.getEyeLocation().getY()) <= 0.3;
            if (previewHeadshot) {
                previewDamage *= weapon.getHeadshotMultiplier();
            }
            if (isImpactBlocked(module, shooter, weapon, weaponItem, target, hitLoc, previewDamage, hitDistance,
                    previewHeadshot)) {
                drawBulletTrail(shooter, eyeLoc, hitLoc, direction, weapon);
                return;
            }

            // ── Check Riot/Ballistic Shield ─────────────────────
            if (target instanceof Player targetPlayer && module != null) {
                ShieldManager shieldManager = module.getShieldManager();
                ItemStack mainHand = targetPlayer.getInventory().getItemInMainHand();
                JavaPlugin shieldPlugin = JavaPlugin.getProvidingPlugin(ShootingMechanics.class);
                boolean riotBlocksBullets = shieldPlugin.getConfig().getBoolean("riotShield.blockBullets", true);
                int riotMaxBulletHits = Math.max(1, shieldPlugin.getConfig().getInt("riotShield.maxBulletHits", 5));
                String ammoType = weapon.getAmmoType();
                boolean specialAmmoPenetrates = isAntiMaterial(ammoType, ammo);

                if (riotBlocksBullets && shieldManager.isRiotShieldBlocking(targetPlayer, shooter.getLocation())) {
                    if (!specialAmmoPenetrates) {
                        int hitsTaken = incrementRiotShieldHits(mainHand, riotMaxBulletHits, shieldPlugin);
                        targetPlayer.playSound(targetPlayer.getLocation(), Sound.ITEM_SHIELD_BLOCK, 1.0f, 0.9f);

                        if (hitsTaken >= riotMaxBulletHits) {
                            targetPlayer.getInventory().setItemInMainHand(null);
                            targetPlayer.sendMessage(Component.text("⚠ Your Riot Shield is broken!", NamedTextColor.RED));
                            targetPlayer.playSound(targetPlayer.getLocation(), Sound.ENTITY_ITEM_BREAK, 1.0f, 1.0f);
                        } else {
                            targetPlayer.sendActionBar(Component.text("Riot Shield hits: " + hitsTaken + "/" + riotMaxBulletHits, NamedTextColor.YELLOW));
                        }

                        drawBulletTrail(shooter, eyeLoc, hitLoc, direction, weapon);
                        shooter.playSound(shooter.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 0.5f);
                        return;
                    }
                    // Special ammo penetration: riot shield does not fully absorb this shot.
                }

                if (shieldManager.isBallisticShieldBlocking(targetPlayer, shooter.getLocation())) {
                    int currentDur = shieldManager.getDurability(mainHand);
                    int shieldDmg = shieldManager.getShieldDurabilityDamage(ammo);
                    int newDur = currentDur - shieldDmg;

                    if (isAntiMaterial(ammoType, ammo)) {
                        // .50 BMG destroys the shield completely
                        targetPlayer.getInventory().setItemInMainHand(null);
                        targetPlayer.sendMessage(Component.text("⚠ Your Ballistic Shield was destroyed by .50 BMG!", NamedTextColor.DARK_RED));
                        targetPlayer.playSound(targetPlayer.getLocation(), Sound.ENTITY_ITEM_BREAK, 1.0f, 0.5f);
                        shooter.getWorld().spawnParticle(Particle.EXPLOSION, hitLoc, 1);
                        // Bullet passes through after destroying shield — apply some damage
                        double penetratingDamage = applyDamageFalloff(weapon, baseDamage, hitDistance) * fleshDamageMultiplier(ammo) * 0.5D;
                        boolean lethal = target.getHealth() - penetratingDamage <= 0.0D;
                        applyBulletDamage(target, penetratingDamage, shooter, hitLoc);
                        notifyHospitalGunshot(targetPlayer, false, true, penetratingDamage);
                        module.notifyWeaponHit(shooter, weapon, weaponItem, target, hitLoc, penetratingDamage,
                                hitDistance, false, lethal);
                        applyHitJumpRestriction(targetPlayer, shieldPlugin);
                    } else if (newDur <= 0) {
                        // Shield broken — start 10s cooldown
                        shieldManager.setDurability(mainHand, 0);
                        shieldManager.startCooldown(targetPlayer, shooter.getLocation());
                        targetPlayer.sendMessage(Component.text("⚠ Your Ballistic Shield is broken! 10s cooldown.", NamedTextColor.RED));
                        targetPlayer.playSound(targetPlayer.getLocation(), Sound.ITEM_SHIELD_BREAK, 1.0f, 1.0f);
                    } else {
                        // Shield absorbs
                        shieldManager.setDurability(mainHand, newDur);
                        targetPlayer.playSound(targetPlayer.getLocation(), Sound.ITEM_SHIELD_BLOCK, 1.0f, 1.0f);
                    }

                    // Shield blocked the bullet — draw trail and return
                    drawBulletTrail(shooter, eyeLoc, hitLoc, direction, weapon);
                    shooter.playSound(shooter.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 0.5f);
                    return;
                }
            }
            
            boolean antiMaterial = isAntiMaterial(weapon.getAmmoType(), ammo);
            double damage = applyDamageFalloff(weapon, baseDamage, hitDistance) * fleshDamageMultiplier(ammo);
            boolean isHeadshot = false;

            // ── Headshot check ──────────────────────────────────
            double hitY = result.getHitPosition().getY();
            double targetEyeY = target.getEyeLocation().getY();
            if (Math.abs(hitY - targetEyeY) <= 0.3) {
                // Check for helmet that negates headshot
                boolean headshotNegated = false;
                if (module != null) {
                    HelmetManager helmetManager = module.getHelmetManager();
                    ItemStack helmet = getHelmet(target);
                    HelmetDefinition helmetDef = helmetManager.getHelmet(helmet);

                    if (helmetDef != null && helmetDef.negatesHeadshot()) {
                        headshotNegated = true;
                        // Apply helmet damage reduction
                        damage *= (1.0 - helmetDef.getDamageReduction());

                        // Consume helmet durability
                        if (helmetDef.getMaxDurability() > 0) {
                            int currentDur = helmetManager.getDurability(helmet);
                            if (currentDur < 0) currentDur = helmetDef.getMaxDurability();
                            int armorDmg = module.getArmorManager().getDurabilityDamage(ammo);
                            int newDur = currentDur - armorDmg;
                            if (newDur <= 0) {
                                setHelmet(target, null);
                                if (target instanceof Player targetPlayer) {
                                    targetPlayer.sendMessage(Component.text("Your " + helmetDef.getDisplayName() + " broke!", NamedTextColor.RED));
                                    targetPlayer.playSound(targetPlayer.getLocation(), Sound.ENTITY_ITEM_BREAK, 1.0f, 1.0f);
                                }
                            } else {
                                helmetManager.setDurability(helmet, newDur);
                            }
                        }
                    }
                }

                if (!headshotNegated) {
                    isHeadshot = true;
                    damage *= weapon.getHeadshotMultiplier();
                    playHitFeedback(shooter, weaponItem, module, true);
                } else {
                    // Still a hit, but headshot negated by helmet
                    playHitFeedback(shooter, weaponItem, module, false);
                }
            } else {
                playHitFeedback(shooter, weaponItem, module, false);
            }

            // ── Vest damage reduction + durability ──────────────
            if (module != null) {
                ArmorManager armorManager = module.getArmorManager();
                ItemStack chestplate = getChestplate(target);
                ArmorDefinition armorDef = armorManager.getArmor(chestplate);

                if (armorDef != null) {
                    damage *= armorManager.getBulletDamageMultiplier(armorDef, ammo);

                    // Consume durability
                    int currentDur = armorManager.getDurability(chestplate);
                    if (currentDur < 0) currentDur = armorDef.getMaxDurability();
                    int durDmg = armorManager.getDurabilityDamage(ammo);
                    int newDur = currentDur - durDmg;

                    if (armorDef.hasPlate()) {
                        // Plated vest: plate absorbs first
                        int plateThreshold = armorManager.getPlateBreakThreshold(armorDef);
                        if (newDur <= plateThreshold) {
                            // Plate has broken — convert to normal heavy vest
                            int vestDurRemaining = Math.min(newDur, plateThreshold);
                            armorManager.convertPlatedToHeavy(chestplate, vestDurRemaining);
                            if (target instanceof Player targetPlayer) {
                                targetPlayer.sendMessage(Component.text("⚠ Your ceramic plate broke!", NamedTextColor.RED));
                                targetPlayer.playSound(targetPlayer.getLocation(), Sound.ENTITY_ITEM_BREAK, 1.0f, 1.0f);
                            }
                        } else {
                            armorManager.setDurability(chestplate, newDur);
                        }
                    } else {
                        // Normal vest
                        if (newDur <= 0) {
                            // Vest breaks
                            setChestplate(target, null);
                            if (target instanceof Player targetPlayer) {
                                targetPlayer.sendMessage(Component.text("⚠ Your " + armorDef.getDisplayName() + " broke!", NamedTextColor.RED));
                                targetPlayer.playSound(targetPlayer.getLocation(), Sound.ENTITY_ITEM_BREAK, 1.0f, 1.0f);
                                // Remove slowness since vest is gone
                                targetPlayer.removePotionEffect(PotionEffectType.SLOWNESS);
                            }
                        } else {
                            armorManager.setDurability(chestplate, newDur);
                        }
                    }
                }

                // Also handle helmet durability for body shots (non-headshot helmets still exist)
                // Already handled in headshot section above if it was a headshot
            }

            // Apply damage (reset invulnerability ticks so multiple shotgun pellets can hit)
            target.setNoDamageTicks(0);
            if (target instanceof Player targetPlayer && module != null) {
                module.getArmorManager().markNextDamageArmorHandled(targetPlayer.getUniqueId());
            }
            boolean lethal = target.getHealth() - damage <= 0.0D;
            applyBulletDamage(target, damage, shooter, hitLoc);
            if (target instanceof Player targetPlayer) {
                notifyHospitalGunshot(targetPlayer, isHeadshot, antiMaterial, damage);
            }
            if (module != null) {
                module.notifyWeaponHit(shooter, weapon, weaponItem, target, hitLoc, damage, hitDistance, isHeadshot, lethal);
            }
            emitWeaponHit(shooter, target, weapon, weaponItem, hitLoc, damage, hitDistance, isHeadshot);
            if (target instanceof Player targetPlayer) {
                applyHitJumpRestriction(targetPlayer, JavaPlugin.getProvidingPlugin(ShootingMechanics.class));
            }

            // Remove ALL knockback from gun damage
            target.setVelocity(new Vector(0, 0, 0));
            Bukkit.getScheduler().runTaskLater(JavaPlugin.getProvidingPlugin(ShootingMechanics.class), () -> {
                if (!target.isDead() && target.isValid()) {
                    target.setVelocity(new Vector(0, 0, 0));
                }
            }, 1L);

        } else if (blockResult != null && blockResult.getHitBlock() != null) {
            hitLoc = blockResult.getHitPosition().toLocation(shooter.getWorld());
        } else {
            hitLoc = eyeLoc.clone().add(direction.clone().multiply(maxDistance));
        }

        // ── Bullet trail (gray particles) ───────────────────
        drawBulletTrail(shooter, eyeLoc, hitLoc, direction, weapon);
    }

    private static RayTraceResult firstBlockingRayTrace(World world, Location eyeLoc, Vector direction,
                                                       double maxDistance, WeaponsModule module, Player shooter) {
        if (!bulletGlassBreakingEnabled(module)) {
            return world.rayTraceBlocks(eyeLoc, direction, maxDistance, FluidCollisionMode.NEVER, true);
        }
        Vector normalizedDirection = direction.clone().normalize();
        Location origin = eyeLoc.clone();
        double remaining = maxDistance;
        int penetrations = BulletGlassRules.clampMaxPenetrations(
                module.getCore().getConfig().getInt("weapons.bullets.glass-breaking.max-penetrations", 2));
        int glassBreaks = 0;
        int passThroughLimit = penetrations + 8;
        for (int i = 0; i <= passThroughLimit && remaining > 0.05D; i++) {
            RayTraceResult result = world.rayTraceBlocks(origin, normalizedDirection, remaining, FluidCollisionMode.NEVER, true);
            if (result == null || result.getHitBlock() == null || result.getHitPosition() == null) {
                return null;
            }
            Block block = result.getHitBlock();
            boolean passThrough = BulletGlassRules.isBulletPassThrough(block.getType());
            boolean breakableGlass = BulletGlassRules.isBreakableGlass(block.getType(),
                    module.getCore().getConfig().getStringList("weapons.bullets.glass-breaking.materials"));
            if (!passThrough && !breakableGlass) {
                return result;
            }
            Location hit = result.getHitPosition().toLocation(world);
            if (breakableGlass) {
                if (glassBreaks >= penetrations) {
                    return result;
                }
                glassBreaks++;
                breakGlass(block, hit, shooter);
            }
            double traveled = Math.max(0.01D, result.getHitPosition().distance(origin.toVector()));
            remaining -= traveled + 0.08D;
            origin = hit.add(normalizedDirection.clone().multiply(0.08D));
        }
        return world.rayTraceBlocks(origin, normalizedDirection, remaining, FluidCollisionMode.NEVER, true);
    }

    private static boolean bulletGlassBreakingEnabled(WeaponsModule module) {
        return module != null && module.getCore().getConfig().getBoolean("weapons.bullets.glass-breaking.enabled", true);
    }

    private static void breakGlass(Block block, Location hit, Player shooter) {
        block.getWorld().playSound(hit, Sound.BLOCK_GLASS_BREAK, SoundCategory.BLOCKS, 0.75F, 1.05F);
        block.getWorld().spawnParticle(Particle.BLOCK, hit, 12, 0.18D, 0.18D, 0.18D, block.getBlockData());
        if (!recordCorelineBreak(shooter, block)) {
            block.setType(Material.AIR, false);
        }
    }

    private static boolean recordCorelineBreak(Player shooter, Block block) {
        if (shooter == null || block == null) {
            return false;
        }
        try {
            org.bukkit.plugin.Plugin plugin = Bukkit.getPluginManager().getPlugin("Coreline");
            if (plugin == null || !plugin.isEnabled()) {
                return false;
            }
            Object service = plugin.getClass().getMethod("getDestructibleMapService").invoke(plugin);
            Object result = service.getClass().getMethod("recordExternalBreak", Player.class, Block.class)
                    .invoke(service, shooter, block);
            return result instanceof Boolean value && value;
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            return false;
        }
    }

    private static void playShootSound(Player shooter, WeaponDefinition weapon, ItemStack weaponItem, Location eyeLoc,
                                       double attachmentSoundMultiplier, WeaponsModule module) {
        double configuredVolume = module != null
                ? module.getCore().getConfig().getDouble("weapons.audio.fire-volume", 1.0D)
                : 1.0D;
        float soundVolume = (float) Math.max(0.0D, configuredVolume * attachmentSoundMultiplier);
        boolean suppressAutomaticSkinFire = module != null
                && module.isAutomaticSkinFireSuppressed(shooter.getUniqueId())
                && getWeaponSkinSound(module, weaponItem, WeaponCosmeticManager.SOUND_AUTOMATIC) != null;
        String skinSound = suppressAutomaticSkinFire
                ? null
                : getWeaponSkinSound(module, weaponItem, WeaponCosmeticManager.SOUND_FIRE);
        if (skinSound != null) {
            shooter.getWorld().playSound(eyeLoc, skinSound, SoundCategory.PLAYERS, soundVolume, 1.0f);
            return;
        }
        if (playConfiguredCustomSound(shooter.getWorld(), eyeLoc, weapon.getSoundShoot(), soundVolume, 1.0f)) {
            return;
        }
        boolean customFireSounds = module != null
                && module.getCore().getConfig().getBoolean("weapons.audio.custom-fire-sounds", false);
        if (customFireSounds) {
            shooter.getWorld().playSound(eyeLoc, customFireSound(weapon), SoundCategory.PLAYERS, soundVolume, 1.0f);
            return;
        }
        try {
            Sound shootSound = Sound.valueOf(weapon.getSoundShoot().toUpperCase().replace(".", "_"));
            shooter.getWorld().playSound(eyeLoc, shootSound, soundVolume, 1.0f);
        } catch (Exception e) {
            shooter.getWorld().playSound(eyeLoc, Sound.ENTITY_FIREWORK_ROCKET_BLAST, soundVolume, 1.0f);
        }
    }

    private static boolean playConfiguredCustomSound(World world, Location location, String configuredSound,
                                                     float volume, float pitch) {
        if (configuredSound == null || configuredSound.isBlank()) {
            return false;
        }
        try {
            Sound.valueOf(configuredSound.toUpperCase().replace(".", "_"));
            return false;
        } catch (IllegalArgumentException ignored) {
            world.playSound(location, configuredSound, SoundCategory.PLAYERS, volume, pitch);
            return true;
        }
    }

    private static void playHitFeedback(Player shooter, ItemStack weaponItem, WeaponsModule module, boolean headshot) {
        String soundKey = headshot ? WeaponCosmeticManager.SOUND_HEADSHOT : WeaponCosmeticManager.SOUND_HIT;
        String skinSound = getWeaponSkinSound(module, weaponItem, soundKey);
        if (skinSound != null) {
            shooter.getWorld().playSound(shooter.getLocation(), skinSound, SoundCategory.PLAYERS, 1.0f, 1.0f);
            return;
        }
        shooter.playSound(shooter.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, headshot ? 1.5f : 1.0f);
    }

    private static String getWeaponSkinSound(WeaponsModule module, ItemStack weaponItem, String soundKey) {
        WeaponCosmeticManager cosmetics = module == null ? null : module.getWeaponCosmeticManager();
        return cosmetics == null ? null : cosmetics.getWeaponSkinSound(weaponItem, soundKey);
    }

    private static String customFireSound(WeaponDefinition weapon) {
        return switch (weapon.getCategory()) {
            case PISTOL -> "pistol.fire";
            case SMG -> ThreadLocalRandom.current().nextInt(7) == 0 ? "smg.fire2" : "smg.fire";
            default -> ThreadLocalRandom.current().nextBoolean() ? "ar.fire" : "ar.fire2";
        };
    }

    private static boolean canTarget(WeaponsModule module, Player shooter, WeaponDefinition weapon,
                                     ItemStack weaponItem, LivingEntity target) {
        if (module == null) {
            return true;
        }
        return module.evaluateWeaponTarget(shooter, weapon, weaponItem, target).isAllowed();
    }

    private static ItemStack getHelmet(LivingEntity target) {
        if (target == null) {
            return null;
        }
        EntityEquipment equipment = target.getEquipment();
        return equipment == null ? null : equipment.getHelmet();
    }

    private static void setHelmet(LivingEntity target, ItemStack item) {
        if (target == null) {
            return;
        }
        EntityEquipment equipment = target.getEquipment();
        if (equipment != null) {
            equipment.setHelmet(item);
        }
    }

    private static ItemStack getChestplate(LivingEntity target) {
        if (target == null) {
            return null;
        }
        EntityEquipment equipment = target.getEquipment();
        return equipment == null ? null : equipment.getChestplate();
    }

    private static void setChestplate(LivingEntity target, ItemStack item) {
        if (target == null) {
            return;
        }
        EntityEquipment equipment = target.getEquipment();
        if (equipment != null) {
            equipment.setChestplate(item);
        }
    }

    private static boolean isImpactBlocked(WeaponsModule module, Player shooter, WeaponDefinition weapon,
                                           ItemStack weaponItem, LivingEntity target, Location hitLoc,
                                           double damage, double distance, boolean headshot) {
        if (module == null) {
            return false;
        }
        boolean lethal = target.getHealth() - damage <= 0.0D;
        WeaponCombatDecision decision = module.evaluateWeaponImpact(
                shooter, weapon, weaponItem, target, hitLoc, damage, distance, headshot, lethal);
        return decision.isDenied();
    }

    private static Vector applySpread(Player shooter, WeaponDefinition weapon, ItemStack weaponItem,
                                      Vector direction, AttachmentManager attachmentManager) {
        double spreadDeg = effectiveSpreadDeg(shooter, weapon, weaponItem, attachmentManager);
        if (spreadDeg <= 0.0D) {
            return direction.clone().normalize();
        }
        return randomDirectionInCone(direction, spreadDeg);
    }

    private static double effectiveSpreadDeg(Player shooter, WeaponDefinition weapon, ItemStack weaponItem,
                                             AttachmentManager attachmentManager) {
        boolean aiming = shooter.isSneaking();
        double spreadDeg = aiming ? weapon.getAdsSpreadDeg() : weapon.getHipfireSpreadDeg();

        if (isMoving(shooter)) {
            spreadDeg *= Math.max(0.0D, weapon.getMovingSpreadMultiplier());
        }
        if (aiming) {
            spreadDeg *= Math.max(0.0D, weapon.getSneakSpreadMultiplier());
        }
        if (!shooter.isOnGround()) {
            spreadDeg *= Math.max(0.0D, weapon.getJumpSpreadMultiplier());
        }

        if (attachmentManager != null) {
            spreadDeg *= attachmentManager.getSpreadMultiplier(weaponItem);
            spreadDeg *= aiming
                    ? attachmentManager.getAdsSpreadMultiplier(weaponItem)
                    : attachmentManager.getHipfireSpreadMultiplier(weaponItem);
        }
        return Math.max(0.0D, Math.min(45.0D, spreadDeg));
    }

    private static boolean isMoving(Player shooter) {
        Vector velocity = shooter.getVelocity();
        return velocity != null && velocity.clone().setY(0).lengthSquared() > 0.0036D;
    }

    private static Vector randomDirectionInCone(Vector direction, double spreadDeg) {
        Vector forward = direction.clone().normalize();
        Vector up = Math.abs(forward.dot(new Vector(0, 1, 0))) > 0.98D
                ? new Vector(1, 0, 0)
                : new Vector(0, 1, 0);
        Vector right = forward.clone().crossProduct(up);
        if (right.lengthSquared() < 0.0001D) {
            right = new Vector(1, 0, 0);
        } else {
            right.normalize();
        }
        Vector vertical = right.clone().crossProduct(forward);
        if (vertical.lengthSquared() < 0.0001D) {
            vertical = new Vector(0, 1, 0);
        } else {
            vertical.normalize();
        }

        double radius = Math.tan(Math.toRadians(spreadDeg)) * Math.sqrt(Math.random());
        double theta = Math.random() * Math.PI * 2.0D;
        Vector offset = right.multiply(Math.cos(theta) * radius).add(vertical.multiply(Math.sin(theta) * radius));
        return forward.add(offset).normalize();
    }

    private static double applyDamageFalloff(WeaponDefinition weapon, double damage, double distance) {
        double start = Math.max(0.0D, weapon.getFalloffStartDistance());
        double end = Math.max(start, weapon.getFalloffEndDistance());
        double minMultiplier = Math.max(0.0D, Math.min(1.0D, weapon.getFalloffMinMultiplier()));
        if (distance <= start) {
            return damage;
        }
        if (end <= start) {
            return damage * minMultiplier;
        }
        double progress = Math.max(0.0D, Math.min(1.0D, (distance - start) / (end - start)));
        double multiplier = 1.0D - ((1.0D - minMultiplier) * progress);
        return damage * multiplier;
    }

    private static double fleshDamageMultiplier(AmmoDefinition ammo) {
        return ammo != null ? Math.max(0.0D, ammo.getFleshDamageMultiplier()) : 1.0D;
    }

    private static boolean isAntiMaterial(String ammoType, AmmoDefinition ammo) {
        return "50bmg".equalsIgnoreCase(ammoType)
                || (ammo != null && "anti_material".equalsIgnoreCase(ammo.getPenetrationClass()));
    }

    private static double effectiveMaxDistance(WeaponDefinition weapon, ItemStack weaponItem) {
        AttachmentManager attachmentManager = getAttachmentManager();
        double multiplier = attachmentManager != null ? attachmentManager.getMaxDistanceMultiplier(weaponItem) : 1.0D;
        return weapon.getMaxDistance() * multiplier;
    }

    private static AttachmentManager getAttachmentManager() {
        WeaponsModule module = getModule();
        return module != null ? module.getAttachmentManager() : null;
    }

    private static WeaponsModule getModule() {
        JavaPlugin plugin = JavaPlugin.getProvidingPlugin(ShootingMechanics.class);
        if (!(plugin instanceof it.meridian.core.CorePlugin corePlugin)) {
            return null;
        }
        return corePlugin.getModuleManager().getModule(WeaponsModule.class);
    }

    private static int incrementRiotShieldHits(ItemStack shieldItem, int maxHits, JavaPlugin plugin) {
        if (shieldItem == null || shieldItem.getType() == Material.AIR) {
            return maxHits;
        }

        ItemMeta meta = shieldItem.getItemMeta();
        if (meta == null) {
            return maxHits;
        }

        NamespacedKey key = new NamespacedKey(plugin, RIOT_SHIELD_HITS_KEY);
        Integer current = meta.getPersistentDataContainer().get(key, PersistentDataType.INTEGER);
        int updated = Math.min(maxHits, (current == null ? 0 : current) + 1);
        meta.getPersistentDataContainer().set(key, PersistentDataType.INTEGER, updated);
        shieldItem.setItemMeta(meta);
        return updated;
    }

    private static void applyHitJumpRestriction(Player targetPlayer, JavaPlugin plugin) {
        UUID uuid = targetPlayer.getUniqueId();
        JumpRestrictionManager.restrict(targetPlayer, JumpRestrictionManager.REASON_GUN_HIT);

        BukkitTask existing = hitJumpRestrictionTasks.remove(uuid);
        if (existing != null) {
            existing.cancel();
        }

        BukkitTask task = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            hitJumpRestrictionTasks.remove(uuid);
            Player onlineTarget = Bukkit.getPlayer(uuid);
            if (onlineTarget != null) {
                JumpRestrictionManager.release(onlineTarget, JumpRestrictionManager.REASON_GUN_HIT);
            }
        }, HIT_JUMP_LOCK_TICKS);
        hitJumpRestrictionTasks.put(uuid, task);
    }

    private static void drawBulletTrail(Player shooter, Location eyeLoc, Location hitLoc, Vector direction, WeaponDefinition weapon) {
        double distance = eyeLoc.distance(hitLoc);
        for (double d = 0.5; d < distance; d += 0.5) {
            Location point = eyeLoc.clone().add(direction.clone().multiply(d));
            Particle.DustOptions dust = new Particle.DustOptions(org.bukkit.Color.fromRGB(160, 160, 160), 0.4f);
            shooter.getWorld().spawnParticle(Particle.DUST, point, 1, 0, 0, 0, 0, dust);
        }
    }

    private static void applyBulletDamage(LivingEntity target, double damage, Player shooter, Location hitLoc) {
        DamageSource source = DamageSource.builder(DamageType.MAGIC)
                .withCausingEntity(shooter)
                .withDirectEntity(shooter)
                .withDamageLocation(hitLoc)
                .build();
        target.damage(damage, source);
    }

    private static void notifyHospitalGunshot(Player target, boolean headshot, boolean antiMaterial, double damage) {
        if (target == null || damage <= 0.0D) {
            return;
        }
        WeaponsModule module = getModule();
        if (module == null || module.getCore() == null || module.getCore().getModuleManager() == null) {
            return;
        }

        HospitalModule hospital = module.getCore().getModuleManager().getModule(HospitalModule.class);
        if (hospital == null || hospital.getGunshotService() == null) {
            return;
        }
        hospital.getGunshotService().applyAutomatic(target, severityForHospital(headshot, antiMaterial, damage));
    }

    private static GunshotSeverity severityForHospital(boolean headshot, boolean antiMaterial, double damage) {
        if (headshot || antiMaterial || damage >= 14.0D) {
            return GunshotSeverity.CRITICAL;
        }
        if (damage >= 9.0D) {
            return GunshotSeverity.SEVERE;
        }
        if (damage >= 5.0D) {
            return GunshotSeverity.MODERATE;
        }
        return GunshotSeverity.MINOR;
    }

    private static void emitWeaponShot(Player shooter, WeaponDefinition weapon, ItemStack weaponItem) {
        if (shooter == null || weapon == null) {
            return;
        }
        StaffBoardMetadata metadata = weaponMetadata(weapon, weaponItem)
                .put("shooter_uuid", shooter.getUniqueId())
                .put("shooter_name", shooter.getName())
                .put("ammo_type", weapon.getAmmoType())
                .put("pellet_count", weapon.getPelletCount())
                .putLocation(shooter.getLocation());

        StaffBoardModuleRegistry.emit(StaffBoardLogEvent.builder("combat.weapon.shot", "OpenWeapons")
                .category(StaffBoardCategory.COMBAT)
                .severity(StaffBoardSeverity.NOTICE)
                .sensitivity(StaffBoardSensitivity.DEPARTMENT_ONLY)
                .actor(shooter)
                .location(shooter.getLocation())
                .message(shooter.getName() + " fired " + weapon.getDisplayName() + ".")
                .metadataJson(metadata.toJson())
                .build());
    }

    private static void emitWeaponHit(Player shooter, LivingEntity target, WeaponDefinition weapon,
                                      ItemStack weaponItem, Location hitLoc, double damage, double distance,
                                      boolean headshot) {
        if (shooter == null || target == null || weapon == null) {
            return;
        }
        boolean bodyshot = !headshot;
        String hitZone = headshot ? "HEAD" : "BODY";
        StaffBoardMetadata metadata = weaponMetadata(weapon, weaponItem)
                .put("shooter_uuid", shooter.getUniqueId())
                .put("shooter_name", shooter.getName())
                .put("damage", damage)
                .put("distance", distance)
                .put("headshot", headshot)
                .put("bodyshot", bodyshot)
                .put("hit_zone", hitZone)
                .putLocation(hitLoc);
        StaffBoardLogEvent.Builder hitBuilder = StaffBoardLogEvent.builder("combat.weapon.hit", "OpenWeapons")
                .category(StaffBoardCategory.COMBAT)
                .severity(headshot ? StaffBoardSeverity.WARNING : StaffBoardSeverity.NOTICE)
                .sensitivity(StaffBoardSensitivity.DEPARTMENT_ONLY)
                .actor(shooter)
                .location(hitLoc)
                .message(shooter.getName() + (bodyshot ? " body-shot a target with " : " headshot a target with ")
                        + weapon.getDisplayName() + ".")
                .metadataJson(metadata.toJson());

        if (target instanceof Player targetPlayer) {
            metadata.put("victim_uuid", targetPlayer.getUniqueId())
                    .put("victim_name", targetPlayer.getName());
            hitBuilder.target(targetPlayer)
                    .message(shooter.getName() + (bodyshot ? " body-shot " : " headshot ")
                            + targetPlayer.getName() + " with " + weapon.getDisplayName() + ".");
        }
        StaffBoardModuleRegistry.emit(hitBuilder.metadataJson(metadata.toJson()).build());

        if (target instanceof Player targetPlayer) {
            StaffBoardModuleRegistry.emit(StaffBoardLogEvent.builder("combat.player.damaged", "OpenWeapons")
                    .category(StaffBoardCategory.COMBAT)
                    .severity(headshot ? StaffBoardSeverity.WARNING : StaffBoardSeverity.NOTICE)
                    .sensitivity(StaffBoardSensitivity.DEPARTMENT_ONLY)
                    .actor(shooter)
                    .target(targetPlayer)
                    .location(hitLoc)
                    .message(targetPlayer.getName()
                            + (bodyshot ? " body-shot by " : " headshot by ")
                            + shooter.getName() + ".")
                    .metadataJson(metadata.toJson())
                    .build());
        }
    }

    private static StaffBoardMetadata weaponMetadata(WeaponDefinition weapon, ItemStack weaponItem) {
        WeaponsModule module = getModule();
        String instanceId = module != null ? module.getWeaponRegistry().getInstanceId(weaponItem) : null;
        return StaffBoardMetadata.create()
                .put("weapon_id", weapon.getId())
                .put("weapon_name", weapon.getDisplayName())
                .put("weapon_category", weapon.getCategory().name())
                .put("weapon_instance_id", instanceId)
                .put("source_system", "OpenWeapons");
    }

    private static void ejectSpentCasing(Player shooter, WeaponDefinition weapon) {
        Location eyeLoc = shooter.getEyeLocation();
        Vector forward = eyeLoc.getDirection().normalize();
        Vector right = new Vector(-forward.getZ(), 0, forward.getX());
        if (right.lengthSquared() == 0) {
            right = new Vector(1, 0, 0);
        } else {
            right.normalize();
        }

        Location dropLoc = eyeLoc.clone()
                .add(forward.clone().multiply(0.25))
                .add(right.clone().multiply(0.28))
                .add(0, -0.2, 0);

        Item dropped = shooter.getWorld().dropItem(dropLoc, createSpentCasing(weapon));
        Vector velocity = right.clone().multiply(0.24 + Math.random() * 0.08)
                .add(forward.clone().multiply(-0.04))
                .add(new Vector((Math.random() - 0.5) * 0.04, 0.18 + Math.random() * 0.08,
                        (Math.random() - 0.5) * 0.04));
        dropped.setVelocity(velocity);
        dropped.setPickupDelay(40);

        Bukkit.getScheduler().runTaskLater(JavaPlugin.getProvidingPlugin(ShootingMechanics.class), () -> {
            if (dropped.isValid()) {
                dropped.remove();
            }
        }, 20L * 30L);
    }

    private static ItemStack createSpentCasing(WeaponDefinition weapon) {
        ItemStack item = new ItemStack(Material.IRON_NUGGET);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text(spentCasingName(weapon), NamedTextColor.GOLD)
                    .decoration(TextDecoration.ITALIC, false));
            int customModelData = spentCasingModelData(weapon);
            if (customModelData > 0) {
                meta.setCustomModelData(customModelData);
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    private static int spentCasingModelData(WeaponDefinition weapon) {
        String ammoType = weapon.getAmmoType() == null ? "" : weapon.getAmmoType().toLowerCase(java.util.Locale.ROOT);
        return switch (ammoType) {
            case "9mm" -> 2;
            case "46mm", "4.6x30mm" -> 3;
            case "762nato", "762", "7.62", "7.72" -> 4;
            case "50ae", "50_ae" -> 6;
            case "12gauge", "12_gauge" -> 8;
            default -> 1;
        };
    }

    private static String spentCasingName(WeaponDefinition weapon) {
        return switch (weapon.getCategory()) {
            case PISTOL -> "Spent Pistol Casing";
            case SMG -> "Spent SMG Casing";
            case SNIPER -> "Spent Sniper Casing";
            case SHOTGUN -> "Spent Shotgun Shell";
            default -> "Spent Rifle Casing";
        };
    }

}
