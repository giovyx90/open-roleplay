package dev.openrp.crime.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import dev.openrp.crime.adapter.AdapterRegistry;
import dev.openrp.crime.config.CrimeConfig;
import dev.openrp.crime.config.Good;
import dev.openrp.crime.model.TrackedGood;
import dev.openrp.crime.model.TrackedGoodStatus;

/**
 * Owns the illegal-good items. Builds marked item stacks from the catalogue, identifies them by the
 * persistent data the core writes (never by display name, which players can fake), and keeps a
 * tracking record per item so the good can be followed from production to seizure. The core decides
 * <em>that</em> an item is illegal; the config decides what it looks like.
 */
public final class GoodsService {

    private final CrimeConfig config;
    private final AdapterRegistry adapters;
    private final NamespacedKey goodKey;
    private final NamespacedKey itemKey;
    private final NamespacedKey qualityKey;
    private final NamespacedKey producerKey;
    private final Map<String, TrackedGood> tracked = new ConcurrentHashMap<>();

    public GoodsService(JavaPlugin plugin, CrimeConfig config, AdapterRegistry adapters) {
        this.config = config;
        this.adapters = adapters;
        this.goodKey = new NamespacedKey(plugin, "good_id");
        this.itemKey = new NamespacedKey(plugin, "item_uuid");
        this.qualityKey = new NamespacedKey(plugin, "quality");
        this.producerKey = new NamespacedKey(plugin, "producer");
    }

    public void loadAll() {
        tracked.clear();
        for (TrackedGood good : adapters.storage().loadTrackedGoods()) {
            tracked.put(good.itemUuid(), good);
        }
    }

    /**
     * Builds {@code amount} marked items for the good and registers a tracking record. Each call mints
     * a fresh per-item UUID so seizures and transfers can be followed.
     */
    public ItemStack create(Good good, int amount, int quality, UUID producer, String orgId) {
        String itemUuid = UUID.randomUUID().toString();
        Material material = Material.matchMaterial(good.material());
        ItemStack stack = new ItemStack(material == null ? Material.PAPER : material, Math.max(1, amount));
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            applyCosmetics(meta, good);
            if (good.customModelData() > 0) {
                meta.setCustomModelData(good.customModelData());
            }
            PersistentDataContainer data = meta.getPersistentDataContainer();
            data.set(goodKey, PersistentDataType.STRING, good.id());
            data.set(itemKey, PersistentDataType.STRING, itemUuid);
            data.set(qualityKey, PersistentDataType.INTEGER, clampQuality(quality));
            if (producer != null) {
                data.set(producerKey, PersistentDataType.STRING, producer.toString());
            }
            stack.setItemMeta(meta);
        }
        TrackedGood record = new TrackedGood(itemUuid, good.id(), producer, orgId, System.currentTimeMillis());
        record.setQuality(clampQuality(quality));
        tracked.put(itemUuid, record);
        adapters.storage().saveTrackedGood(record);
        return stack;
    }

    private void applyCosmetics(ItemMeta meta, Good good) {
        LegacyComponentSerializer legacy = LegacyComponentSerializer.legacyAmpersand();
        String name = good.itemName() == null || good.itemName().isBlank() ? good.displayName() : good.itemName();
        meta.displayName(legacy.deserialize(name).decoration(TextDecoration.ITALIC, false));
        if (!good.lore().isEmpty()) {
            List<Component> lore = new ArrayList<>();
            for (String line : good.lore()) {
                lore.add(legacy.deserialize(line).decoration(TextDecoration.ITALIC, false));
            }
            meta.lore(lore);
        }
    }

    public boolean isIllegal(ItemStack item) {
        return goodId(item).isPresent();
    }

    public Optional<String> goodId(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return Optional.empty();
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(meta.getPersistentDataContainer().get(goodKey, PersistentDataType.STRING));
    }

    public Optional<Good> getIllegalGood(ItemStack item) {
        return goodId(item).flatMap(config.goods()::get);
    }

    public Optional<String> itemUuid(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return Optional.empty();
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(meta.getPersistentDataContainer().get(itemKey, PersistentDataType.STRING));
    }

    /**
     * Marks an existing item stack as the given illegal good, writing a fresh tracking record. Used by
     * the public API so an external module can introduce an illegal good the core then tracks.
     */
    public void markIllegal(ItemStack item, String goodId, UUID producer) {
        if (item == null || goodId == null || config.goods().get(goodId).isEmpty()) {
            return;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return;
        }
        String itemUuid = UUID.randomUUID().toString();
        PersistentDataContainer data = meta.getPersistentDataContainer();
        data.set(goodKey, PersistentDataType.STRING, goodId);
        data.set(itemKey, PersistentDataType.STRING, itemUuid);
        data.set(qualityKey, PersistentDataType.INTEGER, 1);
        if (producer != null) {
            data.set(producerKey, PersistentDataType.STRING, producer.toString());
        }
        item.setItemMeta(meta);
        TrackedGood record = new TrackedGood(itemUuid, goodId, producer, null, System.currentTimeMillis());
        record.setQuality(1); // keep the tracking record in step with the PDC quality written above
        tracked.put(itemUuid, record);
        adapters.storage().saveTrackedGood(record);
    }

    public Optional<TrackedGood> tracked(String itemUuid) {
        return Optional.ofNullable(itemUuid == null ? null : tracked.get(itemUuid));
    }

    /** Updates the tracking status of an item (e.g. seized, sold) and persists it. */
    public void setStatus(String itemUuid, TrackedGoodStatus status) {
        TrackedGood record = itemUuid == null ? null : tracked.get(itemUuid);
        if (record != null) {
            record.setStatus(status);
            adapters.storage().saveTrackedGood(record);
        }
    }

    /** Marks the item stack's tracking record as seized, if it is one of ours. */
    public void markSeized(ItemStack item) {
        itemUuid(item).ifPresent(uuid -> setStatus(uuid, TrackedGoodStatus.SEIZED));
    }

    public Collection<TrackedGood> all() {
        return Collections.unmodifiableCollection(tracked.values());
    }

    private static int clampQuality(int quality) {
        return Math.max(1, Math.min(5, quality));
    }
}
