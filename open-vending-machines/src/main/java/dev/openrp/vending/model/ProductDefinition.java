package dev.openrp.vending.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

/**
 * Immutable catalogue definition of a sellable product (from products.yml).
 *
 * <p>This is pure data plus a convenience builder for the vanilla {@link ItemStack}. Servers that
 * use a custom item plugin do not have to rely on {@link #createStack(int)}: they replace the
 * {@code InventoryAdapter} and build the item however they like, keyed off {@link #id()}.</p>
 */
public record ProductDefinition(
        String id,
        String displayName,
        Material material,
        int amount,
        double defaultPrice,
        int defaultMaxStock,
        int customModelData,
        List<String> lore) {

    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

    public ProductDefinition {
        id = id == null ? "" : id.trim().toLowerCase(Locale.ROOT);
        displayName = displayName == null || displayName.isBlank() ? id : displayName;
        material = material == null ? Material.PAPER : material;
        amount = Math.max(1, amount);
        defaultPrice = Math.max(0.0, defaultPrice);
        defaultMaxStock = Math.max(1, defaultMaxStock);
        customModelData = Math.max(0, customModelData);
        lore = lore == null ? List.of() : List.copyOf(lore);
    }

    /** Display name with MiniMessage tags stripped, for plain chat output. */
    public String plainName() {
        return MINI_MESSAGE.stripTags(displayName);
    }

    /**
     * Builds the vanilla item handed to buyers / shown in the GUI. {@code quantity} overrides the
     * configured stack size (used so a single GUI icon can show one unit).
     */
    public ItemStack createStack(int quantity) {
        ItemStack item = new ItemStack(material, Math.max(1, quantity));
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }
        meta.displayName(MINI_MESSAGE.deserialize(displayName).decoration(TextDecoration.ITALIC, false));
        if (!lore.isEmpty()) {
            List<Component> rendered = new ArrayList<>(lore.size());
            for (String line : lore) {
                rendered.add(MINI_MESSAGE.deserialize(line).decoration(TextDecoration.ITALIC, false));
            }
            meta.lore(rendered);
        }
        if (customModelData > 0) {
            meta.setCustomModelData(customModelData);
        }
        item.setItemMeta(meta);
        return item;
    }
}
