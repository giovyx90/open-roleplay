package dev.openrp.weapons.config;

import dev.openrp.weapons.model.FireMode;
import dev.openrp.weapons.model.WeaponCategory;
import dev.openrp.weapons.model.WeaponVisualState;
import dev.openrp.weapons.module.WeaponsModule;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

public class WeaponConfigEditor {
    private static final List<FieldSpec> BASE_FIELDS = List.of(
            new FieldSpec("display-name", FieldType.STRING, "Nome visualizzato"),
            new FieldSpec("category", FieldType.WEAPON_CATEGORY, "Categoria"),
            new FieldSpec("material", FieldType.MATERIAL, "Materiale"),
            new FieldSpec("custom-model-data", FieldType.INT, "Custom model data"),
            new FieldSpec("magazine-visual-offset", FieldType.INT, "Offset visuale caricatore"),
            new FieldSpec("magazine-model-data", FieldType.INT, "Model data caricatore"),
            new FieldSpec("damage", FieldType.DOUBLE, "Danno"),
            new FieldSpec("headshot-multiplier", FieldType.DOUBLE, "Moltiplicatore headshot"),
            new FieldSpec("fire-rate-ticks", FieldType.INT, "Tick cadenza fuoco"),
            new FieldSpec("reload-time-ticks", FieldType.INT, "Tick ricarica"),
            new FieldSpec("magazine-size", FieldType.INT, "Capacita' caricatore"),
            new FieldSpec("max-distance", FieldType.DOUBLE, "Distanza massima"),
            new FieldSpec("ammo-type", FieldType.STRING, "Tipo munizioni"),
            new FieldSpec("sound-shoot", FieldType.STRING, "Suono sparo"),
            new FieldSpec("sound-reload", FieldType.STRING, "Suono ricarica"),
            new FieldSpec("automatic", FieldType.BOOLEAN, "Automatico"),
            new FieldSpec("fire-modes", FieldType.FIRE_MODE_LIST, "Modalita' fuoco"),
            new FieldSpec("scope-zoom-level", FieldType.NULLABLE_INT, "Zoom ottica"),
            new FieldSpec("recoil", FieldType.DOUBLE, "Rinculo"),
            new FieldSpec("pellet-count", FieldType.INT, "Numero pallini"),
            new FieldSpec("hipfire-spread-deg", FieldType.DOUBLE, "Dispersione senza mira"),
            new FieldSpec("ads-spread-deg", FieldType.DOUBLE, "Dispersione in mira"),
            new FieldSpec("moving-spread-multiplier", FieldType.DOUBLE, "Dispersione in movimento"),
            new FieldSpec("sneak-spread-multiplier", FieldType.DOUBLE, "Dispersione accovacciato"),
            new FieldSpec("jump-spread-multiplier", FieldType.DOUBLE, "Dispersione in salto"),
            new FieldSpec("falloff-start-distance", FieldType.DOUBLE, "Inizio calo danno"),
            new FieldSpec("falloff-end-distance", FieldType.DOUBLE, "Fine calo danno"),
            new FieldSpec("falloff-min-multiplier", FieldType.DOUBLE, "Moltiplicatore minimo calo"),
            new FieldSpec("attack-speed", FieldType.DOUBLE, "Velocita' attacco"),
            new FieldSpec("knockback", FieldType.DOUBLE, "Contraccolpo"),
            new FieldSpec("sound-hit", FieldType.STRING, "Suono colpo")
    );
    private static final List<FieldSpec> ARMOR_FIELDS = List.of(
            new FieldSpec("display-name", FieldType.STRING, "Nome visualizzato"),
            new FieldSpec("custom-model-data", FieldType.INT, "Custom model data"),
            new FieldSpec("color-rgb", FieldType.STRING, "Colore armatura in pelle, es. #282828"),
            new FieldSpec("slowness-level", FieldType.INT, "Amplificatore lentezza, -1 lo disabilita"),
            new FieldSpec("damage-reduction", FieldType.DOUBLE, "Riduzione danno base, 0.45 = 45%"),
            new FieldSpec("nij-level", FieldType.STRING, "Livello NIJ visualizzato"),
            new FieldSpec("max-durability", FieldType.INT, "Durabilita' massima giubbotto"),
            new FieldSpec("has-plate", FieldType.BOOLEAN, "Indica se il giubbotto ha una piastra ceramica")
    );
    private static final List<FieldSpec> HELMET_FIELDS = List.of(
            new FieldSpec("display-name", FieldType.STRING, "Nome visualizzato"),
            new FieldSpec("custom-model-data", FieldType.INT, "Custom model data"),
            new FieldSpec("color-rgb", FieldType.STRING, "Colore casco in pelle, es. #323C32"),
            new FieldSpec("damage-reduction", FieldType.DOUBLE, "Riduzione danno proiettili, 0.25 = 25%"),
            new FieldSpec("negates-headshot", FieldType.BOOLEAN, "Indica se il bonus headshot viene annullato"),
            new FieldSpec("prevents-melee-stun", FieldType.BOOLEAN, "Indica se lo stun melee viene bloccato"),
            new FieldSpec("max-durability", FieldType.INT, "Durabilita' massima casco, 0 la disabilita")
    );

