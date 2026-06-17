package dev.openrp.fdo.item;

import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import dev.openrp.fdo.OpenFdoPlugin;
import dev.openrp.fdo.model.Agent;

/**
 * The identity badge. An NBT item carrying only ids ({@code corps_id}, {@code rank_id},
 * {@code matricola}); the visible corps and rank labels are resolved from config at render time, so
 * renaming a corps updates every freshly issued badge. The badge unlocks no command - capabilities
 * come from the member's rank - it exists to identify the holder and make procedures uncontestable.
 */
public final class Tesserino {

    private final OpenFdoPlugin plugin;
    private final NamespacedKey corpsKey;
    private final NamespacedKey rankKey;
    private final NamespacedKey matricolaKey;
    private final Material material;

    public Tesserino(OpenFdoPlugin plugin) {
        this.plugin = plugin;
        this.corpsKey = new NamespacedKey(plugin, "badge_corps");
        this.rankKey = new NamespacedKey(plugin, "badge_rank");
        this.matricolaKey = new NamespacedKey(plugin, "badge_matricola");
        Material parsed = Material.matchMaterial(plugin.config().settings().badgeMaterial());
        this.material = parsed == null ? Material.PAPER : parsed;
    }

    /** Issues a freshly rendered badge for a member, with labels resolved from the current config. */
    public ItemStack create(Agent agent) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }
        String corpsLabel = plugin.config().corps().get(agent.corpsId())
                .map(corps -> corps.displayName()).orElse(agent.corpsId());
        String rankLabel = plugin.config().ranks().rank(agent.corpsId(), agent.rankId())
                .map(rank -> rank.displayName()).orElse(agent.rankId());

        meta.displayName(Component.text(corpsLabel, NamedTextColor.GOLD).decoration(TextDecoration.ITALIC, false));
        List<Component> lore = new ArrayList<>();
        lore.add(line(rankLabel, NamedTextColor.YELLOW));
        lore.add(line(agent.name(), NamedTextColor.WHITE));
        lore.add(line("Matricola " + agent.matricola(), NamedTextColor.GRAY));
        meta.lore(lore);

        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(corpsKey, PersistentDataType.STRING, agent.corpsId());
        pdc.set(rankKey, PersistentDataType.STRING, agent.rankId());
        pdc.set(matricolaKey, PersistentDataType.STRING, agent.matricola());
        item.setItemMeta(meta);
        return item;
    }

    public boolean isBadge(ItemStack item) {
        return matricola(item) != null;
    }

    public String corpsId(ItemStack item) {
        return read(item, corpsKey);
    }

    public String rankId(ItemStack item) {
        return read(item, rankKey);
    }

    public String matricola(ItemStack item) {
        return read(item, matricolaKey);
    }

    private String read(ItemStack item, NamespacedKey key) {
        if (item == null || item.getType() == Material.AIR || !item.hasItemMeta()) {
            return null;
        }
        return item.getItemMeta().getPersistentDataContainer().get(key, PersistentDataType.STRING);
    }

    private static Component line(String text, NamedTextColor color) {
        return Component.text(text, color).decoration(TextDecoration.ITALIC, false);
    }
}
