package dev.openrp.weapons.armor;

import it.meridian.core.CorePlugin;
import dev.openrp.weapons.model.AmmoDefinition;
import dev.openrp.weapons.model.ArmorDefinition;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.persistence.PersistentDataType;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages bulletproof vest creation, identification, and durability.
 * Three vest types:
 * - Light (IIIA): No slowness, 25% DR, 50 durability.
 * - Heavy (IV): Slowness I, 45% DR, 100 durability.
 * - Heavy Plated: Slowness I, 45% DR, 125 durability (plate adds 25).
 *
 * Also manages the standalone Ceramic Plate item.
 */
public class ArmorManager {
    private final Map<String, ArmorDefinition> armors = new LinkedHashMap<>();
    private final NamespacedKey armorKey;
    private final NamespacedKey durabilityKey;
    private final NamespacedKey ceramicPlateKey;
    private final CorePlugin core;
    private final Set<UUID> armorHandledDamageEvents = ConcurrentHashMap.newKeySet();

    public ArmorManager(CorePlugin core) {
        this.core = core;
        this.armorKey = new NamespacedKey(core, "armor_id");
        this.durabilityKey = new NamespacedKey(core, "armor_durability");
        this.ceramicPlateKey = new NamespacedKey(core, "ceramic_plate");
        registerDefaults();
    }

    private void registerDefaults() {
        armors.clear();
        armors.putAll(defaultDefinitions());
    }