    private final WeaponsModule module;
    private final File weaponsFile;
    private final File armorFile;

    public WeaponConfigEditor(WeaponsModule module) {
        this.module = module;
        this.weaponsFile = new File(module.getCore().getDataFolder(), "weapons.yml");
        this.armorFile = new File(module.getCore().getDataFolder(), "armor.yml");
    }

    public List<String> weaponIds() {
        YamlConfiguration config = loadConfig();
        return config.getKeys(false).stream()
                .filter(key -> config.isConfigurationSection(key) && config.getConfigurationSection(key).contains("category"))
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();
    }

    public List<FieldSpec> fieldsFor(String weaponId) {
        YamlConfiguration config = loadConfig();
        ConfigurationSection section = config.getConfigurationSection(weaponId);
        List<FieldSpec> fields = new ArrayList<>(BASE_FIELDS);
        if (section != null) {
            ConfigurationSection visualStates = section.getConfigurationSection("visual-states");
            if (visualStates != null) {
                visualStates.getKeys(false).forEach(key -> fields.add(
                        new FieldSpec("visual-states." + key, FieldType.INT, "Visual " + key)));
            }
            ConfigurationSection visualVariants = section.getConfigurationSection("visual-variants");
            if (visualVariants != null) {
                for (String state : visualVariants.getKeys(false)) {
                    ConfigurationSection variants = visualVariants.getConfigurationSection(state);
                    if (variants == null) {
                        continue;
                    }
                    variants.getKeys(false).forEach(variant -> fields.add(
                            new FieldSpec("visual-variants." + state + "." + variant, FieldType.INT,
                                    "Variant " + state + "/" + variant)));
                }
            }
        }
        fields.sort(Comparator.comparing(FieldSpec::path));
        return fields;
    }

    public EditResult get(String weaponId, String path) {
        YamlConfiguration config = loadConfig();
        ConfigurationSection section = config.getConfigurationSection(weaponId);
        if (section == null) {
            return EditResult.error("ID arma sconosciuto: " + weaponId);
        }
        if (!isAllowedPath(path)) {
            return EditResult.error("Percorso non modificabile in weapons.yml.");
        }
        Object value = section.get(path);
        return EditResult.success(value == null ? "<unset>" : String.valueOf(value));
    }

    public EditResult set(String actorName, String weaponId, String path, String rawValue) {
        if (!isAllowedPath(path)) {
            return EditResult.error("Percorso non modificabile in weapons.yml.");
        }
        YamlConfiguration config = loadConfig();
        ConfigurationSection section = config.getConfigurationSection(weaponId);
        if (section == null) {
            return EditResult.error("ID arma sconosciuto: " + weaponId);
        }

        FieldSpec spec = specFor(path);
        Object parsed;
        try {
            parsed = parseValue(spec.type(), rawValue);
        } catch (IllegalArgumentException ex) {
            return EditResult.error(ex.getMessage());
        }

        Object oldValue = section.get(path);
        section.set(path, parsed);
        try {
            config.save(weaponsFile);
        } catch (IOException ex) {
            return EditResult.error("Impossibile salvare weapons.yml: " + ex.getMessage());
        }
        reload();
        audit(actorName, weaponId, path, oldValue, parsed);
        return EditResult.success("Imposta " + weaponId + "." + path + " = " + parsed);
    }

