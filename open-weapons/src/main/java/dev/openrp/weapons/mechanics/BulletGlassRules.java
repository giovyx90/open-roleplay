package dev.openrp.weapons.mechanics;

import java.util.Collection;
import java.util.Locale;
import org.bukkit.Material;

public final class BulletGlassRules {
    private BulletGlassRules() {
    }

    public static boolean isBreakableGlass(Material material, Collection<String> configuredMaterials) {
        if (material == null || material == Material.AIR || material == Material.CAVE_AIR || material == Material.VOID_AIR) {
            return false;
        }
        if (configuredMaterials != null && !configuredMaterials.isEmpty()) {
            return configuredMaterials.stream()
                    .filter(value -> value != null && !value.isBlank())
                    .map(value -> value.trim().toUpperCase(Locale.ROOT))
                    .anyMatch(value -> value.equals(material.name()));
        }
        String name = material.name();
        return name.equals("GLASS")
                || name.equals("GLASS_PANE")
                || name.equals("TINTED_GLASS")
                || name.endsWith("_STAINED_GLASS")
                || name.endsWith("_STAINED_GLASS_PANE");
    }

    public static boolean isBulletPassThrough(Material material) {
        return material == Material.IRON_BARS;
    }

    public static int clampMaxPenetrations(int configured) {
        return Math.max(0, Math.min(8, configured));
    }
}
