package dev.openrp.weapons.registry;

import it.meridian.core.CorePlugin;
import dev.openrp.weapons.model.FireMode;
import dev.openrp.weapons.model.WeaponCategory;
import dev.openrp.weapons.model.WeaponDefinition;
import dev.openrp.weapons.model.WeaponVisualState;
import io.papermc.paper.datacomponent.DataComponentTypes;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
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
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

public class WeaponRegistry {
    private static final String GLYPH_AMMO = "\u0A19";
    private static final String GLYPH_DAMAGE = "\u2F35";
    private static final String GLYPH_FIRE_RATE = "\u2F42";
    private static final String GLYPH_RANGE = "\u2F83";
    private static final String GLYPH_ACCURACY = "\u2F5C";
    private static final String FILLED_STAT = "\u2605";
    private static final TextColor WEAPON_NAME_COLOR = TextColor.color(0xFF5A5A);
    private static final TextColor TAG_TEXT_COLOR = TextColor.color(0xFFFFFF);
    private static final TextColor LABEL_COLOR = TextColor.color(0xBDB7C8);
    private static final TextColor SHOTS_COLOR = TextColor.color(0xE5E5E5);
    private static final TextColor DAMAGE_COLOR = TextColor.color(0xFFF45A);
    private static final TextColor RATE_COLOR = TextColor.color(0xFFB02E);
    private static final TextColor AIM_COLOR = TextColor.color(0x49F27A);
    private static final TextColor EMPTY_STAT_COLOR = TextColor.color(0x6F6878);

    private final Map<String, WeaponDefinition> weapons = new HashMap<>();
    private final NamespacedKey weaponKey;
    private final NamespacedKey weaponInstanceKey;
    private final CorePlugin core;
    private WeaponDisplayNameDecorator displayNameDecorator = (item, weapon, baseName) -> baseName;

    public WeaponRegistry(CorePlugin core) {
        this.core = core;
        this.weaponKey = new NamespacedKey(core, "weapon_id");
        this.weaponInstanceKey = new NamespacedKey(core, "weapon_instance_id");
    }

    public void load(File configFile) {
        weapons.clear();
        YamlConfiguration config = YamlConfiguration.loadConfiguration(configFile);

        for (String key : config.getKeys(false)) {
            ConfigurationSection section = config.getConfigurationSection(key);
            if (section == null) continue;
            if (!section.contains("category") || !section.contains("material")) {
                continue;
            }

            try {
                String displayName = section.getString("display-name", key);
                WeaponCategory category = WeaponCategory.valueOf(section.getString("category", "PISTOL").toUpperCase());
                Material material = Material.valueOf(section.getString("material", "CROSSBOW").toUpperCase());
                int customModelData = section.getInt("custom-model-data", 0);
                Map<WeaponVisualState, Integer> visualStates = parseVisualStates(section);
                Map<WeaponVisualState, Map<String, Integer>> visualVariants = parseVisualVariants(section);
                int magazineVisualOffset = section.getInt("magazine-visual-offset", 0);
                int magazineModelData = section.getInt("magazine-model-data", 0);
                double damage = section.getDouble("damage", 1.0);

                if (category == WeaponCategory.MELEE) {
                    double attackSpeed = section.getDouble("attack-speed", 1.0);
                    double knockback = section.getDouble("knockback", 0.0);
                    String soundHit = section.getString("sound-hit", "entity.player.attack.sweep");
                    
                    weapons.put(key, new WeaponDefinition(key, displayName, category, material, customModelData,
                            visualStates, visualVariants, magazineVisualOffset, magazineModelData,
                            damage, attackSpeed, knockback, soundHit));
                } else {
                    double headshotMultiplier = section.getDouble("headshot-multiplier", 1.0);
                    int fireRateTicks = section.getInt("fire-rate-ticks", 10);
                    int reloadTimeTicks = section.getInt("reload-time-ticks", 40);
                    int magazineSize = section.getInt("magazine-size", 10);
                    double maxDistance = section.getDouble("max-distance", 50.0);
                    String ammoType = section.getString("ammo-type", "9mm");
                    String soundShoot = section.getString("sound-shoot", "entity.firework_rocket.blast");
                    String soundReload = section.getString("sound-reload", "block.iron_door.close");
                    boolean automatic = section.getBoolean("automatic", false);
                    List<FireMode> fireModes = parseFireModes(key, section, automatic);
                    Integer scopeZoomLevel = section.contains("scope-zoom-level") ? section.getInt("scope-zoom-level") : null;
                    double recoil = section.getDouble("recoil", 0.03);
                    int pelletCount = section.getInt("pellet-count", 1);
                    double hipfireSpreadDeg = section.getDouble("hipfire-spread-deg", defaultHipfireSpreadDeg(category));
                    double adsSpreadDeg = section.getDouble("ads-spread-deg", defaultAdsSpreadDeg(category));
                    double movingSpreadMultiplier = section.getDouble("moving-spread-multiplier", 1.75D);
                    double sneakSpreadMultiplier = section.getDouble("sneak-spread-multiplier", 0.75D);
                    double jumpSpreadMultiplier = section.getDouble("jump-spread-multiplier", 4.0D);
                    double falloffStartDistance = section.getDouble("falloff-start-distance", defaultFalloffStartDistance(category));
                    double falloffEndDistance = section.getDouble("falloff-end-distance", defaultFalloffEndDistance(category, maxDistance));
                    double falloffMinMultiplier = section.getDouble("falloff-min-multiplier", defaultFalloffMinMultiplier(category));

                    weapons.put(key, new WeaponDefinition(key, displayName, category, material, customModelData,
                            visualStates, visualVariants, magazineVisualOffset, magazineModelData,
                            damage, headshotMultiplier,
                            fireRateTicks, reloadTimeTicks, magazineSize, maxDistance, ammoType, soundShoot, soundReload, automatic,
                            fireModes, scopeZoomLevel, recoil, pelletCount, hipfireSpreadDeg, adsSpreadDeg, movingSpreadMultiplier,
                            sneakSpreadMultiplier, jumpSpreadMultiplier, falloffStartDistance, falloffEndDistance, falloffMinMultiplier));
                }
            } catch (Exception e) {
                core.getLogger().warning("[OpenWeapons] Failed to load weapon '" + key + "': " + e.getMessage());
            }
        }
        core.getLogger().info("[OpenWeapons] Loaded " + weapons.size() + " weapons.");
    }