    public void load(File armorFile) {
        Map<String, ArmorDefinition> defaults = defaultDefinitions();
        if (armorFile == null || !armorFile.exists()) {
            armors.clear();
            armors.putAll(defaults);
            return;
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(armorFile);
        ConfigurationSection root = config.getConfigurationSection("armors");
        if (root == null) {
            core.getLogger().warning("[OpenWeapons] armor.yml has no 'armors' section; using built-in armor defaults.");
            armors.clear();
            armors.putAll(defaults);
            return;
        }

        Map<String, ArmorDefinition> loaded = new LinkedHashMap<>();
        for (Map.Entry<String, ArmorDefinition> entry : defaults.entrySet()) {
            ConfigurationSection section = root.getConfigurationSection(entry.getKey());
            loaded.put(entry.getKey(), readArmor(entry.getKey(), section, entry.getValue()));
        }
        for (String id : root.getKeys(false)) {
            String normalizedId = id.toLowerCase();
            if (loaded.containsKey(normalizedId)) {
                continue;
            }
            ConfigurationSection section = root.getConfigurationSection(id);
            if (section != null) {
                loaded.put(normalizedId, readArmor(normalizedId, section, null));
            }
        }

        if (loaded.isEmpty()) {
            loaded.putAll(defaults);
        }
        armors.clear();
        armors.putAll(loaded);
        core.getLogger().info("[OpenWeapons] Loaded " + armors.size() + " armor definition(s) from armor.yml.");
    }

    private Map<String, ArmorDefinition> defaultDefinitions() {
        Map<String, ArmorDefinition> defaults = new LinkedHashMap<>();
        // Light vest — no slowness, 50 durability
        defaults.put("vest_light", new ArmorDefinition(
                "vest_light",
                "Light Bulletproof Vest",
                12010,
                -1,      // No slowness
                0.25,    // 25% damage reduction
                "IIIA",
                50,      // 50 durability
                false,   // No plate
                0x191950
        ));

        // Heavy vest — Slowness I, 100 durability
        defaults.put("vest_heavy", new ArmorDefinition(
                "vest_heavy",
                "Heavy Bulletproof Vest",
                12011,
                0,       // Slowness I (amplifier 0)
                0.45,    // 45% damage reduction
                "IV",
                100,     // 100 durability
                false,   // No plate
                0x282828
        ));

        // Heavy vest with ceramic plate — Slowness II, 125 durability (100 vest + 25 plate)
        defaults.put("vest_heavy_plated", new ArmorDefinition(
                "vest_heavy_plated",
                "Heavy Plated Bulletproof Vest",
                12012,
                1,       // Slowness II (amplifier 1)
                0.45,    // 45% damage reduction
                "IV+",
                125,     // 125 total (plate 25, vest 100)
                true,    // Has plate
                0x3C3C1E
        ));
        return defaults;
    }

    private ArmorDefinition readArmor(String id, ConfigurationSection section, ArmorDefinition fallback) {
        String displayName = fallback == null ? id : fallback.getDisplayName();
        int customModelData = fallback == null ? 0 : fallback.getCustomModelData();
        int slownessLevel = fallback == null ? -1 : fallback.getSlownessLevel();
        double damageReduction = fallback == null ? 0.0D : fallback.getDamageReduction();
        String nijLevel = fallback == null ? "N/A" : fallback.getNijLevel();
        int maxDurability = fallback == null ? 1 : fallback.getMaxDurability();
        boolean hasPlate = fallback != null && fallback.hasPlate();
        int colorRgb = fallback == null ? -1 : fallback.getColorRgb();

        if (section != null) {
            displayName = section.getString("display-name", displayName);
            customModelData = section.getInt("custom-model-data", customModelData);
            slownessLevel = section.getInt("slowness-level", slownessLevel);
            damageReduction = section.getDouble("damage-reduction", damageReduction);
            nijLevel = section.getString("nij-level", nijLevel);
            maxDurability = section.getInt("max-durability", maxDurability);
            hasPlate = section.getBoolean("has-plate", hasPlate);
            colorRgb = parseColor(section.getString("color-rgb", section.getString("color")), colorRgb);
        }

        return new ArmorDefinition(
                id,
                displayName == null || displayName.isBlank() ? id : displayName,
                Math.max(0, customModelData),
                Math.max(-1, slownessLevel),
                clamp(damageReduction, 0.0D, 0.90D),
                nijLevel == null || nijLevel.isBlank() ? "N/A" : nijLevel,
                Math.max(1, maxDurability),
                hasPlate,
                colorRgb);
    }

    private int parseColor(String rawValue, int fallback) {
        if (rawValue == null || rawValue.isBlank()) {
            return fallback;
        }
        String value = rawValue.trim();
        try {
            if (value.startsWith("#")) {
                return Integer.parseInt(value.substring(1), 16) & 0xFFFFFF;
            }
            if (value.contains(",")) {
                String[] parts = value.split(",");
                if (parts.length == 3) {
                    int red = Integer.parseInt(parts[0].trim());
                    int green = Integer.parseInt(parts[1].trim());
                    int blue = Integer.parseInt(parts[2].trim());
                    return (clampColor(red) << 16) | (clampColor(green) << 8) | clampColor(blue);
                }
            }
            return Integer.decode(value) & 0xFFFFFF;
        } catch (NumberFormatException ex) {
            core.getLogger().warning("[OpenWeapons] Invalid armor color '" + rawValue + "', using fallback.");
            return fallback;
        }
    }

    private int clampColor(int value) {
        return Math.max(0, Math.min(255, value));
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    public ArmorDefinition getArmor(String id) {
        return armors.get(id);
    }

    public ArmorDefinition getArmor(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        String id = item.getItemMeta().getPersistentDataContainer().get(armorKey, PersistentDataType.STRING);
        if (id == null) return null;
        return getArmor(id);
    }

    public String getArmorId(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        return item.getItemMeta().getPersistentDataContainer().get(armorKey, PersistentDataType.STRING);
    }

    public List<ArmorDefinition> getAll() {
        return new ArrayList<>(armors.values());
    }

    /**
     * Get current durability of an armor item.
     */
    public int getDurability(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return 0;
        return item.getItemMeta().getPersistentDataContainer()
                .getOrDefault(durabilityKey, PersistentDataType.INTEGER, -1);
    }

    /**
     * Set current durability on an armor item. If <= 0, the vest should be removed.
     */
    public void setDurability(ItemStack item, int durability) {
        if (item == null || !item.hasItemMeta()) return;
        var meta = item.getItemMeta();
        meta.getPersistentDataContainer().set(durabilityKey, PersistentDataType.INTEGER, durability);

        // Update lore to show remaining durability
        ArmorDefinition def = getArmor(item);
        if (def != null) {
            List<Component> lore = buildLore(def, durability);
            meta.lore(lore);
        }
        item.setItemMeta(meta);
    }

    /**
     * Calculate damage to durability based on ammo type.
     * Heavy calibers do more durability damage.
     */
    public int getDurabilityDamage(String ammoType) {
        if (ammoType == null) return 1;
        return switch (ammoType) {
            case "50bmg" -> 5;
            case "338lapua" -> 4;
            case "762nato" -> 3;
            case "762x39" -> 3;
            case "556nato" -> 2;
            case "12gauge" -> 2;
            case "50ae" -> 3;
            case "500magnum" -> 3;
            case "357magnum" -> 2;
            default -> 1; // 9mm, 22lr, 45acp, etc.
        };
    }

    public int getDurabilityDamage(AmmoDefinition ammo) {
        return ammo != null ? Math.max(0, ammo.getArmorDurabilityDamage()) : 1;
    }

    public double getBulletDamageMultiplier(ArmorDefinition armor, AmmoDefinition ammo) {
        if (armor == null) {
            return 1.0D;
        }
        String armorId = armor.getId();
        String penetrationClass = ammo != null ? ammo.getPenetrationClass() : "handgun";

        double reduction = switch (penetrationClass) {
            case "handgun" -> switch (armorId) {
                case "vest_light" -> 0.45D;
                case "vest_heavy" -> 0.55D;
                case "vest_heavy_plated" -> 0.62D;
                default -> armor.getDamageReduction();
            };
            case "pdw" -> switch (armorId) {
                case "vest_light" -> 0.35D;
                case "vest_heavy" -> 0.50D;
                case "vest_heavy_plated" -> 0.58D;
                default -> armor.getDamageReduction();
            };
            case "handgun_heavy" -> switch (armorId) {
                case "vest_light" -> 0.25D;
                case "vest_heavy" -> 0.45D;
                case "vest_heavy_plated" -> 0.55D;
                default -> armor.getDamageReduction();
            };
            case "shotgun" -> switch (armorId) {
                case "vest_light" -> 0.30D;
                case "vest_heavy" -> 0.45D;
                case "vest_heavy_plated" -> 0.55D;
                default -> armor.getDamageReduction();
            };
            case "rifle" -> switch (armorId) {
                case "vest_light" -> 0.10D;
                case "vest_heavy" -> 0.35D;
                case "vest_heavy_plated" -> 0.55D;
                default -> armor.getDamageReduction();
            };
            case "rifle_heavy" -> switch (armorId) {
                case "vest_light" -> 0.05D;
                case "vest_heavy" -> 0.25D;
                case "vest_heavy_plated" -> 0.50D;
                default -> armor.getDamageReduction();
            };
            case "anti_material" -> switch (armorId) {
                case "vest_heavy_plated" -> 0.15D;
                default -> 0.0D;
            };
            default -> armor.getDamageReduction();
        };

        reduction = Math.max(0.0D, Math.min(0.90D, reduction));
        return 1.0D - reduction;
    }

    public void markNextDamageArmorHandled(UUID playerId) {
        if (playerId != null) {
            armorHandledDamageEvents.add(playerId);
        }
    }

    public boolean consumeNextDamageArmorHandled(UUID playerId) {
        return playerId != null && armorHandledDamageEvents.remove(playerId);
    }

    /**
     * Creates an armor item with full durability.
     */
    public ItemStack createItemStack(String armorId) {
        ArmorDefinition def = getArmor(armorId);
        if (def == null) return null;

        ItemStack item = new ItemStack(Material.LEATHER_CHESTPLATE);
        LeatherArmorMeta meta = (LeatherArmorMeta) item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text(def.getDisplayName(), NamedTextColor.GRAY)
                    .decoration(TextDecoration.BOLD, false)
                    .decoration(TextDecoration.ITALIC, false));
            if (def.getCustomModelData() > 0) {
                meta.setCustomModelData(def.getCustomModelData());
            }

            meta.setColor(resolveArmorColor(def));

            meta.getPersistentDataContainer().set(armorKey, PersistentDataType.STRING, armorId);
            meta.getPersistentDataContainer().set(durabilityKey, PersistentDataType.INTEGER, def.getMaxDurability());

            meta.lore(buildLore(def, def.getMaxDurability()));
            item.setItemMeta(meta);
        }
        return item;
    }

