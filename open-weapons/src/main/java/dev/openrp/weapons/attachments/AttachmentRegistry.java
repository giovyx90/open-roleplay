package dev.openrp.weapons.attachments;

import it.meridian.core.CorePlugin;
import dev.openrp.weapons.model.WeaponCategory;
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
import java.util.Comparator;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class AttachmentRegistry {
    private final Map<String, AttachmentDefinition> attachments = new LinkedHashMap<>();
    private final NamespacedKey attachmentKey;
    private final CorePlugin core;

    public AttachmentRegistry(CorePlugin core) {
        this.core = core;
        this.attachmentKey = new NamespacedKey(core, "attachment_id");
    }

    public void load(File configFile) {
        attachments.clear();
        YamlConfiguration config = YamlConfiguration.loadConfiguration(configFile);

        for (String key : config.getKeys(false)) {
            ConfigurationSection section = config.getConfigurationSection(key);
            if (section == null) continue;

            try {
                String id = key.toLowerCase(Locale.ROOT);
                String displayName = section.getString("display-name", key);
                Material material = Material.valueOf(section.getString("material", "IRON_INGOT").toUpperCase(Locale.ROOT));
                int customModelData = section.getInt("custom-model-data", 0);
                AttachmentSlot slot = AttachmentSlot.fromConfig(section.getString("slot", "optic"));
                if (slot == null) {
                    throw new IllegalArgumentException("Slot non valido");
                }

                EnumSet<WeaponCategory> compatibleCategories = EnumSet.noneOf(WeaponCategory.class);
                for (String category : section.getStringList("compatible")) {
                    compatibleCategories.add(WeaponCategory.valueOf(category.trim().toUpperCase(Locale.ROOT)));
                }
                if (compatibleCategories.isEmpty()) {
                    throw new IllegalArgumentException("Nessuna categoria compatibile configurata");
                }
                Set<String> compatibleWeaponIds = section.getStringList("compatible-weapons").stream()
                        .map(value -> value.trim().toLowerCase(Locale.ROOT))
                        .filter(value -> !value.isBlank())
                        .collect(Collectors.toSet());

                attachments.put(id, new AttachmentDefinition(
                        id,
                        displayName,
                        material,
                        customModelData,
                        slot,
                        compatibleCategories,
                        compatibleWeaponIds,
                        section.getDouble("recoil-multiplier", 1.0D),
                        section.getDouble("spread-multiplier", 1.0D),
                        section.getDouble("sound-multiplier", 1.0D),
                        section.getDouble("max-distance-multiplier", 1.0D),
                        section.getDouble("ads-spread-multiplier", 1.0D),
                        section.getDouble("hipfire-spread-multiplier", 1.0D),
                        section.getDouble("mobility-multiplier", 1.0D),
                        section.getDouble("reload-time-multiplier", 1.0D),
                        section.getInt("zoom-bonus", 0),
                        section.getInt("install-time-ticks", 160),
                        section.getBoolean("illegal", false)
                ));
            } catch (Exception e) {
                core.getLogger().warning("[OpenWeapons] Impossibile caricare l'accessorio '" + key + "': " + e.getMessage());
            }
        }
        core.getLogger().info("[OpenWeapons] Caricati " + attachments.size() + " accessori arma.");
    }

    public AttachmentDefinition getAttachment(String id) {
        if (id == null) {
            return null;
        }
        return attachments.get(id.toLowerCase(Locale.ROOT));
    }

    public AttachmentDefinition getAttachment(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        String id = item.getItemMeta().getPersistentDataContainer().get(attachmentKey, PersistentDataType.STRING);
        if (id == null) return null;
        return getAttachment(id);
    }

    public List<AttachmentDefinition> getAll() {
        return new ArrayList<>(attachments.values());
    }

    public List<AttachmentDefinition> getBySlot(AttachmentSlot slot) {
        return attachments.values().stream()
                .filter(attachment -> attachment.getSlot() == slot)
                .collect(Collectors.toList());
    }

    public ItemStack createItemStack(String attachmentId) {
        AttachmentDefinition def = getAttachment(attachmentId);
        if (def == null) return null;

        ItemStack item = new ItemStack(def.getMaterial());
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text(def.getDisplayName(), NamedTextColor.GRAY)
                    .decoration(TextDecoration.BOLD, false)
                    .decoration(TextDecoration.ITALIC, false));
            meta.getPersistentDataContainer().set(attachmentKey, PersistentDataType.STRING, def.getId());
            if (def.getCustomModelData() > 0) {
                meta.setCustomModelData(def.getCustomModelData());
            }

            List<Component> lore = new ArrayList<>();
            lore.add(Component.text("Slot: ", NamedTextColor.GRAY)
                    .append(Component.text(slotDisplayName(def.getSlot()), NamedTextColor.WHITE))
                    .decoration(TextDecoration.ITALIC, false));
            lore.add(Component.text("Compatibile: ", NamedTextColor.GRAY)
                    .append(Component.text(compatibleDisplay(def), NamedTextColor.WHITE))
                    .decoration(TextDecoration.ITALIC, false));
            if (def.isIllegal()) {
                lore.add(Component.text("Modifica illegale", NamedTextColor.RED)
                        .decoration(TextDecoration.ITALIC, false));
            }
            meta.lore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    public NamespacedKey getAttachmentKey() {
        return attachmentKey;
    }

    public static String slotDisplayName(AttachmentSlot slot) {
        return switch (slot) {
            case OPTIC -> "Ottica";
            case BARREL -> "Canna";
            case UNDERBARREL -> "Sottocanna";
            case SIDE -> "Laterale";
            case MAGAZINE -> "Caricatore";
            case INTERNAL -> "Interno";
        };
    }

    private String compatibleDisplay(AttachmentDefinition attachment) {
        if (!attachment.getCompatibleWeaponIds().isEmpty()) {
            return attachment.getCompatibleWeaponIds().stream()
                    .sorted()
                    .collect(Collectors.joining(", "));
        }
        return compatibleDisplay(attachment.getCompatibleCategories());
    }

    private String compatibleDisplay(Set<WeaponCategory> categories) {
        return categories.stream()
                .sorted(Comparator.comparingInt(Enum::ordinal))
                .map(WeaponCategory::getDisplayName)
                .collect(Collectors.joining(", "));
    }
}
