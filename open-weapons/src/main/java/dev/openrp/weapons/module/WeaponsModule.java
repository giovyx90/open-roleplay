package dev.openrp.weapons.module;

import it.meridian.core.CorePlugin;
import it.meridian.core.module.NextModule;
import dev.openrp.weapons.api.WeaponCombatDecision;
import dev.openrp.weapons.api.WeaponCombatPolicy;
import dev.openrp.weapons.api.WeaponImpactContext;
import dev.openrp.weapons.api.WeaponTargetContext;
import dev.openrp.weapons.api.WeaponUseContext;
import dev.openrp.weapons.attachments.AttachmentManager;
import dev.openrp.weapons.attachments.AttachmentAuditLogger;
import dev.openrp.weapons.attachments.AttachmentRegistry;
import dev.openrp.weapons.attachments.AttachmentWorkbenchCommand;
import dev.openrp.weapons.attachments.AttachmentWorkbenchGUI;
import dev.openrp.weapons.armor.ArmorListener;
import dev.openrp.weapons.armor.ArmorManager;
import dev.openrp.weapons.armor.HelmetManager;
import dev.openrp.weapons.actions.QuickActionListener;
import dev.openrp.weapons.arrest.ArrestAdminCommand;
import dev.openrp.weapons.arrest.ArrestCommand;
import dev.openrp.weapons.arrest.ArrestGUI;
import dev.openrp.weapons.arrest.ArrestListener;
import dev.openrp.weapons.arrest.ArrestManager;
import dev.openrp.weapons.arrest.BailCommand;
import dev.openrp.weapons.balaclava.BalaclavaListener;
import dev.openrp.weapons.balaclava.BalaclavaManager;
import dev.openrp.weapons.commands.UncuffCommand;
import dev.openrp.weapons.commands.DamageDummyListener;
import dev.openrp.weapons.commands.ItemsCommand;
import dev.openrp.weapons.commands.WeaponsCommand;
import dev.openrp.weapons.config.WeaponConfigCommand;
import dev.openrp.weapons.config.WeaponConfigEditor;
import dev.openrp.weapons.config.WeaponConfigGUI;
import dev.openrp.weapons.cosmetics.WeaponCosmeticCommand;
import dev.openrp.weapons.cosmetics.WeaponCosmeticEditorGUI;
import dev.openrp.weapons.cosmetics.WeaponCosmeticManager;
import dev.openrp.weapons.cosmetics.WeaponCosmeticStationManager;
import dev.openrp.weapons.cosmetics.WeaponCosmeticWorkbenchGUI;
import dev.openrp.weapons.c4.C4Manager;
import dev.openrp.weapons.dispatch.DispatchGpsManager;
import dev.openrp.weapons.grenades.GrenadeListener;
import dev.openrp.weapons.grenades.GrenadeManager;
import dev.openrp.weapons.gui.ItemsGUI;
import dev.openrp.weapons.gui.WeaponsGUI;
import dev.openrp.weapons.phone.MobilePhoneManager;
import dev.openrp.weapons.phone.PhoneGUI;
import dev.openrp.weapons.phone.PhoneListener;
import dev.openrp.weapons.phone.SosCommand;
import dev.openrp.weapons.phone.SosGUI;
import dev.openrp.weapons.phone.SosManager;
import dev.openrp.weapons.handcuffs.HandcuffListener;
import dev.openrp.weapons.handcuffs.HandcuffManager;
import dev.openrp.weapons.listeners.GunListener;
import dev.openrp.weapons.magazine.MagazineManager;
import dev.openrp.weapons.listeners.MeleeListener;
import dev.openrp.weapons.mechanics.CombatStunManager;
import dev.openrp.weapons.mechanics.WeaponAnimationSuppressor;
import dev.openrp.weapons.model.WeaponDefinition;
import dev.openrp.weapons.frisk.FriskCommand;
import dev.openrp.weapons.frisk.FriskListener;
import dev.openrp.weapons.robbery.RobCommand;
import dev.openrp.weapons.robbery.RobListener;
import dev.openrp.weapons.robbery.RobberyManager;
import dev.openrp.weapons.radio.LawRadioManager;
import dev.openrp.weapons.registry.AmmoRegistry;
import dev.openrp.weapons.registry.WeaponRegistry;
import dev.openrp.weapons.shield.ShieldListener;
import dev.openrp.weapons.shield.ShieldManager;
import dev.openrp.weapons.taser.TaserListener;
import dev.openrp.weapons.util.JumpRestrictionManager;
import dev.openrp.weapons.utility.UtilityItemListener;
import dev.openrp.weapons.utility.UtilityItemManager;
import dev.openrp.weapons.utility.UtilitySettings;
import dev.openrp.weapons.wanted.WantedCommand;
import dev.openrp.weapons.wanted.WantedGUI;
import dev.openrp.weapons.wanted.WantedManager;
import it.meridian.azienda.module.AziendaModule;
import it.meridian.azienda.model.Company;
import it.meridian.police.module.PoliceModule;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class WeaponsModule implements NextModule {
    private CorePlugin core;
    private WeaponRegistry weaponRegistry;
    private AmmoRegistry ammoRegistry;
    private AttachmentRegistry attachmentRegistry;
    private AttachmentManager attachmentManager;
    private AttachmentAuditLogger attachmentAuditLogger;
    private AttachmentWorkbenchGUI attachmentWorkbenchGUI;
    private MagazineManager magazineManager;
    private BalaclavaManager balaclavaManager;
    private ArmorManager armorManager;
    private HelmetManager helmetManager;
    private GrenadeManager grenadeManager;
    private C4Manager c4Manager;
    private HandcuffManager handcuffManager;
    private CombatStunManager combatStunManager;
    private ArrestManager arrestManager;
    private RobberyManager robberyManager;
    private ArrestGUI arrestGUI;
    private GunListener gunListener;
    private MobilePhoneManager mobilePhoneManager;
    private PhoneGUI phoneGUI;
    private SosGUI sosGUI;
    private SosManager sosManager;
    private DispatchGpsManager dispatchGpsManager;
    private LawRadioManager lawRadioManager;
    private WantedManager wantedManager;
    private WantedGUI wantedGUI;
    private ShieldManager shieldManager;
    private UtilityItemManager utilityItemManager;
    private UtilityItemListener utilityItemListener;
    private UtilitySettings utilitySettings;
    private WeaponAnimationSuppressor weaponAnimationSuppressor;
    private WeaponCosmeticManager weaponCosmeticManager;
    private WeaponCosmeticWorkbenchGUI weaponCosmeticWorkbenchGUI;
    private WeaponCosmeticEditorGUI weaponCosmeticEditorGUI;
    private WeaponCosmeticStationManager weaponCosmeticStationManager;
    private WeaponConfigEditor weaponConfigEditor;
    private WeaponConfigGUI weaponConfigGUI;
    private YamlConfiguration messagesConfig;
    private final List<Listener> listeners = new ArrayList<>();
    private final List<WeaponCombatPolicy> combatPolicies = new CopyOnWriteArrayList<>();
    private final Set<UUID> automaticSkinFireSuppression = ConcurrentHashMap.newKeySet();

    @Override
    public String getName() {
        return "weapons";
    }

    @Override
    public void onEnable(CorePlugin core) {
        this.core = core;

        // Ensure config files exist
        File weaponsFile = new File(core.getDataFolder(), "weapons.yml");
        if (!weaponsFile.exists()) {
            core.saveResource("weapons.yml", false);
        }
        backfillWeaponVisualDefaults(weaponsFile);
        File ammoFile = new File(core.getDataFolder(), "ammo.yml");
        if (!ammoFile.exists()) {
            core.saveResource("ammo.yml", false);
        }
        File armorFile = new File(core.getDataFolder(), "armor.yml");
        if (!armorFile.exists()) {
            core.saveResource("armor.yml", false);
        }
        File attachmentsFile = new File(core.getDataFolder(), "attachments.yml");
        if (!attachmentsFile.exists()) {
            core.saveResource("attachments.yml", false);
        }
        File messagesFile = new File(core.getDataFolder(), "messages_en.yml");
        if (!messagesFile.exists()) {
            core.saveResource("messages_en.yml", false);
        }
        this.messagesConfig = YamlConfiguration.loadConfiguration(messagesFile);

        // Initialize Registries
        this.weaponRegistry = new WeaponRegistry(core);
        this.weaponRegistry.load(weaponsFile);

        this.ammoRegistry = new AmmoRegistry(core);
        this.ammoRegistry.load(ammoFile);

        this.attachmentRegistry = new AttachmentRegistry(core);
        this.attachmentRegistry.load(attachmentsFile);
        this.attachmentManager = new AttachmentManager(this, attachmentRegistry);
        this.attachmentAuditLogger = new AttachmentAuditLogger(this);
        File cosmeticsFile = new File(core.getDataFolder(), "weapon_cosmetics.yml");
        if (!cosmeticsFile.exists()) {
            core.saveResource("weapon_cosmetics.yml", false);
        }
        backfillWeaponCosmeticDefaults(cosmeticsFile);
        this.weaponCosmeticManager = new WeaponCosmeticManager(core, weaponRegistry);
        this.weaponRegistry.setDisplayNameDecorator(weaponCosmeticManager::decorateWeaponDisplayName);
        this.weaponCosmeticWorkbenchGUI = new WeaponCosmeticWorkbenchGUI(this, weaponCosmeticManager);
        this.weaponCosmeticEditorGUI = new WeaponCosmeticEditorGUI(this, weaponCosmeticManager);
        this.weaponCosmeticStationManager = new WeaponCosmeticStationManager(this, weaponCosmeticWorkbenchGUI);

        this.magazineManager = new MagazineManager(core);
        this.utilitySettings = UtilitySettings.fromConfig(core.getConfig().getConfigurationSection("weapons.utility"));

        // Initialize Balaclava
        this.balaclavaManager = new BalaclavaManager(core);

        // Initialize Armor
        this.armorManager = new ArmorManager(core);
        this.armorManager.load(armorFile);

        // Initialize Helmets
        this.helmetManager = new HelmetManager(core);
        this.helmetManager.load(armorFile);

        // Initialize Shields
        this.shieldManager = new ShieldManager(core);

        // Initialize Grenades
        File grenadesFile = new File(core.getDataFolder(), "grenades.yml");
        if (!grenadesFile.exists()) {
            core.saveResource("grenades.yml", false);
        }
        backfillGrenadeVisualDefaults(grenadesFile);
        this.grenadeManager = new GrenadeManager(core);
        this.grenadeManager.load(grenadesFile);
        this.c4Manager = new C4Manager(this);

        // Initialize Handcuffs
        this.handcuffManager = new HandcuffManager(core);
        this.utilityItemManager = new UtilityItemManager(core, utilitySettings);

        // Initialize Mobile Phone
        this.dispatchGpsManager = new DispatchGpsManager(core);
        this.mobilePhoneManager = new MobilePhoneManager(core);
        this.sosManager = new SosManager(this);
        this.sosGUI = new SosGUI(this);
        this.phoneGUI = new PhoneGUI(this);
        this.lawRadioManager = new LawRadioManager(this);
        this.wantedManager = new WantedManager(this);
        this.wantedGUI = new WantedGUI(this);

        this.combatStunManager = new CombatStunManager();

        // Listeners
        this.weaponAnimationSuppressor = new WeaponAnimationSuppressor(this);
        registerListener(weaponAnimationSuppressor);
        weaponAnimationSuppressor.enablePacketHook();

        this.gunListener = new GunListener(this);
        registerListener(gunListener);
        registerListener(new MeleeListener(this));
        registerListener(new BalaclavaListener(this));
        registerListener(new ArmorListener(this));
        registerListener(new HandcuffListener(this));
        registerListener(new QuickActionListener(this));
        registerListener(new TaserListener(this));
        registerListener(new ShieldListener(this));
        registerListener(new GrenadeListener(this));
        registerListener(c4Manager);
        registerListener(new PhoneListener(this));
        registerListener(phoneGUI);
        registerListener(sosGUI);
        registerListener(sosManager);
        registerListener(lawRadioManager);
        registerListener(wantedGUI);
        this.utilityItemListener = new UtilityItemListener(this);
        registerListener(utilityItemListener);
        
        this.robberyManager = new RobberyManager(this);
        registerListener(new RobListener(this));
        registerListener(new FriskListener(this));

        // GUI and Commands — Categorized Items GUI (replaces old Weapons GUI)
        this.attachmentWorkbenchGUI = new AttachmentWorkbenchGUI(this);
        registerListener(attachmentWorkbenchGUI);
        registerListener(weaponCosmeticWorkbenchGUI);
        registerListener(weaponCosmeticEditorGUI);
        registerListener(weaponCosmeticStationManager);
        weaponCosmeticStationManager.load();

        ItemsGUI itemsGui = new ItemsGUI(this);
        registerListener(itemsGui);
        this.weaponConfigEditor = new WeaponConfigEditor(this);
        this.weaponConfigGUI = new WeaponConfigGUI(this, weaponConfigEditor);
        registerListener(weaponConfigGUI);

        DamageDummyListener damageDummyListener = new DamageDummyListener(core);
        registerListener(damageDummyListener);
        ItemsCommand itemsCommand = new ItemsCommand(this, itemsGui, damageDummyListener);
        core.getCommand("items").setExecutor(itemsCommand);
        core.getCommand("items").setTabCompleter(itemsCommand);
        // /weapons is registered as an alias of /items in plugin.yml

        if (core.getCommand("weaponconfig") != null) {
            WeaponConfigCommand weaponConfigCommand = new WeaponConfigCommand(this, weaponConfigEditor, weaponConfigGUI);
            core.getCommand("weaponconfig").setExecutor(weaponConfigCommand);
            core.getCommand("weaponconfig").setTabCompleter(weaponConfigCommand);
        } else {
            core.getLogger().warning("[OpenWeapons] /weaponconfig is missing from plugin.yml.");
        }

        if (core.getCommand("weaponcosmetic") != null) {
            WeaponCosmeticCommand cosmeticCommand = new WeaponCosmeticCommand(this, weaponCosmeticManager);
            core.getCommand("weaponcosmetic").setExecutor(cosmeticCommand);
            core.getCommand("weaponcosmetic").setTabCompleter(cosmeticCommand);
        } else {
            core.getLogger().warning("[OpenWeapons] /weaponcosmetic is missing from plugin.yml.");
        }

        if (core.getCommand("weaponbench") != null) {
            core.getCommand("weaponbench").setExecutor(new AttachmentWorkbenchCommand(this));
        } else {
            core.getLogger().warning("[OpenWeapons] /weaponbench is missing from plugin.yml.");
        }

        UncuffCommand uncuffCommand = new UncuffCommand(this);
        core.getCommand("uncuff").setExecutor(uncuffCommand);
        registerListener(uncuffCommand);
        
        core.getCommand("rob").setExecutor(new RobCommand(this));
        core.getCommand("frisk").setExecutor(new FriskCommand(this));

        // Arrest System
        this.arrestManager = new ArrestManager(this);
        this.arrestGUI = new ArrestGUI(this);
        registerListener(arrestGUI);
        registerListener(new ArrestListener(this));

        ArrestCommand arrestCommand = new ArrestCommand(this);
        core.getCommand("arrest").setExecutor(arrestCommand);
        core.getCommand("arrest").setTabCompleter(arrestCommand);

        core.getCommand("bail").setExecutor(new BailCommand(this));

        ArrestAdminCommand arrestAdminCmd = new ArrestAdminCommand(this);
        core.getCommand("arrests").setExecutor(arrestAdminCmd);
        core.getCommand("arrests").setTabCompleter(arrestAdminCmd);

        WantedCommand wantedCommand = new WantedCommand(this);
        if (core.getCommand("wanted") != null) {
            core.getCommand("wanted").setExecutor(wantedCommand);
            core.getCommand("wanted").setTabCompleter(wantedCommand);
        } else {
            core.getLogger().warning("[OpenWeapons] /wanted is missing from plugin.yml.");
        }

        if (core.getCommand("sos") != null) {
            core.getCommand("sos").setExecutor(new SosCommand(this));
        } else {
            core.getLogger().warning("[OpenWeapons] /sos is missing from plugin.yml.");
        }

        if (core.getCommand("lawradio") != null) {
            core.getCommand("lawradio").setExecutor(lawRadioManager);
        } else {
            core.getLogger().warning("[OpenWeapons] /lawradio is missing from plugin.yml.");
        }

        syncOnlineJumpRestrictions();

        core.getLogger().info("[OpenWeapons] Module enabled successfully.");
    }

    private void backfillWeaponVisualDefaults(File weaponsFile) {
        try (InputStream resource = core.getResource("weapons.yml")) {
            if (resource == null) {
                return;
            }

            YamlConfiguration defaults = YamlConfiguration.loadConfiguration(
                    new InputStreamReader(resource, StandardCharsets.UTF_8));
            YamlConfiguration current = YamlConfiguration.loadConfiguration(weaponsFile);
            boolean changed = false;

            for (String weaponId : defaults.getKeys(false)) {
                ConfigurationSection defaultSection = defaults.getConfigurationSection(weaponId);
                ConfigurationSection currentSection = current.getConfigurationSection(weaponId);
                if (defaultSection == null || currentSection == null
                        || !defaultSection.contains("category") || !defaultSection.contains("material")) {
                    continue;
                }

                for (String key : List.of(
                        "custom-model-data",
                        "visual-states",
                        "visual-variants",
                        "magazine-visual-offset")) {
                    if (defaultSection.contains(key)) {
                        changed |= mergeMissingConfigValue(currentSection, defaultSection, key);
                    }
                }
            }

            if (changed) {
                current.save(weaponsFile);
                core.getLogger().info("[OpenWeapons] Added missing weapon visual model data to weapons.yml.");
            }
        } catch (Exception e) {
            core.getLogger().warning("[OpenWeapons] Could not backfill weapon visual model data: " + e.getMessage());
        }
    }

    private void backfillWeaponCosmeticDefaults(File cosmeticsFile) {
        try (InputStream resource = core.getResource("weapon_cosmetics.yml")) {
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
                core.getLogger().info("[OpenWeapons] Added missing weapon cosmetic token defaults to weapon_cosmetics.yml.");
            }
        } catch (Exception e) {
            core.getLogger().warning("[OpenWeapons] Could not backfill weapon cosmetic defaults: " + e.getMessage());
        }
    }

    private void backfillGrenadeVisualDefaults(File grenadesFile) {
        try (InputStream resource = core.getResource("grenades.yml")) {
            if (resource == null) {
                return;
            }

            YamlConfiguration defaults = YamlConfiguration.loadConfiguration(
                    new InputStreamReader(resource, StandardCharsets.UTF_8));
            YamlConfiguration current = YamlConfiguration.loadConfiguration(grenadesFile);
            boolean changed = false;

            ConfigurationSection defaultRoot = defaults.getConfigurationSection("grenades");
            ConfigurationSection currentRoot = current.getConfigurationSection("grenades");
            if (defaultRoot == null || currentRoot == null) {
                return;
            }

            for (String grenadeId : defaultRoot.getKeys(false)) {
                ConfigurationSection defaultSection = defaultRoot.getConfigurationSection(grenadeId);
                ConfigurationSection currentSection = currentRoot.getConfigurationSection(grenadeId);
                if (defaultSection == null || currentSection == null) {
                    continue;
                }
                if (usesLegacyGrenadeVisual(currentSection)) {
                    currentSection.set("material", defaultSection.getString("material"));
                    currentSection.set("custom-model-data", defaultSection.getInt("custom-model-data"));
                    changed = true;
                }
            }

            if (changed) {
                current.save(grenadesFile);
                core.getLogger().info("[OpenWeapons] Migrated grenade visuals to dedicated resource-pack models.");
            }
        } catch (Exception e) {
            core.getLogger().warning("[OpenWeapons] Could not backfill grenade visual model data: " + e.getMessage());
        }
    }

    private boolean usesLegacyGrenadeVisual(ConfigurationSection section) {
        String material = section.getString("material", "");
        int customModelData = section.getInt("custom-model-data", 0);
        return material.equalsIgnoreCase("FIREWORK_STAR")
                && customModelData >= 271
                && customModelData <= 275;
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
            if (defaultCustomModelData <= 0) {
                continue;
            }
            if (!currentOption.contains("token-custom-model-data")) {
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

    @Override
    public void onDisable() {
        if (gunListener != null) {
            gunListener.cleanup();
        }
        if (sosManager != null) {
            sosManager.cleanup();
        }
        if (dispatchGpsManager != null) {
            dispatchGpsManager.cleanup();
        }
        if (c4Manager != null) {
            c4Manager.cleanup();
        }
        if (lawRadioManager != null) {
            lawRadioManager.cleanup();
        }
        if (utilityItemListener != null) {
            utilityItemListener.cleanup();
        }
        if (weaponAnimationSuppressor != null) {
            weaponAnimationSuppressor.disablePacketHook();
        }
        Bukkit.getOnlinePlayers().forEach(JumpRestrictionManager::clearAll);
        for (Listener listener : listeners) {
            HandlerList.unregisterAll(listener);
        }
        listeners.clear();
        automaticSkinFireSuppression.clear();
    }

    public CorePlugin getCore() {
        return core;
    }

    public WeaponRegistry getWeaponRegistry() {
        return weaponRegistry;
    }

    public AmmoRegistry getAmmoRegistry() {
        return ammoRegistry;
    }

    public AttachmentRegistry getAttachmentRegistry() {
        return attachmentRegistry;
    }

    public AttachmentManager getAttachmentManager() {
        return attachmentManager;
    }

    public AttachmentAuditLogger getAttachmentAuditLogger() {
        return attachmentAuditLogger;
    }

    public AttachmentWorkbenchGUI getAttachmentWorkbenchGUI() {
        return attachmentWorkbenchGUI;
    }

    public WeaponCosmeticManager getWeaponCosmeticManager() {
        return weaponCosmeticManager;
    }

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

    public boolean isAutomaticSkinFireSuppressed(UUID playerId) {
        return playerId != null && automaticSkinFireSuppression.contains(playerId);
    }

    public WeaponCosmeticStationManager getWeaponCosmeticStationManager() {
        return weaponCosmeticStationManager;
    }

    public MagazineManager getMagazineManager() {
        return magazineManager;
    }

    public void registerCombatPolicy(WeaponCombatPolicy policy) {
        if (policy != null && !combatPolicies.contains(policy)) {
            combatPolicies.add(policy);
        }
    }

    public void unregisterCombatPolicy(WeaponCombatPolicy policy) {
        combatPolicies.remove(policy);
    }

    public WeaponCombatDecision evaluateWeaponUse(Player shooter, WeaponDefinition weapon, ItemStack weaponItem) {
        WeaponUseContext context = new WeaponUseContext(shooter, weapon, weaponItem);
        WeaponCombatDecision decision = WeaponCombatDecision.allow();
        for (WeaponCombatPolicy policy : combatPolicies) {
            decision = WeaponCombatDecision.merge(decision, callCanUse(policy, context));
            if (decision.isDenied()) {
                return decision;
            }
        }
        return decision;
    }

    public WeaponCombatDecision evaluateWeaponTarget(Player shooter, WeaponDefinition weapon, ItemStack weaponItem,
                                                     LivingEntity target) {
        WeaponTargetContext context = new WeaponTargetContext(shooter, weapon, weaponItem, target);
        WeaponCombatDecision decision = WeaponCombatDecision.allow();
        for (WeaponCombatPolicy policy : combatPolicies) {
            decision = WeaponCombatDecision.merge(decision, callCanTarget(policy, context));
            if (decision.isDenied()) {
                return decision;
            }
        }
        return decision;
    }

    public WeaponCombatDecision evaluateWeaponImpact(Player shooter, WeaponDefinition weapon, ItemStack weaponItem,
                                                     LivingEntity target, Location hitLocation, double damage,
                                                     double distance, boolean headshot, boolean lethal) {
        WeaponImpactContext context = new WeaponImpactContext(
                shooter, weapon, weaponItem, target, hitLocation, damage, distance, headshot, lethal);
        WeaponCombatDecision decision = WeaponCombatDecision.allow();
        for (WeaponCombatPolicy policy : combatPolicies) {
            decision = WeaponCombatDecision.merge(decision, callBeforeImpact(policy, context));
            if (decision.isDenied()) {
                return decision;
            }
        }
        return decision;
    }

    public void notifyWeaponShot(Player shooter, WeaponDefinition weapon, ItemStack weaponItem) {
        WeaponUseContext context = new WeaponUseContext(shooter, weapon, weaponItem);
        for (WeaponCombatPolicy policy : combatPolicies) {
            try {
                policy.onShot(context);
            } catch (Throwable error) {
                logPolicyError(policy, "onShot", error);
            }
        }
    }

    public void notifyWeaponHit(Player shooter, WeaponDefinition weapon, ItemStack weaponItem, LivingEntity target,
                                Location hitLocation, double damage, double distance, boolean headshot,
                                boolean lethal) {
        WeaponImpactContext context = new WeaponImpactContext(
                shooter, weapon, weaponItem, target, hitLocation, damage, distance, headshot, lethal);
        for (WeaponCombatPolicy policy : combatPolicies) {
            try {
                policy.onHit(context);
            } catch (Throwable error) {
                logPolicyError(policy, "onHit", error);
            }
        }
    }

    public WeaponDefinition getWeaponDefinition(String id) {
        return weaponRegistry == null ? null : weaponRegistry.getWeapon(id);
    }

    public WeaponDefinition getWeaponDefinition(ItemStack item) {
        return weaponRegistry == null ? null : weaponRegistry.getWeapon(item);
    }

    public List<WeaponDefinition> getWeaponDefinitions() {
        return weaponRegistry == null ? List.of() : weaponRegistry.getAll();
    }

    public ItemStack createWeaponItem(String weaponId) {
        return weaponRegistry == null ? null : weaponRegistry.createItemStack(weaponId);
    }

    public ItemStack createMagazineItem(String weaponId, int ammoCount) {
        WeaponDefinition weapon = getWeaponDefinition(weaponId);
        if (weapon == null || magazineManager == null) {
            return null;
        }
        return magazineManager.createMagazine(weapon, ammoCount);
    }

    public void refillWeapon(Player player, ItemStack weaponItem) {
        if (gunListener != null) {
            gunListener.refillWeapon(player, weaponItem);
        }
    }

    public void refillPlayerWeapons(Player player) {
        if (gunListener != null) {
            gunListener.refillPlayerWeapons(player);
        }
    }

    public void refreshWeaponVisual(ItemStack weaponItem) {
        WeaponDefinition weapon = getWeaponDefinition(weaponItem);
        if (weapon == null) {
            return;
        }
        weaponRegistry.refreshWeaponDisplayName(weaponItem, weapon);
        if (attachmentManager != null) {
            attachmentManager.refreshWeaponVisual(weaponItem, weapon);
        }
    }

    public boolean supportsWeaponCosmetics(String weaponId) {
        return weaponCosmeticManager != null && weaponCosmeticManager.supportsWeapon(weaponId);
    }

    public boolean supportsWeaponCosmeticType(String weaponId, String type) {
        return weaponCosmeticManager != null && weaponCosmeticManager.supportsWeaponCosmeticType(weaponId, type);
    }

    public List<String> getWeaponCosmeticSkinIds(String weaponId) {
        return weaponCosmeticManager == null ? List.of() : weaponCosmeticManager.getSkinIds(weaponId);
    }

    public List<String> getWeaponCosmeticLedIds() {
        return weaponCosmeticManager == null ? List.of() : weaponCosmeticManager.getLedIds();
    }

    public List<String> getWeaponCosmeticColorIds() {
        return weaponCosmeticManager == null ? List.of() : weaponCosmeticManager.getColorIds();
    }

    public String getWeaponCosmeticSkinDisplayName(String weaponId, String skinId) {
        if (weaponCosmeticManager == null) {
            return skinId;
        }
        WeaponCosmeticManager.SkinOption option = weaponCosmeticManager.getSkinOption(weaponId, skinId);
        return option == null ? skinId : option.displayName();
    }

    public String getWeaponCosmeticLedDisplayName(String ledId) {
        if (weaponCosmeticManager == null) {
            return ledId;
        }
        WeaponCosmeticManager.CosmeticOption option = weaponCosmeticManager.getLedOption(ledId);
        return option == null ? ledId : option.displayName();
    }

    public String getWeaponCosmeticColorDisplayName(String colorId) {
        if (weaponCosmeticManager == null) {
            return colorId;
        }
        WeaponCosmeticManager.CosmeticOption option = weaponCosmeticManager.getColorOption(colorId);
        return option == null ? colorId : option.displayName();
    }

    public String getWeaponCosmeticColorHex(String colorId) {
        if (weaponCosmeticManager == null) {
            return WeaponCosmeticManager.NONE;
        }
        WeaponCosmeticManager.CosmeticOption option = weaponCosmeticManager.getColorOption(colorId);
        return option == null || option.rgb() == null
                ? WeaponCosmeticManager.NONE
                : WeaponCosmeticManager.formatHex(option.rgb());
    }

    public ItemStack createWeaponCosmeticToken(String type, String id, int amount) {
        return weaponCosmeticManager == null ? null : weaponCosmeticManager.createToken(type, id, amount);
    }

    public boolean applyWeaponCosmeticSelection(ItemStack weaponItem, String skinId, String ledId, String color) {
        if (weaponCosmeticManager == null) {
            return false;
        }
        Integer rgb = WeaponCosmeticManager.normalize(color).equals(WeaponCosmeticManager.NONE)
                ? null
                : WeaponCosmeticManager.parseColorRgb(color);
        if (color != null && !color.isBlank() && rgb == null
                && !WeaponCosmeticManager.normalize(color).equals(WeaponCosmeticManager.NONE)) {
            return false;
        }
        boolean applied = weaponCosmeticManager.applySelection(weaponItem, skinId, ledId, rgb);
        if (applied) {
            refreshWeaponVisual(weaponItem);
        }
        return applied;
    }

    public void openWeaponCosmeticWorkbench(Player player) {
        if (player != null && weaponCosmeticWorkbenchGUI != null) {
            weaponCosmeticWorkbenchGUI.open(player);
        }
    }

    public void openWeaponCosmeticEditor(Player player) {
        if (player != null && weaponCosmeticEditorGUI != null) {
            weaponCosmeticEditorGUI.open(player);
        }
    }

    public BalaclavaManager getBalaclavaManager() {
        return balaclavaManager;
    }

    public ArmorManager getArmorManager() {
        return armorManager;
    }

    public HelmetManager getHelmetManager() {
        return helmetManager;
    }

    public ShieldManager getShieldManager() {
        return shieldManager;
    }

    public UtilityItemManager getUtilityItemManager() {
        return utilityItemManager;
    }

    public UtilitySettings getUtilitySettings() {
        return utilitySettings == null ? UtilitySettings.defaults() : utilitySettings;
    }

    public WeaponAnimationSuppressor getWeaponAnimationSuppressor() {
        return weaponAnimationSuppressor;
    }

    public GrenadeManager getGrenadeManager() {
        return grenadeManager;
    }

    public C4Manager getC4Manager() {
        return c4Manager;
    }

    public HandcuffManager getHandcuffManager() {
        return handcuffManager;
    }

    public CombatStunManager getCombatStunManager() {
        return combatStunManager;
    }

    public ArrestManager getArrestManager() {
        return arrestManager;
    }

    public ArrestGUI getArrestGUI() {
        return arrestGUI;
    }

    public RobberyManager getRobberyManager() {
        return robberyManager;
    }

    public MobilePhoneManager getMobilePhoneManager() {
        return mobilePhoneManager;
    }

    public PhoneGUI getPhoneGUI() {
        return phoneGUI;
    }

    public SosGUI getSosGUI() {
        return sosGUI;
    }

    public SosManager getSosManager() {
        return sosManager;
    }

    public DispatchGpsManager getDispatchGpsManager() {
        return dispatchGpsManager;
    }

    public LawRadioManager getLawRadioManager() {
        return lawRadioManager;
    }

    public WantedManager getWantedManager() {
        return wantedManager;
    }


    public String message(String path, String fallback) {
        if (messagesConfig == null) return fallback;
        return messagesConfig.getString(path, fallback);
    }

    public WantedGUI getWantedGUI() {
        return wantedGUI;
    }

    public boolean isLEO(UUID uuid) {
        PoliceModule police = core.getModuleManager().getModule(PoliceModule.class);
        if (police != null && police.getService() != null && police.getService().isLawAuthority(uuid)) {
            return true;
        }
        return isCompanyEmployeeOfType(uuid, "LAW_ENFORCEMENT");
    }

    public boolean isCompanyEmployeeOfType(UUID uuid, String... companyTypes) {
        AziendaModule azienda = core.getModuleManager().getModule(AziendaModule.class);
        if (azienda == null) return false;

        Set<String> normalizedTypes = normalizeTypes(companyTypes);
        try {
            for (Company company : azienda.getCompanyDAO().getAllCompanies().get()) {
                String companyType = company.getType();
                if (companyType != null
                        && normalizedTypes.contains(companyType.toUpperCase(Locale.ROOT))
                        && company.hasEmployee(uuid)) {
                    return true;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public List<org.bukkit.entity.Player> getOnlineCompanyEmployees(String... companyTypes) {
        List<org.bukkit.entity.Player> players = new ArrayList<>();
        AziendaModule azienda = core.getModuleManager().getModule(AziendaModule.class);
        if (azienda == null) return players;

        Set<String> normalizedTypes = normalizeTypes(companyTypes);
        Set<UUID> employeeUuids = new HashSet<>();
        try {
            for (Company company : azienda.getCompanyDAO().getAllCompanies().get()) {
                String companyType = company.getType();
                if (companyType == null || !normalizedTypes.contains(companyType.toUpperCase(Locale.ROOT))) {
                    continue;
                }
                company.getEmployees().forEach(employee -> employeeUuids.add(employee.getPlayerUuid()));
            }
        } catch (Exception e) {
            e.printStackTrace();
            return players;
        }

        for (org.bukkit.entity.Player player : org.bukkit.Bukkit.getOnlinePlayers()) {
            if (employeeUuids.contains(player.getUniqueId())) {
                players.add(player);
            }
        }
        return players;
    }

    private Set<String> normalizeTypes(String... companyTypes) {
        Set<String> normalized = new HashSet<>();
        Arrays.stream(companyTypes)
                .filter(type -> type != null && !type.isBlank())
                .map(type -> type.toUpperCase(Locale.ROOT))
                .forEach(normalized::add);
        return normalized;
    }

    private void syncOnlineJumpRestrictions() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (handcuffManager != null && handcuffManager.isRestrained(player)) {
                handcuffManager.applyEffects(player);
                handcuffManager.ensureBoundRestraintItem(player);
            } else {
                JumpRestrictionManager.repairStale(player);
            }

            if (shieldManager == null) {
                continue;
            }

            ItemStack mainHand = player.getInventory().getItemInMainHand();
            ItemStack offHand = player.getInventory().getItemInOffHand();
            if (shieldManager.isShield(mainHand) || shieldManager.isShield(offHand)) {
                JumpRestrictionManager.restrict(player, JumpRestrictionManager.REASON_SHIELD);
            } else {
                JumpRestrictionManager.release(player, JumpRestrictionManager.REASON_SHIELD);
            }
        }
    }

    private WeaponCombatDecision callCanUse(WeaponCombatPolicy policy, WeaponUseContext context) {
        try {
            return normalizeDecision(policy.canUse(context));
        } catch (Throwable error) {
            logPolicyError(policy, "canUse", error);
            return WeaponCombatDecision.allow();
        }
    }

    private WeaponCombatDecision callCanTarget(WeaponCombatPolicy policy, WeaponTargetContext context) {
        try {
            return normalizeDecision(policy.canTarget(context));
        } catch (Throwable error) {
            logPolicyError(policy, "canTarget", error);
            return WeaponCombatDecision.allow();
        }
    }

    private WeaponCombatDecision callBeforeImpact(WeaponCombatPolicy policy, WeaponImpactContext context) {
        try {
            return normalizeDecision(policy.beforeImpact(context));
        } catch (Throwable error) {
            logPolicyError(policy, "beforeImpact", error);
            return WeaponCombatDecision.allow();
        }
    }

    private WeaponCombatDecision normalizeDecision(WeaponCombatDecision decision) {
        return decision == null ? WeaponCombatDecision.allow() : decision;
    }

    private void logPolicyError(WeaponCombatPolicy policy, String phase, Throwable error) {
        if (core != null) {
            core.getLogger().warning("[OpenWeapons] Combat policy " + policy.getClass().getName()
                    + " failed during " + phase + ": " + error.getMessage());
        }
    }

    private void registerListener(Listener listener) {
        listeners.add(listener);
        core.getServer().getPluginManager().registerEvents(listener, core);
    }
}