    /**
     * Creates a standalone Ceramic Plate item.
     */
    public ItemStack createCeramicPlate() {
        ItemStack item = new ItemStack(Material.IRON_INGOT);
        var meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text("Ceramic Plate", NamedTextColor.GRAY)
                    .decoration(TextDecoration.BOLD, false)
                    .decoration(TextDecoration.ITALIC, false));
            meta.getPersistentDataContainer().set(ceramicPlateKey, PersistentDataType.BOOLEAN, true);

            List<Component> lore = new ArrayList<>();
            lore.add(Component.text(""));
            lore.add(Component.text("Right-click while wearing a Heavy Vest", NamedTextColor.GRAY)
                    .decoration(TextDecoration.ITALIC, false));
            lore.add(Component.text("to insert this plate (3s application).", NamedTextColor.GRAY)
                    .decoration(TextDecoration.ITALIC, false));
            lore.add(Component.text(""));
            lore.add(Component.text("Adds +25 durability to the vest.", NamedTextColor.GREEN)
                    .decoration(TextDecoration.ITALIC, false));
            meta.lore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    public boolean isCeramicPlate(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        return item.getItemMeta().getPersistentDataContainer()
                .getOrDefault(ceramicPlateKey, PersistentDataType.BOOLEAN, false);
    }