    public EditResult remove(String actorName, String weaponId, String path) {
        if (!isAllowedPath(path)) {
            return EditResult.error("Percorso non modificabile in weapons.yml.");
        }
        YamlConfiguration config = loadConfig();
        ConfigurationSection section = config.getConfigurationSection(weaponId);
        if (section == null) {
            return EditResult.error("ID arma sconosciuto: " + weaponId);
        }
        if (!section.contains(path)) {
            return EditResult.error("Il percorso e' gia' non impostato.");
        }
        Object oldValue = section.get(path);
        section.set(path, null);
        try {
            config.save(weaponsFile);
        } catch (IOException ex) {
            return EditResult.error("Impossibile salvare weapons.yml: " + ex.getMessage());
        }
        reload();
        audit(actorName, weaponId, path, oldValue, null);
        return EditResult.success("Rimosso " + weaponId + "." + path);
    }

    public void reload() {
        module.getWeaponRegistry().load(weaponsFile);
    }

    public List<String> armorIds() {
        YamlConfiguration config = loadArmorConfig();
        ConfigurationSection root = config.getConfigurationSection("armors");
        if (root == null) {
            return List.of();
        }
        return root.getKeys(false).stream()
                .filter(key -> root.isConfigurationSection(key))
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();
    }

    public List<FieldSpec> armorFieldsFor(String armorId) {
        List<FieldSpec> fields = new ArrayList<>(ARMOR_FIELDS);
        fields.sort(Comparator.comparing(FieldSpec::path));
        return fields;
    }

    public EditResult getArmor(String armorId, String path) {
        if (!isAllowedArmorPath(path)) {
            return EditResult.error("Percorso non modificabile in armor.yml.");
        }
        ConfigurationSection section = armorSection(loadArmorConfig(), armorId);
        if (section == null) {
            return EditResult.error("ID armatura sconosciuto: " + armorId);
        }
        Object value = section.get(path);
        return EditResult.success(value == null ? "<unset>" : String.valueOf(value));
    }

    public EditResult setArmor(String actorName, String armorId, String path, String rawValue) {
        if (!isAllowedArmorPath(path)) {
            return EditResult.error("Percorso non modificabile in armor.yml.");
        }
        YamlConfiguration config = loadArmorConfig();
        ConfigurationSection section = armorSection(config, armorId);
        if (section == null) {
            return EditResult.error("ID armatura sconosciuto: " + armorId);
        }

        FieldSpec spec = armorSpecFor(path);
        Object parsed;
        try {
            parsed = parseValue(spec.type(), rawValue);
        } catch (IllegalArgumentException ex) {
            return EditResult.error(ex.getMessage());
        }

        Object oldValue = section.get(path);
        section.set(path, parsed);
        try {
            config.save(armorFile);
        } catch (IOException ex) {
            return EditResult.error("Impossibile salvare armor.yml: " + ex.getMessage());
        }
        reloadArmor();
        auditArmor(actorName, armorId, path, oldValue, parsed);
        return EditResult.success("Impostata armatura " + armorId + "." + path + " = " + parsed);
    }

    public EditResult removeArmor(String actorName, String armorId, String path) {
        if (!isAllowedArmorPath(path)) {
            return EditResult.error("Percorso non modificabile in armor.yml.");
        }
        YamlConfiguration config = loadArmorConfig();
        ConfigurationSection section = armorSection(config, armorId);
        if (section == null) {
            return EditResult.error("ID armatura sconosciuto: " + armorId);
        }
        if (!section.contains(path)) {
            return EditResult.error("Il percorso e' gia' non impostato.");
        }
        Object oldValue = section.get(path);
        section.set(path, null);
        try {
            config.save(armorFile);
        } catch (IOException ex) {
            return EditResult.error("Impossibile salvare armor.yml: " + ex.getMessage());
        }
        reloadArmor();
        auditArmor(actorName, armorId, path, oldValue, null);
        return EditResult.success("Rimossa armatura " + armorId + "." + path);
    }

    public void reloadArmor() {
        if (module.getArmorManager() != null) {
            module.getArmorManager().load(armorFile);
        }
    }

