package dev.openrp.cosmetics;

import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.Assert;
import org.junit.Test;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class WeaponVisualVariantResolverTest {

    @Test
    public void candidatesPreferFullCosmeticCombinationThenFallbacks() {
        List<String> candidates = WeaponVisualVariantResolver.candidates(true, true, true, "usa", "color");

        Assert.assertEquals("optic-magazine-grip-led-usa-color", candidates.get(0));
        Assert.assertTrue(candidates.indexOf("optic-magazine-grip-led-usa")
                < candidates.indexOf("optic-magazine-grip"));
        Assert.assertTrue(candidates.contains("magazine-led-usa-color"));
        Assert.assertEquals("empty", candidates.get(candidates.size() - 1));
    }

    @Test
    public void noCosmeticsPreservesLegacyVariantOrder() {
        List<String> candidates = WeaponVisualVariantResolver.candidates(true, true, true, "none", "none");

        Assert.assertEquals(List.of(
                "optic-magazine-grip",
                "optic-magazine",
                "magazine-grip",
                "optic-grip",
                "magazine",
                "grip",
                "optic",
                "empty"), candidates);
    }

    @Test
    public void noSkinPreservesM4a1CosmeticOrdering() {
        List<String> legacy = WeaponVisualVariantResolver.candidates(true, true, true, "anime", "color");
        List<String> withNoSkin = WeaponVisualVariantResolver.candidates(true, true, true, "anime", "color", "none");

        Assert.assertEquals(legacy, withNoSkin);
        Assert.assertEquals("optic-magazine-grip-led-anime-color", withNoSkin.get(0));
    }

    @Test
    public void skinCandidatesWinBeforeColorAndBaseFallbacks() {
        List<String> candidates = WeaponVisualVariantResolver.candidates(true, true, true,
                "none", "color", "sugarline-bakery");

        Assert.assertEquals("optic-magazine-grip-skin-sugarline-bakery", candidates.get(0));
        Assert.assertTrue(candidates.indexOf("magazine-skin-sugarline-bakery")
                < candidates.indexOf("optic-magazine-grip-color"));
        Assert.assertTrue(candidates.indexOf("skin-sugarline-bakery")
                < candidates.indexOf("color"));
        Assert.assertEquals("empty", candidates.get(candidates.size() - 1));
    }

    @Test
    public void normalizesEmptyCosmeticAsNone() {
        Assert.assertEquals("none", WeaponVisualVariantResolver.normalizeCosmetic("empty"));
        Assert.assertEquals("led-usa", WeaponVisualVariantResolver.candidates(false, false, false, "USA", "none").get(0));
    }

    @Test
    public void parsesHexAndAliasColors() {
        Assert.assertEquals(Integer.valueOf(0xA14DFF), WeaponCosmeticManager.parseColorRgb("#A14DFF"));
        Assert.assertEquals(Integer.valueOf(0xA14DFF), WeaponCosmeticManager.parseColorRgb("A14DFF"));
        Assert.assertEquals(Integer.valueOf(0xB02E26), WeaponCosmeticManager.parseColorRgb("red"));
        Assert.assertNull(WeaponCosmeticManager.parseColorRgb("#XYZ123"));
        Assert.assertEquals("#A14DFF", WeaponCosmeticManager.formatHex(0xA14DFF));
    }

    @Test
    public void gradientTextKeepsCosmeticSuffixText() {
        String text = PlainTextComponentSerializer.plainText().serialize(
                WeaponCosmeticManager.gradientText("[Aurum Reserve]", List.of(
                        TextColor.color(0xA96F00),
                        TextColor.color(0xFFD76B),
                        TextColor.color(0xFFF2B0))));

        Assert.assertEquals("[Aurum Reserve]", text);
    }

    @Test
    public void ppkCosmeticConfigDisablesLedAndSharesSugarlineSounds() throws Exception {
        YamlConfiguration config = loadCosmeticConfig();

        Assert.assertTrue(config.getBoolean("weapons.ppk.enabled"));
        Assert.assertTrue(config.getBoolean("weapons.ppk.skin"));
        Assert.assertTrue(config.getBoolean("weapons.ppk.color"));
        Assert.assertFalse(config.getBoolean("weapons.ppk.led"));
        Assert.assertEquals("PPK Riserva Aurum", config.getString("skins.ppk.gold-reserve.display-name"));
        Assert.assertEquals("Pasticceria Sugarline", config.getString("skins.ppk.sugarline-bakery.name-suffix"));
        Assert.assertEquals("weapons.ak47.sugarline.fire",
                config.getString("skins.ppk.sugarline-bakery.sound-fire"));
        Assert.assertEquals("weapons.ak47.sugarline.reload",
                config.getString("skins.ppk.sugarline-bakery.sound-reload"));
    }

    @Test
    public void m4a1CosmeticConfigEnablesRoyalMasqueradeSounds() throws Exception {
        YamlConfiguration config = loadCosmeticConfig();

        Assert.assertTrue(config.getBoolean("weapons.m4a1.enabled"));
        Assert.assertTrue(config.getBoolean("weapons.m4a1.skin"));
        Assert.assertTrue(config.getBoolean("weapons.m4a1.color"));
        Assert.assertTrue(config.getBoolean("weapons.m4a1.led"));
        Assert.assertEquals("M4A1 Mascherata Reale",
                config.getString("skins.m4a1.royal-masquerade.display-name"));
        Assert.assertEquals("Mascherata Reale",
                config.getString("skins.m4a1.royal-masquerade.name-suffix"));
        Assert.assertEquals("weapons.m4a1.royal.fire",
                config.getString("skins.m4a1.royal-masquerade.sound-fire"));
        Assert.assertEquals("weapons.m4a1.royal.reload",
                config.getString("skins.m4a1.royal-masquerade.sound-reload"));
        Assert.assertNull(config.getString("skins.m4a1.royal-masquerade.sound-automatic"));
    }

    @Test
    public void mp5AndRemingtonCosmeticConfigsDisableLedAndShareSugarlineSounds() throws Exception {
        YamlConfiguration config = loadCosmeticConfig();

        Assert.assertTrue(config.getBoolean("weapons.mp5.skin"));
        Assert.assertFalse(config.getBoolean("weapons.mp5.led"));
        Assert.assertEquals("weapons.ak47.sugarline.fire",
                config.getString("skins.mp5.sugarline-bakery.sound-fire"));

        Assert.assertTrue(config.getBoolean("weapons.remington_870.skin"));
        Assert.assertFalse(config.getBoolean("weapons.remington_870.led"));
        Assert.assertEquals("weapons.ak47.sugarline.reload",
                config.getString("skins.remington_870.sugarline-bakery.sound-reload"));
    }

    private YamlConfiguration loadCosmeticConfig() throws Exception {
        try (InputStream input = getClass().getClassLoader().getResourceAsStream("weapon_cosmetics.yml")) {
            Assert.assertNotNull(input);
            return YamlConfiguration.loadConfiguration(new InputStreamReader(input, StandardCharsets.UTF_8));
        }
    }
}
