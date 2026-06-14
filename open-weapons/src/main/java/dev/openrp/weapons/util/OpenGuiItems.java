package dev.openrp.weapons.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

public final class OpenGuiItems {
    private OpenGuiItems() {
    }

    public static Component getGlyphTitle(String glyphId, String fallback) {
        return Component.text(fallback == null || fallback.isBlank() ? glyphId : fallback);
    }

    public static ItemStack getFiller() {
        return ItemBuilder.filler();
    }

    public static ItemStack getPrevPageButton() {
        return new ItemBuilder(Material.ARROW)
                .name(Component.text("Pagina precedente", NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false))
                .build();
    }

    public static ItemStack getNextPageButton() {
        return new ItemBuilder(Material.ARROW)
                .name(Component.text("Pagina successiva", NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false))
                .build();
    }

    public static ItemStack getConfirmButton(Component name) {
        return new ItemBuilder(Material.LIME_CONCRETE).name(name).build();
    }

    public static ItemStack getCancelButton(Component name) {
        return new ItemBuilder(Material.RED_CONCRETE).name(name).build();
    }

}