    public List<String> helmetIds() {
        YamlConfiguration config = loadArmorConfig();
        ConfigurationSection root = config.getConfigurationSection("helmets");
        List<String> ids = new ArrayList<>();
        if (root != null) {
            ids.addAll(root.getKeys(false).stream()
                    .filter(root::isConfigurationSection)
                    .map(key -> key.toLowerCase(Locale.ROOT))
                    .toList());
        }
        if (module.getHelmetManager() != null) {
            module.getHelmetManager().getAll().forEach(helmet -> {
                if (!ids.contains(helmet.getId())) {
                    ids.add(helmet.getId());
                }
            });
        }
        ids.sort(String.CASE_INSENSITIVE_ORDER);
        return ids;
    }

    public List<FieldSpec> helmetFieldsFor(String helmetId) {
        List<FieldSpec> fields = new ArrayList<>(HELMET_FIELDS);
        fields.sort(Comparator.comparing(FieldSpec::path));
        return fields;
    }

    public EditResult getHelmet(String helmetId, String path) {
        if (!isAllowedHelmetPath(path)) {
            return EditResult.error("Percorso non modificabile nei caschi di armor.yml.");
        }
        String normalizedId = normalizeConfigId(helmetId);
        ConfigurationSection section = helmetSection(loadArmorConfig(), normalizedId, false);
        if (section != null && section.contains(path)) {
            Object value = section.get(path);
            return EditResult.success(value == null ? "<unset>" : String.valueOf(value));
        }
        if (module.getHelmetManager() != null && module.getHelmetManager().getHelmet(normalizedId) != null) {
            Object fallback = defaultHelmetValue(normalizedId, path);
            return EditResult.success(fallback == null ? "<unset>" : String.valueOf(fallback));
        }
        return EditResult.error("ID casco sconosciuto: " + helmetId);
    }

    public EditResult setHelmet(String actorName, String helmetId, String path, String rawValue) {
        if (!isAllowedHelmetPath(path)) {
            return EditResult.error("Percorso non modificabile nei caschi di armor.yml.");
        }
        YamlConfiguration config = loadArmorConfig();
        String normalizedId = normalizeConfigId(helmetId);
        ConfigurationSection section = helmetSection(config, normalizedId, true);
        if (section == null) {
            return EditResult.error("ID casco sconosciuto: " + helmetId);
        }

        FieldSpec spec = helmetSpecFor(path);
        Object parsed;
        try {
            parsed = parseValue(spec.type(), rawValue);
        } catch (IllegalArgumentException ex) {
            return EditResult.error(ex.getMessage());
        }

        Object oldValue = section.get(path);
        section.set(path, parsed);
        try {
            config.save(armorFile);
        } catch (IOException ex) {
            return EditResult.error("Impossibile salvare armor.yml: " + ex.getMessage());
        }
        reloadHelmet();
        auditHelmet(actorName, normalizedId, path, oldValue, parsed);
        return EditResult.success("Impostato casco " + normalizedId + "." + path + " = " + parsed);
    }

    public EditResult removeHelmet(String actorName, String helmetId, String path) {
        if (!isAllowedHelmetPath(path)) {
            return EditResult.error("Percorso non modificabile nei caschi di armor.yml.");
        }
        YamlConfiguration config = loadArmorConfig();
        String normalizedId = normalizeConfigId(helmetId);
        ConfigurationSection section = helmetSection(config, normalizedId, false);
        if (section == null) {
            return EditResult.error("ID casco sconosciuto: " + helmetId);
        }
        if (!section.contains(path)) {
            return EditResult.error("Il percorso e' gia' non impostato.");
        }
        Object oldValue = section.get(path);
        section.set(path, null);
        try {
            config.save(armorFile);
        } catch (IOException ex) {
            return EditResult.error("Impossibile salvare armor.yml: " + ex.getMessage());
        }
        reloadHelmet();
        auditHelmet(actorName, normalizedId, path, oldValue, null);
        return EditResult.success("Rimosso casco " + normalizedId + "." + path);
    }

    public void reloadHelmet() {
        if (module.getHelmetManager() != null) {
            module.getHelmetManager().load(armorFile);
        }
    }

    private YamlConfiguration loadConfig() {
        return YamlConfiguration.loadConfiguration(weaponsFile);
    }

