package dev.openrp.fdo.item;

import java.time.format.DateTimeFormatter;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import dev.openrp.fdo.OpenFdoPlugin;
import dev.openrp.fdo.config.ActDefinition;
import dev.openrp.fdo.model.Agent;

/**
 * The physical act document. The plugin hands the member a writable book tagged with the act type and
 * its target; the member writes the content themselves (the plugin never composes it). On signing,
 * the plugin stamps it - corps, rank, badge number, date and dossier id - and re-tags the resulting
 * written book so it stays identifiable. This is the "registra, non sostituire" rule made physical.
 */
public final class ActBook {

    private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final OpenFdoPlugin plugin;
    private final NamespacedKey actKey;
    private final NamespacedKey targetKey;
    private final NamespacedKey targetNameKey;
    private final Material material;

    public ActBook(OpenFdoPlugin plugin) {
        this.plugin = plugin;
        this.actKey = new NamespacedKey(plugin, "act_type");
        this.targetKey = new NamespacedKey(plugin, "act_target");
        this.targetNameKey = new NamespacedKey(plugin, "act_target_name");
        Material parsed = Material.matchMaterial(plugin.config().settings().bookMaterial());
        this.material = parsed == null || parsed != Material.WRITABLE_BOOK ? Material.WRITABLE_BOOK : parsed;
    }

    /** A blank, tagged writable book for the given act and (optional) target, ready for the member to fill. */
    public ItemStack createWritable(ActDefinition act, String issuerName, String targetUuid, String targetName) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }
        meta.displayName(Component.text(act.displayName(), NamedTextColor.GOLD)
                .decoration(TextDecoration.ITALIC, false));
        List<Component> lore = new ArrayList<>();
        lore.add(line(plugin.messages().text(plugin.getServer().getConsoleSender(), "act.book_lore"), NamedTextColor.GRAY));
        if (targetName != null && !targetName.isBlank()) {
            lore.add(line("→ " + targetName, NamedTextColor.YELLOW));
        }
        meta.lore(lore);
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(actKey, PersistentDataType.STRING, act.id());
        if (targetUuid != null) {
            pdc.set(targetKey, PersistentDataType.STRING, targetUuid);
        }
        if (targetName != null) {
            pdc.set(targetNameKey, PersistentDataType.STRING, targetName);
        }
        item.setItemMeta(meta);
        return item;
    }

    public boolean isActBook(ItemMeta meta) {
        return actId(meta) != null;
    }

    public String actId(ItemMeta meta) {
        return read(meta, actKey);
    }

    public String targetUuid(ItemMeta meta) {
        return read(meta, targetKey);
    }

    public String targetName(ItemMeta meta) {
        return read(meta, targetNameKey);
    }

    /**
     * Stamps a signed book: re-applies the act tags (so the written book is still identifiable), sets
     * the author to the issuing member and appends a stamp page with corps, rank, matricola, date and
     * the dossier id. Mutates the meta in place.
     */
    public void stamp(BookMeta meta, Agent issuer, ActDefinition act, String dossierId) {
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(actKey, PersistentDataType.STRING, act.id());
        String corpsLabel = plugin.config().corps().get(issuer.corpsId())
                .map(corps -> corps.displayName()).orElse(issuer.corpsId());
        String rankLabel = plugin.config().ranks().rank(issuer.corpsId(), issuer.rankId())
                .map(rank -> rank.displayName()).orElse(issuer.rankId());
        meta.author(Component.text(issuer.name()));
        meta.title(Component.text(act.displayName()));
        List<Component> stamp = new ArrayList<>();
        stamp.add(line("— " + act.displayName() + " —", NamedTextColor.DARK_RED));
        stamp.add(Component.empty());
        stamp.add(line(corpsLabel, NamedTextColor.BLACK));
        stamp.add(line(rankLabel + " " + issuer.name(), NamedTextColor.BLACK));
        stamp.add(line("Matricola " + issuer.matricola(), NamedTextColor.DARK_GRAY));
        stamp.add(line(LocalDateTime.now().format(STAMP), NamedTextColor.DARK_GRAY));
        if (dossierId != null) {
            stamp.add(line("Fascicolo " + dossierId, NamedTextColor.DARK_GRAY));
        }
        meta.addPages(Component.join(net.kyori.adventure.text.JoinConfiguration.newlines(), stamp));
    }

    private String read(ItemMeta meta, NamespacedKey key) {
        if (meta == null) {
            return null;
        }
        return meta.getPersistentDataContainer().get(key, PersistentDataType.STRING);
    }

    private static Component line(String text, NamedTextColor color) {
        return Component.text(text, color).decoration(TextDecoration.ITALIC, false);
    }
}
