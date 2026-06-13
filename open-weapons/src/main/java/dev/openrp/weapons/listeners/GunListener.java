package dev.openrp.weapons.listeners;

import dev.openrp.weapons.attachments.AttachmentSlot;
import dev.openrp.weapons.api.WeaponCombatDecision;
import dev.openrp.weapons.cosmetics.WeaponCosmeticManager;
import dev.openrp.weapons.cosmetics.WeaponVisualVariantResolver;
import dev.openrp.weapons.mechanics.GroundedFireRules;
import dev.openrp.weapons.mechanics.ShootingMechanics;
import dev.openrp.weapons.model.AmmoDefinition;
import dev.openrp.weapons.model.FireMode;
import dev.openrp.weapons.model.WeaponCategory;
import dev.openrp.weapons.model.WeaponDefinition;
import dev.openrp.weapons.model.WeaponState;
import dev.openrp.weapons.model.WeaponVisualState;
import dev.openrp.weapons.module.WeaponsModule;
import dev.openrp.weapons.util.JumpRestrictionManager;
import it.meridian.core.module.ModuleManager;
import it.meridian.core.staffboard.StaffBoardMetadata;
import it.meridian.core.staffboard.model.StaffBoardCategory;
import it.meridian.core.staffboard.model.StaffBoardLogEvent;
import it.meridian.core.staffboard.model.StaffBoardSensitivity;
import it.meridian.core.staffboard.model.StaffBoardSeverity;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.ChargedProjectiles;
import io.papermc.paper.datacomponent.item.CustomModelData;
import io.papermc.paper.event.player.PlayerArmSwingEvent;
import io.papermc.paper.event.player.PlayerStopUsingItemEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerAnimationEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.components.CustomModelDataComponent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class GunListener implements Listener {
    private final WeaponsModule module;
    // UUID -> (weapon instance ID -> State)
    private final Map<UUID, Map<String, WeaponState>> states = new HashMap<>();
    private final Map<UUID, BukkitTask> autoFireTasks = new HashMap<>();
    private final Map<UUID, BukkitTask> burstFireTasks = new HashMap<>();
    // Track players currently scoped (have scope slowness applied)
    private final Set<UUID> scopedPlayers = new HashSet<>();
    private final Set<UUID> aimingAnimationPlayers = new HashSet<>();
    private final Map<UUID, SemiFireLock> semiFireLocks = new HashMap<>();
    private final Map<UUID, Long> reloadInputLocks = new HashMap<>();
    private final Map<UUID, Long> sneakShootLocks = new HashMap<>();
    private final Map<UUID, Long> fireInputLocks = new HashMap<>();
    private final NamespacedKey ghostDurabilityKey;
    private final NamespacedKey ghostMaxDurabilityKey;
    private final NamespacedKey weaponVisualStateKey;
    private final NamespacedKey weaponHasMagazineVisualKey;
    private final NamespacedKey weaponCurrentAmmoKey;
    private final NamespacedKey weaponMagazineLoadedKey;
    private final NamespacedKey aimingProxyKey;
    private BukkitTask aimingAnimationTask;
    private final Map<UUID, AimingSwap> aimingSwaps = new HashMap<>();
    private static final int CROSSHAIR_CUSTOM_MODEL_DATA = 9001;
    private static final long FIRE_INPUT_DEBOUNCE_MILLIS = 75L;

    public GunListener(WeaponsModule module) {
        this.module = module;
        this.ghostDurabilityKey = new NamespacedKey(module.getCore(), "ghost_weapon_durability");
        this.ghostMaxDurabilityKey = new NamespacedKey(module.getCore(), "ghost_weapon_max_durability");
        this.weaponVisualStateKey = new NamespacedKey(module.getCore(), "weapon_visual_state");
        this.weaponHasMagazineVisualKey = new NamespacedKey(module.getCore(), "weapon_has_magazine_visual");
        this.weaponCurrentAmmoKey = new NamespacedKey(module.getCore(), "weapon_current_ammo");
        this.weaponMagazineLoadedKey = new NamespacedKey(module.getCore(), "weapon_magazine_loaded");
        this.aimingProxyKey = new NamespacedKey(module.getCore(), "weapon_aiming_proxy");
        if (module.getWeaponAnimationSuppressor() != null) {
            module.getWeaponAnimationSuppressor().setAimingSwingHandler(this::handleAimingPacketSwing);
        }
        startAimingAnimationTask();
    }

    public void cleanup() {
        if (module.getWeaponAnimationSuppressor() != null) {
            module.getWeaponAnimationSuppressor().setAimingSwingHandler(null);
        }
        for (BukkitTask task : autoFireTasks.values()) {
            task.cancel();
        }
        for (BukkitTask task : burstFireTasks.values()) {
            task.cancel();
        }
        autoFireTasks.clear();
        burstFireTasks.clear();
        semiFireLocks.clear();
        reloadInputLocks.clear();
        sneakShootLocks.clear();
        fireInputLocks.clear();
        if (aimingAnimationTask != null) {
            aimingAnimationTask.cancel();
            aimingAnimationTask = null;
        }
        for (UUID uuid : new HashSet<>(aimingSwaps.keySet())) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                restoreAimingSwap(player);
            }
        }
        for (UUID uuid : new HashSet<>(aimingAnimationPlayers)) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                stopAimingAnimation(player);
            }
        }
        for (UUID uuid : scopedPlayers) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                resetWeaponVisual(player.getInventory().getItemInMainHand());
                JumpRestrictionManager.release(player, JumpRestrictionManager.REASON_GUN);
            }
        }
        states.clear();
        scopedPlayers.clear();
        aimingSwaps.clear();
    }

    public void refillWeapon(Player player, ItemStack item) {
        if (player == null || item == null || item.getType().isAir()) {
            return;
        }
        WeaponDefinition weapon = module.getWeaponRegistry().getWeapon(item);
        if (!isAimingFirearm(weapon)) {
            return;
        }
        WeaponState state = getState(player, item, weapon);
        state.setCurrentAmmo(effectiveMagazineSize(item, weapon));
        state.setHasMagazine(true);
        state.setReloading(false);
        syncStateWithWeaponItem(item, weapon, state);
        updateWeaponVisual(player, weapon, player.isSneaking() ? WeaponVisualState.AIMING : WeaponVisualState.IDLE);
        updateActionBar(player, state, weapon, item);
    }

    public void refillPlayerWeapons(Player player) {
        if (player == null) {
            return;
        }
        for (ItemStack item : player.getInventory().getStorageContents()) {
            refillWeapon(player, item);
        }
        refillWeapon(player, player.getInventory().getItemInMainHand());
        refillWeapon(player, player.getInventory().getItemInOffHand());
    }

    private WeaponState getState(Player player, ItemStack item, WeaponDefinition weapon) {
        Map<String, WeaponState> playerStates = states.computeIfAbsent(player.getUniqueId(), k -> new HashMap<>());
        boolean duplicatedInstance = hasDuplicateWeaponInstance(player, item);
        String instanceId = duplicatedInstance
                ? module.getWeaponRegistry().assignNewInstanceId(item)
                : module.getWeaponRegistry().getOrCreateInstanceId(item);
        String stateKey = instanceId != null ? instanceId : weapon.getId();
        return playerStates.computeIfAbsent(stateKey, k -> {
            WeaponState state = new WeaponState(effectiveMagazineSize(item, weapon), initialFireMode(weapon));
            if (duplicatedInstance) {
                state.setCurrentAmmo(0);
                state.setHasMagazine(false);
                persistState(item, weapon, state);
            } else {
                loadPersistentState(item, weapon, state);
            }
            return state;
        });
    }

    private FireMode initialFireMode(WeaponDefinition weapon) {
        List<FireMode> modes = weapon.getFireModes();
        if (modes.isEmpty()) {
            return weapon.isAutomatic() ? FireMode.AUTO : FireMode.SEMI;
        }
        if (weapon.isAutomatic() && modes.contains(FireMode.AUTO)) {
            return FireMode.AUTO;
        }
        return modes.get(0);
    }

    private void loadPersistentState(ItemStack item, WeaponDefinition weapon, WeaponState state) {
        if (item == null || weapon == null || state == null || !item.hasItemMeta()) {
            return;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return;
        }
        int capacity = effectiveMagazineSize(item, weapon);
        Integer storedAmmo = meta.getPersistentDataContainer().get(weaponCurrentAmmoKey, PersistentDataType.INTEGER);
        Byte storedMagazine = meta.getPersistentDataContainer().get(weaponMagazineLoadedKey, PersistentDataType.BYTE);
        if (storedMagazine != null) {
            state.setHasMagazine(storedMagazine == (byte) 1);
        }
        if (storedAmmo != null) {
            state.setCurrentAmmo(Math.max(0, Math.min(capacity, storedAmmo)));
        } else if (!state.hasMagazine()) {
            state.setCurrentAmmo(0);
        } else {
            state.setCurrentAmmo(capacity);
        }
    }

    private boolean hasDuplicateWeaponInstance(Player player, ItemStack item) {
        String instanceId = module.getWeaponRegistry().getInstanceId(item);
        if (instanceId == null || instanceId.isBlank()) {
            return false;
        }

        int matches = 0;
        for (ItemStack inventoryItem : player.getInventory().getStorageContents()) {
            if (inventoryItem == null || inventoryItem.getType().isAir()) {
                continue;
            }
            if (instanceId.equals(module.getWeaponRegistry().getInstanceId(inventoryItem)) && ++matches > 1) {
                return true;
            }
        }
        ItemStack offhand = player.getInventory().getItemInOffHand();
        return offhand != null
                && !offhand.getType().isAir()
                && instanceId.equals(module.getWeaponRegistry().getInstanceId(offhand))
                && ++matches > 1;
    }

    private void startAimingAnimationTask() {
        aimingAnimationTask = new BukkitRunnable() {
            @Override
            public void run() {
                for (UUID uuid : new HashSet<>(aimingAnimationPlayers)) {
                    Player player = Bukkit.getPlayer(uuid);
                    if (player == null || !player.isOnline() || player.isDead() || !player.isSneaking()) {
                        if (player != null) {
                            stopAimingAnimation(player);
                        } else {
                            aimingAnimationPlayers.remove(uuid);
                        }
                        continue;
                    }

                    ItemStack item = getActiveWeaponItem(player);
                    WeaponDefinition weapon = module.getWeaponRegistry().getWeapon(item);
                    if (!canStartAiming(player, item, weapon)) {
                        stopAimingAnimation(player);
                    } else {
                        ensureAimingUseAnimation(player, item);
                        refreshAimingPose(player, item, weapon);
                    }
                }
            }
        }.runTaskTimer(module.getCore(), 1L, 5L);
    }

    private boolean isAimingFirearm(WeaponDefinition weapon) {
        return weapon != null
                && weapon.getCategory() != WeaponCategory.MELEE
                && weapon.getCategory() != WeaponCategory.TASER;
    }

    private boolean canStartAiming(Player player, ItemStack item, WeaponDefinition weapon) {
        if (player == null || module.getHandcuffManager().isRestrained(player) || !isAimingFirearm(weapon)) {
            return false;
        }
        if (item == null || item.getType().isAir()) {
            return false;
        }
        WeaponDefinition registeredWeapon = module.getWeaponRegistry().getWeapon(item);
        return registeredWeapon != null
                && registeredWeapon.getId().equals(weapon.getId())
                && !getState(player, item, registeredWeapon).isReloading();
    }

    private WeaponVisualState resolveInactiveVisualState(Player player, ItemStack item, WeaponDefinition weapon) {
        if (!isAimingFirearm(weapon) || item == null || item.getType().isAir()) {
            return WeaponVisualState.IDLE;
        }
        return getState(player, item, weapon).isReloading() ? WeaponVisualState.RELOADING : WeaponVisualState.IDLE;
    }

    private void refreshAimingPose(Player player, ItemStack item, WeaponDefinition weapon) {
        applyAimingUseVisual(item, weapon);
        refreshAimingOffhandVisual(player, item, weapon);
        if (module.getWeaponAnimationSuppressor() != null) {
            module.getWeaponAnimationSuppressor().showAimingPose(player, true);
        }
    }

    private boolean startAimingAnimation(Player player, WeaponDefinition weapon) {
        ItemStack currentItem = getActiveWeaponItem(player);
        WeaponDefinition currentWeapon = module.getWeaponRegistry().getWeapon(currentItem);
        if (currentWeapon == null || (weapon != null && !currentWeapon.getId().equals(weapon.getId()))) {
            currentWeapon = weapon;
        }
        if (!canStartAiming(player, currentItem, currentWeapon)) {
            return false;
        }
        if (!beginAimingSwap(player, currentWeapon)) {
            return false;
        }
        ItemStack item = getActiveWeaponItem(player);
        WeaponDefinition activeWeapon = module.getWeaponRegistry().getWeapon(item);
        if (!canStartAiming(player, item, activeWeapon)) {
            restoreAimingSwap(player);
            return false;
        }
        module.getWeaponRegistry().applyFirearmUseAnimation(item, activeWeapon);
        updateWeaponVisual(player, activeWeapon, WeaponVisualState.AIMING);
        ensureAimingUseAnimation(player, item);
        aimingAnimationPlayers.add(player.getUniqueId());
        refreshAimingPose(player, item, activeWeapon);
        Bukkit.getScheduler().runTaskLater(module.getCore(), () -> {
            if (!aimingAnimationPlayers.contains(player.getUniqueId()) || !player.isOnline() || player.isDead() || !player.isSneaking()) {
                return;
            }
            ItemStack activeItem = getActiveWeaponItem(player);
            WeaponDefinition scheduledWeapon = module.getWeaponRegistry().getWeapon(activeItem);
            if (canStartAiming(player, activeItem, scheduledWeapon)) {
                refreshAimingPose(player, activeItem, scheduledWeapon);
            }
        }, 1L);
        return true;
    }

    private void stopAimingAnimation(Player player) {
        boolean wasAiming = aimingAnimationPlayers.remove(player.getUniqueId());
        clearAimingUseAnimation(player);
        ItemStack item = getActiveWeaponItem(player);
        WeaponDefinition weapon = module.getWeaponRegistry().getWeapon(item);
        if (weapon != null && isAimingFirearm(weapon) && player.isOnline() && !player.isDead()) {
            updateWeaponVisual(player, weapon, resolveInactiveVisualState(player, item, weapon));
        }
        restoreAimingSwap(player);
        if (wasAiming && module.getWeaponAnimationSuppressor() != null) {
            module.getWeaponAnimationSuppressor().showAimingPose(player, false);
        }
    }

    private void ensureAimingUseAnimation(Player player, ItemStack item) {
        WeaponDefinition weapon = module.getWeaponRegistry().getWeapon(item);
        if (!isAimingFirearm(weapon)) {
            return;
        }
        applyAimingUseVisual(item, weapon);
        refreshAimingOffhandVisual(player, item, weapon);
        if (isAimingProxy(player.getInventory().getItemInMainHand())) {
            if (player.hasActiveItem()) {
                player.clearActiveItem();
            }
            return;
        }
        if (weapon.getMaterial() == Material.CROSSBOW
                && item != null
                && item.getType() == Material.CROSSBOW) {
            if (!player.hasActiveItem() || player.getActiveItemHand() != EquipmentSlot.HAND) {
                if (player.hasActiveItem()) {
                    player.clearActiveItem();
                }
                player.startUsingItem(EquipmentSlot.HAND);
            }
        } else if (player.hasActiveItem()
                && (player.getActiveItemHand() == EquipmentSlot.HAND || player.getActiveItemHand() == EquipmentSlot.OFF_HAND)) {
            player.clearActiveItem();
        }
    }

    private void clearAimingUseAnimation(Player player) {
        if (player.hasActiveItem()
                && (player.getActiveItemHand() == EquipmentSlot.HAND || player.getActiveItemHand() == EquipmentSlot.OFF_HAND)) {
            ItemStack item = getActiveWeaponItem(player);
            WeaponDefinition weapon = module.getWeaponRegistry().getWeapon(item);
            if (isAimingFirearm(weapon)) {
                player.clearActiveItem();
            }
        }
    }

    private ItemStack getActiveWeaponItem(Player player) {
        ItemStack mainHand = player.getInventory().getItemInMainHand();
        if (isAimingProxy(mainHand)) {
            ItemStack offhand = player.getInventory().getItemInOffHand();
            if (module.getWeaponRegistry().getWeapon(offhand) != null) {
                return offhand;
            }
        }
        return mainHand;
    }

    private boolean beginAimingSwap(Player player, WeaponDefinition weapon) {
        ItemStack mainHand = player.getInventory().getItemInMainHand();
        if (isActiveAimingSwap(player)) {
            return weapon.getId().equals(getAimingWeaponId(player));
        }
        if (isAimingProxy(mainHand)) {
            return weapon.getId().equals(getAimingWeaponId(player));
        }
        WeaponDefinition mainWeapon = module.getWeaponRegistry().getWeapon(mainHand);
        if (mainWeapon == null || !mainWeapon.getId().equals(weapon.getId())) {
            return false;
        }

        ItemStack offhand = player.getInventory().getItemInOffHand();
        ItemStack stashedOffhand = offhand == null || offhand.getType().isAir() ? null : offhand.clone();
        aimingSwaps.put(player.getUniqueId(), new AimingSwap(player.getInventory().getHeldItemSlot(), stashedOffhand));
        player.getInventory().setItemInOffHand(mainHand);
        player.getInventory().setItemInMainHand(createAimingProxy());
        applyAimingUseVisual(mainHand, mainWeapon);
        player.updateInventory();
        return true;
    }

    private String getAimingWeaponId(Player player) {
        ItemStack item = getActiveWeaponItem(player);
        WeaponDefinition weapon = module.getWeaponRegistry().getWeapon(item);
        return weapon != null ? weapon.getId() : "";
    }

    private void restoreAimingSwap(Player player) {
        AimingSwap swap = aimingSwaps.remove(player.getUniqueId());
        ItemStack mainHand = player.getInventory().getItemInMainHand();
        boolean hadProxy = isAimingProxy(mainHand);
        if (swap == null && !hadProxy) {
            return;
        }

        ItemStack offhand = player.getInventory().getItemInOffHand();
        WeaponDefinition mainWeapon = module.getWeaponRegistry().getWeapon(mainHand);
        if (swap != null && !hadProxy && mainWeapon != null) {
            clearAimingUseVisual(mainHand, mainWeapon);
            module.getWeaponRegistry().applyFirearmUseAnimation(mainHand, mainWeapon);
            player.updateInventory();
            return;
        }

        WeaponDefinition offhandWeapon = module.getWeaponRegistry().getWeapon(offhand);
        ItemStack weaponItem = offhandWeapon != null ? offhand : null;
        if (weaponItem != null) {
            player.getInventory().setItemInOffHand(null);
            clearAimingUseVisual(weaponItem, offhandWeapon);
            module.getWeaponRegistry().applyFirearmUseAnimation(weaponItem, offhandWeapon);
        }
        if (hadProxy) {
            player.getInventory().setItemInMainHand(null);
        }

        if (weaponItem != null && !weaponItem.getType().isAir()) {
            int targetSlot = swap != null ? swap.slot() : player.getInventory().getHeldItemSlot();
            if (targetSlot == player.getInventory().getHeldItemSlot()) {
                ItemStack currentMain = player.getInventory().getItemInMainHand();
                if (currentMain == null || currentMain.getType().isAir() || isAimingProxy(currentMain)) {
                    player.getInventory().setItemInMainHand(weaponItem);
                } else {
                    giveOrDrop(player, weaponItem);
                }
            } else {
                ItemStack slotItem = player.getInventory().getItem(targetSlot);
                if (slotItem == null || slotItem.getType().isAir() || isAimingProxy(slotItem)) {
                    player.getInventory().setItem(targetSlot, weaponItem);
                } else {
                    giveOrDrop(player, weaponItem);
                }
            }
        }
        if (swap != null && swap.stashedOffhand() != null && !swap.stashedOffhand().getType().isAir()) {
            ItemStack currentOffhand = player.getInventory().getItemInOffHand();
            if (currentOffhand == null || currentOffhand.getType().isAir()) {
                player.getInventory().setItemInOffHand(swap.stashedOffhand());
            } else {
                giveOrDrop(player, swap.stashedOffhand());
            }
        }
        player.updateInventory();
    }

    private void removeActiveWeapon(Player player) {
        ItemStack mainHand = player.getInventory().getItemInMainHand();
        if (aimingSwaps.containsKey(player.getUniqueId()) && !isAimingProxy(mainHand)) {
            AimingSwap swap = aimingSwaps.remove(player.getUniqueId());
            if (module.getWeaponRegistry().getWeapon(mainHand) != null) {
                player.getInventory().setItemInMainHand(null);
            }
            if (isAimingProxy(player.getInventory().getItemInOffHand())) {
                player.getInventory().setItemInOffHand(null);
            }
            if (swap != null && swap.stashedOffhand() != null && !swap.stashedOffhand().getType().isAir()) {
                player.getInventory().setItemInOffHand(swap.stashedOffhand());
            }
            player.updateInventory();
            return;
        }
        if (isAimingProxy(mainHand) || aimingSwaps.containsKey(player.getUniqueId())) {
            AimingSwap swap = aimingSwaps.remove(player.getUniqueId());
            if (module.getWeaponRegistry().getWeapon(player.getInventory().getItemInOffHand()) != null) {
                player.getInventory().setItemInOffHand(null);
            }
            if (isAimingProxy(mainHand)) {
                player.getInventory().setItemInMainHand(null);
            }
            if (swap != null && swap.stashedOffhand() != null && !swap.stashedOffhand().getType().isAir()) {
                player.getInventory().setItemInOffHand(swap.stashedOffhand());
            }
            player.updateInventory();
            return;
        }
        player.getInventory().setItemInMainHand(null);
    }

    private ItemStack createAimingProxy() {
        ItemStack item = new ItemStack(Material.PAPER);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.itemName(Component.empty());
            meta.setHideTooltip(true);
            meta.setCustomModelData(CROSSHAIR_CUSTOM_MODEL_DATA);
            CustomModelDataComponent customModelData = meta.getCustomModelDataComponent();
            customModelData.setFloats(List.of((float) CROSSHAIR_CUSTOM_MODEL_DATA));
            meta.setCustomModelDataComponent(customModelData);
            meta.getPersistentDataContainer().set(aimingProxyKey, PersistentDataType.BYTE, (byte) 1);
            item.setItemMeta(meta);
        }
        item.setData(DataComponentTypes.CUSTOM_MODEL_DATA,
                CustomModelData.customModelData().addFloat((float) CROSSHAIR_CUSTOM_MODEL_DATA));
        return item;
    }

    private boolean isAimingProxy(ItemStack item) {
        return item != null
                && !item.getType().isAir()
                && item.hasItemMeta()
                && item.getItemMeta().getPersistentDataContainer().has(aimingProxyKey, PersistentDataType.BYTE);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onDropAimingProxy(PlayerDropItemEvent event) {
        if (!isAimingProxy(event.getItemDrop().getItemStack())) {
            return;
        }
        event.setCancelled(true);
        event.getItemDrop().remove();
        Player player = event.getPlayer();
        ItemStack aimedItem = getActiveWeaponItem(player);
        WeaponDefinition aimedWeapon = module.getWeaponRegistry().getWeapon(aimedItem);
        JumpRestrictionManager.release(player, JumpRestrictionManager.REASON_GUN);
        stopAimingAnimation(player);
        stopAutoFire(player);
        stopBurstFire(player);
        semiFireLocks.remove(player.getUniqueId());
        reloadInputLocks.remove(player.getUniqueId());
        if (scopedPlayers.contains(player.getUniqueId())) {
            removeScopeSlowness(player);
        }
        ItemStack restoredMainHand = player.getInventory().getItemInMainHand();
        WeaponDefinition restoredWeapon = module.getWeaponRegistry().getWeapon(restoredMainHand);
        if (aimedWeapon != null
                && restoredWeapon != null
                && aimedWeapon.getId().equals(restoredWeapon.getId())) {
            player.getInventory().setItemInMainHand(null);
            player.getWorld().dropItemNaturally(player.getLocation(), restoredMainHand);
        }
        player.updateInventory();
    }

    private boolean isActiveAimingSwap(Player player) {
        AimingSwap swap = aimingSwaps.get(player.getUniqueId());
        if (swap == null) {
            return false;
        }
        WeaponDefinition mainWeapon = module.getWeaponRegistry().getWeapon(player.getInventory().getItemInMainHand());
        if (isAimingFirearm(mainWeapon)) {
            return true;
        }
        WeaponDefinition weapon = module.getWeaponRegistry().getWeapon(player.getInventory().getItemInOffHand());
        return isAimingFirearm(weapon);
    }

    private ItemStack getStashedOffhand(Player player) {
        AimingSwap swap = aimingSwaps.get(player.getUniqueId());
        return swap != null ? swap.stashedOffhand() : null;
    }

    private void applyAimingUseVisual(ItemStack item, WeaponDefinition weapon) {
        if (!isAimingFirearm(weapon) || item == null) {
            return;
        }
        if (weapon.getMaterial() == Material.CROSSBOW && item.getType() == Material.CROSSBOW) {
            item.setData(DataComponentTypes.CHARGED_PROJECTILES, ChargedProjectiles.chargedProjectiles()
                    .addAll(List.of(new ItemStack(Material.ARROW)))
                    .build());
            return;
        }
        module.getWeaponRegistry().applyFirearmUseAnimation(item, weapon);
    }

    private void clearAimingUseVisual(ItemStack item, WeaponDefinition weapon) {
        if (!isAimingFirearm(weapon) || item == null) {
            return;
        }
        if (weapon.getMaterial() == Material.CROSSBOW && item.getType() == Material.CROSSBOW) {
            item.resetData(DataComponentTypes.CHARGED_PROJECTILES);
        }
        module.getWeaponRegistry().applyFirearmUseAnimation(item, weapon);
    }

    private void refreshAimingOffhandVisual(Player player, ItemStack item, WeaponDefinition weapon) {
        if (isAimingProxy(player.getInventory().getItemInMainHand()) && isAimingFirearm(weapon)) {
            ItemStack offhand = player.getInventory().getItemInOffHand();
            if (offhand != item) {
                player.getInventory().setItemInOffHand(item);
            }
            return;
        }
        if (isAimingProxy(player.getInventory().getItemInOffHand())) {
            player.getInventory().setItemInOffHand(null);
        }
    }

    @EventHandler
    public void onConsumeWeapon(PlayerItemConsumeEvent event) {
        ItemStack item = event.getItem();
        WeaponDefinition weapon = module.getWeaponRegistry().getWeapon(item);
        if (weapon == null) {
            return;
        }
        event.setCancelled(true);
        module.getWeaponRegistry().applyFirearmUseAnimation(item, weapon);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onVanillaCrossbowShoot(EntityShootBowEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        ItemStack bow = event.getBow();
        WeaponDefinition weapon = module.getWeaponRegistry().getWeapon(bow);
        if (!isAimingFirearm(weapon)) {
            ItemStack activeItem = getActiveWeaponItem(player);
            weapon = module.getWeaponRegistry().getWeapon(activeItem);
            if (!isAimingFirearm(weapon)) {
                return;
            }
        }

        event.setCancelled(true);
        Bukkit.getScheduler().runTask(module.getCore(), () -> {
            ItemStack activeItem = getActiveWeaponItem(player);
            WeaponDefinition activeWeapon = module.getWeaponRegistry().getWeapon(activeItem);
            if (isAimingFirearm(activeWeapon)) {
                module.getWeaponRegistry().applyFirearmUseAnimation(activeItem, activeWeapon);
                if (isActiveAimingSwap(player)) {
                    refreshAimingPose(player, activeItem, activeWeapon);
                }
            }
        });
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) {
            if (event.getHand() == EquipmentSlot.OFF_HAND
                    && (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK)
                    && module.getMagazineManager().isMagazine(event.getItem())
                    && module.getWeaponRegistry().getWeapon(event.getPlayer().getInventory().getItemInMainHand()) == null) {
                event.setCancelled(true);
                tryFillMagazine(event.getPlayer(), event.getItem());
            }
            return;
        }

        Player player = event.getPlayer();
        
        if (module.getHandcuffManager().isRestrained(player)) {
            return;
        }

        Action action = event.getAction();
        ItemStack item = event.getItem();
        if (item == null || item.getType().isAir()) {
            item = player.getInventory().getItemInMainHand();
        }
        if ((action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK)
                && module.getMagazineManager().isMagazine(item)) {
            event.setCancelled(true);
            tryFillMagazine(player, item);
            return;
        }

        if (isAimingProxy(item)) {
            handleAimingInteract(event, player, action);
            return;
        }

        WeaponDefinition weapon = module.getWeaponRegistry().getWeapon(item);
        if (weapon == null || weapon.getCategory() == WeaponCategory.MELEE || weapon.getCategory() == WeaponCategory.TASER) return;

        module.getWeaponRegistry().applyFirearmUseAnimation(item, weapon);
        event.setCancelled(true);
        event.setUseInteractedBlock(Event.Result.DENY);
        event.setUseItemInHand(Event.Result.DENY);

        WeaponState state = getState(player, item, weapon);
        syncStateWithWeaponItem(item, weapon, state);

        if (action == Action.LEFT_CLICK_AIR || action == Action.LEFT_CLICK_BLOCK) {
            handleFireInput(player, action);
            return;
        } else if (action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK) {
            handleReloadInput(player, item, weapon, state);
        }
    }

    private void handleAimingInteract(PlayerInteractEvent event, Player player, Action action) {
        event.setCancelled(true);
        event.setUseInteractedBlock(Event.Result.DENY);
        event.setUseItemInHand(Event.Result.DENY);

        handleAimingAction(player, action);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onAimingArmSwing(PlayerAnimationEvent event) {
        Player player = event.getPlayer();
        if (!isActiveAimingSwap(player) && !isAimingProxy(player.getInventory().getItemInMainHand())) {
            return;
        }
        event.setCancelled(true);
        handleFireInput(player, Action.LEFT_CLICK_AIR);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onAimingPaperArmSwing(PlayerArmSwingEvent event) {
        if (event.getHand() != EquipmentSlot.HAND && event.getHand() != EquipmentSlot.OFF_HAND) {
            return;
        }
        Player player = event.getPlayer();
        if (!isActiveAimingSwap(player) && !isAimingProxy(player.getInventory().getItemInMainHand())) {
            return;
        }
        event.setCancelled(true);
        handleFireInput(player, Action.LEFT_CLICK_AIR);
    }

    private void handleAimingPacketSwing(Player player) {
        handleFireInput(player, Action.LEFT_CLICK_AIR);
    }

    private void handleAimingAction(Player player, Action action) {
        ItemStack weaponItem = getActiveWeaponItem(player);
        WeaponDefinition weapon = module.getWeaponRegistry().getWeapon(weaponItem);
        if (!isAimingFirearm(weapon)) {
            stopAimingAnimation(player);
            return;
        }

        WeaponState state = getState(player, weaponItem, weapon);
        syncStateWithWeaponItem(weaponItem, weapon, state);

        if (action == Action.LEFT_CLICK_AIR || action == Action.LEFT_CLICK_BLOCK) {
            handleFireInput(player, action);
        } else if (action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK) {
            handleReloadInput(player, weaponItem, weapon, state);
        }
    }

    private void handleFireInput(Player player, Action action) {
        if (action != Action.LEFT_CLICK_AIR && action != Action.LEFT_CLICK_BLOCK) {
            return;
        }
        if (isDuplicateFireInput(player)) {
            return;
        }

        ItemStack weaponItem = getActiveWeaponItem(player);
        WeaponDefinition weapon = module.getWeaponRegistry().getWeapon(weaponItem);
        if (!isAimingFirearm(weapon)) {
            if (isAimingProxy(player.getInventory().getItemInMainHand())) {
                stopAimingAnimation(player);
            }
            return;
        }

        WeaponState state = getState(player, weaponItem, weapon);
        syncStateWithWeaponItem(weaponItem, weapon, state);
        fireWithLeftClick(player, weapon, state);
    }

    private boolean isDuplicateFireInput(Player player) {
        long now = System.currentTimeMillis();
        Long lastInput = fireInputLocks.get(player.getUniqueId());
        if (lastInput != null && now - lastInput < FIRE_INPUT_DEBOUNCE_MILLIS) {
            return true;
        }
        fireInputLocks.put(player.getUniqueId(), now);
        return false;
    }

    private void fireWithLeftClick(Player player, WeaponDefinition weapon, WeaponState state) {
        boolean aiming = player.isSneaking()
                || isActiveAimingSwap(player)
                || isAimingProxy(player.getInventory().getItemInMainHand());
        if (!canAttemptShot(player, state, aiming, true)) {
            return;
        }
        switch (state.getFireMode()) {
            case AUTO -> {
                if (aiming) {
                    startAutoFire(player, weapon, state);
                } else {
                    tryShoot(player, weapon, state);
                }
            }
            case BURST -> startBurstFire(player, weapon, state);
            case SEMI -> tryShoot(player, weapon, state);
        }
    }

    private void handleReloadInput(Player player, ItemStack weaponItem, WeaponDefinition weapon, WeaponState state) {
        long now = System.currentTimeMillis();
        Long lastReloadInput = reloadInputLocks.get(player.getUniqueId());
        if (lastReloadInput != null && now - lastReloadInput < 150L) {
            return;
        }
        reloadInputLocks.put(player.getUniqueId(), now);

        stopAutoFire(player);
        stopBurstFire(player);

        if (usesLooseAmmoReload(weapon)) {
            tryReloadLooseAmmo(player, weaponItem, weapon, state);
            if (!state.isReloading()) {
                updateActionBar(player, state, weapon, weaponItem);
            }
            return;
        }

        MagazineSource magazine = findMagazineSource(player, weapon);
        if (magazine != null) {
            tryLoadMagazine(player, weaponItem, weapon, state, magazine);
        } else {
            sendWeaponStatus(player, Component.text(state.hasMagazine()
                    ? "Press F to remove the inserted magazine."
                    : "No compatible magazine found.", NamedTextColor.YELLOW));
        }
        if (!state.isReloading()) {
            updateActionBar(player, state, weapon, weaponItem);
        }
    }

    private void tryShoot(Player player, WeaponDefinition weapon, WeaponState state) {
        long now = System.currentTimeMillis();
        ItemStack heldWeapon = getActiveWeaponItem(player);
        long fireRateMs = effectiveFireRateTicks(heldWeapon, weapon) * 50L;

        // Allow 10ms of jitter for Bukkit tasks
        if (now - state.getLastShotTime() < fireRateMs - 10) return;

        boolean aiming = player.isSneaking()
                || isActiveAimingSwap(player)
                || isAimingProxy(player.getInventory().getItemInMainHand());
        if (!canAttemptShot(player, state, aiming, true)) {
            return;
        }

        if (!canUseWeapon(player, weapon, heldWeapon)) {
            return;
        }

        if (module.getCombatStunManager().isStunned(player.getUniqueId())) {
            long remaining = module.getCombatStunManager().getRemainingStunTimeSeconds(player.getUniqueId());
            sendWeaponStatus(player, Component.text("You are stunned and cannot shoot for " + remaining + "s", NamedTextColor.RED));
            return;
        }

        if (!state.hasMagazine()) {
            if (tryStartReloadFromAvailableAmmunition(player, heldWeapon, weapon, state)) {
                return;
            }
            player.playSound(player.getLocation(), Sound.BLOCK_DISPENSER_FAIL, 1.0f, 1.5f);
            sendWeaponStatus(player, Component.text(usesLooseAmmoReload(weapon) ? "No shells loaded." : "No magazine loaded.", NamedTextColor.RED));
            return;
        }

        if (state.getCurrentAmmo() <= 0) {
            if (tryStartReloadFromAvailableAmmunition(player, heldWeapon, weapon, state)) {
                return;
            }
            player.playSound(player.getLocation(), Sound.BLOCK_DISPENSER_FAIL, 1.0f, 2.0f); // Click
            sendWeaponStatus(player, Component.text(usesLooseAmmoReload(weapon) ? "No shells." : "Empty magazine.", NamedTextColor.RED));
            return;
        }

        if (!handleGhostWeaponBeforeShot(player, heldWeapon, weapon)) {
            return;
        }

        // Shoot
        state.setLastShotTime(now);
        state.setCurrentAmmo(state.getCurrentAmmo() - 1);
        persistState(heldWeapon, weapon, state);
        updateActionBar(player, state, weapon, heldWeapon);

        if (module.getWeaponAnimationSuppressor() != null) {
            module.getWeaponAnimationSuppressor().suppress(player);
        }
        ShootingMechanics.shoot(player, weapon, heldWeapon);
        refreshShotVisual(player, weapon, state);
    }

    private boolean canAttemptShot(Player player, WeaponState state, boolean aiming, boolean feedback) {
        if (module.getHandcuffManager().isRestrained(player)) {
            stopAutoFire(player);
            stopBurstFire(player);
            return false;
        }
        if (state.isReloading()) {
            stopAutoFire(player);
            stopBurstFire(player);
            return false;
        }
        if (!player.isSneaking() || !aiming) {
            stopAutoFire(player);
            stopBurstFire(player);
            if (feedback) {
                sendWeaponStatus(player, Component.text("Aim with shift to fire.", NamedTextColor.RED));
            }
            return false;
        }
        if (!isGroundedEnough(player)) {
            stopAutoFire(player);
            stopBurstFire(player);
            if (feedback) {
                sendWeaponStatus(player, Component.text("You cannot fire while airborne.", NamedTextColor.RED));
            }
            return false;
        }
        if (isSneakShootLocked(player)) {
            stopAutoFire(player);
            stopBurstFire(player);
            if (feedback) {
                sendWeaponStatus(player, Component.text("Stabilizing aim...", NamedTextColor.YELLOW));
            }
            return false;
        }
        if (module.getCombatStunManager().isStunned(player.getUniqueId())) {
            stopAutoFire(player);
            stopBurstFire(player);
            if (feedback) {
                long remaining = module.getCombatStunManager().getRemainingStunTimeSeconds(player.getUniqueId());
                sendWeaponStatus(player, Component.text("You are stunned and cannot shoot for " + remaining + "s", NamedTextColor.RED));
            }
            return false;
        }
        return true;
    }

    private void refreshShotVisual(Player player, WeaponDefinition weapon, WeaponState state) {
        if (!isAimingFirearm(weapon)) {
            return;
        }
        boolean aiming = player.isSneaking()
                || isActiveAimingSwap(player)
                || isAimingProxy(player.getInventory().getItemInMainHand());
        WeaponVisualState visual = aiming ? WeaponVisualState.AIMING : WeaponVisualState.IDLE;
        Runnable refresh = () -> {
            if (!player.isOnline() || player.isDead()) {
                return;
            }
            ItemStack activeItem = getActiveWeaponItem(player);
            WeaponDefinition activeWeapon = module.getWeaponRegistry().getWeapon(activeItem);
            if (activeWeapon == null || !activeWeapon.getId().equals(weapon.getId())) {
                return;
            }
            setWeaponVisual(activeItem, activeWeapon, visual, state.hasMagazine());
            if (aiming) {
                applyAimingUseVisual(activeItem, activeWeapon);
                if (module.getWeaponAnimationSuppressor() != null) {
                    module.getWeaponAnimationSuppressor().suppress(player);
                    module.getWeaponAnimationSuppressor().showAimingPose(player, true);
                }
            } else {
                module.getWeaponRegistry().applyFirearmUseAnimation(activeItem, activeWeapon);
                player.sendEquipmentChange(player, EquipmentSlot.HAND, activeItem.clone());
            }
            player.updateInventory();
        };
        refresh.run();
        Bukkit.getScheduler().runTaskLater(module.getCore(), refresh, 1L);
    }

    private boolean canUseWeapon(Player player, WeaponDefinition weapon, ItemStack heldWeapon) {
        WeaponCombatDecision decision = module.evaluateWeaponUse(player, weapon, heldWeapon);
        if (decision.isAllowed()) {
            return true;
        }
        String feedback = decision.feedback();
        if (feedback != null && !feedback.isBlank()) {
            sendWeaponStatus(player, Component.text(feedback, NamedTextColor.RED));
        }
        stopAutoFire(player);
        stopBurstFire(player);
        return false;
    }

    private boolean handleGhostWeaponBeforeShot(Player player, ItemStack item, WeaponDefinition weapon) {
        if (!weapon.getId().startsWith("ghost_")) {
            return true;
        }
        if (item == null || item.getType().isAir()) {
            return false;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return false;
        }
        int max = meta.getPersistentDataContainer().getOrDefault(ghostMaxDurabilityKey,
                PersistentDataType.INTEGER, ghostMaxDurability(weapon));
        int current = meta.getPersistentDataContainer().getOrDefault(ghostDurabilityKey,
                PersistentDataType.INTEGER, max);
        int wear = ghostWearPerShot(weapon);
        int next = Math.max(0, current - wear);

        double durabilityRatio = max <= 0 ? 0.0 : (double) current / (double) max;
        double jamChance = ghostBaseJamChance(weapon);
        if (durabilityRatio < 0.25) jamChance += 6.0;
        if (durabilityRatio < 0.10) jamChance += 10.0;

        if (durabilityRatio < 0.05 && Math.random() < 0.15) {
            player.getWorld().createExplosion(player.getLocation(), 1.4f, false, false);
            player.damage(8.0);
            removeActiveWeapon(player);
            sendWeaponStatus(player, Component.text("The printed frame detonated.", NamedTextColor.RED));
            return false;
        }

        if (Math.random() * 100.0 < jamChance) {
            meta.getPersistentDataContainer().set(ghostMaxDurabilityKey, PersistentDataType.INTEGER, max);
            meta.getPersistentDataContainer().set(ghostDurabilityKey, PersistentDataType.INTEGER, next);
            updateGhostLore(meta, next, max);
            item.setItemMeta(meta);
            player.damage(ghostJamDamage(weapon));
            player.playSound(player.getLocation(), Sound.BLOCK_DISPENSER_FAIL, 1.0f, 0.7f);
            sendWeaponStatus(player, Component.text("The printed mechanism jammed.", NamedTextColor.RED));
            if (next <= 0) {
                removeActiveWeapon(player);
                player.sendMessage(Component.text("The printed weapon broke apart.", NamedTextColor.RED));
            }
            return false;
        }

        if (next <= 0) {
            removeActiveWeapon(player);
            player.sendMessage(Component.text("The printed weapon broke apart.", NamedTextColor.RED));
            return false;
        }

        meta.getPersistentDataContainer().set(ghostMaxDurabilityKey, PersistentDataType.INTEGER, max);
        meta.getPersistentDataContainer().set(ghostDurabilityKey, PersistentDataType.INTEGER, next);
        updateGhostLore(meta, next, max);
        item.setItemMeta(meta);
        return true;
    }

    private int ghostMaxDurability(WeaponDefinition weapon) {
        return switch (weapon.getId()) {
            case "ghost_pistol" -> 25;
            case "ghost_shotgun" -> 14;
            case "ghost_smg" -> 22;
            case "ghost_rifle" -> 16;
            default -> 20;
        };
    }

    private int ghostWearPerShot(WeaponDefinition weapon) {
        return switch (weapon.getId()) {
            case "ghost_shotgun" -> 3;
            case "ghost_smg" -> 2;
            case "ghost_rifle" -> 2;
            default -> 1;
        };
    }

    private double ghostBaseJamChance(WeaponDefinition weapon) {
        return switch (weapon.getId()) {
            case "ghost_pistol" -> 5.0;
            case "ghost_shotgun" -> 10.0;
            case "ghost_smg" -> 8.0;
            case "ghost_rifle" -> 15.0;
            default -> 8.0;
        };
    }

    private double ghostJamDamage(WeaponDefinition weapon) {
        return switch (weapon.getId()) {
            case "ghost_shotgun" -> 3.0;
            case "ghost_rifle" -> 4.0;
            default -> 2.0;
        };
    }

    private void updateGhostLore(ItemMeta meta, int durability, int max) {
        meta.lore(List.of(
                Component.text("Printed weapon. No repair.", NamedTextColor.GRAY)
                        .decoration(net.kyori.adventure.text.format.TextDecoration.ITALIC, false),
                Component.text("Ghost durability: " + durability + " / " + max, NamedTextColor.YELLOW)
                        .decoration(net.kyori.adventure.text.format.TextDecoration.ITALIC, false)
        ));
    }

    private void startAutoFire(Player player, WeaponDefinition weapon, WeaponState state) {
        if (autoFireTasks.containsKey(player.getUniqueId())) return; // Already firing
        int periodTicks = effectiveFireRateTicks(getActiveWeaponItem(player), weapon);
        ItemStack activeWeapon = getActiveWeaponItem(player);
        String automaticSound = getWeaponSkinSound(activeWeapon, WeaponCosmeticManager.SOUND_AUTOMATIC);
        if (automaticSound != null) {
            playCustomWeaponSound(player, automaticSound, 1.0f, 1.0f);
            module.setAutomaticSkinFireSuppressed(player.getUniqueId(), true);
        }

        BukkitTask task = new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline() || player.isDead() || state.isReloading() || !state.hasMagazine() || state.getCurrentAmmo() <= 0) {
                    if (!state.hasMagazine()) {
                        sendWeaponStatus(player, Component.text("No magazine loaded.", NamedTextColor.RED));
                    } else if (state.getCurrentAmmo() <= 0) {
                        player.playSound(player.getLocation(), Sound.BLOCK_DISPENSER_FAIL, 1.0f, 2.0f);
                    }
                    stopAutoFire(player);
                    return;
                }

                // Check player is still holding the same weapon
                ItemStack current = getActiveWeaponItem(player);
                WeaponDefinition currentWeapon = module.getWeaponRegistry().getWeapon(current);
                if (currentWeapon == null || !currentWeapon.getId().equals(weapon.getId())) {
                    stopAutoFire(player);
                    return;
                }

                // Must stay in a valid firing stance to continue automatic fire.
                if (!player.isSneaking() || !isGroundedEnough(player)) {
                    stopAutoFire(player);
                    return;
                }

                tryShoot(player, weapon, state);
            }
        }.runTaskTimer(module.getCore(), 0L, periodTicks);

        autoFireTasks.put(player.getUniqueId(), task);
    }

    private void startBurstFire(Player player, WeaponDefinition weapon, WeaponState state) {
        if (burstFireTasks.containsKey(player.getUniqueId())) return;
        int periodTicks = effectiveFireRateTicks(getActiveWeaponItem(player), weapon);

        BukkitTask task = new BukkitRunnable() {
            private int shots = 0;

            @Override
            public void run() {
                if (shots >= 3 || !player.isOnline() || player.isDead() || state.isReloading()) {
                    stopBurstFire(player);
                    return;
                }

                ItemStack current = getActiveWeaponItem(player);
                WeaponDefinition currentWeapon = module.getWeaponRegistry().getWeapon(current);
                if (currentWeapon == null || !currentWeapon.getId().equals(weapon.getId())) {
                    stopBurstFire(player);
                    return;
                }

                int before = state.getCurrentAmmo();
                tryShoot(player, weapon, state);
                if (state.getCurrentAmmo() < before) {
                    shots++;
                } else {
                    stopBurstFire(player);
                }
            }
        }.runTaskTimer(module.getCore(), 0L, Math.max(1L, periodTicks));

        burstFireTasks.put(player.getUniqueId(), task);
    }

    private boolean isGroundedEnough(Player player) {
        Vector velocity = player.getVelocity();
        double verticalVelocity = velocity == null ? 0.0D : velocity.getY();
        return GroundedFireRules.canFire(player.isOnGround(), verticalVelocity, player.getFallDistance(), hasSupportBelow(player));
    }

    private boolean hasSupportBelow(Player player) {
        Location location = player.getLocation();
        if (location.getWorld() == null) {
            return false;
        }
        RayTraceResult result = location.getWorld().rayTraceBlocks(
                location.clone().add(0.0D, 0.05D, 0.0D),
                new Vector(0.0D, -1.0D, 0.0D),
                0.35D,
                FluidCollisionMode.NEVER,
                true);
        return result != null && result.getHitBlock() != null && result.getHitBlock().getType().isCollidable();
    }

    private void stopAutoFire(Player player) {
        BukkitTask task = autoFireTasks.remove(player.getUniqueId());
        if (task != null) task.cancel();
        module.setAutomaticSkinFireSuppressed(player.getUniqueId(), false);
    }

    private void stopBurstFire(Player player) {
        BukkitTask task = burstFireTasks.remove(player.getUniqueId());
        if (task != null) task.cancel();
    }

    private void cycleFireMode(Player player, ItemStack item, WeaponDefinition weapon, WeaponState state) {
        List<FireMode> modes = getEffectiveFireModes(item, weapon);
        int currentIndex = modes.indexOf(state.getFireMode());
        if (currentIndex < 0) {
            currentIndex = 0;
        }
        FireMode nextMode = modes.get((currentIndex + 1) % modes.size());
        state.setFireMode(nextMode);
        stopAutoFire(player);
        stopBurstFire(player);
        semiFireLocks.remove(player.getUniqueId());
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.6f, 1.4f);
        sendWeaponStatus(player, Component.text("Fire mode: " + nextMode.getDisplayName(), NamedTextColor.YELLOW));
    }

    private boolean lockSemiFire(Player player, WeaponDefinition weapon) {
        UUID uuid = player.getUniqueId();
        int slot = player.getInventory().getHeldItemSlot();
        SemiFireLock existing = semiFireLocks.get(uuid);
        if (existing != null) {
            if (existing.matches(weapon, slot)) {
                return false;
            }
            semiFireLocks.remove(uuid);
        }

        SemiFireLock lock = new SemiFireLock(weapon.getId(), slot);
        semiFireLocks.put(uuid, lock);
        return true;
    }

    private static final class SemiFireLock {
        private final String weaponId;
        private final int slot;

        private SemiFireLock(String weaponId, int slot) {
            this.weaponId = weaponId;
            this.slot = slot;
        }

        private boolean matches(WeaponDefinition weapon, int slot) {
            return weapon != null && this.slot == slot && weaponId.equals(weapon.getId());
        }
    }

    private record AimingSwap(int slot, ItemStack stashedOffhand) {
    }

    private enum MagazineSourceType {
        STASHED_OFFHAND,
        OFFHAND,
        INVENTORY
    }

    private record MagazineSource(MagazineSourceType type, int slot, ItemStack item) {
    }

    private void tryUnloadMagazine(Player player, ItemStack weaponItem, WeaponDefinition weapon, WeaponState state) {
        if (state.isReloading()) return;
        if (usesLooseAmmoReload(weapon)) {
            tryReloadLooseAmmo(player, weaponItem, weapon, state);
            return;
        }
        if (!state.hasMagazine()) {
            sendWeaponStatus(player, Component.text("No magazine inserted.", NamedTextColor.RED));
            return;
        }

        ItemStack magazine = module.getMagazineManager().createMagazine(weapon, state.getCurrentAmmo());
        state.setCurrentAmmo(0);
        state.setHasMagazine(false);
        persistState(weaponItem, weapon, state);
        giveOrDrop(player, magazine);
        player.playSound(player.getLocation(), Sound.ITEM_ARMOR_EQUIP_IRON, 0.8f, 1.3f);
        animateWeaponBriefly(player, weapon);
        sendWeaponStatus(player, Component.text("Magazine removed.", NamedTextColor.YELLOW));
    }

    private boolean tryStartReloadFromAvailableAmmunition(Player player, ItemStack weaponItem, WeaponDefinition weapon, WeaponState state) {
        if (usesLooseAmmoReload(weapon)) {
            tryReloadLooseAmmo(player, weaponItem, weapon, state);
            return state.isReloading();
        }
        MagazineSource source = findCompatibleLoadedMagazineSource(player, weapon);
        if (source == null) {
            return false;
        }
        tryLoadMagazine(player, weaponItem, weapon, state, source);
        return state.isReloading();
    }

    private MagazineSource findMagazineSource(Player player, WeaponDefinition weapon) {
        MagazineSource offhand = findOffhandMagazineSource(player);
        if (offhand != null) {
            return offhand;
        }
        return findInventoryMagazineSource(player, weapon, false);
    }

    private MagazineSource findCompatibleLoadedMagazineSource(Player player, WeaponDefinition weapon) {
        MagazineSource offhand = findOffhandMagazineSource(player);
        if (isCompatibleLoadedMagazine(offhand, weapon)) {
            return offhand;
        }
        return findInventoryMagazineSource(player, weapon, true);
    }

    private MagazineSource findOffhandMagazineSource(Player player) {
        if (isActiveAimingSwap(player)) {
            ItemStack stashedOffhand = getStashedOffhand(player);
            if (module.getMagazineManager().isMagazine(stashedOffhand)) {
                return new MagazineSource(MagazineSourceType.STASHED_OFFHAND, -1, stashedOffhand);
            }
        }

        ItemStack offhand = player.getInventory().getItemInOffHand();
        if (module.getMagazineManager().isMagazine(offhand)) {
            return new MagazineSource(MagazineSourceType.OFFHAND, -1, offhand);
        }
        return null;
    }

    private MagazineSource findInventoryMagazineSource(Player player, WeaponDefinition weapon, boolean requireLoaded) {
        ItemStack[] storage = player.getInventory().getStorageContents();
        for (int slot = 0; slot < storage.length; slot++) {
            ItemStack item = storage[slot];
            if (!module.getMagazineManager().isMagazine(item)) {
                continue;
            }
            if (!weapon.getId().equals(module.getMagazineManager().getWeaponId(item))) {
                continue;
            }
            if (requireLoaded && module.getMagazineManager().getAmmoCount(item) <= 0) {
                continue;
            }
            return new MagazineSource(MagazineSourceType.INVENTORY, slot, item);
        }
        return null;
    }

    private boolean isCompatibleLoadedMagazine(MagazineSource source, WeaponDefinition weapon) {
        if (source == null || weapon == null || !module.getMagazineManager().isMagazine(source.item())) {
            return false;
        }
        return weapon.getId().equals(module.getMagazineManager().getWeaponId(source.item()))
                && module.getMagazineManager().getAmmoCount(source.item()) > 0;
    }

    private void tryLoadMagazine(Player player, ItemStack weaponItem, WeaponDefinition weapon, WeaponState state, MagazineSource source) {
        if (state.isReloading()) return;

        ItemStack sourceItem = source.item();
        String magazineWeaponId = module.getMagazineManager().getWeaponId(sourceItem);
        if (!weapon.getId().equals(magazineWeaponId)) {
            sendWeaponStatus(player, Component.text("Incompatible magazine.", NamedTextColor.RED));
            return;
        }

        ItemStack insertedMagazine = sourceItem.clone();
        insertedMagazine.setAmount(1);
        if (module.getMagazineManager().getAmmoCount(insertedMagazine) <= 0) {
            sendWeaponStatus(player, Component.text("Magazine is empty.", NamedTextColor.RED));
            return;
        }
        int insertedAmmo = Math.min(module.getMagazineManager().getAmmoCount(insertedMagazine),
                effectiveMagazineSize(weaponItem, weapon));
        ItemStack oldMagazine = state.hasMagazine()
                ? module.getMagazineManager().createMagazine(weapon, state.getCurrentAmmo())
                : null;

        state.setReloading(true);
        persistState(weaponItem, weapon, state);
        consumeOneMagazineSource(player, source);
        sendWeaponStatus(player, Component.text("Reloading...", NamedTextColor.YELLOW));
        stopAimingAnimation(player);
        setWeaponVisual(player, weapon, WeaponVisualState.RELOADING);
        playReloadSound(player, weapon, weaponItem);

        new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline()) {
                    state.setReloading(false);
                    persistState(weaponItem, weapon, state);
                    return;
                }

                if (oldMagazine != null) {
                    giveOrDrop(player, oldMagazine);
                }
                state.setCurrentAmmo(insertedAmmo);
                state.setHasMagazine(true);
                state.setReloading(false);
                persistState(weaponItem, weapon, state);
                emitReload(player, weapon, weaponItem, insertedAmmo);
                updateWeaponVisual(player, weapon, player.isSneaking() ? WeaponVisualState.AIMING : WeaponVisualState.IDLE);
                if (player.isSneaking()) {
                    startAimingAnimation(player, weapon);
                }
                updateActionBar(player, state, weapon, weaponItem);
            }
        }.runTaskLater(module.getCore(), effectiveReloadTimeTicks(weaponItem, weapon));
    }

    private void tryReloadLooseAmmo(Player player, ItemStack weaponItem, WeaponDefinition weapon, WeaponState state) {
        if (state.isReloading()) return;

        int capacity = effectiveMagazineSize(weaponItem, weapon);
        int currentAmmo = Math.max(0, Math.min(capacity, state.getCurrentAmmo()));
        int ammoNeeded = capacity - currentAmmo;
        if (ammoNeeded <= 0) {
            state.setHasMagazine(true);
            persistState(weaponItem, weapon, state);
            sendWeaponStatus(player, Component.text("Shells already loaded.", NamedTextColor.YELLOW));
            return;
        }

        int ammoFound = countAmmo(player, weapon.getAmmoType());
        if (ammoFound <= 0) {
            sendWeaponStatus(player, Component.text("No " + weapon.getAmmoType() + " rounds.", NamedTextColor.RED));
            return;
        }

        int ammoLoaded = Math.min(ammoNeeded, ammoFound);
        consumeAmmo(player, weapon.getAmmoType(), ammoLoaded);
        state.setReloading(true);
        state.setHasMagazine(currentAmmo > 0);
        persistState(weaponItem, weapon, state);
        sendWeaponStatus(player, Component.text("Loading shells...", NamedTextColor.YELLOW));
        stopAimingAnimation(player);
        setWeaponVisual(player, weapon, WeaponVisualState.RELOADING);
        playReloadSound(player, weapon, weaponItem);

        new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline()) {
                    state.setReloading(false);
                    persistState(weaponItem, weapon, state);
                    return;
                }

                int loaded = Math.min(capacity, state.getCurrentAmmo() + ammoLoaded);
                state.setCurrentAmmo(loaded);
                state.setHasMagazine(loaded > 0);
                state.setReloading(false);
                persistState(weaponItem, weapon, state);
                emitReload(player, weapon, weaponItem, ammoLoaded);
                updateWeaponVisual(player, weapon, player.isSneaking() ? WeaponVisualState.AIMING : WeaponVisualState.IDLE);
                if (player.isSneaking()) {
                    startAimingAnimation(player, weapon);
                }
                updateActionBar(player, state, weapon, weaponItem);
            }
        }.runTaskLater(module.getCore(), effectiveReloadTimeTicks(weaponItem, weapon));
    }

    private void consumeOneMagazineSource(Player player, MagazineSource source) {
        switch (source.type()) {
            case STASHED_OFFHAND -> {
                AimingSwap swap = aimingSwaps.get(player.getUniqueId());
                if (swap != null) {
                    consumeOneStashedOffhandItem(player, swap);
                }
            }
            case OFFHAND -> consumeOneOffhandItem(player, source.item());
            case INVENTORY -> consumeOneInventoryItem(player, source.slot());
        }
    }

    private void consumeOneStashedOffhandItem(Player player, AimingSwap swap) {
        ItemStack stashedOffhand = swap.stashedOffhand();
        if (stashedOffhand == null || stashedOffhand.getType().isAir()) {
            return;
        }
        if (stashedOffhand.getAmount() <= 1) {
            aimingSwaps.put(player.getUniqueId(), new AimingSwap(swap.slot(), null));
            return;
        }
        stashedOffhand.setAmount(stashedOffhand.getAmount() - 1);
    }

    private void consumeOneOffhandItem(Player player, ItemStack offhand) {
        if (offhand == null || offhand.getType().isAir()) {
            return;
        }
        if (offhand.getAmount() <= 1) {
            player.getInventory().setItemInOffHand(null);
            return;
        }
        offhand.setAmount(offhand.getAmount() - 1);
        player.getInventory().setItemInOffHand(offhand);
    }

    private void consumeOneInventoryItem(Player player, int slot) {
        if (slot < 0 || slot >= player.getInventory().getStorageContents().length) {
            return;
        }
        ItemStack item = player.getInventory().getItem(slot);
        if (item == null || item.getType().isAir()) {
            return;
        }
        if (item.getAmount() <= 1) {
            player.getInventory().setItem(slot, null);
            return;
        }
        item.setAmount(item.getAmount() - 1);
        player.getInventory().setItem(slot, item);
    }

    private void tryFillMagazine(Player player, ItemStack magazine) {
        String weaponId = module.getMagazineManager().getWeaponId(magazine);
        WeaponDefinition weapon = module.getWeaponRegistry().getWeapon(weaponId);
        if (weapon == null) {
            sendWeaponStatus(player, Component.text("Invalid magazine.", NamedTextColor.RED));
            return;
        }

        int currentAmmo = module.getMagazineManager().getAmmoCount(magazine);
        int capacity = module.getMagazineManager().getCapacity(magazine);
        int ammoNeeded = capacity - currentAmmo;
        if (ammoNeeded <= 0) {
            sendWeaponStatus(player, Component.text("Magazine already full.", NamedTextColor.YELLOW));
            return;
        }

        int ammoFound = countAmmo(player, weapon.getAmmoType());
        if (ammoFound <= 0) {
            sendWeaponStatus(player, Component.text("No " + weapon.getAmmoType() + " rounds.", NamedTextColor.RED));
            return;
        }

        int amountToTake = Math.min(ammoNeeded, ammoFound);
        consumeAmmo(player, weapon.getAmmoType(), amountToTake);
        module.getMagazineManager().setAmmoCount(magazine, weapon, currentAmmo + amountToTake);
        player.playSound(player.getLocation(), Sound.ITEM_BUNDLE_INSERT, 0.8f, 1.4f);
        sendWeaponStatus(player, Component.text("Magazine: " + (currentAmmo + amountToTake) + " / " + capacity, NamedTextColor.YELLOW));
    }

    private int countAmmo(Player player, String ammoType) {
        int ammoFound = 0;
        for (ItemStack item : player.getInventory().getContents()) {
            AmmoDefinition ammoDef = module.getAmmoRegistry().getAmmo(item);
            if (ammoDef != null && ammoDef.getId().equals(ammoType)) {
                ammoFound += item.getAmount();
            }
        }
        return ammoFound;
    }

    private void consumeAmmo(Player player, String ammoType, int amount) {
        int remainingToTake = amount;
        for (int i = 0; i < player.getInventory().getSize(); i++) {
            ItemStack item = player.getInventory().getItem(i);
            AmmoDefinition ammoDef = module.getAmmoRegistry().getAmmo(item);
            if (ammoDef != null && ammoDef.getId().equals(ammoType)) {
                if (item.getAmount() <= remainingToTake) {
                    remainingToTake -= item.getAmount();
                    player.getInventory().setItem(i, null);
                } else {
                    item.setAmount(item.getAmount() - remainingToTake);
                    remainingToTake = 0;
                }
                if (remainingToTake <= 0) break;
            }
        }
    }

    private void giveOrDrop(Player player, ItemStack item) {
        Map<Integer, ItemStack> leftovers = player.getInventory().addItem(item);
        leftovers.values().forEach(leftover -> player.getWorld().dropItemNaturally(player.getLocation(), leftover));
    }

    private void playReloadSound(Player player, WeaponDefinition weapon, ItemStack weaponItem) {
        String skinSound = getWeaponSkinSound(weaponItem, WeaponCosmeticManager.SOUND_RELOAD);
        if (skinSound != null) {
            playCustomWeaponSound(player, skinSound, 1.0f, 1.0f);
            return;
        }
        if (playConfiguredCustomWeaponSound(player, weapon.getSoundReload(), 1.0f, 1.0f)) {
            return;
        }
        int variant = 1 + (int) (Math.random() * 3);
        playCustomWeaponSound(player, getWeaponSoundGroup(weapon) + ".reload_" + variant, 1.0f, 1.0f);
    }

    private String getWeaponSkinSound(ItemStack weaponItem, String soundKey) {
        WeaponCosmeticManager cosmetics = module.getWeaponCosmeticManager();
        return cosmetics == null ? null : cosmetics.getWeaponSkinSound(weaponItem, soundKey);
    }

    private void playScopeSound(Player player, WeaponDefinition weapon, boolean scopeIn) {
        playCustomWeaponSound(player, getWeaponSoundGroup(weapon) + (scopeIn ? ".scope_in" : ".scope_out"), 0.8f, 1.0f);
    }

    private void playCustomWeaponSound(Player player, String sound, float volume, float pitch) {
        player.getWorld().playSound(player.getLocation(), sound, SoundCategory.PLAYERS, volume, pitch);
    }

    private boolean playConfiguredCustomWeaponSound(Player player, String sound, float volume, float pitch) {
        if (sound == null || sound.isBlank()) {
            return false;
        }
        try {
            Sound.valueOf(sound.toUpperCase().replace(".", "_"));
            return false;
        } catch (IllegalArgumentException ignored) {
            playCustomWeaponSound(player, sound, volume, pitch);
            return true;
        }
    }

    private String getWeaponSoundGroup(WeaponDefinition weapon) {
        return switch (weapon.getCategory()) {
            case PISTOL -> "pistol";
            case SMG -> "smg";
            default -> "ar";
        };
    }

    private void animateWeaponBriefly(Player player, WeaponDefinition weapon) {
        if (!weapon.hasVisualState(WeaponVisualState.RELOADING)) {
            return;
        }
        setWeaponVisual(player, weapon, WeaponVisualState.RELOADING);
        new BukkitRunnable() {
            @Override
            public void run() {
                updateWeaponVisual(player, weapon, player.isSneaking() ? WeaponVisualState.AIMING : WeaponVisualState.IDLE);
            }
        }.runTaskLater(module.getCore(), 8L);
    }

    private void updateWeaponVisual(Player player, WeaponDefinition weapon, WeaponVisualState visual) {
        if (!weapon.hasVisualState(visual)) {
            visual = WeaponVisualState.IDLE;
        }
        if (!weapon.hasAnyVisualState()) {
            return;
        }
        setWeaponVisual(player, weapon, visual);
    }

    private void setWeaponVisual(Player player, WeaponDefinition weapon, WeaponVisualState visual) {
        ItemStack item = getActiveWeaponItem(player);
        WeaponDefinition currentWeapon = module.getWeaponRegistry().getWeapon(item);
        if (currentWeapon == null || !currentWeapon.getId().equals(weapon.getId())) {
            return;
        }
        WeaponState state = getState(player, item, weapon);
        setWeaponVisual(item, weapon, visual, state.hasMagazine());
        module.getWeaponRegistry().applyFirearmUseAnimation(item, weapon);
        if (aimingAnimationPlayers.contains(player.getUniqueId()) && isActiveAimingSwap(player)) {
            applyAimingUseVisual(item, weapon);
            refreshAimingOffhandVisual(player, item, weapon);
        }
        broadcastHeldItemVisual(player);
    }

    private void resetWeaponVisual(ItemStack item) {
        WeaponDefinition weapon = module.getWeaponRegistry().getWeapon(item);
        if (weapon != null && weapon.hasAnyVisualState()) {
            setWeaponVisual(item, weapon, WeaponVisualState.IDLE);
            module.getWeaponRegistry().applyFirearmUseAnimation(item, weapon);
        }
    }

    private void setWeaponVisual(ItemStack item, WeaponDefinition weapon, WeaponVisualState visual) {
        setWeaponVisual(item, weapon, visual, hasMagazineVisual(item, weapon));
    }

    private void setWeaponVisual(ItemStack item, WeaponDefinition weapon, WeaponVisualState visual, boolean hasMagazine) {
        if (!weapon.hasAnyVisualState()) {
            return;
        }
        int cmd = resolveWeaponVisualModelData(item, weapon, visual, hasMagazine);
        if (cmd <= 0) {
            return;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return;
        }
        meta.getPersistentDataContainer().set(weaponVisualStateKey, PersistentDataType.STRING, visual.name());
        meta.getPersistentDataContainer().set(weaponHasMagazineVisualKey, PersistentDataType.BYTE, hasMagazine ? (byte) 1 : (byte) 0);
        WeaponCosmeticManager cosmetics = module.getWeaponCosmeticManager();
        if (cosmetics != null) {
            Integer rgb = cosmetics.getWeaponColorRgb(item);
            cosmetics.applyVisualCustomModelData(meta, cmd, rgb);
            item.setItemMeta(meta);
            cosmetics.applyVisualDataComponents(item, cmd, rgb);
            return;
        } else {
            meta.setCustomModelData(cmd);
        }
        item.setItemMeta(meta);
    }

    private int resolveWeaponVisualModelData(ItemStack item, WeaponDefinition weapon, WeaponVisualState visual, boolean hasMagazine) {
        return weapon.resolveVisualModelData(visual, getVisualVariantCandidates(item, hasMagazine), hasMagazine);
    }

    private List<String> getVisualVariantCandidates(ItemStack item, boolean hasMagazine) {
        boolean optic = hasVisualAttachment(item, AttachmentSlot.OPTIC);
        boolean grip = hasVisualAttachment(item, AttachmentSlot.UNDERBARREL);
        WeaponCosmeticManager cosmetics = module.getWeaponCosmeticManager();
        String led = cosmetics == null ? WeaponCosmeticManager.NONE : cosmetics.getWeaponLed(item);
        String color = cosmetics == null ? WeaponCosmeticManager.NONE : cosmetics.getWeaponColor(item);
        String skin = cosmetics == null ? WeaponCosmeticManager.NONE : cosmetics.getWeaponSkin(item);
        return WeaponVisualVariantResolver.candidates(optic, hasMagazine, grip, led, color, skin);
    }

    private boolean hasVisualAttachment(ItemStack item, AttachmentSlot slot) {
        return module.getAttachmentManager() != null
                && module.getAttachmentManager().getAttachment(item, slot) != null;
    }

    private boolean hasMagazineVisual(ItemStack item, WeaponDefinition weapon) {
        if (item == null || weapon == null || !item.hasItemMeta() || weapon.getMagazineVisualOffset() <= 0) {
            return false;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return false;
        }
        Byte stored = meta.getPersistentDataContainer().get(weaponHasMagazineVisualKey, PersistentDataType.BYTE);
        if (stored != null) {
            return stored == (byte) 1;
        }
        return weapon.getMagazineVisualOffset() > 0;
    }

    private void broadcastHeldItemVisual(Player player) {
        if (aimingAnimationPlayers.contains(player.getUniqueId()) && module.getWeaponAnimationSuppressor() != null) {
            module.getWeaponAnimationSuppressor().showAimingPose(player, true);
            return;
        }

        ItemStack activeItem = getActiveWeaponItem(player);
        if (activeItem == null || activeItem.getType().isAir()) {
            return;
        }
        ItemStack visibleItem = activeItem.clone();
        EquipmentSlot slot = isAimingProxy(player.getInventory().getItemInMainHand())
                ? EquipmentSlot.OFF_HAND
                : EquipmentSlot.HAND;
        for (Player viewer : player.getWorld().getPlayers()) {
            if (!viewer.equals(player) && viewer.canSee(player)) {
                viewer.sendEquipmentChange(player, slot, visibleItem);
            }
        }
    }



    // ── Scope (Sniper Zoom) ─────────────────────────────────────────────────────

    @EventHandler
    public void onSneak(PlayerToggleSneakEvent event) {
        Player player = event.getPlayer();
        
        if (module.getHandcuffManager().isRestrained(player)) {
            return;
        }

        ItemStack item = getActiveWeaponItem(player);
        WeaponDefinition weapon = module.getWeaponRegistry().getWeapon(item);

        if (weapon != null && weapon.getCategory() != WeaponCategory.MELEE && weapon.getCategory() != WeaponCategory.TASER) {
            WeaponState state = getState(player, item, weapon);
            if (event.isSneaking()) {
                lockSneakShooting(player);
                if (!state.isReloading()) {
                    if (!startAimingAnimation(player, weapon)) {
                        return;
                    }
                    JumpRestrictionManager.restrict(player, JumpRestrictionManager.REASON_GUN);
                    playScopeSound(player, weapon, true);
                }
                
                int zoomLevel = effectiveScopeZoomLevel(getActiveWeaponItem(player), weapon);
                if (zoomLevel > 0) {
                    applyScopeSlowness(player, zoomLevel);
                }
            } else {
                JumpRestrictionManager.release(player, JumpRestrictionManager.REASON_GUN);
                WeaponVisualState inactiveVisual = resolveInactiveVisualState(player, item, weapon);
                stopAimingAnimation(player);
                updateWeaponVisual(player, weapon, inactiveVisual);
                playScopeSound(player, weapon, false);
                
                if (scopedPlayers.contains(player.getUniqueId())) {
                    removeScopeSlowness(player);
                }
            }
        }
    }

    /**
     * When the player switches their held item slot, remove scope slowness
     * if they were scoped. This fixes the Barrett slowness leak bug.
     */
    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Bukkit.getScheduler().runTask(module.getCore(), () -> {
            Player player = event.getPlayer();
            if (!player.isOnline()) {
                return;
            }

            ItemStack item = player.getInventory().getItemInMainHand();
            WeaponDefinition weapon = module.getWeaponRegistry().getWeapon(item);
            if (!isAimingFirearm(weapon)) {
                return;
            }

            resetWeaponVisual(item);
            player.updateInventory();
        });
    }

    @EventHandler
    public void onItemSwitch(PlayerItemHeldEvent event) {
        Player player = event.getPlayer();
        resetWeaponVisual(player.getInventory().getItem(event.getPreviousSlot()));
        JumpRestrictionManager.release(player, JumpRestrictionManager.REASON_GUN);
        stopAimingAnimation(player);
        semiFireLocks.remove(player.getUniqueId());
        reloadInputLocks.remove(player.getUniqueId());
        sneakShootLocks.remove(player.getUniqueId());
        fireInputLocks.remove(player.getUniqueId());
        if (scopedPlayers.contains(player.getUniqueId())) {
            removeScopeSlowness(player);
        }
        Bukkit.getScheduler().runTask(module.getCore(), () -> {
            ItemStack currentItem = player.getInventory().getItemInMainHand();
            WeaponDefinition currentWeapon = module.getWeaponRegistry().getWeapon(currentItem);
            if (!isAimingFirearm(currentWeapon) || module.getHandcuffManager().isRestrained(player)) {
                return;
            }

            WeaponState state = getState(player, currentItem, currentWeapon);
            if (state.isReloading()) {
                return;
            }
            if (player.isSneaking()) {
                if (!startAimingAnimation(player, currentWeapon)) {
                    return;
                }
                JumpRestrictionManager.restrict(player, JumpRestrictionManager.REASON_GUN);
                int zoomLevel = effectiveScopeZoomLevel(currentItem, currentWeapon);
                if (zoomLevel > 0) {
                    applyScopeSlowness(player, zoomLevel);
                }
            } else {
                updateWeaponVisual(player, currentWeapon, WeaponVisualState.IDLE);
            }
        });
    }

    /**
     * When the player swaps main/off hand (F key), also remove scope slowness.
     */
    @EventHandler
    public void onHandSwap(PlayerSwapHandItemsEvent event) {
        Player player = event.getPlayer();
        ItemStack mainHand = player.getInventory().getItemInMainHand();
        if (isAimingProxy(mainHand)) {
            event.setCancelled(true);
            ItemStack weaponItem = getActiveWeaponItem(player);
            WeaponDefinition aimingWeapon = module.getWeaponRegistry().getWeapon(weaponItem);
            if (!isAimingFirearm(aimingWeapon) || module.getHandcuffManager().isRestrained(player)) {
                stopAimingAnimation(player);
                return;
            }

            WeaponState state = getState(player, weaponItem, aimingWeapon);
            syncStateWithWeaponItem(weaponItem, aimingWeapon, state);
            stopAutoFire(player);
            stopBurstFire(player);
            reloadInputLocks.remove(player.getUniqueId());

            if (player.isSneaking() && getEffectiveFireModes(weaponItem, aimingWeapon).size() > 1) {
                cycleFireMode(player, weaponItem, aimingWeapon, state);
            } else {
                tryUnloadMagazine(player, weaponItem, aimingWeapon, state);
            }
            if (!state.isReloading()) {
                updateActionBar(player, state, aimingWeapon, weaponItem);
            }
            return;
        }

        WeaponDefinition weapon = module.getWeaponRegistry().getWeapon(mainHand);
        if (weapon != null
                && weapon.getCategory() != WeaponCategory.MELEE
                && weapon.getCategory() != WeaponCategory.TASER
                && !module.getHandcuffManager().isRestrained(player)) {
            event.setCancelled(true);
            WeaponState state = getState(player, mainHand, weapon);
            syncStateWithWeaponItem(mainHand, weapon, state);
            stopAutoFire(player);
            stopBurstFire(player);
            reloadInputLocks.remove(player.getUniqueId());

            if (player.isSneaking() && getEffectiveFireModes(mainHand, weapon).size() > 1) {
                cycleFireMode(player, mainHand, weapon, state);
            } else {
                tryUnloadMagazine(player, mainHand, weapon, state);
            }
            if (!state.isReloading()) {
                updateActionBar(player, state, weapon, mainHand);
            }
            return;
        }
        resetWeaponVisual(event.getMainHandItem());
        resetWeaponVisual(event.getOffHandItem());
        JumpRestrictionManager.release(player, JumpRestrictionManager.REASON_GUN);
        stopAimingAnimation(player);
        semiFireLocks.remove(player.getUniqueId());
        reloadInputLocks.remove(player.getUniqueId());
        if (scopedPlayers.contains(player.getUniqueId())) {
            removeScopeSlowness(player);
        }
    }

    @EventHandler
    public void onStopUsingItem(PlayerStopUsingItemEvent event) {
        Player player = event.getPlayer();
        SemiFireLock lock = semiFireLocks.get(player.getUniqueId());
        if (lock == null) {
            return;
        }

        ItemStack stoppedItem = event.getItem();
        WeaponDefinition weapon = module.getWeaponRegistry().getWeapon(stoppedItem);
        if (weapon == null) {
            weapon = module.getWeaponRegistry().getWeapon(getActiveWeaponItem(player));
        }
        if (!lock.matches(weapon, player.getInventory().getHeldItemSlot())) {
            return;
        }

        semiFireLocks.remove(player.getUniqueId());
        stopAutoFire(player);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        stopAimingAnimation(event.getPlayer());
        stopAutoFire(event.getPlayer());
        stopBurstFire(event.getPlayer());
        semiFireLocks.remove(event.getPlayer().getUniqueId());
        reloadInputLocks.remove(event.getPlayer().getUniqueId());
        sneakShootLocks.remove(event.getPlayer().getUniqueId());
        fireInputLocks.remove(event.getPlayer().getUniqueId());
        JumpRestrictionManager.clearAll(event.getPlayer());
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        emitPlayerKilled(event);
        event.getDrops().removeIf(this::isAimingProxy);
        stopAimingAnimation(event.getPlayer());
        stopAutoFire(event.getPlayer());
        stopBurstFire(event.getPlayer());
        semiFireLocks.remove(event.getPlayer().getUniqueId());
        reloadInputLocks.remove(event.getPlayer().getUniqueId());
        sneakShootLocks.remove(event.getPlayer().getUniqueId());
        fireInputLocks.remove(event.getPlayer().getUniqueId());
        JumpRestrictionManager.clearAll(event.getPlayer());
    }

    private void lockSneakShooting(Player player) {
        long durationMillis = sneakShootLockMillis();
        if (durationMillis <= 0) {
            sneakShootLocks.remove(player.getUniqueId());
            return;
        }
        sneakShootLocks.put(player.getUniqueId(), System.currentTimeMillis() + durationMillis);
    }

    private boolean isSneakShootLocked(Player player) {
        Long lockedUntil = sneakShootLocks.get(player.getUniqueId());
        if (lockedUntil == null) {
            return false;
        }
        if (lockedUntil <= System.currentTimeMillis()) {
            sneakShootLocks.remove(player.getUniqueId());
            return false;
        }
        return true;
    }

    private long sneakShootLockMillis() {
        return Math.max(0L, module.getCore().getConfig().getLong("weapons.anti-powergame.sneak-shoot-lock-millis", 400L));
    }

    private int effectiveScopeZoomLevel(ItemStack item, WeaponDefinition weapon) {
        if (!isAimingFirearm(weapon) || module.getAttachmentManager() == null) {
            return weapon != null && weapon.getScopeZoomLevel() != null ? Math.max(0, weapon.getScopeZoomLevel()) : 0;
        }
        int baseZoom = weapon.getScopeZoomLevel() != null ? weapon.getScopeZoomLevel() : 0;
        int attachmentBonus = module.getAttachmentManager().getZoomBonus(item);
        boolean hasOptic = module.getAttachmentManager().getAttachment(item, AttachmentSlot.OPTIC) != null;
        if (baseZoom <= 0 && attachmentBonus <= 0 && hasOptic) {
            baseZoom = 1;
        }
        return Math.max(0, baseZoom + attachmentBonus);
    }

    private void applyScopeSlowness(Player player, int zoomLevel) {
        if (zoomLevel <= 0) {
            return;
        }
        player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 999999, zoomLevel - 1, false, false, false));
        scopedPlayers.add(player.getUniqueId());
    }

    private void removeScopeSlowness(Player player) {
        scopedPlayers.remove(player.getUniqueId());
        player.removePotionEffect(PotionEffectType.SLOWNESS);

        // Re-apply vest slowness if they're still wearing one
        ItemStack chestplate = player.getInventory().getChestplate();
        if (chestplate != null && module.getArmorManager() != null) {
            var armor = module.getArmorManager().getArmor(chestplate);
            if (armor != null) {
                player.addPotionEffect(new PotionEffect(
                        PotionEffectType.SLOWNESS,
                        999999,
                        armor.getSlownessLevel(),
                        false, false, true
                ));
            }
        }
    }

    private void emitReload(Player player, WeaponDefinition weapon, ItemStack weaponItem, int ammoLoaded) {
        StaffBoardMetadata metadata = StaffBoardMetadata.create()
                .put("weapon_id", weapon.getId())
                .put("weapon_name", weapon.getDisplayName())
                .put("weapon_instance_id", module.getWeaponRegistry().getInstanceId(weaponItem))
                .put("ammo_loaded", ammoLoaded)
                .put("ammo_type", weapon.getAmmoType())
                .put("source_system", "OpenWeapons")
                .putLocation(player.getLocation());

        module.getCore().getStaffBoardPublisher().emit(StaffBoardLogEvent.builder("combat.reload", "OpenWeapons")
                .category(StaffBoardCategory.COMBAT)
                .severity(StaffBoardSeverity.INFO)
                .sensitivity(StaffBoardSensitivity.DEPARTMENT_ONLY)
                .actor(player)
                .location(player.getLocation())
                .message(player.getName() + " reloaded " + weapon.getDisplayName() + ".")
                .metadataJson(metadata.toJson())
                .build());
    }

    private void emitPlayerKilled(PlayerDeathEvent event) {
        Player victim = event.getPlayer();
        Player killer = victim.getKiller();
        if (killer == null) {
            return;
        }
        ItemStack weaponItem = killer.getInventory().getItemInMainHand();
        WeaponDefinition weapon = module.getWeaponRegistry().getWeapon(weaponItem);
        StaffBoardMetadata metadata = StaffBoardMetadata.create()
                .put("killer_uuid", killer.getUniqueId())
                .put("killer_name", killer.getName())
                .put("victim_uuid", victim.getUniqueId())
                .put("victim_name", victim.getName())
                .put("weapon_id", weapon != null ? weapon.getId() : null)
                .put("weapon_name", weapon != null ? weapon.getDisplayName() : null)
                .put("weapon_instance_id", weapon != null ? module.getWeaponRegistry().getInstanceId(weaponItem) : null)
                .put("source_system", "OpenWeapons")
                .putLocation(victim.getLocation());

        module.getCore().getStaffBoardPublisher().emit(StaffBoardLogEvent.builder("combat.player.killed", "OpenWeapons")
                .category(StaffBoardCategory.COMBAT)
                .severity(StaffBoardSeverity.CRITICAL)
                .sensitivity(StaffBoardSensitivity.SENSITIVE)
                .actor(killer)
                .target(victim)
                .location(victim.getLocation())
                .message(victim.getName() + " killed by " + killer.getName() + ".")
                .metadataJson(metadata.toJson())
                .build());
    }

    private void syncStateWithWeaponItem(ItemStack item, WeaponDefinition weapon, WeaponState state) {
        int capacity = effectiveMagazineSize(item, weapon);
        if (state.getCurrentAmmo() > capacity) {
            state.setCurrentAmmo(capacity);
        } else if (state.getCurrentAmmo() < 0) {
            state.setCurrentAmmo(0);
        }
        List<FireMode> modes = getEffectiveFireModes(item, weapon);
        if (!modes.contains(state.getFireMode())) {
            FireMode initialMode = initialFireMode(weapon);
            state.setFireMode(modes.contains(initialMode) ? initialMode : modes.get(0));
        }
        persistState(item, weapon, state);
    }

    private void persistState(ItemStack item, WeaponDefinition weapon, WeaponState state) {
        if (item == null || weapon == null || state == null || item.getType().isAir() || !item.hasItemMeta()) {
            return;
        }
        int capacity = effectiveMagazineSize(item, weapon);
        int ammo = Math.max(0, Math.min(capacity, state.getCurrentAmmo()));
        if (ammo != state.getCurrentAmmo()) {
            state.setCurrentAmmo(ammo);
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return;
        }
        meta.getPersistentDataContainer().set(weaponCurrentAmmoKey, PersistentDataType.INTEGER, state.getCurrentAmmo());
        meta.getPersistentDataContainer().set(weaponMagazineLoadedKey, PersistentDataType.BYTE, state.hasMagazine() ? (byte) 1 : (byte) 0);
        meta.getPersistentDataContainer().set(weaponHasMagazineVisualKey, PersistentDataType.BYTE, state.hasMagazine() ? (byte) 1 : (byte) 0);
        item.setItemMeta(meta);
        updateWeaponLore(item, weapon, state);
    }

    private void updateWeaponLore(ItemStack item, WeaponDefinition weapon, WeaponState state) {
        String shots = state.hasMagazine() || usesLooseAmmoReload(weapon)
                ? state.getCurrentAmmo() + " / " + effectiveMagazineSize(item, weapon)
                : "No mag";
        if (module.getAttachmentManager() != null) {
            module.getAttachmentManager().updateWeaponLore(item, weapon, shots);
        } else {
            module.getWeaponRegistry().updateWeaponLore(item, weapon, "None", shots);
        }
    }

    private List<FireMode> getEffectiveFireModes(ItemStack item, WeaponDefinition weapon) {
        List<FireMode> modes = new ArrayList<>(weapon.getFireModes());
        if (modes.isEmpty()) {
            modes.add(weapon.isAutomatic() ? FireMode.AUTO : FireMode.SEMI);
        }
        if (hasAttachment(item, "internal", "full_auto_converter")
                && isFullAutoCompatible(weapon)
                && !modes.contains(FireMode.AUTO)) {
            modes.add(FireMode.AUTO);
        }
        return modes;
    }

    private boolean isFullAutoCompatible(WeaponDefinition weapon) {
        return weapon.getCategory() == WeaponCategory.PISTOL
                || weapon.getCategory() == WeaponCategory.SMG
                || weapon.getCategory() == WeaponCategory.ASSAULT_RIFLE
                || weapon.getCategory() == WeaponCategory.SEMI_AUTO_RIFLE;
    }

    private int effectiveFireRateTicks(ItemStack item, WeaponDefinition weapon) {
        int ticks = Math.max(1, weapon.getFireRateTicks());
        if (hasAttachment(item, "internal", "light_trigger")) {
            ticks = Math.max(1, (int) Math.round(ticks * 0.75D));
        }
        return ticks;
    }

    private int effectiveReloadTimeTicks(ItemStack item, WeaponDefinition weapon) {
        int ticks = Math.max(1, weapon.getReloadTimeTicks());
        if (module.getAttachmentManager() != null) {
            ticks = Math.max(1, (int) Math.round(ticks * module.getAttachmentManager().getReloadTimeMultiplier(item)));
        }
        return ticks;
    }

    private int effectiveMagazineSize(ItemStack item, WeaponDefinition weapon) {
        int size = Math.max(1, weapon.getMagazineSize());
        if (hasAttachment(item, "magazine", "extended_magazine")) {
            return Math.max(size + 1, (int) Math.ceil(size * 1.5D));
        }
        return size;
    }

    private boolean usesLooseAmmoReload(WeaponDefinition weapon) {
        return weapon != null && weapon.getCategory() == WeaponCategory.SHOTGUN;
    }

    private boolean hasAttachment(ItemStack item, String slot, String attachmentId) {
        if (item == null || !item.hasItemMeta()) {
            return false;
        }
        AttachmentSlot attachmentSlot = AttachmentSlot.fromConfig(slot);
        if (attachmentSlot != null && module.getAttachmentManager() != null) {
            return module.getAttachmentManager().hasAttachment(item, attachmentSlot, attachmentId);
        }
        String current = item.getItemMeta().getPersistentDataContainer()
                .get(new NamespacedKey(module.getCore(), "weapon_attachment_" + slot), PersistentDataType.STRING);
        return attachmentId.equals(current);
    }

    private void updateActionBar(Player player, WeaponState state, WeaponDefinition weapon) {
        updateActionBar(player, state, weapon, getActiveWeaponItem(player));
    }

    private void updateActionBar(Player player, WeaponState state, WeaponDefinition weapon, ItemStack item) {
        String ammo = state.hasMagazine() || usesLooseAmmoReload(weapon)
                ? state.getCurrentAmmo() + " / " + effectiveMagazineSize(item, weapon)
                : "No mag";
        sendWeaponStatus(player, Component.text(ammo + "  [" + weapon.getAmmoType() + "]  "
                + state.getFireMode().getDisplayName(), NamedTextColor.WHITE), 25L);
    }

    private void sendWeaponStatus(Player player, Component message) {
        sendWeaponStatus(player, message, 40L);
    }

    private void sendWeaponStatus(Player player, Component message, long ttlTicks) {
        if (isCustomHotbarOverlayEnabled() && module.getCore().getHudStatusService() != null) {
            module.getCore().getHudStatusService().show(player, message, ttlTicks);
            return;
        }
        player.sendActionBar(message);
    }

    private boolean isCustomHotbarOverlayEnabled() {
        ModuleManager moduleManager = module.getCore().getModuleManager();
        return module.getCore().getConfig().getBoolean("hud.overlay.enabled", true)
                && moduleManager != null
                && moduleManager.getModuleState("cityhall") == ModuleManager.ModuleState.ENABLED;
    }
}