    private YamlConfiguration loadArmorConfig() {
        return YamlConfiguration.loadConfiguration(armorFile);
    }

    private ConfigurationSection armorSection(YamlConfiguration config, String armorId) {
        ConfigurationSection root = config.getConfigurationSection("armors");
        if (root == null || armorId == null) {
            return null;
        }
        return root.getConfigurationSection(armorId.toLowerCase(Locale.ROOT));
    }

    private ConfigurationSection helmetSection(YamlConfiguration config, String helmetId, boolean create) {
        if (helmetId == null || helmetId.isBlank()) {
            return null;
        }
        ConfigurationSection root = config.getConfigurationSection("helmets");
        if (root == null) {
            if (!create) {
                return null;
            }
            root = config.createSection("helmets");
        }
        ConfigurationSection section = root.getConfigurationSection(helmetId.toLowerCase(Locale.ROOT));
        if (section == null && create) {
            section = root.createSection(helmetId.toLowerCase(Locale.ROOT));
        }
        return section;
    }

    private String normalizeConfigId(String id) {
        return id == null ? "" : id.toLowerCase(Locale.ROOT);
    }

    private Object defaultHelmetValue(String helmetId, String path) {
        if (module.getHelmetManager() == null || module.getHelmetManager().getHelmet(helmetId) == null) {
            return null;
        }
        var helmet = module.getHelmetManager().getHelmet(helmetId);
        return switch (path.toLowerCase(Locale.ROOT)) {
            case "display-name" -> helmet.getDisplayName();
            case "custom-model-data" -> helmet.getCustomModelData();
            case "color-rgb" -> helmet.getColorRgb() < 0 ? "<unset>" : String.format("#%06X", helmet.getColorRgb());
            case "damage-reduction" -> helmet.getDamageReduction();
            case "negates-headshot" -> helmet.negatesHeadshot();
            case "prevents-melee-stun" -> helmet.preventsMeleeStun();
            case "max-durability" -> helmet.getMaxDurability();
            default -> null;
        };
    }

    private FieldSpec specFor(String path) {
        return BASE_FIELDS.stream()
                .filter(field -> field.path().equalsIgnoreCase(path))
                .findFirst()
                .orElseGet(() -> dynamicSpec(path));
    }

    private FieldSpec dynamicSpec(String path) {
        if (path.startsWith("visual-states.") || path.startsWith("visual-variants.")) {
            return new FieldSpec(path, FieldType.INT, path);
        }
        return new FieldSpec(path, FieldType.STRING, path);
    }

    private boolean isAllowedPath(String path) {
        if (path == null || path.isBlank() || path.contains("..") || path.startsWith(".") || path.endsWith(".")) {
            return false;
        }
        String normalized = path.toLowerCase(Locale.ROOT);
        if (BASE_FIELDS.stream().anyMatch(field -> field.path().equals(normalized))) {
            return true;
        }
        if (normalized.startsWith("visual-states.")) {
            String state = normalized.substring("visual-states.".length());
            return isSingleKey(state) && isVisualState(state);
        }
        if (normalized.startsWith("visual-variants.")) {
            String[] parts = normalized.split("\\.");
            return parts.length == 3 && isVisualState(parts[1]) && isSingleKey(parts[2]);
        }
        return false;
    }

    private FieldSpec armorSpecFor(String path) {
        return ARMOR_FIELDS.stream()
                .filter(field -> field.path().equalsIgnoreCase(path))
                .findFirst()
                .orElse(new FieldSpec(path, FieldType.STRING, path));
    }

    private boolean isAllowedArmorPath(String path) {
        if (path == null || path.isBlank() || path.contains(".") || path.contains("..")) {
            return false;
        }
        String normalized = path.toLowerCase(Locale.ROOT);
        return ARMOR_FIELDS.stream().anyMatch(field -> field.path().equals(normalized));
    }

    private FieldSpec helmetSpecFor(String path) {
        return HELMET_FIELDS.stream()
                .filter(field -> field.path().equalsIgnoreCase(path))
                .findFirst()
                .orElse(new FieldSpec(path, FieldType.STRING, path));
    }

    private boolean isAllowedHelmetPath(String path) {
        if (path == null || path.isBlank() || path.contains(".") || path.contains("..")) {
            return false;
        }
        String normalized = path.toLowerCase(Locale.ROOT);
        return HELMET_FIELDS.stream().anyMatch(field -> field.path().equals(normalized));
    }

