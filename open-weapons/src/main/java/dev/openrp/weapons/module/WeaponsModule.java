package dev.openrp.weapons.module;

import org.bukkit.plugin.java.JavaPlugin;
import dev.openrp.weapons.bridge.OpenBankBridge;
import dev.openrp.weapons.bridge.OpenCompanyBridge;
import dev.openrp.weapons.bridge.OpenCoreBridge;
import dev.openrp.weapons.bridge.OpenHospitalBridge;
import dev.openrp.weapons.bridge.OpenIdentityBridge;
import dev.openrp.weapons.bridge.OpenLootboxBridge;
import dev.openrp.weapons.bridge.staff.OpenStaffLogBridge;
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
import dev.openrp.weapons.balaclava.BalaclavaListener;
import dev.openrp.weapons.balaclava.BalaclavaManager;
import dev.openrp.weapons.commands.UncuffCommand;
import dev.openrp.weapons.commands.DamageDummyListener;
import dev.openrp.weapons.commands.ItemsCommand;
import dev.openrp.weapons.commands.WeaponsCommand;
import dev.openrp.weapons.config.WeaponConfigCommand;
import dev.openrp.weapons.config.WeaponConfigEditor;
import dev.openrp.weapons.config.WeaponConfigGUI;
import dev.openrp.weapons.c4.C4Manager;
import dev.openrp.weapons.dispatch.DispatchGpsManager;
import dev.openrp.weapons.grenades.GrenadeListener;
import dev.openrp.weapons.grenades.GrenadeManager;
import dev.openrp.weapons.gui.ItemsGUI;
import dev.openrp.weapons.gui.WeaponsGUI;
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
import dev.openrp.weapons.registry.AmmoRegistry;
import dev.openrp.weapons.registry.WeaponRegistry;
import dev.openrp.weapons.shield.ShieldListener;
import dev.openrp.weapons.shield.ShieldManager;
import dev.openrp.weapons.taser.TaserListener;
import dev.openrp.weapons.util.JumpRestrictionManager;
import dev.openrp.weapons.utility.UtilityItemListener;
import dev.openrp.weapons.utility.UtilityItemManager;
import dev.openrp.weapons.utility.UtilitySettings;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.ServicePriority;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

public class WeaponsModule {
    private static final String OPEN_COSMETICS_API_CLASS = "dev.openrp.cosmetics.api.OpenCosmeticsApi";
    private static final String OPEN_COSMETICS_WEAPON_BRIDGE_CLASS = "dev.openrp.cosmetics.api.OpenCosmeticsWeaponBridge";
    public static final String COSMETIC_SOUND_FIRE = "fire";
    public static final String COSMETIC_SOUND_HIT = "hit";
    public static final String COSMETIC_SOUND_HEADSHOT = "headshot";
    public static final String COSMETIC_SOUND_RELOAD = "reload";
    public static final String COSMETIC_SOUND_AUTOMATIC = "automatic";
    public static final String COSMETIC_NONE = "none";

    private final JavaPlugin core;
    private final OpenStaffLogBridge staffLogBridge;
    private final OpenIdentityBridge identityBridge = new OpenIdentityBridge();
    private final OpenCompanyBridge companyBridge = new OpenCompanyBridge();
    private final OpenHospitalBridge hospitalBridge = new OpenHospitalBridge();
    private final OpenLootboxBridge lootboxBridge = new OpenLootboxBridge();
    private final OpenBankBridge bankBridge = new OpenBankBridge();
    private OpenCoreBridge openCore;
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
    private RobberyManager robberyManager;
    private GunListener gunListener;
    private DispatchGpsManager dispatchGpsManager;
    private ShieldManager shieldManager;
    private UtilityItemManager utilityItemManager;
    private UtilityItemListener utilityItemListener;
    private UtilitySettings utilitySettings;
    private WeaponAnimationSuppressor weaponAnimationSuppressor;
    private Object cosmeticsBridge;
    private Class<?> cosmeticsBridgeServiceClass;
    private WeaponConfigEditor weaponConfigEditor;
    private WeaponConfigGUI weaponConfigGUI;
    private YamlConfiguration messagesConfig;
    private final List<Listener> listeners = new ArrayList<>();
    private final List<WeaponCombatPolicy> combatPolicies = new CopyOnWriteArrayList<>();

