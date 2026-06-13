package dev.openrp.weapons.balaclava;

import it.meridian.core.CorePlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class BalaclavaManager {
    private static final int BALACLAVA_MODEL_DATA = 292;

    private final Set<UUID> maskedPlayers = ConcurrentHashMap.newKeySet();
    private final NamespacedKey balaclavaKey;
    private final CorePlugin core;

    public BalaclavaManager(CorePlugin core) {
        this.core = core;
        this.balaclavaKey = new NamespacedKey(core, "balaclava");
    }

    public boolean isMasked(UUID uuid) {
        return maskedPlayers.contains(uuid);
    }

    public void setMasked(UUID uuid, boolean masked) {
        if (masked) {
            maskedPlayers.add(uuid);
        } else {
            maskedPlayers.remove(uuid);
        }
    }

    public boolean isBalaclava(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        return item.getItemMeta().getPersistentDataContainer().has(balaclavaKey, PersistentDataType.BOOLEAN);
    }

    public ItemStack createBalaclava() {
        ItemStack item = new ItemStack(Material.FIREWORK_STAR);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text("Passamontagna", NamedTextColor.GRAY)
                    .decoration(TextDecoration.BOLD, false)
                    .decoration(TextDecoration.ITALIC, false));
            meta.setCustomModelData(BALACLAVA_MODEL_DATA);
            
            meta.getPersistentDataContainer().set(balaclavaKey, PersistentDataType.BOOLEAN, true);
            
            List<Component> lore = new ArrayList<>();
            lore.add(Component.text("Nasconde la tua identita'", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
            lore.add(Component.text("Apparirai come 'Non identificato'", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
            meta.lore(lore);
            
            item.setItemMeta(meta);
        }
        return item;
    }
}