    private boolean isSingleKey(String key) {
        return key != null && key.matches("[a-z0-9_-]+");
    }

    private boolean isVisualState(String state) {
        for (WeaponVisualState value : WeaponVisualState.values()) {
            if (value.name().equalsIgnoreCase(state)) {
                return true;
            }
        }
        return false;
    }

    private Object parseValue(FieldType type, String rawValue) {
        String value = rawValue == null ? "" : rawValue.trim();
        if (type == FieldType.STRING) {
            if (value.isEmpty()) {
                throw new IllegalArgumentException("Il valore non puo' essere vuoto. Usa remove per svuotarlo.");
            }
            return value;
        }
        if (type == FieldType.INT || type == FieldType.NULLABLE_INT) {
            if (type == FieldType.NULLABLE_INT && value.equalsIgnoreCase("null")) {
                return null;
            }
            return Integer.parseInt(value);
        }
        if (type == FieldType.DOUBLE) {
            return Double.parseDouble(value);
        }
        if (type == FieldType.BOOLEAN) {
            if (value.equalsIgnoreCase("true") || value.equalsIgnoreCase("yes") || value.equalsIgnoreCase("on")) {
                return true;
            }
            if (value.equalsIgnoreCase("false") || value.equalsIgnoreCase("no") || value.equalsIgnoreCase("off")) {
                return false;
            }
            throw new IllegalArgumentException("Usa true o false.");
        }
        if (type == FieldType.MATERIAL) {
            Material.valueOf(value.toUpperCase(Locale.ROOT));
            return value.toUpperCase(Locale.ROOT);
        }
        if (type == FieldType.WEAPON_CATEGORY) {
            WeaponCategory.valueOf(value.toUpperCase(Locale.ROOT));
            return value.toUpperCase(Locale.ROOT);
        }
        if (type == FieldType.FIRE_MODE_LIST) {
            List<String> modes = new ArrayList<>();
            for (String part : value.split(",")) {
                FireMode mode = parseFireMode(part);
                modes.add(mode.name().toLowerCase(Locale.ROOT));
            }
            if (modes.isEmpty()) {
                throw new IllegalArgumentException("Indica almeno una modalita' di fuoco.");
            }
            return modes;
        }
        return value;
    }

    private FireMode parseFireMode(String rawValue) {
        String value = rawValue == null ? "" : rawValue.trim().toLowerCase(Locale.ROOT);
        return switch (value) {
            case "semi", "single" -> FireMode.SEMI;
            case "auto", "automatic", "full_auto", "full-auto" -> FireMode.AUTO;
            case "burst", "raffica" -> FireMode.BURST;
            default -> throw new IllegalArgumentException("Modalita' fuoco sconosciuta: " + rawValue);
        };
    }

    private void audit(String actorName, String weaponId, String path, Object oldValue, Object newValue) {
        module.getCore().getLogger().info("[OpenWeapons] Audit config arma: " + actorName
                + " ha cambiato " + weaponId + "." + path + " da " + oldValue + " a " + newValue);
    }

    private void auditArmor(String actorName, String armorId, String path, Object oldValue, Object newValue) {
        module.getCore().getLogger().info("[OpenWeapons] Audit config armatura: " + actorName
                + " ha cambiato " + armorId + "." + path + " da " + oldValue + " a " + newValue);
    }

    private void auditHelmet(String actorName, String helmetId, String path, Object oldValue, Object newValue) {
        module.getCore().getLogger().info("[OpenWeapons] Audit config casco: " + actorName
                + " ha cambiato " + helmetId + "." + path + " da " + oldValue + " a " + newValue);
    }

    public record FieldSpec(String path, FieldType type, String label) {
    }

    public record EditResult(boolean success, String message) {
        public static EditResult success(String message) {
            return new EditResult(true, message);
        }

        public static EditResult error(String message) {
            return new EditResult(false, message);
        }
    }

    public enum FieldType {
        STRING,
        INT,
        NULLABLE_INT,
        DOUBLE,
        BOOLEAN,
        MATERIAL,
        WEAPON_CATEGORY,
        FIRE_MODE_LIST
    }
}
