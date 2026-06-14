package dev.openrp.core.api.item;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;
import java.util.List;

public final class OpenItemBuilder {
    private final ItemStack item;
    private final ItemMeta meta;

    public OpenItemBuilder(Material material) {
        this(new ItemStack(material));
    }

    public OpenItemBuilder(ItemStack base) {
        this.item = base == null ? new ItemStack(Material.AIR) : base.clone();
        this.meta = this.item.getItemMeta();
    }

    public OpenItemBuilder name(Component name) {
        if (meta != null) {
            meta.displayName(name);
        }
        return this;
    }

    public OpenItemBuilder lore(List<Component> lore) {
        if (meta != null) {
            meta.lore(lore);
        }
        return this;
    }

    public OpenItemBuilder lore(Component... lines) {
        return lore(Arrays.asList(lines));
    }

    public OpenItemBuilder amount(int amount) {
        item.setAmount(Math.max(1, amount));
        return this;
    }

    public OpenItemBuilder customModelData(int data) {
        if (meta != null) {
            meta.setCustomModelData(data);
        }
        return this;
    }

    public OpenItemBuilder glow() {
        if (meta != null) {
            meta.addEnchant(Enchantment.LUCK_OF_THE_SEA, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        }
        return this;
    }

    public ItemStack build() {
        if (meta != null) {
            item.setItemMeta(meta);
        }
        return item;
    }

    public static ItemStack filler() {
        return filler(Material.GRAY_STAINED_GLASS_PANE);
    }

    public static ItemStack filler(Material pane) {
        return new OpenItemBuilder(pane)
                .name(Component.text(" "))
                .build();
    }
}