    public WeaponDefinition getWeapon(String id) {
        return weapons.get(id);
    }

    public WeaponDefinition getWeapon(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        String id = item.getItemMeta().getPersistentDataContainer().get(weaponKey, PersistentDataType.STRING);
        if (id == null) return null;
        return getWeapon(id);
    }

    public List<WeaponDefinition> getAll() {
        return new ArrayList<>(weapons.values());
    }

    public List<WeaponDefinition> getByCategory(WeaponCategory category) {
        return weapons.values().stream().filter(w -> w.getCategory() == category).collect(Collectors.toList());
    }

    public ItemStack createItemStack(String weaponId) {
        WeaponDefinition def = getWeapon(weaponId);
        if (def == null) return null;

        ItemStack item = new ItemStack(def.getMaterial());
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            // Set NBT Data
            meta.getPersistentDataContainer().set(weaponKey, PersistentDataType.STRING, weaponId);
            meta.getPersistentDataContainer().set(weaponInstanceKey, PersistentDataType.STRING, java.util.UUID.randomUUID().toString());
            if (def.getCustomModelData() > 0) {
                int customModelData = def.getCustomModelData();
                // Apply magazine visual offset for weapons that start with a magazine
                customModelData += def.getMagazineVisualOffset();
                meta.setCustomModelData(customModelData);
            }

            item.setItemMeta(meta);
            updateWeaponLore(item, def, "None");
            applyFirearmUseAnimation(item, def);
        }
        return item;
    }

    public void updateWeaponLore(ItemStack item, WeaponDefinition def, String modsDisplay) {
        if (item == null || def == null || !item.hasItemMeta()) {
            return;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return;
        }
        updateWeaponLore(item, meta, def, modsDisplay);
        item.setItemMeta(meta);
    }

    public void updateWeaponLore(ItemStack item, WeaponDefinition def, String modsDisplay, String shotsText) {
        if (item == null || def == null || !item.hasItemMeta()) {
            return;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return;
        }
        updateWeaponLore(item, meta, def, modsDisplay, shotsText);
        item.setItemMeta(meta);
    }

    private void updateWeaponLore(ItemStack item, ItemMeta meta, WeaponDefinition def, String modsDisplay) {
        updateWeaponLore(item, meta, def, modsDisplay, def.getMagazineSize() + " / " + def.getMagazineSize());
    }

    private void updateWeaponLore(ItemStack item, ItemMeta meta, WeaponDefinition def, String modsDisplay, String shotsText) {
        List<Component> lore = new ArrayList<>();
        applyWeaponDisplayName(item, meta, def);

        if (isTooltipFirearm(def)) {
            lore.add(Component.text(formatTooltipCategory(def), TAG_TEXT_COLOR)
                    .decoration(TextDecoration.BOLD, true)
                    .decoration(TextDecoration.ITALIC, false));
            lore.add(statTextLine(GLYPH_AMMO, "Shots", shotsText, SHOTS_COLOR));
            lore.add(statBarLine(GLYPH_DAMAGE, "Damage", scoreDamage(def), DAMAGE_COLOR));
            lore.add(statBarLine(GLYPH_FIRE_RATE, "Rate", scoreFireRate(def), RATE_COLOR));
            lore.add(statBarLine(GLYPH_RANGE, "Range", scoreRange(def), DAMAGE_COLOR));
            lore.add(statBarLine(GLYPH_ACCURACY, "Aim", scoreAim(def), AIM_COLOR));
        } else {
            lore.add(Component.text(""));
            lore.add(Component.text("Type: ", NamedTextColor.GRAY)
                    .append(Component.text(def.getCategory().getDisplayName(), NamedTextColor.WHITE))
                    .decoration(TextDecoration.ITALIC, false));
            lore.add(Component.text("Damage: ", NamedTextColor.GRAY)
                    .append(Component.text(formatDecimal(def.getDamage()), NamedTextColor.YELLOW))
                    .decoration(TextDecoration.ITALIC, false));
        }
        meta.lore(lore);
    }

    public void refreshWeaponDisplayName(ItemStack item, WeaponDefinition def) {
        if (item == null || def == null || !item.hasItemMeta()) {
            return;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return;
        }
        applyWeaponDisplayName(item, meta, def);
        item.setItemMeta(meta);
    }

    public void setDisplayNameDecorator(WeaponDisplayNameDecorator displayNameDecorator) {
        this.displayNameDecorator = displayNameDecorator == null
                ? (item, weapon, baseName) -> baseName
                : displayNameDecorator;
    }

    private void applyWeaponDisplayName(ItemStack item, ItemMeta meta, WeaponDefinition def) {
        Component baseName = Component.text(def.getDisplayName(), WEAPON_NAME_COLOR)
                .decoration(TextDecoration.BOLD, false)
                .decoration(TextDecoration.ITALIC, false);
        meta.displayName(displayNameDecorator.decorate(item, def, baseName)
                .decoration(TextDecoration.BOLD, false)
                .decoration(TextDecoration.ITALIC, false));
    }

    @FunctionalInterface
    public interface WeaponDisplayNameDecorator {
        Component decorate(ItemStack item, WeaponDefinition weapon, Component baseName);
    }

    private boolean isTooltipFirearm(WeaponDefinition def) {
        return def.getCategory() != WeaponCategory.MELEE && def.getCategory() != WeaponCategory.TASER;
    }

    private String formatTooltipCategory(WeaponDefinition def) {
        return switch (def.getCategory()) {
            case PISTOL -> "Pistol";
            case SHOTGUN -> "Shotgun";
            case SMG, ASSAULT_RIFLE, SEMI_AUTO_RIFLE, SNIPER -> "Rifle";
            case TASER -> "Taser";
            case MELEE -> "Melee";
        };
    }

    private Component statTextLine(String glyph, String label, String value, TextColor valueColor) {
        return Component.text(glyph + " ", NamedTextColor.WHITE)
                .append(Component.text(label + ": ", LABEL_COLOR))
                .append(Component.text(value, valueColor))
                .decoration(TextDecoration.ITALIC, false);
    }

    private Component statBarLine(String glyph, String label, int score, TextColor filledColor) {
        int clampedScore = clamp(score, 1, 5);
        return Component.text(glyph + " ", NamedTextColor.WHITE)
                .append(Component.text(label + ": ", LABEL_COLOR))
                .append(Component.text(FILLED_STAT.repeat(clampedScore), filledColor))
                .append(Component.text(FILLED_STAT.repeat(5 - clampedScore), EMPTY_STAT_COLOR))
                .decoration(TextDecoration.ITALIC, false);
    }

    private int scoreDamage(WeaponDefinition def) {
        return WeaponStatRater.scoreDamage(def.getCategory(), def.getDamage());
    }

    private int scoreFireRate(WeaponDefinition def) {
        return WeaponStatRater.scoreFireRate(def.getCategory(), def.getFireRateTicks());
    }

    private int scoreRange(WeaponDefinition def) {
        return WeaponStatRater.scoreRange(def.getCategory(), def.getMaxDistance());
    }

    private int scoreAim(WeaponDefinition def) {
        return WeaponStatRater.scoreAim(def.getCategory(), def.getAdsSpreadDeg());
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private String formatDecimal(double value) {
        return value == Math.rint(value)
                ? Integer.toString((int) value)
                : String.format(Locale.ROOT, "%.1f", value);
    }

    public void applyFirearmUseAnimation(ItemStack item, WeaponDefinition weapon) {
        if (item == null || weapon == null || weapon.getCategory() == WeaponCategory.MELEE || weapon.getCategory() == WeaponCategory.TASER) {
            return;
        }
        if (item.getType() != weapon.getMaterial()) {
            item.resetData(DataComponentTypes.CHARGED_PROJECTILES);
            item.resetData(DataComponentTypes.CONSUMABLE);
            item.setType(weapon.getMaterial());
        }
        item.resetData(DataComponentTypes.CHARGED_PROJECTILES);
        item.resetData(DataComponentTypes.CONSUMABLE);
    }

    public NamespacedKey getWeaponKey() {
        return weaponKey;
    }

    public String getOrCreateInstanceId(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return null;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.getPersistentDataContainer().has(weaponKey, PersistentDataType.STRING)) {
            return null;
        }
        String instanceId = meta.getPersistentDataContainer().get(weaponInstanceKey, PersistentDataType.STRING);
        if (instanceId == null || instanceId.isBlank()) {
            instanceId = java.util.UUID.randomUUID().toString();
            meta.getPersistentDataContainer().set(weaponInstanceKey, PersistentDataType.STRING, instanceId);
            item.setItemMeta(meta);
        }
        return instanceId;
    }

    public String getInstanceId(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return null;
        }
        return item.getItemMeta().getPersistentDataContainer().get(weaponInstanceKey, PersistentDataType.STRING);
    }

    public String assignNewInstanceId(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return null;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.getPersistentDataContainer().has(weaponKey, PersistentDataType.STRING)) {
            return null;
        }
        String instanceId = java.util.UUID.randomUUID().toString();
        meta.getPersistentDataContainer().set(weaponInstanceKey, PersistentDataType.STRING, instanceId);
        item.setItemMeta(meta);
        return instanceId;
    }

    private String formatFireModes(WeaponDefinition def) {
        if (def.getFireModes().isEmpty()) {
            return def.isAutomatic() ? FireMode.AUTO.getDisplayName() : FireMode.SEMI.getDisplayName();
        }
        return def.getFireModes().stream().map(FireMode::getDisplayName).collect(Collectors.joining(", "));
    }

    private List<FireMode> parseFireModes(String weaponId, ConfigurationSection section, boolean automatic) {
        List<String> configuredModes = section.getStringList("fire-modes");
        if (configuredModes.isEmpty()) {
            List<FireMode> defaultModes = getDefaultFireModes(weaponId);
            if (!defaultModes.isEmpty()) {
                return defaultModes;
            }
            return List.of(automatic ? FireMode.AUTO : FireMode.SEMI);
        }

        List<FireMode> modes = new ArrayList<>();
        for (String configuredMode : configuredModes) {
            FireMode mode = FireMode.fromConfig(configuredMode);
            if (!modes.contains(mode)) {
                modes.add(mode);
            }
        }
        return modes.isEmpty() ? List.of(automatic ? FireMode.AUTO : FireMode.SEMI) : modes;
    }

    private List<FireMode> getDefaultFireModes(String weaponId) {
        return switch (weaponId.toLowerCase(Locale.ROOT)) {
            case "m4a1", "hk416" -> List.of(FireMode.SEMI, FireMode.AUTO, FireMode.BURST);
            case "famas" -> List.of(FireMode.SEMI, FireMode.BURST);
            case "ak_47", "fn_scar_h", "sig_mcx_assault", "mp5", "mp7" -> List.of(FireMode.SEMI, FireMode.AUTO);
            default -> List.of();
        };
    }

    private double defaultHipfireSpreadDeg(WeaponCategory category) {
        return switch (category) {
            case SHOTGUN -> 7.0D;
            case SNIPER -> 6.0D;
            default -> 4.0D;
        };
    }

    private double defaultAdsSpreadDeg(WeaponCategory category) {
        return switch (category) {
            case SHOTGUN -> 4.5D;
            case SNIPER -> 0.18D;
            default -> 0.45D;
        };
    }

    private double defaultFalloffStartDistance(WeaponCategory category) {
        return switch (category) {
            case PISTOL -> 20.0D;
            case SMG -> 25.0D;
            case ASSAULT_RIFLE -> 55.0D;
            case SEMI_AUTO_RIFLE -> 60.0D;
            case SHOTGUN -> 8.0D;
            case SNIPER -> 100.0D;
            default -> 0.0D;
        };
    }

    private double defaultFalloffEndDistance(WeaponCategory category, double maxDistance) {
        double configuredDefault = switch (category) {
            case PISTOL -> 45.0D;
            case SMG -> 60.0D;
            case ASSAULT_RIFLE -> 100.0D;
            case SEMI_AUTO_RIFLE -> 110.0D;
            case SHOTGUN -> 24.0D;
            case SNIPER -> 190.0D;
            default -> maxDistance;
        };
        return Math.min(Math.max(configuredDefault, defaultFalloffStartDistance(category)), maxDistance);
    }

    private double defaultFalloffMinMultiplier(WeaponCategory category) {
        return switch (category) {
            case ASSAULT_RIFLE, SEMI_AUTO_RIFLE -> 0.65D;
            case SHOTGUN -> 0.30D;
            case SNIPER -> 0.80D;
            case PISTOL, SMG -> 0.55D;
            default -> 1.0D;
        };
    }

    private Map<WeaponVisualState, Integer> parseVisualStates(ConfigurationSection section) {
        ConfigurationSection vs = section.getConfigurationSection("visual-states");
        if (vs == null) {
            return null;
        }
        Map<WeaponVisualState, Integer> map = new EnumMap<>(WeaponVisualState.class);
        if (vs.contains("idle")) {
            map.put(WeaponVisualState.IDLE, vs.getInt("idle"));
        }
        if (vs.contains("aiming")) {
            map.put(WeaponVisualState.AIMING, vs.getInt("aiming"));
        }
        if (vs.contains("reloading")) {
            map.put(WeaponVisualState.RELOADING, vs.getInt("reloading"));
        }
        return map.isEmpty() ? null : map;
    }

    private Map<WeaponVisualState, Map<String, Integer>> parseVisualVariants(ConfigurationSection section) {
        ConfigurationSection states = section.getConfigurationSection("visual-variants");
        if (states == null) {
            return null;
        }
        Map<WeaponVisualState, Map<String, Integer>> map = new EnumMap<>(WeaponVisualState.class);
        for (String stateKey : states.getKeys(false)) {
            ConfigurationSection variants = states.getConfigurationSection(stateKey);
            if (variants == null) {
                continue;
            }
            WeaponVisualState state;
            try {
                state = WeaponVisualState.valueOf(stateKey.trim().toUpperCase(Locale.ROOT).replace('-', '_'));
            } catch (IllegalArgumentException ignored) {
                core.getLogger().warning("[OpenWeapons] Ignoring unknown visual state '" + stateKey + "'.");
                continue;
            }
            Map<String, Integer> stateVariants = new HashMap<>();
            for (String variantKey : variants.getKeys(false)) {
                int customModelData = variants.getInt(variantKey, -1);
                if (customModelData > 0) {
                    stateVariants.put(WeaponDefinition.normalizeVisualVariantKey(variantKey), customModelData);
                }
            }
            if (!stateVariants.isEmpty()) {
                map.put(state, stateVariants);
            }
        }
        return map.isEmpty() ? null : map;
    }
}
