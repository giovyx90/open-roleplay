package dev.openrp.weapons.handcuffs;

import it.meridian.core.CorePlugin;
import dev.openrp.weapons.util.JumpRestrictionManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class HandcuffManager {
    private static final int HANDCUFF_MODEL_DATA = 28;
    private static final int BOUND_HANDCUFF_MODEL_DATA = 31;
    private static final int BOUND_ROPE_MODEL_DATA = 41;
    private static final int BOLT_CUTTER_MODEL_DATA = 19;

    private final CorePlugin core;
    // Map of Victim -> Officer who cuffed them
    private final Map<UUID, UUID> handcuffedPlayers = new HashMap<>();
    private final NamespacedKey handcuffKey;
    private final NamespacedKey boundHandcuffKey;
    private final NamespacedKey boundRopeKey;
    private final NamespacedKey boltCutterKey;
    private final NamespacedKey cuffedByKey;
    private final NamespacedKey restraintTypeKey;
    private final NamespacedKey legacyRestraintTypeKey;

    public HandcuffManager(CorePlugin core) {
        this.core = core;
        this.handcuffKey = new NamespacedKey(core, "handcuff_item");
        this.boundHandcuffKey = new NamespacedKey(core, "bound_handcuffs_item");
        this.boundRopeKey = new NamespacedKey(core, "bound_rope_item");
        this.boltCutterKey = new NamespacedKey(core, "bolt_cutter_item");
        this.cuffedByKey = new NamespacedKey(core, "handcuffed_by");
        this.restraintTypeKey = new NamespacedKey(core, "restraint_type");
        this.legacyRestraintTypeKey = new NamespacedKey(core, "tipo_restrizione");
    }

    public void handcuff(Player victim, Player officer) {
        victim.getPersistentDataContainer().set(cuffedByKey, PersistentDataType.STRING, officer.getUniqueId().toString());
        victim.getPersistentDataContainer().set(restraintTypeKey, PersistentDataType.STRING, RestraintType.HANDCUFF.name());
        victim.getPersistentDataContainer().remove(legacyRestraintTypeKey);
        equipBoundHandcuffs(victim);
        applyEffects(victim);
    }

    public void tieWithRope(Player victim, Player actor) {
        victim.getPersistentDataContainer().set(cuffedByKey, PersistentDataType.STRING, actor.getUniqueId().toString());
        victim.getPersistentDataContainer().set(restraintTypeKey, PersistentDataType.STRING, RestraintType.ROPE.name());
        victim.getPersistentDataContainer().remove(legacyRestraintTypeKey);
        equipBoundRope(victim);
        applyEffects(victim);
    }

    public void uncuff(Player victim) {
        victim.getPersistentDataContainer().remove(cuffedByKey);
        victim.getPersistentDataContainer().remove(restraintTypeKey);
        victim.getPersistentDataContainer().remove(legacyRestraintTypeKey);
        victim.removePotionEffect(PotionEffectType.SLOWNESS);
        JumpRestrictionManager.release(victim, JumpRestrictionManager.REASON_HANDCUFF);
        removeBoundRestraintItems(victim);
    }

    public void applyEffects(Player player) {
        // Slowness V
        player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 999999, 4, false, false, false));
        JumpRestrictionManager.restrict(player, JumpRestrictionManager.REASON_HANDCUFF);
    }

    public boolean isHandcuffed(Player player) {
        if (player == null) return false;
        if (!player.getPersistentDataContainer().has(cuffedByKey, PersistentDataType.STRING)) {
            return false;
        }
        RestraintType type = getRestraintType(player);
        return type == null || type == RestraintType.HANDCUFF;
    }

    public boolean isHandcuffed(UUID uuid) {
        Player player = org.bukkit.Bukkit.getPlayer(uuid);
        return isHandcuffed(player);
    }

    public boolean isRestrained(Player player) {
        if (player == null) return false;
        return player.getPersistentDataContainer().has(cuffedByKey, PersistentDataType.STRING);
    }

    public boolean isRestrained(UUID uuid) {
        Player player = org.bukkit.Bukkit.getPlayer(uuid);
        return isRestrained(player);
    }

    public RestraintType getRestraintType(Player player) {
        if (player == null) return null;
        String value = player.getPersistentDataContainer().get(restraintTypeKey, PersistentDataType.STRING);
        if (value == null || value.isBlank()) {
            value = player.getPersistentDataContainer().get(legacyRestraintTypeKey, PersistentDataType.STRING);
        }
        if (value == null || value.isBlank()) {
            return player.getPersistentDataContainer().has(cuffedByKey, PersistentDataType.STRING)
                    ? RestraintType.HANDCUFF
                    : null;
        }
        try {
            return RestraintType.valueOf(value);
        } catch (IllegalArgumentException ignored) {
            return RestraintType.HANDCUFF;
        }
    }

    public UUID getOfficerWhoCuffed(Player victim) {
        if (victim == null) return null;
        String officerStr = victim.getPersistentDataContainer().get(cuffedByKey, PersistentDataType.STRING);
        if (officerStr != null) {
            try {
                return UUID.fromString(officerStr);
            } catch (IllegalArgumentException ignored) {}
        }
        return null;
    }

    public void ensureBoundHandcuffs(Player player) {
        if (isHandcuffed(player)) {
            equipBoundHandcuffs(player);
        }
    }

    public void ensureBoundRestraintItem(Player player) {
        if (!isRestrained(player)) {
            return;
        }
        RestraintType type = getRestraintType(player);
        if (type == RestraintType.ROPE) {
            equipBoundRope(player);
        } else {
            equipBoundHandcuffs(player);
        }
    }

    private void equipBoundHandcuffs(Player player) {
        ItemStack offhand = player.getInventory().getItemInOffHand();
        if (isBoundHandcuffItem(offhand)) {
            offhand.setAmount(1);
            return;
        }

        if (!isEmpty(offhand)) {
            Map<Integer, ItemStack> leftovers = player.getInventory().addItem(offhand.clone());
            leftovers.values().forEach(item -> player.getWorld().dropItemNaturally(player.getLocation(), item));
        }
        player.getInventory().setItemInOffHand(createBoundHandcuffs());
    }

    private void equipBoundRope(Player player) {
        ItemStack offhand = player.getInventory().getItemInOffHand();
        if (isBoundRopeItem(offhand)) {
            offhand.setAmount(1);
            return;
        }

        if (!isEmpty(offhand)) {
            Map<Integer, ItemStack> leftovers = player.getInventory().addItem(offhand.clone());
            leftovers.values().forEach(item -> player.getWorld().dropItemNaturally(player.getLocation(), item));
        }
        player.getInventory().setItemInOffHand(createBoundRope());
    }

    public void removeBoundHandcuffs(Player player) {
        removeBoundRestraintItems(player);
    }

    public void removeBoundRestraintItems(Player player) {
        if (player == null) return;

        if (isBoundHandcuffItem(player.getInventory().getItemInOffHand())
                || isBoundRopeItem(player.getInventory().getItemInOffHand())) {
            player.getInventory().setItemInOffHand(new ItemStack(Material.AIR));
        }

        ItemStack[] contents = player.getInventory().getContents();
        for (int slot = 0; slot < contents.length; slot++) {
            if (isBoundHandcuffItem(contents[slot]) || isBoundRopeItem(contents[slot])) {
                player.getInventory().setItem(slot, null);
            }
        }
    }

    public ItemStack createHandcuffs() {
        ItemStack item = new ItemStack(Material.PAPER);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text("Manette", NamedTextColor.GRAY)
                    .decoration(TextDecoration.BOLD, false)
                    .decoration(TextDecoration.ITALIC, false));
            meta.setCustomModelData(HANDCUFF_MODEL_DATA);
            meta.getPersistentDataContainer().set(handcuffKey, PersistentDataType.BYTE, (byte) 1);
            item.setItemMeta(meta);
        }
        return item;
    }

    public ItemStack createBoundHandcuffs() {
        ItemStack item = new ItemStack(Material.PAPER);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text("Manette", NamedTextColor.GRAY)
                    .decoration(TextDecoration.BOLD, false)
                    .decoration(TextDecoration.ITALIC, false));
            meta.setCustomModelData(BOUND_HANDCUFF_MODEL_DATA);
            meta.getPersistentDataContainer().set(boundHandcuffKey, PersistentDataType.BYTE, (byte) 1);
            item.setItemMeta(meta);
        }
        return item;
    }

    public ItemStack createBoundRope() {
        ItemStack item = new ItemStack(Material.PAPER);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text("Corda", NamedTextColor.GRAY)
                    .decoration(TextDecoration.BOLD, false)
                    .decoration(TextDecoration.ITALIC, false));
            meta.setCustomModelData(BOUND_ROPE_MODEL_DATA);
            meta.getPersistentDataContainer().set(boundRopeKey, PersistentDataType.BYTE, (byte) 1);
            item.setItemMeta(meta);
        }
        return item;
    }

    public ItemStack createBoltCutters() {
        ItemStack item = new ItemStack(Material.PAPER);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text("Tronchesi", NamedTextColor.GRAY)
                    .decoration(TextDecoration.BOLD, false)
                    .decoration(TextDecoration.ITALIC, false));
            meta.setCustomModelData(BOLT_CUTTER_MODEL_DATA);
            meta.getPersistentDataContainer().set(boltCutterKey, PersistentDataType.BYTE, (byte) 1);
            item.setItemMeta(meta);
        }
        return item;
    }

    private boolean isEmpty(ItemStack item) {
        return item == null || item.getType().isAir() || item.getAmount() <= 0;
    }

    public boolean isHandcuffItem(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        return item.getItemMeta().getPersistentDataContainer().has(handcuffKey, PersistentDataType.BYTE);
    }

    public boolean isBoundHandcuffItem(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        return item.getItemMeta().getPersistentDataContainer().has(boundHandcuffKey, PersistentDataType.BYTE);
    }

    public boolean isBoundRopeItem(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        return item.getItemMeta().getPersistentDataContainer().has(boundRopeKey, PersistentDataType.BYTE);
    }

    public boolean isBoltCutterItem(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        return item.getItemMeta().getPersistentDataContainer().has(boltCutterKey, PersistentDataType.BYTE);
    }
}
