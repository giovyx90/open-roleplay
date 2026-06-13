package dev.openrp.weapons.cosmetics;

import dev.openrp.weapons.model.WeaponDefinition;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class WeaponVisualVariantResolver {
    private WeaponVisualVariantResolver() {
    }

    public static List<String> candidates(boolean optic, boolean magazine, boolean grip, String led, String color) {
        return candidates(optic, magazine, grip, led, color, WeaponCosmeticManager.NONE);
    }

    public static List<String> candidates(boolean optic, boolean magazine, boolean grip,
                                          String led, String color, String skin) {
        Set<String> candidates = new LinkedHashSet<>();
        String normalizedSkin = normalizeCosmetic(skin);
        if (!normalizedSkin.equals("none")) {
            addSkinAttachmentFallbacks(candidates, optic, magazine, grip, normalizedSkin);
        }
        addAttachmentFallbacks(candidates, optic, magazine, grip, normalizeCosmetic(led), normalizeCosmetic(color));
        return new ArrayList<>(candidates);
    }

    private static void addSkinAttachmentFallbacks(Set<String> candidates, boolean optic, boolean magazine, boolean grip,
                                                   String skin) {
        addSkinVariant(candidates, optic, magazine, grip, skin);
        if (grip) {
            addSkinVariant(candidates, optic, magazine, false, skin);
        }
        if (optic) {
            addSkinVariant(candidates, false, magazine, grip, skin);
        }
        if (magazine) {
            addSkinVariant(candidates, optic, false, grip, skin);
        }
        if (optic && grip) {
            addSkinVariant(candidates, false, magazine, false, skin);
        }
        if (magazine && grip) {
            addSkinVariant(candidates, false, false, grip, skin);
        }
        if (magazine && optic) {
            addSkinVariant(candidates, optic, false, false, skin);
        }
        if (magazine) {
            addSkinVariant(candidates, false, true, false, skin);
        }
        if (grip) {
            addSkinVariant(candidates, false, false, true, skin);
        }
        if (optic) {
            addSkinVariant(candidates, true, false, false, skin);
        }
        addSkinVariant(candidates, false, false, false, skin);
    }

    private static void addAttachmentFallbacks(Set<String> candidates, boolean optic, boolean magazine, boolean grip,
                                               String led, String color) {
        addCosmeticFallbacks(candidates, optic, magazine, grip, led, color);
        if (grip) {
            addCosmeticFallbacks(candidates, optic, magazine, false, led, color);
        }
        if (optic) {
            addCosmeticFallbacks(candidates, false, magazine, grip, led, color);
        }
        if (magazine) {
            addCosmeticFallbacks(candidates, optic, false, grip, led, color);
        }
        if (optic && grip) {
            addCosmeticFallbacks(candidates, false, magazine, false, led, color);
        }
        if (magazine && grip) {
            addCosmeticFallbacks(candidates, false, false, grip, led, color);
        }
        if (magazine && optic) {
            addCosmeticFallbacks(candidates, optic, false, false, led, color);
        }
        if (magazine) {
            addCosmeticFallbacks(candidates, false, true, false, led, color);
        }
        if (grip) {
            addCosmeticFallbacks(candidates, false, false, true, led, color);
        }
        if (optic) {
            addCosmeticFallbacks(candidates, true, false, false, led, color);
        }
        addCosmeticFallbacks(candidates, false, false, false, led, color);
    }

    private static void addCosmeticFallbacks(Set<String> candidates, boolean optic, boolean magazine, boolean grip,
                                             String led, String color) {
        boolean hasLed = !led.equals("none");
        boolean hasColor = !color.equals("none");
        if (hasLed && hasColor) {
            addVariant(candidates, optic, magazine, grip, led, color);
        }
        if (hasLed) {
            addVariant(candidates, optic, magazine, grip, led, "none");
        }
        if (hasColor) {
            addVariant(candidates, optic, magazine, grip, "none", color);
        }
        addVariant(candidates, optic, magazine, grip, "none", "none");
    }

    private static void addVariant(Set<String> variants, boolean optic, boolean magazine, boolean grip,
                                   String led, String color) {
        List<String> tokens = new ArrayList<>();
        if (optic) {
            tokens.add("optic");
        }
        if (magazine) {
            tokens.add("magazine");
        }
        if (grip) {
            tokens.add("grip");
        }
        if (!led.equals("none")) {
            tokens.add("led-" + led);
        }
        if (!color.equals("none")) {
            tokens.add("color");
        }
        variants.add(tokens.isEmpty() ? "empty" : String.join("-", tokens));
    }

    private static void addSkinVariant(Set<String> variants, boolean optic, boolean magazine, boolean grip,
                                       String skin) {
        List<String> tokens = new ArrayList<>();
        if (optic) {
            tokens.add("optic");
        }
        if (magazine) {
            tokens.add("magazine");
        }
        if (grip) {
            tokens.add("grip");
        }
        tokens.add("skin-" + skin);
        variants.add(String.join("-", tokens));
    }

    public static String normalizeCosmetic(String value) {
        String normalized = WeaponDefinition.normalizeVisualVariantKey(value);
        return normalized.equals("empty") ? "none" : normalized;
    }
}
