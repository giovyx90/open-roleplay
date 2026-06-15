package dev.openrp.vending.model;

import java.util.List;
import java.util.Locale;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;

/**
 * Immutable machine type/model definition (from machines.yml).
 *
 * <p>{@link #icon()} doubles as the placed block players interact with and the GUI header icon.
 * {@link #slots()} caps how many distinct products a machine of this type can stock, and
 * {@link #defaultProducts()} pre-fills those slots when a machine is created.</p>
 */
public record MachineType(
        String id,
        String displayName,
        Material icon,
        int slots,
        List<String> defaultProducts) {

    public MachineType {
        id = id == null ? "" : id.trim().toLowerCase(Locale.ROOT);
        displayName = displayName == null || displayName.isBlank() ? id : displayName;
        icon = icon == null ? Material.DROPPER : icon;
        slots = Math.max(1, slots);
        defaultProducts = defaultProducts == null ? List.of() : List.copyOf(defaultProducts);
    }

    /** Display name with MiniMessage tags stripped, for plain chat output. */
    public String plainName() {
        return MiniMessage.miniMessage().stripTags(displayName);
    }
}
