package dev.openrp.weapons.registry;

import it.meridian.core.CorePlugin;
import dev.openrp.weapons.model.AmmoDefinition;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class AmmoRegistry {
    private final Map<String, AmmoDefinition> ammoTypes = new HashMap<>();
    private final NamespacedKey ammoKey;
    private final CorePlugin core;

    public AmmoRegistry(CorePlugin core) {
        this.core = core;
        this.ammoKey = new NamespacedKey(core, "ammo_id");
    }

    public void load(File configFile) {
        ammoTypes.clear();
        YamlConfiguration config = YamlConfiguration.loadConfiguration(configFile);

        for (String key : config.getKeys(false)) {
            ConfigurationSection section = config.getConfigurationSection(key);
            if (section == null) continue;

            try {
                String displayName = section.getString("display-name", key);
                Material material = Material.valueOf(section.getString("material", "IRON_NUGGET").toUpperCase());
                int customModelData = section.getInt("custom-model-data", 0);
                int maxStack = section.getInt("max-stack", 64);
                String penetrationClass = section.getString("penetration-class", defaultPenetrationClass(key))
                        .toLowerCase(Locale.ROOT);
                int armorDurabilityDamage = section.getInt("armor-durability-damage", defaultArmorDurabilityDamage(key));
                double fleshDamageMultiplier = section.getDouble("flesh-damage-multiplier", defaultFleshDamageMultiplier(key));
                int shieldDurabilityDamage = section.getInt("shield-durability-damage", defaultShieldDurabilityDamage(key));

                ammoTypes.put(key, new AmmoDefinition(key, displayName, material, customModelData, maxStack,
                        penetrationClass, armorDurabilityDamage, fleshDamageMultiplier, shieldDurabilityDamage));
            } catch (Exception e) {
                core.getLogger().warning("[OpenWeapons] Impossibile caricare le munizioni '" + key + "': " + e.getMessage());
            }
        }
        core.getLogger().info("[OpenWeapons] Caricati " + ammoTypes.size() + " tipi di munizioni.");
    }

    public AmmoDefinition getAmmo(String id) {
        return ammoTypes.get(id);
    }

    public AmmoDefinition getAmmo(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        String id = item.getItemMeta().getPersistentDataContainer().get(ammoKey, PersistentDataType.STRING);
        if (id == null) return null;
        return getAmmo(id);
    }

    public List<AmmoDefinition> getAll() {
        return new ArrayList<>(ammoTypes.values());
    }

    public ItemStack createItemStack(String ammoId, int amount) {
        AmmoDefinition def = getAmmo(ammoId);
        if (def == null) return null;

        ItemStack item = new ItemStack(def.getMaterial(), Math.min(amount, def.getMaxStack()));
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text(def.getDisplayName(), NamedTextColor.GRAY)
                    .decoration(TextDecoration.BOLD, false)
                    .decoration(TextDecoration.ITALIC, false));
            
            // Set NBT Data
            meta.getPersistentDataContainer().set(ammoKey, PersistentDataType.STRING, ammoId);
            if (def.getCustomModelData() > 0) {
                meta.setCustomModelData(def.getCustomModelData());
            }
            
            item.setItemMeta(meta);
        }
        return item;
    }

    public NamespacedKey getAmmoKey() {
        return ammoKey;
    }

    private String defaultPenetrationClass(String ammoId) {
        return switch (ammoId.toLowerCase(Locale.ROOT)) {
            case "46mm" -> "pdw";
            case "556nato" -> "rifle";
            case "762nato", "762x39", "338lapua" -> "rifle_heavy";
            case "50ae", "500magnum", "357magnum" -> "handgun_heavy";
            case "12gauge" -> "shotgun";
            case "50bmg" -> "anti_material";
            default -> "handgun";
        };
    }

    private int defaultArmorDurabilityDamage(String ammoId) {
        return switch (ammoId.toLowerCase(Locale.ROOT)) {
            case "50bmg" -> 8;
            case "338lapua" -> 4;
            case "762nato", "762x39", "50ae", "500magnum" -> 3;
            case "556nato", "12gauge", "357magnum" -> 2;
            default -> 1;
        };
    }

    private double defaultFleshDamageMultiplier(String ammoId) {
        return switch (ammoId.toLowerCase(Locale.ROOT)) {
            case "762nato", "762x39" -> 1.12D;
            case "50ae", "500magnum" -> 1.20D;
            case "46mm" -> 0.95D;
            default -> 1.0D;
        };
    }

    private int defaultShieldDurabilityDamage(String ammoId) {
        return switch (ammoId.toLowerCase(Locale.ROOT)) {
            case "50bmg" -> 999;
            case "338lapua" -> 8;
            case "762nato", "50ae", "500magnum" -> 5;
            case "762x39" -> 4;
            case "556nato", "12gauge" -> 3;
            case "357magnum" -> 2;
            default -> 1;
        };
    }
}