    public WeaponsModule(JavaPlugin core) {
        this.core = java.util.Objects.requireNonNull(core, "core");
        this.staffLogBridge = new OpenStaffLogBridge(core);
    }

    public String id() {
        return "weapons";
    }

    public String getName() {
        return id();
    }

    public void onEnable(OpenCoreBridge openCore) {
        this.openCore = openCore == null ? OpenCoreBridge.unavailable(core.getLogger()) : openCore;
        JavaPlugin core = this.core;
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
        File messagesFile = new File(core.getDataFolder(), "messages_it.yml");
        if (!messagesFile.exists()) {
            core.saveResource("messages_it.yml", false);
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
        registerCosmeticsBridge();
        this.weaponRegistry.setDisplayNameDecorator((item, weapon, baseName) -> {
            return decorateWeaponDisplayName(item, baseName);
        });

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

        // Initialize utility GPS used by trackers.
        this.dispatchGpsManager = new DispatchGpsManager(core);

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
        this.utilityItemListener = new UtilityItemListener(this);
        registerListener(utilityItemListener);
        
        this.robberyManager = new RobberyManager(this);
        registerListener(new RobListener(this));
        registerListener(new FriskListener(this));

        // GUI and Commands — Categorized Items GUI (replaces old Weapons GUI)
        this.attachmentWorkbenchGUI = new AttachmentWorkbenchGUI(this);
        registerListener(attachmentWorkbenchGUI);

        ItemsGUI itemsGui = new ItemsGUI(this);
        registerListener(itemsGui);
        this.weaponConfigEditor = new WeaponConfigEditor(this);
        this.weaponConfigGUI = new WeaponConfigGUI(this, weaponConfigEditor);
        registerListener(weaponConfigGUI);

        DamageDummyListener damageDummyListener = new DamageDummyListener(core);
        registerListener(damageDummyListener);
        ItemsCommand itemsCommand = new ItemsCommand(this, itemsGui, damageDummyListener);
        core.getCommand("oggetti").setExecutor(itemsCommand);
        core.getCommand("oggetti").setTabCompleter(itemsCommand);
        // /armi is registered as an alias of /oggetti in plugin.yml

        if (core.getCommand("configarmi") != null) {
            WeaponConfigCommand weaponConfigCommand = new WeaponConfigCommand(this, weaponConfigEditor, weaponConfigGUI);
            core.getCommand("configarmi").setExecutor(weaponConfigCommand);
            core.getCommand("configarmi").setTabCompleter(weaponConfigCommand);
        } else {
            core.getLogger().warning("[OpenWeapons] /configarmi manca in plugin.yml.");
        }

        if (core.getCommand("bancoarmi") != null) {
            core.getCommand("bancoarmi").setExecutor(new AttachmentWorkbenchCommand(this));
        } else {
            core.getLogger().warning("[OpenWeapons] /bancoarmi manca in plugin.yml.");
        }

        UncuffCommand uncuffCommand = new UncuffCommand(this);
        core.getCommand("libera").setExecutor(uncuffCommand);
        registerListener(uncuffCommand);
        
        core.getCommand("rapina").setExecutor(new RobCommand(this));
        core.getCommand("perquisisci").setExecutor(new FriskCommand(this));

        syncOnlineJumpRestrictions();

        core.getLogger().info("[OpenWeapons] Modulo abilitato correttamente.");
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
                core.getLogger().info("[OpenWeapons] Aggiunti i model data visuali mancanti delle armi in weapons.yml.");
            }
        } catch (Exception e) {
            core.getLogger().warning("[OpenWeapons] Impossibile completare il backfill dei model data visuali delle armi: " + e.getMessage());
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
            core.getLogger().warning("[OpenWeapons] Impossibile completare il backfill dei model data visuali delle granate: " + e.getMessage());
        }
    }

    private boolean usesLegacyGrenadeVisual(ConfigurationSection section) {
        String material = section.getString("material", "");
        int customModelData = section.getInt("custom-model-data", 0);
        return material.equalsIgnoreCase("FIREWORK_STAR")
                && customModelData >= 271
                && customModelData <= 275;
    }

    private void registerCosmeticsBridge() {
        unregisterCosmeticsBridge();
        Class<?> bridgeClass = loadOptionalClass(OPEN_COSMETICS_WEAPON_BRIDGE_CLASS);
        if (bridgeClass == null) {
            core.getLogger().info("[OpenWeapons] OpenCosmetics non trovato: cosmetici arma disabilitati.");
            return;
        }

        this.cosmeticsBridgeServiceClass = bridgeClass;
        this.cosmeticsBridge = Proxy.newProxyInstance(
                bridgeClass.getClassLoader(),
                new Class<?>[]{bridgeClass},
                (proxy, method, args) -> switch (method.getName()) {
                    case "getWeaponId" -> {
                        ItemStack item = args != null && args.length > 0 && args[0] instanceof ItemStack stack ? stack : null;
                        WeaponDefinition weapon = getWeaponDefinition(item);
                        yield weapon == null ? null : weapon.getId();
                    }
                    case "isWeapon" -> {
                        ItemStack item = args != null && args.length > 0 && args[0] instanceof ItemStack stack ? stack : null;
                        yield getWeaponDefinition(item) != null;
                    }
                    case "refreshWeaponVisual" -> {
                        if (args != null && args.length > 0 && args[0] instanceof ItemStack item) {
                            WeaponsModule.this.refreshWeaponVisual(item);
                        }
                        yield null;
                    }
                    case "toString" -> "OpenWeaponsCosmeticsBridge";
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "equals" -> proxy == (args == null || args.length == 0 ? null : args[0]);
                    default -> null;
                });

        registerService(bridgeClass, cosmeticsBridge);
        core.getLogger().info("[OpenWeapons] Bridge OpenCosmetics registrato.");
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void registerService(Class<?> serviceClass, Object provider) {
        Bukkit.getServicesManager().register((Class) serviceClass, provider, core, ServicePriority.Normal);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void unregisterCosmeticsBridge() {
        if (cosmeticsBridge != null && cosmeticsBridgeServiceClass != null) {
            Bukkit.getServicesManager().unregister((Class) cosmeticsBridgeServiceClass, cosmeticsBridge);
        }
        cosmeticsBridge = null;
        cosmeticsBridgeServiceClass = null;
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

    public void onDisable() {
        if (gunListener != null) {
            gunListener.cleanup();
        }
        if (dispatchGpsManager != null) {
            dispatchGpsManager.cleanup();
        }
        if (c4Manager != null) {
            c4Manager.cleanup();
        }
        if (utilityItemListener != null) {
            utilityItemListener.cleanup();
        }
        if (weaponAnimationSuppressor != null) {
            weaponAnimationSuppressor.disablePacketHook();
        }
        unregisterCosmeticsBridge();
        Bukkit.getOnlinePlayers().forEach(JumpRestrictionManager::clearAll);
        for (Listener listener : listeners) {
            HandlerList.unregisterAll(listener);
        }
        listeners.clear();
        openCore = OpenCoreBridge.unavailable(core.getLogger());
    }

    public JavaPlugin getCore() {
        return core;
    }

    public OpenCoreBridge getOpenCoreBridge() {
        return openCore == null ? OpenCoreBridge.unavailable(core.getLogger()) : openCore;
    }

    public OpenStaffLogBridge getStaffLogBridge() {
        return staffLogBridge;
    }

    public OpenIdentityBridge getIdentityBridge() {
        return identityBridge;
    }

    public OpenCompanyBridge getCompanyBridge() {
        return companyBridge;
    }

    public OpenHospitalBridge getHospitalBridge() {
        return hospitalBridge;
    }

    public OpenLootboxBridge getLootboxBridge() {
        return lootboxBridge;
    }

    public OpenBankBridge getBankBridge() {
        return bankBridge;
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

    public void setAutomaticSkinFireSuppressed(UUID playerId, boolean suppressed) {
        invokeOpenCosmetics("setAutomaticSkinFireSuppressed",
                new Class<?>[]{UUID.class, boolean.class}, playerId, suppressed);
    }

    public boolean isAutomaticSkinFireSuppressed(UUID playerId) {
        Object result = invokeOpenCosmetics("isAutomaticSkinFireSuppressed",
                new Class<?>[]{UUID.class}, playerId);
        return result instanceof Boolean value && value;
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

    public boolean hasOpenCosmeticsApi() {
        return getOpenCosmeticsApiProvider() != null;
    }

    public void reloadOpenCosmetics() {
        invokeOpenCosmetics("reload", new Class<?>[0]);
    }

    public String getWeaponSkinSound(ItemStack weaponItem, String soundKey) {
        Object result = invokeOpenCosmetics("getWeaponSkinSound",
                new Class<?>[]{ItemStack.class, String.class}, weaponItem, soundKey);
        return result instanceof String value && !value.isBlank() ? value : null;
    }

    public Component decorateWeaponDisplayName(ItemStack weaponItem, Component baseName) {
        Object result = invokeOpenCosmetics("decorateWeaponDisplayName",
                new Class<?>[]{ItemStack.class, Component.class}, weaponItem, baseName);
        return result instanceof Component component ? component : baseName;
    }

    public List<String> visualVariantCandidates(ItemStack weaponItem, boolean optic, boolean hasMagazine, boolean grip) {
        Object result = invokeOpenCosmetics("visualVariantCandidates",
                new Class<?>[]{ItemStack.class, boolean.class, boolean.class, boolean.class},
                weaponItem, optic, hasMagazine, grip);
        return stringList(result);
    }

    public Integer getWeaponColorRgb(ItemStack weaponItem) {
        Object result = invokeOpenCosmetics("getWeaponColorRgb",
                new Class<?>[]{ItemStack.class}, weaponItem);
        return result instanceof Integer value ? value : null;
    }

    public boolean applyCosmeticVisualData(ItemStack item, ItemMeta meta, int customModelData) {
        Object cosmetics = getOpenCosmeticsApiProvider();
        if (cosmetics == null) {
            return false;
        }
        Integer rgb = getWeaponColorRgb(item);
        if (!invokeOpenCosmetics(cosmetics, "applyVisualCustomModelData",
                new Class<?>[]{ItemMeta.class, int.class, Integer.class}, meta, customModelData, rgb)) {
            return false;
        }
        item.setItemMeta(meta);
        invokeOpenCosmetics(cosmetics, "applyVisualDataComponents",
                new Class<?>[]{ItemStack.class, int.class, Integer.class}, item, customModelData, rgb);
        return true;
    }

    public boolean supportsWeaponCosmetics(String weaponId) {
        Object result = invokeOpenCosmetics("supportsWeapon", new Class<?>[]{String.class}, weaponId);
        return result instanceof Boolean value && value;
    }

    public boolean supportsWeaponCosmeticType(String weaponId, String type) {
        Object result = invokeOpenCosmetics("supportsWeaponCosmeticType",
                new Class<?>[]{String.class, String.class}, weaponId, type);
        return result instanceof Boolean value && value;
    }

    public List<String> getWeaponCosmeticSkinIds(String weaponId) {
        return stringList(invokeOpenCosmetics("getSkinIds", new Class<?>[]{String.class}, weaponId));
    }

    public List<String> getWeaponCosmeticLedIds() {
        return stringList(invokeOpenCosmetics("getLedIds", new Class<?>[0]));
    }

    public List<String> getWeaponCosmeticColorIds() {
        return stringList(invokeOpenCosmetics("getColorIds", new Class<?>[0]));
    }

    public String getWeaponCosmeticSkinDisplayName(String weaponId, String skinId) {
        Object result = invokeOpenCosmetics("getSkinDisplayName",
                new Class<?>[]{String.class, String.class}, weaponId, skinId);
        return result instanceof String value ? value : skinId;
    }

    public String getWeaponCosmeticLedDisplayName(String ledId) {
        Object result = invokeOpenCosmetics("getLedDisplayName", new Class<?>[]{String.class}, ledId);
        return result instanceof String value ? value : ledId;
    }

    public String getWeaponCosmeticColorDisplayName(String colorId) {
        Object result = invokeOpenCosmetics("getColorDisplayName", new Class<?>[]{String.class}, colorId);
        return result instanceof String value ? value : colorId;
    }

    public String getWeaponCosmeticColorHex(String colorId) {
        Object result = invokeOpenCosmetics("getColorHex", new Class<?>[]{String.class}, colorId);
        return result instanceof String value ? value : COSMETIC_NONE;
    }

    public ItemStack createWeaponCosmeticToken(String type, String id, int amount) {
        Object result = invokeOpenCosmetics("createToken",
                new Class<?>[]{String.class, String.class, int.class}, type, id, amount);
        return result instanceof ItemStack item ? item : null;
    }

    public boolean applyWeaponCosmeticSelection(ItemStack weaponItem, String skinId, String ledId, String color) {
        Object result = invokeOpenCosmetics("applySelection",
                new Class<?>[]{ItemStack.class, String.class, String.class, String.class},
                weaponItem, skinId, ledId, color);
        return result instanceof Boolean value && value;
    }

    public void openWeaponCosmeticWorkbench(Player player) {
        if (player != null) {
            invokeOpenCosmetics("openWorkbench", new Class<?>[]{Player.class}, player);
        }
    }

    public void openWeaponCosmeticEditor(Player player) {
        if (player != null) {
            invokeOpenCosmetics("openEditor", new Class<?>[]{Player.class}, player);
        }
    }

    private Object getOpenCosmeticsApiProvider() {
        Class<?> apiClass = loadOptionalClass(OPEN_COSMETICS_API_CLASS);
        if (apiClass == null) {
            return null;
        }
        try {
            return getServiceProvider(apiClass);
        } catch (RuntimeException error) {
            return null;
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private Object getServiceProvider(Class<?> serviceClass) {
        var registration = Bukkit.getServicesManager().getRegistration((Class) serviceClass);
        return registration == null ? null : registration.getProvider();
    }

    private Object invokeOpenCosmetics(String methodName, Class<?>[] parameterTypes, Object... args) {
        Object cosmetics = getOpenCosmeticsApiProvider();
        if (cosmetics == null) {
            return null;
        }
        try {
            return cosmetics.getClass().getMethod(methodName, parameterTypes).invoke(cosmetics, args);
        } catch (ReflectiveOperationException | RuntimeException | LinkageError error) {
            return null;
        }
    }

    private boolean invokeOpenCosmetics(Object cosmetics, String methodName, Class<?>[] parameterTypes, Object... args) {
        if (cosmetics == null) {
            return false;
        }
        try {
            cosmetics.getClass().getMethod(methodName, parameterTypes).invoke(cosmetics, args);
            return true;
        } catch (ReflectiveOperationException | RuntimeException | LinkageError error) {
            return false;
        }
    }

    private Class<?> loadOptionalClass(String className) {
        try {
            return Class.forName(className, false, getClass().getClassLoader());
        } catch (ClassNotFoundException | LinkageError ignored) {
            var plugin = Bukkit.getPluginManager().getPlugin("OpenCosmetics");
            if (plugin == null) {
                return null;
            }
            try {
                return Class.forName(className, false, plugin.getClass().getClassLoader());
            } catch (ClassNotFoundException | LinkageError ignoredAgain) {
                return null;
            }
        }
    }

    private List<String> stringList(Object result) {
        if (!(result instanceof List<?> list)) {
            return List.of();
        }
        return list.stream()
                .filter(String.class::isInstance)
                .map(String.class::cast)
                .toList();
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

    public RobberyManager getRobberyManager() {
        return robberyManager;
    }

    public DispatchGpsManager getDispatchGpsManager() {
        return dispatchGpsManager;
    }


    public String message(String path, String fallback) {
        if (messagesConfig == null) return fallback;
        return messagesConfig.getString(path, fallback);
    }

    public boolean isCompanyEmployeeOfType(UUID uuid, String... companyTypes) {
        return companyBridge.isCompanyEmployeeOfType(uuid, companyTypes);
    }

    public List<org.bukkit.entity.Player> getOnlineCompanyEmployees(String... companyTypes) {
        return companyBridge.onlineCompanyEmployees(companyTypes);
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
            core.getLogger().warning("[OpenWeapons] Policy combattimento " + policy.getClass().getName()
                    + " fallita durante " + phase + ": " + error.getMessage());
        }
    }

    private void registerListener(Listener listener) {
        listeners.add(listener);
        core.getServer().getPluginManager().registerEvents(listener, core);
    }
}
