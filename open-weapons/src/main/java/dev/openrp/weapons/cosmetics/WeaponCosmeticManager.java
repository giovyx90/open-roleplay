package dev.openrp.weapons.cosmetics;

import it.meridian.core.CorePlugin;
import dev.openrp.weapons.model.WeaponDefinition;
import dev.openrp.weapons.registry.WeaponRegistry;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.CustomModelData;
import io.papermc.paper.datacomponent.item.DyedItemColor;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.components.CustomModelDataComponent;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class WeaponCosmeticManager {
    public static final String TYPE_LED = "led";
    public static final String TYPE_COLOR = "color";
    public static final String TYPE_SKIN = "skin";
    public static final String NONE = "none";
    public static final String COLOR_VARIANT = "color";
    public static final String SOUND_FIRE = "fire";
    public static final String SOUND_HIT = "hit";
    public static final String SOUND_HEADSHOT = "headshot";
    public static final String SOUND_RELOAD = "reload";
    public static final String SOUND_AUTOMATIC = "automatic";
    public static final int COLOR_TOKEN_MODEL_DATA = 9014;
    private static final List<TextColor> SUGARLINE_GRADIENT = List.of(
            TextColor.color(0xFF35B8), TextColor.color(0xFF8BD8), TextColor.color(0xFFFFFF));
    private static final List<TextColor> AURUM_GRADIENT = List.of(
            TextColor.color(0xA96F00), TextColor.color(0xFFD76B), TextColor.color(0xFFF2B0));

    private final CorePlugin core;
    private final WeaponRegistry weaponRegistry;
    private final NamespacedKey weaponCosmeticLedKey;
    private final NamespacedKey weaponCosmeticColorKey;
    private final NamespacedKey weaponCosmeticColorRgbKey;
    private final NamespacedKey weaponCosmeticSkinKey;
    private final NamespacedKey tokenTypeKey;
    private final NamespacedKey tokenIdKey;
    private final NamespacedKey tokenColorRgbKey;
    private final Map<String, CosmeticOption> ledOptions = new LinkedHashMap<>();
    private final Map<String, CosmeticOption> colorOptions = new LinkedHashMap<>();
    private final Map<String, Map<String, SkinOption>> skinOptions = new LinkedHashMap<>();
    private final Map<String, WeaponCosmeticSupport> weaponSupports = new LinkedHashMap<>();
    private static final Map<String, Integer> COLOR_ALIASES = createColorAliases();

    public WeaponCosmeticManager(CorePlugin core, WeaponRegistry weaponRegistry) {
        this.core = core;
        this.weaponRegistry = weaponRegistry;
        this.weaponCosmeticLedKey = new NamespacedKey(core, "weapon_cosmetic_led");
        this.weaponCosmeticColorKey = new NamespacedKey(core, "weapon_cosmetic_color");
        this.weaponCosmeticColorRgbKey = new NamespacedKey(core, "weapon_cosmetic_color_rgb");
        this.weaponCosmeticSkinKey = new NamespacedKey(core, "weapon_cosmetic_skin");
        this.tokenTypeKey = new NamespacedKey(core, "weapon_cosmetic_token_type");
        this.tokenIdKey = new NamespacedKey(core, "weapon_cosmetic_token_id");
        this.tokenColorRgbKey = new NamespacedKey(core, "weapon_cosmetic_token_rgb");
        load(new File(core.getDataFolder(), "weapon_cosmetics.yml"));
    }

    public void load(File file) {
        ledOptions.clear();
        colorOptions.clear();
        skinOptions.clear();
        weaponSupports.clear();
        registerDefaultOptions();

        if (!file.exists()) {
            return;
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        loadOptions(config.getConfigurationSection("leds"), ledOptions);
        loadOptions(config.getConfigurationSection("colors"), colorOptions);
        loadSkinOptions(config.getConfigurationSection("skins"));
        loadWeaponSupports(config.getConfigurationSection("weapons"));
    }

    private void registerDefaultOptions() {
        ledOptions.put("usa", new CosmeticOption("usa", "USA LED", Material.PAPER, 9010, null));
        ledOptions.put("italy", new CosmeticOption("italy", "Italy LED", Material.PAPER, 9011, null));
        ledOptions.put("france", new CosmeticOption("france", "France LED", Material.PAPER, 9012, null));
        ledOptions.put("anime", new CosmeticOption("anime", "Anime LED", Material.PAPER, 9013, null));
        ledOptions.put("pacman", new CosmeticOption("pacman", "Pacman LED", Material.PAPER, 9015, null));

        COLOR_ALIASES.forEach((id, rgb) -> colorOptions.put(id,
                new CosmeticOption(id, aliasDisplayName(id), Material.PAPER, COLOR_TOKEN_MODEL_DATA, rgb)));

        registerSkinOption("ak_47", new SkinOption("ak_47", "gold-reserve",
                "AK-47 Aurum Reserve", "Aurum Reserve", AURUM_GRADIENT, Map.of()));
        registerSkinOption("ak_47", new SkinOption("ak_47", "sugarline-bakery",
                "AK-47 Sugarline Bakery", "Sugarline Bakery", SUGARLINE_GRADIENT, Map.of(
                SOUND_FIRE, "weapons.ak47.sugarline.fire",
                SOUND_HIT, "weapons.ak47.sugarline.hit",
                SOUND_HEADSHOT, "weapons.ak47.sugarline.headshot",
                SOUND_RELOAD, "weapons.ak47.sugarline.reload")));
        registerSkinOption("ppk", new SkinOption("ppk", "gold-reserve",
                "PPK Aurum Reserve", "Aurum Reserve", AURUM_GRADIENT, Map.of()));
        registerSkinOption("ppk", new SkinOption("ppk", "sugarline-bakery",
                "PPK Sugarline Bakery", "Sugarline Bakery", SUGARLINE_GRADIENT, Map.of(
                SOUND_FIRE, "weapons.ak47.sugarline.fire",
                SOUND_HIT, "weapons.ak47.sugarline.hit",
                SOUND_HEADSHOT, "weapons.ak47.sugarline.headshot",
                SOUND_RELOAD, "weapons.ak47.sugarline.reload")));
        registerSkinOption("m4a1", new SkinOption("m4a1", "gold-reserve",
                "M4A1 Aurum Reserve", "Aurum Reserve", AURUM_GRADIENT, Map.of()));
        registerSkinOption("m4a1", new SkinOption("m4a1", "royal-masquerade",
                "M4A1 Royal Masquerade", "Royal Masquerade", List.of(
                TextColor.color(0xA96F00), TextColor.color(0x7B2FF7), TextColor.color(0xFFFFFF)), Map.of(
                SOUND_FIRE, "weapons.m4a1.royal.fire",
                SOUND_HIT, "weapons.m4a1.royal.fire",
                SOUND_HEADSHOT, "weapons.m4a1.royal.fire",
                SOUND_RELOAD, "weapons.m4a1.royal.reload")));

        weaponSupports.put("m4a1", new WeaponCosmeticSupport(true, true, true, true));
        weaponSupports.put("ak_47", new WeaponCosmeticSupport(true, true, true, true));
        weaponSupports.put("ppk", new WeaponCosmeticSupport(true, false, true, true));
    }

    private void loadOptions(ConfigurationSection section, Map<String, CosmeticOption> target) {
        if (section == null) {
            return;
        }
        for (String rawId : section.getKeys(false)) {
            String id = normalize(rawId);
            if (id.equals(NONE)) {
                continue;
            }
            ConfigurationSection optionSection = section.getConfigurationSection(rawId);
            CosmeticOption defaultOption = target.get(id);
            String defaultDisplayName = defaultOption == null ? rawId : defaultOption.displayName();
            Material defaultMaterial = defaultOption == null ? Material.PAPER : defaultOption.material();
            int defaultCustomModelData = defaultOption == null ? 0 : defaultOption.customModelData();
            String displayName = optionSection == null ? defaultDisplayName : optionSection.getString("display-name", defaultDisplayName);
            String materialName = optionSection == null ? defaultMaterial.name() : optionSection.getString("token-material", defaultMaterial.name());
            Material material = Material.matchMaterial(materialName);
            int customModelData = optionSection == null ? defaultCustomModelData : optionSection.getInt("token-custom-model-data", defaultCustomModelData);
            Integer rgb = defaultOption == null ? null : defaultOption.rgb();
            if (target == colorOptions) {
                String configuredHex = optionSection == null ? null : optionSection.getString("hex");
                Integer configuredRgb = parseColorRgb(configuredHex == null ? rawId : configuredHex);
                rgb = configuredRgb == null ? rgb : configuredRgb;
                material = Material.PAPER;
                customModelData = optionSection == null
                        ? COLOR_TOKEN_MODEL_DATA
                        : optionSection.getInt("token-custom-model-data", COLOR_TOKEN_MODEL_DATA);
            }
            target.put(id, new CosmeticOption(id, displayName, material == null ? defaultMaterial : material, Math.max(0, customModelData), rgb));
        }
    }

    private void loadSkinOptions(ConfigurationSection skinsSection) {
        if (skinsSection == null) {
            return;
        }
        for (String rawWeaponId : skinsSection.getKeys(false)) {
            String weaponId = normalizeWeaponId(rawWeaponId);
            ConfigurationSection weaponSection = skinsSection.getConfigurationSection(rawWeaponId);
            if (weaponSection == null) {
                continue;
            }
            for (String rawSkinId : weaponSection.getKeys(false)) {
                String skinId = normalize(rawSkinId);
                if (skinId.equals(NONE)) {
                    continue;
                }
                ConfigurationSection skinSection = weaponSection.getConfigurationSection(rawSkinId);
                SkinOption defaultOption = getSkinOption(weaponId, skinId);
                String displayName = skinSection == null
                        ? defaultOption == null ? rawSkinId : defaultOption.displayName()
                        : skinSection.getString("display-name", defaultOption == null ? rawSkinId : defaultOption.displayName());
                String suffix = skinSection == null
                        ? defaultOption == null ? displayName : defaultOption.nameSuffix()
                        : skinSection.getString("name-suffix", defaultOption == null ? displayName : defaultOption.nameSuffix());
                List<TextColor> suffixGradient = skinSection == null
                        ? defaultOption == null ? List.of(TextColor.color(0xFFFFFF)) : defaultOption.suffixGradient()
                        : readGradient(skinSection, defaultOption == null ? List.of(TextColor.color(0xFFFFFF)) : defaultOption.suffixGradient());
                Map<String, String> sounds = new LinkedHashMap<>(
                        defaultOption == null ? Map.of() : defaultOption.sounds());
                readSound(skinSection, sounds, SOUND_FIRE, "sound-fire");
                readSound(skinSection, sounds, SOUND_HIT, "sound-hit");
                readSound(skinSection, sounds, SOUND_HEADSHOT, "sound-headshot");
                readSound(skinSection, sounds, SOUND_RELOAD, "sound-reload");
                readSound(skinSection, sounds, SOUND_AUTOMATIC, "sound-automatic");
                registerSkinOption(weaponId, new SkinOption(weaponId, skinId, displayName, suffix, suffixGradient,
                        Collections.unmodifiableMap(sounds)));
            }
        }
    }

    private List<TextColor> readGradient(ConfigurationSection section, List<TextColor> fallback) {
        List<String> configured = section.getStringList("suffix-gradient");
        if (configured.isEmpty()) {
            configured = section.getStringList("name-gradient");
        }
        List<TextColor> colors = new ArrayList<>();
        for (String value : configured) {
            Integer rgb = parseColorRgb(value);
            if (rgb != null) {
                colors.add(TextColor.color(rgb));
            }
        }
        return colors.isEmpty() ? fallback : List.copyOf(colors);
    }

    private void readSound(ConfigurationSection section, Map<String, String> sounds, String key, String path) {
        if (section == null) {
            return;
        }
        String sound = section.getString(path);
        if (sound != null && !sound.isBlank()) {
            sounds.put(key, sound.trim());
        }
    }

    private void loadWeaponSupports(ConfigurationSection section) {
        if (section == null) {
            return;
        }
        for (String rawWeaponId : section.getKeys(false)) {
            String weaponId = normalizeWeaponId(rawWeaponId);
            if (weaponId.equals(NONE)) {
                continue;
            }
            ConfigurationSection weaponSection = section.getConfigurationSection(rawWeaponId);
            WeaponCosmeticSupport defaults = weaponSupports.getOrDefault(weaponId,
                    new WeaponCosmeticSupport(skinOptions.containsKey(weaponId), false, false, skinOptions.containsKey(weaponId)));
            if (weaponSection == null) {
                continue;
            }
            weaponSupports.put(weaponId, new WeaponCosmeticSupport(
                    weaponSection.getBoolean("enabled", defaults.enabled()),
                    weaponSection.getBoolean("led", defaults.led()),
                    weaponSection.getBoolean("color", defaults.color()),
                    weaponSection.getBoolean("skin", defaults.skin())));
        }
    }

    private void registerSkinOption(String weaponId, SkinOption option) {
        String normalizedWeaponId = normalizeWeaponId(weaponId);
        skinOptions.computeIfAbsent(normalizedWeaponId, ignored -> new LinkedHashMap<>())
                .put(normalize(option.id()), new SkinOption(normalizedWeaponId, normalize(option.id()),
                        option.displayName(), option.nameSuffix(), List.copyOf(option.suffixGradient()),
                        Collections.unmodifiableMap(new LinkedHashMap<>(option.sounds()))));
    }

    public boolean isValidType(String type) {
        String normalized = normalize(type);
        return normalized.equals(TYPE_LED) || normalized.equals(TYPE_COLOR);
    }

    public boolean isValidOption(String type, String id) {
        String normalized = normalize(id);
        if (normalized.equals(NONE)) {
            return true;
        }
        String normalizedType = normalize(type);
        if (normalizedType.equals(TYPE_COLOR)) {
            return parseColorRgb(id) != null;
        }
        if (normalizedType.equals(TYPE_LED)) {
            return options(type).containsKey(normalized);
        }
        return false;
    }

    public Set<String> getOptionIds(String type) {
        return options(type).keySet();
    }

    public List<String> getLedIds() {
        return List.copyOf(ledOptions.keySet());
    }

    public List<String> getColorIds() {
        return List.copyOf(colorOptions.keySet());
    }

    public CosmeticOption getLedOption(String id) {
        return ledOptions.get(normalize(id));
    }

    public CosmeticOption getColorOption(String id) {
        return colorOptions.get(normalize(id));
    }

    public String getWeaponId(ItemStack item) {
        WeaponDefinition weapon = weaponRegistry.getWeapon(item);
        return weapon == null ? null : normalizeWeaponId(weapon.getId());
    }

    public boolean supportsCosmeticType(ItemStack weaponItem, String type) {
        String weaponId = getWeaponId(weaponItem);
        if (weaponId == null) {
            return false;
        }
        WeaponCosmeticSupport support = weaponSupport(weaponId);
        if (!support.enabled()) {
            return false;
        }
        String normalizedType = normalize(type);
        if (weaponId.equals("ak_47")) {
            if (normalizedType.equals(TYPE_LED)) {
                return support.led() && getWeaponSkin(weaponItem).equals(NONE);
            }
            return normalizedType.equals(TYPE_COLOR) && support.color()
                    || normalizedType.equals(TYPE_SKIN) && support.skin();
        }
        if (weaponId.equals("m4a1")) {
            if (normalizedType.equals(TYPE_LED) || normalizedType.equals(TYPE_COLOR)) {
                return getWeaponSkin(weaponItem).equals(NONE)
                        && (normalizedType.equals(TYPE_LED) ? support.led() : support.color());
            }
            return normalizedType.equals(TYPE_SKIN) && support.skin() && skinOptions.containsKey(weaponId);
        }
        if (weaponId.equals("ppk")) {
            return normalizedType.equals(TYPE_COLOR) && support.color()
                    || normalizedType.equals(TYPE_SKIN) && support.skin() && skinOptions.containsKey(weaponId);
        }
        return normalizedType.equals(TYPE_SKIN) && support.skin() && skinOptions.containsKey(weaponId);
    }

    public boolean supportsWeaponCosmeticType(String weaponId, String type) {
        WeaponCosmeticSupport support = weaponSupport(weaponId);
        if (!support.enabled()) {
            return false;
        }
        String normalizedType = normalize(type);
        return normalizedType.equals(TYPE_LED) && support.led()
                || normalizedType.equals(TYPE_COLOR) && support.color()
                || normalizedType.equals(TYPE_SKIN) && support.skin() && skinOptions.containsKey(normalizeWeaponId(weaponId));
    }

    public List<String> getSkinIds(String weaponId) {
        Map<String, SkinOption> options = skinOptions.get(normalizeWeaponId(weaponId));
        return options == null ? List.of() : List.copyOf(options.keySet());
    }

    public List<String> getSkinnableWeaponIds() {
        return List.copyOf(skinOptions.keySet());
    }

    public List<String> getAllSkinIds() {
        List<String> ids = new ArrayList<>();
        for (Map<String, SkinOption> options : skinOptions.values()) {
            ids.addAll(options.keySet());
        }
        return ids.stream().distinct().toList();
    }

    public SkinOption getSkinOption(String weaponId, String skinId) {
        Map<String, SkinOption> options = skinOptions.get(normalizeWeaponId(weaponId));
        return options == null ? null : options.get(normalize(skinId));
    }

    public String getWeaponLed(ItemStack weaponItem) {
        if (!getWeaponSkin(weaponItem).equals(NONE)) {
            return NONE;
        }
        return getWeaponCosmetic(weaponItem, weaponCosmeticLedKey);
    }

    public String getWeaponColor(ItemStack weaponItem) {
        return getWeaponColorRgb(weaponItem) == null ? NONE : COLOR_VARIANT;
    }

    public Integer getWeaponColorRgb(ItemStack weaponItem) {
        if (!getWeaponSkin(weaponItem).equals(NONE)) {
            return null;
        }
        if (weaponItem == null || !weaponItem.hasItemMeta()) {
            return null;
        }
        PersistentDataContainer pdc = weaponItem.getItemMeta().getPersistentDataContainer();
        Integer storedRgb = pdc.get(weaponCosmeticColorRgbKey, PersistentDataType.INTEGER);
        if (storedRgb != null && isRgb(storedRgb)) {
            return storedRgb;
        }
        String legacy = pdc.get(weaponCosmeticColorKey, PersistentDataType.STRING);
        return parseColorRgb(legacy);
    }

    public String getWeaponSkin(ItemStack weaponItem) {
        String weaponId = getWeaponId(weaponItem);
        if (weaponId == null || weaponItem == null || !weaponItem.hasItemMeta()) {
            return NONE;
        }
        String stored = weaponItem.getItemMeta().getPersistentDataContainer()
                .get(weaponCosmeticSkinKey, PersistentDataType.STRING);
        String normalized = normalize(stored);
        return getSkinOption(weaponId, normalized) == null ? NONE : normalized;
    }

    public String getWeaponSkinSound(ItemStack weaponItem, String soundKey) {
        String weaponId = getWeaponId(weaponItem);
        String skinId = getWeaponSkin(weaponItem);
        if (weaponId == null || skinId.equals(NONE)) {
            return null;
        }
        SkinOption option = getSkinOption(weaponId, skinId);
        if (option == null) {
            return null;
        }
        String sound = option.sounds().get(normalize(soundKey));
        return sound == null || sound.isBlank() ? null : sound;
    }

    public Component decorateWeaponDisplayName(ItemStack weaponItem, WeaponDefinition weapon, Component baseName) {
        String weaponId = weapon == null ? getWeaponId(weaponItem) : normalizeWeaponId(weapon.getId());
        String skinId = getWeaponSkin(weaponItem);
        if (weaponId == null || skinId.equals(NONE)) {
            return baseName;
        }
        SkinOption option = getSkinOption(weaponId, skinId);
        if (option == null || option.nameSuffix().isBlank()) {
            return baseName;
        }
        return baseName.append(Component.text(" ")).append(gradientText("[" + option.nameSuffix() + "]", option.suffixGradient()))
                .decoration(TextDecoration.BOLD, false)
                .decoration(TextDecoration.ITALIC, false);
    }

    public static Component gradientText(String text, List<TextColor> colors) {
        if (text == null || text.isEmpty()) {
            return Component.empty();
        }
        List<TextColor> palette = colors == null || colors.isEmpty()
                ? List.of(TextColor.color(0xFFFFFF))
                : colors;
        Component result = Component.empty();
        int length = text.length();
        for (int i = 0; i < length; i++) {
            TextColor color = interpolate(palette, length == 1 ? 0.0D : i / (double) (length - 1));
            result = result.append(Component.text(String.valueOf(text.charAt(i)), color));
        }
        return result.decoration(TextDecoration.ITALIC, false);
    }

    private static TextColor interpolate(List<TextColor> colors, double position) {
        if (colors.size() == 1) {
            return colors.get(0);
        }
        double scaled = Math.max(0.0D, Math.min(1.0D, position)) * (colors.size() - 1);
        int index = Math.min(colors.size() - 2, (int) Math.floor(scaled));
        double local = scaled - index;
        TextColor start = colors.get(index);
        TextColor end = colors.get(index + 1);
        int red = interpolateChannel(start.red(), end.red(), local);
        int green = interpolateChannel(start.green(), end.green(), local);
        int blue = interpolateChannel(start.blue(), end.blue(), local);
        return TextColor.color(red, green, blue);
    }

    private static int interpolateChannel(int start, int end, double local) {
        return (int) Math.round(start + (end - start) * local);
    }

    public CosmeticSelection getSelection(ItemStack weaponItem) {
        String weaponId = getWeaponId(weaponItem);
        if (weaponId == null) {
            return CosmeticSelection.empty(NONE);
        }
        return new CosmeticSelection(weaponId, getWeaponSkin(weaponItem), getWeaponLed(weaponItem), getWeaponColorRgb(weaponItem));
    }

    public boolean applySelection(ItemStack weaponItem, CosmeticSelection selection) {
        if (selection == null) {
            return false;
        }
        return applySelection(weaponItem, selection.skinId(), selection.ledId(), selection.colorRgb());
    }

    public boolean applySelection(ItemStack weaponItem, String skinId, String ledId, Integer colorRgb) {
        if (!isSupportedWeapon(weaponItem)) {
            return false;
        }
        String weaponId = getWeaponId(weaponItem);
        String normalizedSkin = normalize(skinId);
        String normalizedLed = normalize(ledId);
        if (!normalizedSkin.equals(NONE) && getSkinOption(weaponId, normalizedSkin) == null) {
            return false;
        }
        if (!normalizedLed.equals(NONE) && !ledOptions.containsKey(normalizedLed)) {
            return false;
        }
        if (colorRgb != null && !isRgb(colorRgb)) {
            return false;
        }
        WeaponCosmeticSupport support = weaponSupport(weaponId);
        if (!support.enabled()) {
            return false;
        }
        if (!normalizedSkin.equals(NONE) && !support.skin()) {
            return false;
        }
        if (normalizedSkin.equals(NONE) && !normalizedLed.equals(NONE) && !support.led()) {
            return false;
        }
        if (normalizedSkin.equals(NONE) && colorRgb != null && !support.color()) {
            return false;
        }
        ItemMeta meta = weaponItem.getItemMeta();
        if (meta == null) {
            return false;
        }
        if (!normalizedSkin.equals(NONE)) {
            clearColor(meta);
            clearLed(meta);
            meta.getPersistentDataContainer().set(weaponCosmeticSkinKey, PersistentDataType.STRING, normalizedSkin);
        } else {
            clearSkin(meta);
            if (normalizedLed.equals(NONE)) {
                clearLed(meta);
            } else {
                meta.getPersistentDataContainer().set(weaponCosmeticLedKey, PersistentDataType.STRING, normalizedLed);
            }
            if (colorRgb == null) {
                clearColor(meta);
            } else {
                meta.getPersistentDataContainer().set(weaponCosmeticColorRgbKey, PersistentDataType.INTEGER, colorRgb);
                meta.getPersistentDataContainer().set(weaponCosmeticColorKey, PersistentDataType.STRING, formatHex(colorRgb));
            }
        }
        weaponItem.setItemMeta(meta);
        return true;
    }

    public boolean applyCosmetic(ItemStack weaponItem, String type, String id) {
        if (!isSupportedWeapon(weaponItem) || !isValidOption(type, id) || !supportsCosmeticType(weaponItem, type)) {
            return false;
        }
        String normalizedType = normalize(type);
        String normalizedId = normalize(id);
        ItemMeta meta = weaponItem.getItemMeta();
        if (meta == null) {
            return false;
        }
        if (normalizedType.equals(TYPE_COLOR)) {
            if (normalizedId.equals(NONE)) {
                clearColor(meta);
            } else {
                Integer rgb = parseColorRgb(id);
                if (rgb == null) {
                    return false;
                }
                meta.getPersistentDataContainer().set(weaponCosmeticColorRgbKey, PersistentDataType.INTEGER, rgb);
                meta.getPersistentDataContainer().set(weaponCosmeticColorKey, PersistentDataType.STRING, formatHex(rgb));
                clearSkin(meta);
            }
            weaponItem.setItemMeta(meta);
            return true;
        }

        if (normalizedId.equals(NONE)) {
            meta.getPersistentDataContainer().remove(weaponCosmeticLedKey);
        } else {
            meta.getPersistentDataContainer().set(weaponCosmeticLedKey, PersistentDataType.STRING, normalizedId);
        }
        weaponItem.setItemMeta(meta);
        return true;
    }

    public boolean applySkin(ItemStack weaponItem, String skinId) {
        String weaponId = getWeaponId(weaponItem);
        if (weaponId == null || !supportsCosmeticType(weaponItem, TYPE_SKIN)) {
            return false;
        }
        String normalizedSkin = normalize(skinId);
        if (!normalizedSkin.equals(NONE) && getSkinOption(weaponId, normalizedSkin) == null) {
            return false;
        }
        ItemMeta meta = weaponItem.getItemMeta();
        if (meta == null) {
            return false;
        }
        if (normalizedSkin.equals(NONE)) {
            clearSkin(meta);
        } else {
            clearColor(meta);
            clearLed(meta);
            meta.getPersistentDataContainer().set(weaponCosmeticSkinKey, PersistentDataType.STRING, normalizedSkin);
        }
        weaponItem.setItemMeta(meta);
        return true;
    }

    public boolean clearCosmetic(ItemStack weaponItem, String type) {
        if (!isSupportedWeapon(weaponItem) || !isValidClearType(type)) {
            return false;
        }
        String normalizedType = normalize(type);
        if (!normalizedType.equals("all") && !supportsCosmeticType(weaponItem, normalizedType)) {
            return false;
        }
        ItemMeta meta = weaponItem.getItemMeta();
        if (meta == null) {
            return false;
        }
        if (normalizedType.equals(TYPE_LED) || normalizedType.equals("all")) {
            clearLed(meta);
        }
        if (normalizedType.equals(TYPE_COLOR) || normalizedType.equals("all")) {
            clearColor(meta);
        }
        if (normalizedType.equals(TYPE_SKIN) || normalizedType.equals("all")) {
            clearSkin(meta);
        }
        weaponItem.setItemMeta(meta);
        return true;
    }

    private void clearColor(ItemMeta meta) {
        meta.getPersistentDataContainer().remove(weaponCosmeticColorKey);
        meta.getPersistentDataContainer().remove(weaponCosmeticColorRgbKey);
    }

    private void clearLed(ItemMeta meta) {
        meta.getPersistentDataContainer().remove(weaponCosmeticLedKey);
    }

    private void clearSkin(ItemMeta meta) {
        meta.getPersistentDataContainer().remove(weaponCosmeticSkinKey);
    }

    public boolean isSupportedWeapon(ItemStack item) {
        String weaponId = getWeaponId(item);
        return weaponId != null && weaponSupport(weaponId).enabled();
    }

    public boolean supportsWeapon(String weaponId) {
        return weaponSupport(normalizeWeaponId(weaponId)).enabled();
    }

    public WeaponCosmeticSupport weaponSupport(String weaponId) {
        String normalized = normalizeWeaponId(weaponId);
        WeaponCosmeticSupport configured = weaponSupports.get(normalized);
        if (configured != null) {
            return configured;
        }
        return new WeaponCosmeticSupport(skinOptions.containsKey(normalized), false, false, skinOptions.containsKey(normalized));
    }

    public ItemStack createToken(String type, String id, int amount) {
        String normalizedType = normalize(type);
        if (!isValidType(normalizedType) || !isValidOption(normalizedType, id) || normalize(id).equals(NONE)) {
            return null;
        }
        Integer colorRgb = null;
        String normalizedId = normalize(id);
        if (normalizedType.equals(TYPE_COLOR)) {
            colorRgb = parseColorRgb(id);
            if (colorRgb == null) {
                return null;
            }
            normalizedId = formatHex(colorRgb);
        }
        CosmeticOption option = normalizedType.equals(TYPE_COLOR)
                ? new CosmeticOption(normalizedId, "Color " + formatHex(colorRgb), Material.PAPER, COLOR_TOKEN_MODEL_DATA, colorRgb)
                : options(normalizedType).get(normalizedId);
        ItemStack token = new ItemStack(option.material(), Math.max(1, Math.min(amount, option.material().getMaxStackSize())));
        ItemMeta meta = token.getItemMeta();
        if (meta == null) {
            return token;
        }
        meta.displayName(Component.text(option.displayName() + " Token", NamedTextColor.AQUA)
                .decoration(TextDecoration.ITALIC, false));
        applyVisualCustomModelData(meta, option.customModelData(), option.rgb());
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("Weapon cosmetic: " + normalizedType, NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false));
        if (colorRgb != null) {
            lore.add(Component.text("Color: " + formatHex(colorRgb), NamedTextColor.GRAY)
                    .decoration(TextDecoration.ITALIC, false));
        }
        lore.add(Component.text("Use at the weapon cosmetic station.", NamedTextColor.DARK_GRAY)
                .decoration(TextDecoration.ITALIC, false));
        meta.lore(lore);
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(tokenTypeKey, PersistentDataType.STRING, normalizedType);
        pdc.set(tokenIdKey, PersistentDataType.STRING, normalizedId);
        if (colorRgb != null) {
            pdc.set(tokenColorRgbKey, PersistentDataType.INTEGER, colorRgb);
        }
        token.setItemMeta(meta);
        applyVisualDataComponents(token, option.customModelData(), option.rgb());
        return token;
    }

    public TokenData getToken(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return null;
        }
        PersistentDataContainer pdc = item.getItemMeta().getPersistentDataContainer();
        String type = pdc.get(tokenTypeKey, PersistentDataType.STRING);
        String id = pdc.get(tokenIdKey, PersistentDataType.STRING);
        if (type == null || id == null || !isValidType(type) || normalize(id).equals(NONE)) {
            return null;
        }
        String normalizedType = normalize(type);
        if (normalizedType.equals(TYPE_COLOR)) {
            Integer rgb = pdc.get(tokenColorRgbKey, PersistentDataType.INTEGER);
            if (rgb == null || !isRgb(rgb)) {
                rgb = parseColorRgb(id);
            }
            return rgb == null ? null : new TokenData(TYPE_COLOR, formatHex(rgb), rgb);
        }
        if (!isValidOption(type, id)) {
            return null;
        }
        return new TokenData(TYPE_LED, normalize(id), null);
    }

    public void consumeOne(ItemStack token) {
        if (token == null) {
            return;
        }
        token.setAmount(Math.max(0, token.getAmount() - 1));
    }

    public void applyVisualCustomModelData(ItemMeta meta, int customModelData, Integer rgb) {
        if (meta == null || customModelData <= 0) {
            return;
        }
        meta.setCustomModelData(customModelData);
        CustomModelDataComponent component = meta.getCustomModelDataComponent();
        component.setFloats(List.of((float) customModelData));
        component.setColors(rgb == null ? Collections.emptyList() : List.of(Color.fromRGB(rgb)));
        meta.setCustomModelDataComponent(component);
    }

    public void applyVisualDataComponents(ItemStack item, int customModelData, Integer rgb) {
        if (item == null || customModelData <= 0) {
            return;
        }
        CustomModelData.Builder modelData = CustomModelData.customModelData().addFloat((float) customModelData);
        if (rgb != null) {
            Color color = Color.fromRGB(rgb);
            modelData.addColor(color);
            item.setData(DataComponentTypes.DYED_COLOR, DyedItemColor.dyedItemColor(color));
        } else {
            item.unsetData(DataComponentTypes.DYED_COLOR);
        }
        item.setData(DataComponentTypes.CUSTOM_MODEL_DATA, modelData);
    }

    private String getWeaponCosmetic(ItemStack weaponItem, NamespacedKey key) {
        if (weaponItem == null || !weaponItem.hasItemMeta()) {
            return NONE;
        }
        String stored = weaponItem.getItemMeta().getPersistentDataContainer().get(key, PersistentDataType.STRING);
        return stored == null || stored.isBlank() ? NONE : normalize(stored);
    }

    private boolean isValidClearType(String type) {
        String normalized = normalize(type);
        return normalized.equals(TYPE_LED) || normalized.equals(TYPE_COLOR)
                || normalized.equals(TYPE_SKIN) || normalized.equals("all");
    }

    private Map<String, CosmeticOption> options(String type) {
        return normalize(type).equals(TYPE_LED) ? ledOptions : colorOptions;
    }

    public static Integer parseColorRgb(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = normalize(value);
        Integer alias = COLOR_ALIASES.get(normalized);
        if (alias != null) {
            return alias;
        }
        String hex = value.trim();
        if (hex.startsWith("#")) {
            hex = hex.substring(1);
        }
        if (!hex.matches("(?i)[0-9a-f]{6}")) {
            return null;
        }
        try {
            return Integer.parseInt(hex, 16);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    public static String formatHex(int rgb) {
        return String.format(Locale.ROOT, "#%06X", rgb & 0xFFFFFF);
    }

    private static boolean isRgb(int rgb) {
        return rgb >= 0 && rgb <= 0xFFFFFF;
    }

    public static String normalize(String value) {
        if (value == null || value.isBlank()) {
            return NONE;
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT).replace('_', '-');
        return normalized.equals("empty") ? NONE : normalized;
    }

    public static String normalizeWeaponId(String value) {
        if (value == null || value.isBlank()) {
            return NONE;
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT).replace('-', '_');
        if (normalized.equals("ak47")) {
            return "ak_47";
        }
        return normalized.equals("empty") ? NONE : normalized;
    }

    private static String aliasDisplayName(String id) {
        String normalized = normalize(id);
        if (normalized.equals(NONE)) {
            return "Color";
        }
        return normalized.substring(0, 1).toUpperCase(Locale.ROOT) + normalized.substring(1) + " Finish";
    }

    private static Map<String, Integer> createColorAliases() {
        Map<String, Integer> aliases = new LinkedHashMap<>();
        aliases.put("red", 0xB02E26);
        aliases.put("blue", 0x3C44AA);
        aliases.put("green", 0x5E7C16);
        aliases.put("white", 0xF9FFFE);
        aliases.put("black", 0x1D1D21);
        return Collections.unmodifiableMap(aliases);
    }

    public record CosmeticOption(String id, String displayName, Material material, int customModelData, Integer rgb) {
    }

    public record SkinOption(String weaponId, String id, String displayName, String nameSuffix,
                             List<TextColor> suffixGradient, Map<String, String> sounds) {
    }

    public record TokenData(String type, String id, Integer rgb) {
    }

    public record CosmeticSelection(String weaponId, String skinId, String ledId, Integer colorRgb) {
        public CosmeticSelection {
            weaponId = normalizeWeaponId(weaponId);
            skinId = normalize(skinId);
            ledId = normalize(ledId);
        }

        public static CosmeticSelection empty(String weaponId) {
            return new CosmeticSelection(weaponId, NONE, NONE, null);
        }

        public String colorHex() {
            return colorRgb == null ? NONE : formatHex(colorRgb);
        }
    }

    public record WeaponCosmeticSupport(boolean enabled, boolean led, boolean color, boolean skin) {
    }
}
