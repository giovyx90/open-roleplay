package dev.openrp.cosmetics;

import dev.openrp.cosmetics.api.OpenCosmeticsApi;
import dev.openrp.cosmetics.api.OpenCosmeticsWeaponBridge;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class OpenCosmeticsPlugin extends JavaPlugin implements OpenCosmeticsApi {
    private WeaponCosmeticManager manager;
    private WeaponCosmeticWorkbenchGUI workbenchGUI;
    private WeaponCosmeticEditorGUI editorGUI;
    private WeaponCosmeticStationManager stationManager;
    private final java.util.Set<UUID> automaticSkinFireSuppression = ConcurrentHashMap.newKeySet();

    @Override
    public void onEnable() {
        File cosmeticsFile = new File(getDataFolder(), "weapon_cosmetics.yml");
        if (!cosmeticsFile.exists()) {
            saveResource("weapon_cosmetics.yml", false);
        }
        backfillWeaponCosmeticDefaults(cosmeticsFile);

        this.manager = new WeaponCosmeticManager(this);
        this.workbenchGUI = new WeaponCosmeticWorkbenchGUI(this, manager);
        this.editorGUI = new WeaponCosmeticEditorGUI(this, manager);
        this.stationManager = new WeaponCosmeticStationManager(this, workbenchGUI);

        getServer().getPluginManager().registerEvents(workbenchGUI, this);
        getServer().getPluginManager().registerEvents(editorGUI, this);
        getServer().getPluginManager().registerEvents(stationManager, this);
        stationManager.load();

        WeaponCosmeticCommand command = new WeaponCosmeticCommand(this, manager);
        PluginCommand pluginCommand = getCommand("opencosmetics");
        if (pluginCommand != null) {
            pluginCommand.setExecutor(command);
            pluginCommand.setTabCompleter(command);
        } else {
            getLogger().warning("[OpenCosmetics] /opencosmetics manca in plugin.yml.");
        }

        Bukkit.getServicesManager().register(OpenCosmeticsApi.class, this, this, ServicePriority.Normal);
        if (getWeaponBridge() == null) {
            getLogger().warning("[OpenCosmetics] Nessun bridge armi registrato: GUI e token restano disponibili, ma non possono modificare armi finche' Open Weapons non si collega.");
        }
        getLogger().info("[OpenCosmetics] Plugin abilitato correttamente.");
    }

    @Override
    public void onDisable() {
        HandlerList.unregisterAll(this);
        Bukkit.getServicesManager().unregister(OpenCosmeticsApi.class, this);
        automaticSkinFireSuppression.clear();
    }

    OpenCosmeticsWeaponBridge getWeaponBridge() {
        var registration = Bukkit.getServicesManager().getRegistration(OpenCosmeticsWeaponBridge.class);
        return registration == null ? null : registration.getProvider();
    }

    void refreshWeaponVisual(ItemStack weaponItem) {
        OpenCosmeticsWeaponBridge bridge = getWeaponBridge();
        if (bridge != null) {
            bridge.refreshWeaponVisual(weaponItem);
        }
    }

    WeaponCosmeticStationManager getStationManager() {
        return stationManager;
    }

    @Override
    public void reload() {
        if (manager != null) {
            File cosmeticsFile = new File(getDataFolder(), "weapon_cosmetics.yml");
            backfillWeaponCosmeticDefaults(cosmeticsFile);
            manager.load(cosmeticsFile);
        }
        if (stationManager != null) {
            stationManager.load();
        }
    }

    @Override
    public boolean supportsWeapon(String weaponId) {
        return manager != null && manager.supportsWeapon(weaponId);
    }

    @Override
    public boolean supportsWeaponCosmeticType(String weaponId, String type) {
        return manager != null && manager.supportsWeaponCosmeticType(weaponId, type);
    }

    @Override
    public List<String> getSkinIds(String weaponId) {
        return manager == null ? List.of() : manager.getSkinIds(weaponId);
    }

    @Override
    public List<String> getLedIds() {
        return manager == null ? List.of() : manager.getLedIds();
    }

    @Override
    public List<String> getColorIds() {
        return manager == null ? List.of() : manager.getColorIds();
    }

    @Override
    public String getSkinDisplayName(String weaponId, String skinId) {
        WeaponCosmeticManager.SkinOption option = manager == null ? null : manager.getSkinOption(weaponId, skinId);
        return option == null ? skinId : option.displayName();
    }

    @Override
    public String getLedDisplayName(String ledId) {
        WeaponCosmeticManager.CosmeticOption option = manager == null ? null : manager.getLedOption(ledId);
        return option == null ? ledId : option.displayName();
    }

    @Override
    public String getColorDisplayName(String colorId) {
        WeaponCosmeticManager.CosmeticOption option = manager == null ? null : manager.getColorOption(colorId);
        return option == null ? colorId : option.displayName();
    }

    @Override
    public String getColorHex(String colorId) {
        WeaponCosmeticManager.CosmeticOption option = manager == null ? null : manager.getColorOption(colorId);
        return option == null || option.rgb() == null ? NONE : WeaponCosmeticManager.formatHex(option.rgb());
    }

    @Override
    public ItemStack createToken(String type, String id, int amount) {
        return manager == null ? null : manager.createToken(type, id, amount);
    }

    @Override
    public boolean applySelection(ItemStack weaponItem, String skinId, String ledId, String color) {
        if (manager == null) {
            return false;
        }
        Integer rgb = WeaponCosmeticManager.normalize(color).equals(WeaponCosmeticManager.NONE)
                ? null
                : WeaponCosmeticManager.parseColorRgb(color);
        if (color != null && !color.isBlank() && rgb == null
                && !WeaponCosmeticManager.normalize(color).equals(WeaponCosmeticManager.NONE)) {
            return false;
        }
        boolean applied = manager.applySelection(weaponItem, skinId, ledId, rgb);
        if (applied) {
            refreshWeaponVisual(weaponItem);
        }
        return applied;
    }

    @Override
    public void openWorkbench(Player player) {
        if (player != null && workbenchGUI != null) {
            workbenchGUI.open(player);
        }
    }

    @Override
    public void openEditor(Player player) {
        if (player != null && editorGUI != null) {
            editorGUI.open(player);
        }
    }

    @Override
    public String getWeaponSkinSound(ItemStack weaponItem, String soundKey) {
        return manager == null ? null : manager.getWeaponSkinSound(weaponItem, soundKey);
    }

    @Override
    public Component decorateWeaponDisplayName(ItemStack weaponItem, Component baseName) {
        return manager == null ? baseName : manager.decorateWeaponDisplayName(weaponItem, baseName);
    }

    @Override
    public List<String> visualVariantCandidates(ItemStack weaponItem, boolean optic, boolean hasMagazine, boolean grip) {
        if (manager == null) {
            return List.of();
        }
        return WeaponVisualVariantResolver.candidates(
                optic,
                hasMagazine,
                grip,
                manager.getWeaponLed(weaponItem),
                manager.getWeaponColor(weaponItem),
                manager.getWeaponSkin(weaponItem));
    }

    @Override
    public Integer getWeaponColorRgb(ItemStack weaponItem) {
        return manager == null ? null : manager.getWeaponColorRgb(weaponItem);
    }

    @Override
    public void applyVisualCustomModelData(ItemMeta meta, int customModelData, Integer rgb) {
        if (manager != null) {
            manager.applyVisualCustomModelData(meta, customModelData, rgb);
        } else if (meta != null && customModelData > 0) {
            meta.setCustomModelData(customModelData);
        }
    }

    @Override
    public void applyVisualDataComponents(ItemStack item, int customModelData, Integer rgb) {
        if (manager != null) {
            manager.applyVisualDataComponents(item, customModelData, rgb);
        }
    }

    @Override
    public void setAutomaticSkinFireSuppressed(UUID playerId, boolean suppressed) {
        if (playerId == null) {
            return;
        }
        if (suppressed) {
            automaticSkinFireSuppression.add(playerId);
        } else {
            automaticSkinFireSuppression.remove(playerId);
        }
    }

    @Override
    public boolean isAutomaticSkinFireSuppressed(UUID playerId) {
        return playerId != null && automaticSkinFireSuppression.contains(playerId);
    }

    private void backfillWeaponCosmeticDefaults(File cosmeticsFile) {
        try (InputStream resource = getResource("weapon_cosmetics.yml")) {
            if (resource == null) {
                return;
            }

            YamlConfiguration defaults = YamlConfiguration.loadConfiguration(
                    new InputStreamReader(resource, StandardCharsets.UTF_8));
            YamlConfiguration current = YamlConfiguration.loadConfiguration(cosmeticsFile);
            boolean changed = false;

            changed |= migrateLegacyLedTokenDefaults(current, defaults);
            changed |= migrateLegacyColorTokenDefaults(current, defaults);
            changed |= migrateLegacySkinDisplayDefaults(current, defaults);
            for (String key : List.of("weapons", "leds", "colors", "skins")) {
                if (defaults.contains(key)) {
                    changed |= mergeMissingConfigValue(current, defaults, key);
                }
            }

            if (changed) {
                current.save(cosmeticsFile);
                getLogger().info("[OpenCosmetics] Aggiunti i default mancanti dei gettoni cosmetici arma in weapon_cosmetics.yml.");
            }
        } catch (Exception e) {
            getLogger().warning("[OpenCosmetics] Impossibile completare il backfill dei default cosmetici arma: " + e.getMessage());
        }
    }

    private boolean migrateLegacyLedTokenDefaults(YamlConfiguration current, YamlConfiguration defaults) {
        ConfigurationSection defaultLeds = defaults.getConfigurationSection("leds");
        ConfigurationSection currentLeds = current.getConfigurationSection("leds");
        if (defaultLeds == null || currentLeds == null) {
            return false;
        }

        boolean changed = false;
        for (String ledId : defaultLeds.getKeys(false)) {
            ConfigurationSection defaultOption = defaultLeds.getConfigurationSection(ledId);
            ConfigurationSection currentOption = currentLeds.getConfigurationSection(ledId);
            if (defaultOption == null || currentOption == null) {
                continue;
            }
            int defaultCustomModelData = defaultOption.getInt("token-custom-model-data", 0);
            if (defaultCustomModelData > 0 && !currentOption.contains("token-custom-model-data")) {
                currentOption.set("token-custom-model-data", defaultCustomModelData);
                changed = true;
            }
            String currentMaterial = currentOption.getString("token-material", "");
            String defaultMaterial = defaultOption.getString("token-material", "");
            if (currentMaterial.equalsIgnoreCase("REDSTONE_TORCH") && !defaultMaterial.isBlank()) {
                currentOption.set("token-material", defaultMaterial);
                changed = true;
            }
        }
        return changed;
    }

    private boolean migrateLegacyColorTokenDefaults(YamlConfiguration current, YamlConfiguration defaults) {
        ConfigurationSection defaultColors = defaults.getConfigurationSection("colors");
        ConfigurationSection currentColors = current.getConfigurationSection("colors");
        if (defaultColors == null || currentColors == null) {
            return false;
        }

        boolean changed = false;
        for (String colorId : defaultColors.getKeys(false)) {
            ConfigurationSection defaultOption = defaultColors.getConfigurationSection(colorId);
            ConfigurationSection currentOption = currentColors.getConfigurationSection(colorId);
            if (defaultOption == null || currentOption == null) {
                continue;
            }
            String defaultMaterial = defaultOption.getString("token-material", "");
            if (!defaultMaterial.isBlank() && !currentOption.getString("token-material", "").equalsIgnoreCase(defaultMaterial)) {
                currentOption.set("token-material", defaultMaterial);
                changed = true;
            }
            int defaultCustomModelData = defaultOption.getInt("token-custom-model-data", 0);
            if (defaultCustomModelData > 0 && currentOption.getInt("token-custom-model-data", 0) != defaultCustomModelData) {
                currentOption.set("token-custom-model-data", defaultCustomModelData);
                changed = true;
            }
            String defaultHex = defaultOption.getString("hex", "");
            if (!defaultHex.isBlank() && !currentOption.contains("hex")) {
                currentOption.set("hex", defaultHex);
                changed = true;
            }
        }
        return changed;
    }

    private boolean migrateLegacySkinDisplayDefaults(YamlConfiguration current, YamlConfiguration defaults) {
        ConfigurationSection defaultSkins = defaults.getConfigurationSection("skins");
        ConfigurationSection currentSkins = current.getConfigurationSection("skins");
        if (defaultSkins == null || currentSkins == null) {
            return false;
        }

        boolean changed = false;
        for (String weaponId : defaultSkins.getKeys(false)) {
            ConfigurationSection defaultWeapon = defaultSkins.getConfigurationSection(weaponId);
            ConfigurationSection currentWeapon = currentSkins.getConfigurationSection(weaponId);
            if (defaultWeapon == null || currentWeapon == null) {
                continue;
            }
            for (String skinId : defaultWeapon.getKeys(false)) {
                ConfigurationSection defaultSkin = defaultWeapon.getConfigurationSection(skinId);
                ConfigurationSection currentSkin = currentWeapon.getConfigurationSection(skinId);
                if (defaultSkin == null || currentSkin == null) {
                    continue;
                }
                String currentDisplayName = currentSkin.getString("display-name", "");
                String defaultDisplayName = defaultSkin.getString("display-name", "");
                if (skinId.equalsIgnoreCase("gold-reserve") && currentDisplayName.contains("Gold Reserve")
                        && !defaultDisplayName.isBlank()) {
                    currentSkin.set("display-name", defaultDisplayName);
                    changed = true;
                }
                if (!currentSkin.contains("name-suffix") && defaultSkin.contains("name-suffix")) {
                    currentSkin.set("name-suffix", defaultSkin.get("name-suffix"));
                    changed = true;
                }
                if (!currentSkin.contains("suffix-gradient") && defaultSkin.contains("suffix-gradient")) {
                    currentSkin.set("suffix-gradient", defaultSkin.get("suffix-gradient"));
                    changed = true;
                }
            }
        }
        return changed;
    }

    private boolean mergeMissingConfigValue(ConfigurationSection currentParent, ConfigurationSection defaultParent, String key) {
        if (!currentParent.contains(key)) {
            currentParent.set(key, defaultParent.get(key));
            return true;
        }

        ConfigurationSection defaultSection = defaultParent.getConfigurationSection(key);
        ConfigurationSection currentSection = currentParent.getConfigurationSection(key);
        if (defaultSection == null || currentSection == null) {
            return false;
        }

        boolean changed = false;
        for (String childKey : defaultSection.getKeys(false)) {
            changed |= mergeMissingConfigValue(currentSection, defaultSection, childKey);
        }
        return changed;
    }
}
