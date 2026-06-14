package dev.openrp.weapons.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public final class ItemBuilder {
    private final ItemStack item;

    public ItemBuilder(Material material) {
        this.item = new ItemStack(material == null || material.isAir() ? Material.STONE : material);
    }

    public ItemBuilder name(Component name) {
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(name);
            item.setItemMeta(meta);
        }
        return this;
    }

    public ItemBuilder lore(Component... lore) {
        return lore(Arrays.asList(lore));
    }

    public ItemBuilder lore(Collection<? extends Component> lore) {
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.lore(lore == null ? List.of() : List.copyOf(lore));
            item.setItemMeta(meta);
        }
        return this;
    }

    public ItemBuilder customModelData(int customModelData) {
        ItemMeta meta = item.getItemMeta();
        if (meta != null && customModelData > 0) {
            meta.setCustomModelData(customModelData);
            item.setItemMeta(meta);
        }
        return this;
    }

    public ItemStack build() {
        return item;
    }

    public static ItemStack filler() {
        return new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE)
                .name(Component.text(" ", NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false))
                .build();
    }
}
