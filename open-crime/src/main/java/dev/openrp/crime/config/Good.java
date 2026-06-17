package dev.openrp.crime.config;

import java.util.List;

/**
 * A configured illegal good. <strong>This is where the setting lives</strong>: {@code displayName},
 * {@code category} and the cosmetic item fields are all from config. The core only ever handles "an
 * illegal good with this id, danger level and street value" - never "cocaine" or "black viper venom".
 *
 * @param id              stable config key, e.g. {@code sostanza_a}
 * @param displayName     human name shown to players, e.g. {@code Cocaina}
 * @param category        free string from config ({@code narcotic}, {@code poison}, {@code document}...)
 * @param dangerLevel     1..5, narrative only - a judge may weigh it; it never triggers anything
 * @param streetValue     base value per unit, read by economy-aware modules
 * @param material        Bukkit material name backing the in-game item
 * @param customModelData CustomModelData for a resource pack, or 0 for none
 * @param itemName        display name for the item (legacy {@code &} colours supported)
 * @param lore            item lore lines
 */
public record Good(String id, String displayName, String category, int dangerLevel, long streetValue,
                   String material, int customModelData, String itemName, List<String> lore) {

    public Good {
        lore = lore == null ? List.of() : List.copyOf(lore);
        dangerLevel = Math.max(1, Math.min(5, dangerLevel));
        streetValue = Math.max(0L, streetValue);
    }
}
