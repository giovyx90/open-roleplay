package dev.openrp.companies.gui;

import java.util.Arrays;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

/** Small helpers for building GUI button/filler items without pulling in an item-builder dependency. */
public final class Items {

    private Items() {
    }

    public static ItemStack button(Material material, Component name, Component... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(name.decoration(TextDecoration.ITALIC, false));
            if (lore.length > 0) {
                List<Component> lines = Arrays.stream(lore)
                        .map(line -> line.decoration(TextDecoration.ITALIC, false))
                        .toList();
                meta.lore(lines);
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    public static ItemStack label(Material material, String name, NamedTextColor color) {
        return button(material, Component.text(name, color));
    }

    public static ItemStack filler() {
        return button(Material.GRAY_STAINED_GLASS_PANE, Component.text(" "));
    }
}
