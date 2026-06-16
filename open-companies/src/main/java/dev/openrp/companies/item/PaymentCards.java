package dev.openrp.companies.item;

import java.util.Optional;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.OfflinePlayer;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import dev.openrp.companies.OpenCompaniesPlugin;

/**
 * Payment cards. A card is an {@link ItemStack} bound to its owner's UUID in its persistent data; the
 * card authorizes payments from that player's personal bank account at a POS/register. The bank
 * account itself lives in the economy adapter (so it is Vault-compatible) - the card is just the
 * physical token a player taps. Cards use a fixed base material with {@code CustomModelData} so a
 * resource pack can give them a card model.
 */
public final class PaymentCards {

    private static final int CARD_MODEL_DATA = 9001;

    private final NamespacedKey ownerKey;

    public PaymentCards(OpenCompaniesPlugin plugin) {
        this.ownerKey = new NamespacedKey(plugin, "card_owner");
    }

    /** A payment card bound to {@code owner}. */
    public ItemStack create(OfflinePlayer owner, String ownerName) {
        ItemStack item = new ItemStack(Material.PAPER);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            String label = ownerName == null || ownerName.isBlank() ? owner.getUniqueId().toString() : ownerName;
            meta.displayName(Component.text("Carta — " + label, NamedTextColor.AQUA)
                    .decoration(TextDecoration.ITALIC, false));
            meta.setCustomModelData(CARD_MODEL_DATA);
            meta.getPersistentDataContainer().set(ownerKey, PersistentDataType.STRING, owner.getUniqueId().toString());
            item.setItemMeta(meta);
        }
        return item;
    }

    public boolean isCard(ItemStack item) {
        return ownerOf(item).isPresent();
    }

    /** The owner bound to a card, or empty if the item is not a card. */
    public Optional<UUID> ownerOf(ItemStack item) {
        if (item == null || item.getType() == Material.AIR || !item.hasItemMeta()) {
            return Optional.empty();
        }
        String raw = item.getItemMeta().getPersistentDataContainer().get(ownerKey, PersistentDataType.STRING);
        if (raw == null || raw.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(UUID.fromString(raw));
        } catch (IllegalArgumentException ignored) {
            return Optional.empty();
        }
    }
}