    /**
     * Convert a plated vest back to a normal heavy vest (when plate breaks).
     * Keeps the remaining durability capped at 100.
     */
    public ItemStack convertPlatedToHeavy(ItemStack platedVest, int remainingDurability) {
        ArmorDefinition heavyDef = getArmor("vest_heavy");
        if (heavyDef == null) return platedVest;

        // Clamp durability to heavy vest max
        int newDurability = Math.min(remainingDurability, heavyDef.getMaxDurability());

        LeatherArmorMeta meta = (LeatherArmorMeta) platedVest.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text(heavyDef.getDisplayName(), NamedTextColor.GRAY)
                    .decoration(TextDecoration.BOLD, false)
                    .decoration(TextDecoration.ITALIC, false));
            if (heavyDef.getCustomModelData() > 0) {
                meta.setCustomModelData(heavyDef.getCustomModelData());
            }
            meta.setColor(resolveArmorColor(heavyDef));
            meta.getPersistentDataContainer().set(armorKey, PersistentDataType.STRING, "vest_heavy");
            meta.getPersistentDataContainer().set(durabilityKey, PersistentDataType.INTEGER, newDurability);
            meta.lore(buildLore(heavyDef, newDurability));
            platedVest.setItemMeta(meta);
        }
        return platedVest;
    }

    /**
     * Convert a heavy vest into a plated vest (when plate is applied).
     */
    public ItemStack convertHeavyToPlated(ItemStack heavyVest) {
        ArmorDefinition platedDef = getArmor("vest_heavy_plated");
        if (platedDef == null) return heavyVest;

        ArmorDefinition heavyDef = getArmor("vest_heavy");
        int currentDurability = getDurability(heavyVest);
        if (currentDurability < 0) {
            currentDurability = heavyDef == null ? 100 : heavyDef.getMaxDurability();
        }
        int plateBonus = heavyDef == null ? 25 : Math.max(0, platedDef.getMaxDurability() - heavyDef.getMaxDurability());
        int newDurability = Math.min(platedDef.getMaxDurability(), currentDurability + plateBonus);

        LeatherArmorMeta meta = (LeatherArmorMeta) heavyVest.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text(platedDef.getDisplayName(), NamedTextColor.GRAY)
                    .decoration(TextDecoration.BOLD, false)
                    .decoration(TextDecoration.ITALIC, false));
            if (platedDef.getCustomModelData() > 0) {
                meta.setCustomModelData(platedDef.getCustomModelData());
            }
            meta.setColor(resolveArmorColor(platedDef));
            meta.getPersistentDataContainer().set(armorKey, PersistentDataType.STRING, "vest_heavy_plated");
            meta.getPersistentDataContainer().set(durabilityKey, PersistentDataType.INTEGER, newDurability);
            meta.lore(buildLore(platedDef, newDurability));
            heavyVest.setItemMeta(meta);
        }
        return heavyVest;
    }

    public int getPlateBreakThreshold(ArmorDefinition platedArmor) {
        if (platedArmor == null || !platedArmor.hasPlate()) {
            return 0;
        }
        ArmorDefinition heavyDef = getArmor("vest_heavy");
        return heavyDef == null ? Math.max(0, platedArmor.getMaxDurability() - 25) : heavyDef.getMaxDurability();
    }

    private Color resolveArmorColor(ArmorDefinition def) {
        int rgb = def.getColorRgb();
        if (rgb >= 0) {
            return Color.fromRGB((rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, rgb & 0xFF);
        }
        return switch (def.getId()) {
            case "vest_light" -> Color.fromRGB(25, 25, 80);
            case "vest_heavy" -> Color.fromRGB(40, 40, 40);
            case "vest_heavy_plated" -> Color.fromRGB(60, 60, 30);
            default -> Color.fromRGB(45, 45, 45);
        };
    }

    private List<Component> buildLore(ArmorDefinition def, int currentDurability) {
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text(""));
        lore.add(Component.text("NIJ Level: ", NamedTextColor.GRAY)
                .append(Component.text(def.getNijLevel(), NamedTextColor.AQUA))
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text("Damage Reduction: ", NamedTextColor.GRAY)
                .append(Component.text(String.format("%.0f%%", def.getDamageReduction() * 100), NamedTextColor.GREEN))
                .decoration(TextDecoration.ITALIC, false));

        if (def.hasSlowness()) {
            lore.add(Component.text("Slowness: ", NamedTextColor.GRAY)
                    .append(Component.text("Level " + (def.getSlownessLevel() + 1), NamedTextColor.RED))
                    .decoration(TextDecoration.ITALIC, false));
        } else {
            lore.add(Component.text("Slowness: ", NamedTextColor.GRAY)
                    .append(Component.text("None", NamedTextColor.GREEN))
                    .decoration(TextDecoration.ITALIC, false));
        }

        // Durability bar
        lore.add(Component.text(""));
        int maxDur = def.getMaxDurability();
        int bars = 20;
        int filled = (maxDur > 0) ? (int) Math.ceil((double) currentDurability / maxDur * bars) : bars;
        filled = Math.max(0, Math.min(bars, filled));
        StringBuilder durBar = new StringBuilder();
        for (int i = 0; i < bars; i++) {
            durBar.append(i < filled ? "█" : "░");
        }
        NamedTextColor durColor = filled > 10 ? NamedTextColor.GREEN : filled > 5 ? NamedTextColor.YELLOW : NamedTextColor.RED;
        lore.add(Component.text("Durability: ", NamedTextColor.GRAY)
                .append(Component.text(durBar.toString(), durColor))
                .append(Component.text(" " + currentDurability + "/" + maxDur, NamedTextColor.GRAY))
                .decoration(TextDecoration.ITALIC, false));

        if (def.hasPlate()) {
            lore.add(Component.text(""));
            lore.add(Component.text("✦ Ceramic Plate Installed", NamedTextColor.GOLD)
                    .decoration(TextDecoration.ITALIC, false));
        }

        lore.add(Component.text(""));
        if ("vest_light".equals(def.getId())) {
            lore.add(Component.text("Stops handgun rounds", NamedTextColor.DARK_GRAY)
                    .decoration(TextDecoration.ITALIC, false));
        } else {
            lore.add(Component.text("Stops rifle & AP rounds", NamedTextColor.DARK_GRAY)
                    .decoration(TextDecoration.ITALIC, false));
        }

        return lore;
    }

    public NamespacedKey getArmorKey() {
        return armorKey;
    }

    public NamespacedKey getDurabilityKey() {
        return durabilityKey;
    }

    public NamespacedKey getCeramicPlateKey() {
        return ceramicPlateKey;
    }
}
