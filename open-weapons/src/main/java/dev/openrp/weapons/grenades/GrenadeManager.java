package dev.openrp.weapons.grenades;

import org.bukkit.plugin.java.JavaPlugin;
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
import java.util.Map;

public class GrenadeManager {
    private static final Map<String, String> LEGACY_GRENADE_IDS = Map.of(
            "granata_stordente", "stun_grenade",
            "granata_fumogena", "smoke_grenade",
            "granata_frammentazione", "fragmentation_grenade",
            "granata_impatto", "impact_grenade",
            "bomba_c4", "c4_charge"
    );

    private final Map<String, GrenadeDefinition> grenades = new HashMap<>();
    private final NamespacedKey grenadeKey;
    private final JavaPlugin core;

    public GrenadeManager(JavaPlugin core) {
        this.core = core;
        this.grenadeKey = new NamespacedKey(core, "grenade_id");
    }

    public void load(File configFile) {
        grenades.clear();
        if (!configFile.exists()) return;
        
        YamlConfiguration config = YamlConfiguration.loadConfiguration(configFile);
        ConfigurationSection root = config.getConfigurationSection("grenades");
        if (root == null) return;

        for (String key : root.getKeys(false)) {
            ConfigurationSection section = root.getConfigurationSection(key);
            if (section == null) continue;

            try {
                String displayName = section.getString("display-name", key);
                GrenadeType type = GrenadeType.valueOf(section.getString("type", "FRAG").toUpperCase());
                Material material = Material.valueOf(section.getString("material", "SNOWBALL").toUpperCase());
                int customModelData = section.getInt("custom-model-data", 0);
                int fuseTimeTicks = section.getInt("fuse-time", 60);
                double radius = section.getDouble("radius", 5.0);
                double damage = section.getDouble("damage", 10.0);
                int effectDurationTicks = section.getInt("effect-duration", 100);

                grenades.put(key, new GrenadeDefinition(key, displayName, type, material, customModelData,
                        fuseTimeTicks, radius, damage, effectDurationTicks));
            } catch (Exception e) {
                core.getLogger().warning("[OpenWeapons] Impossibile caricare la granata '" + key + "': " + e.getMessage());
            }
        }
        core.getLogger().info("[OpenWeapons] Caricate " + grenades.size() + " granate.");
    }

    public GrenadeDefinition getGrenade(String id) {
        if (id == null || id.isBlank()) {
            return null;
        }
        GrenadeDefinition definition = grenades.get(id);
        if (definition != null) {
            return definition;
        }
        String currentId = LEGACY_GRENADE_IDS.get(id);
        return currentId == null ? null : grenades.get(currentId);
    }

    public GrenadeDefinition getGrenade(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        String id = item.getItemMeta().getPersistentDataContainer().get(grenadeKey, PersistentDataType.STRING);
        if (id == null) return null;
        return getGrenade(id);
    }

    public List<GrenadeDefinition> getAll() {
        return new ArrayList<>(grenades.values());
    }

    public ItemStack createItemStack(String grenadeId) {
        GrenadeDefinition def = getGrenade(grenadeId);
        if (def == null) return null;

        ItemStack item = new ItemStack(def.getMaterial());
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text(def.getDisplayName(), NamedTextColor.GRAY)
                    .decoration(TextDecoration.BOLD, false)
                    .decoration(TextDecoration.ITALIC, false));
            if (def.getCustomModelData() > 0) {
                meta.setCustomModelData(def.getCustomModelData());
            }
            
            // Set NBT Data
            meta.getPersistentDataContainer().set(grenadeKey, PersistentDataType.STRING, grenadeId);
            
            List<Component> lore = new ArrayList<>();
            lore.add(Component.text(""));
            lore.add(Component.text("Tipo: ", NamedTextColor.GRAY).append(Component.text(def.getType().name(), NamedTextColor.WHITE)).decoration(TextDecoration.ITALIC, false));
            lore.add(Component.text("Raggio: ", NamedTextColor.GRAY).append(Component.text(def.getRadius(), NamedTextColor.YELLOW)).decoration(TextDecoration.ITALIC, false));
            lore.add(Component.text("Miccia: ", NamedTextColor.GRAY).append(Component.text(def.getFuseTimeTicks() / 20.0 + "s", NamedTextColor.WHITE)).decoration(TextDecoration.ITALIC, false));
            meta.lore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    public NamespacedKey getGrenadeKey() {
        return grenadeKey;
    }
}
