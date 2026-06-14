package dev.openrp.weapons.cosmetics;

import org.junit.Assert;
import org.junit.Test;

import dev.openrp.cosmetics.WeaponCosmeticManager;
import dev.openrp.cosmetics.WeaponVisualVariantResolver;
import dev.openrp.weapons.model.WeaponCategory;
import dev.openrp.weapons.model.WeaponDefinition;
import dev.openrp.weapons.model.WeaponVisualState;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

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
    public void m4a1ColorCandidatesResolveToGenericColorableCmds() {
        Map<WeaponVisualState, Integer> visualStates = new EnumMap<>(WeaponVisualState.class);
        visualStates.put(WeaponVisualState.IDLE, 41);

        Map<String, Integer> idleVariants = Map.of(
                "empty", 41,
                "magazine", 43,
                "color", 9001,
                "magazine-color", 9030,
                "led-anime-color", 9025,
                "magazine-led-anime-color", 9054,
                "led-pacman-color", 9176,
                "magazine-led-pacman-color", 9178);
        Map<WeaponVisualState, Map<String, Integer>> variants = new EnumMap<>(WeaponVisualState.class);
        variants.put(WeaponVisualState.IDLE, idleVariants);

        WeaponDefinition m4a1 = new WeaponDefinition("m4a1", "M4A1", WeaponCategory.ASSAULT_RIFLE,
                Material.CROSSBOW, 41, visualStates, variants, 2,
                6.4, 2.0, 3, 55, 30, 90,
                "556nato", "shoot", "reload", true,
                List.of(), null, 0.009, 1,
                4.0, 0.45, 1.75, 0.75, 4.0, 55, 100, 0.65);

        Assert.assertEquals(9001, m4a1.resolveVisualModelData(WeaponVisualState.IDLE,
                WeaponVisualVariantResolver.candidates(false, false, false, "none", "color"), false));
        Assert.assertEquals(9030, m4a1.resolveVisualModelData(WeaponVisualState.IDLE,
                WeaponVisualVariantResolver.candidates(false, true, false, "none", "color"), true));
        Assert.assertEquals(9025, m4a1.resolveVisualModelData(WeaponVisualState.IDLE,
                WeaponVisualVariantResolver.candidates(false, false, false, "anime", "color"), false));
        Assert.assertEquals(9054, m4a1.resolveVisualModelData(WeaponVisualState.IDLE,
                WeaponVisualVariantResolver.candidates(false, true, false, "anime", "color"), true));
        Assert.assertEquals(9176, m4a1.resolveVisualModelData(WeaponVisualState.IDLE,
                WeaponVisualVariantResolver.candidates(false, false, false, "pacman", "color"), false));
        Assert.assertEquals(9178, m4a1.resolveVisualModelData(WeaponVisualState.IDLE,
                WeaponVisualVariantResolver.candidates(false, true, false, "pacman", "color"), true));
    }

    @Test
    public void m4a1SkinVariantsResolveBeforeColorLedAndBase() {
        Map<WeaponVisualState, Integer> visualStates = new EnumMap<>(WeaponVisualState.class);
        visualStates.put(WeaponVisualState.IDLE, 41);
        visualStates.put(WeaponVisualState.AIMING, 1041);
        visualStates.put(WeaponVisualState.RELOADING, 3041);

        Map<WeaponVisualState, Map<String, Integer>> variants = new EnumMap<>(WeaponVisualState.class);
        variants.put(WeaponVisualState.IDLE, Map.ofEntries(
                Map.entry("empty", 41),
                Map.entry("magazine", 43),
                Map.entry("color", 9001),
                Map.entry("led-anime-color", 9025),
                Map.entry("skin-gold-reserve", 9521),
                Map.entry("magazine-skin-gold-reserve", 9523),
                Map.entry("optic-magazine-grip-skin-royal-masquerade", 9555)));
        variants.put(WeaponVisualState.AIMING, Map.ofEntries(
                Map.entry("color", 9059),
                Map.entry("skin-gold-reserve", 10521),
                Map.entry("optic-magazine-grip-skin-royal-masquerade", 10555)));
        variants.put(WeaponVisualState.RELOADING, Map.ofEntries(
                Map.entry("color", 9117),
                Map.entry("skin-gold-reserve", 12521),
                Map.entry("optic-magazine-grip-skin-royal-masquerade", 12555)));

        WeaponDefinition m4a1 = new WeaponDefinition("m4a1", "M4A1", WeaponCategory.ASSAULT_RIFLE,
                Material.CROSSBOW, 41, visualStates, variants, 2,
                6.4, 2.0, 3, 55, 30, 90,
                "556nato", "shoot", "reload", true,
                List.of(), null, 0.009, 1,
                4.0, 0.45, 1.75, 0.75, 4.0, 55, 100, 0.65);

        Assert.assertEquals(9555, m4a1.resolveVisualModelData(WeaponVisualState.IDLE,
                WeaponVisualVariantResolver.candidates(true, true, true, "anime", "color", "royal-masquerade"), true));
        Assert.assertEquals(10555, m4a1.resolveVisualModelData(WeaponVisualState.AIMING,
                WeaponVisualVariantResolver.candidates(true, true, true, "anime", "color", "royal-masquerade"), true));
        Assert.assertEquals(12555, m4a1.resolveVisualModelData(WeaponVisualState.RELOADING,
                WeaponVisualVariantResolver.candidates(true, true, true, "anime", "color", "royal-masquerade"), true));
        Assert.assertEquals(9001, m4a1.resolveVisualModelData(WeaponVisualState.IDLE,
                WeaponVisualVariantResolver.candidates(false, false, false, "none", "color", "none"), false));
    }

    @Test
    public void ak47SkinVariantsResolveBeforeColorAndBase() {
        Map<WeaponVisualState, Integer> visualStates = new EnumMap<>(WeaponVisualState.class);
        visualStates.put(WeaponVisualState.IDLE, 201);
        visualStates.put(WeaponVisualState.AIMING, 1201);
        visualStates.put(WeaponVisualState.RELOADING, 3201);

        Map<String, Integer> idleVariants = Map.of(
                "empty", 201,
                "magazine", 203,
                "color", 9201,
                "magazine-color", 9203,
                "led-anime-color", 14071,
                "magazine-led-anime-color", 14073,
                "skin-gold-reserve", 9221,
                "magazine-skin-gold-reserve", 9223,
                "skin-sugarline-bakery", 9241,
                "magazine-skin-sugarline-bakery", 9243);
        Map<WeaponVisualState, Map<String, Integer>> variants = new EnumMap<>(WeaponVisualState.class);
        variants.put(WeaponVisualState.IDLE, idleVariants);
        variants.put(WeaponVisualState.AIMING, Map.of(
                "empty", 1201,
                "magazine", 1203,
                "color", 11201,
                "magazine-color", 11203,
                "led-anime-color", 15071,
                "magazine-led-anime-color", 15073,
                "skin-sugarline-bakery", 10241,
                "magazine-skin-sugarline-bakery", 10243));
        variants.put(WeaponVisualState.RELOADING, Map.of(
                "empty", 3201,
                "magazine", 3203,
                "color", 13201,
                "magazine-color", 13203,
                "led-anime-color", 16071,
                "magazine-led-anime-color", 16073,
                "skin-sugarline-bakery", 12241,
                "magazine-skin-sugarline-bakery", 12243));

        WeaponDefinition ak47 = new WeaponDefinition("ak_47", "AK-47", WeaponCategory.ASSAULT_RIFLE,
                Material.CROSSBOW, 201, visualStates, variants, 2,
                7.0, 2.0, 4, 60, 30, 78,
                "762nato", "shoot", "reload", true,
                List.of(), null, 0.018, 1,
                4.0, 0.45, 1.75, 0.75, 4.0, 55, 100, 0.65);

        Assert.assertEquals(9241, ak47.resolveVisualModelData(WeaponVisualState.IDLE,
                WeaponVisualVariantResolver.candidates(false, false, false, "none", "color", "sugarline-bakery"), false));
        Assert.assertEquals(9243, ak47.resolveVisualModelData(WeaponVisualState.IDLE,
                WeaponVisualVariantResolver.candidates(false, true, false, "none", "color", "sugarline-bakery"), true));
        Assert.assertEquals(9203, ak47.resolveVisualModelData(WeaponVisualState.IDLE,
                WeaponVisualVariantResolver.candidates(false, true, false, "none", "color", "none"), true));
        Assert.assertEquals(11203, ak47.resolveVisualModelData(WeaponVisualState.AIMING,
                WeaponVisualVariantResolver.candidates(false, true, false, "none", "color", "none"), true));
        Assert.assertEquals(13201, ak47.resolveVisualModelData(WeaponVisualState.RELOADING,
                WeaponVisualVariantResolver.candidates(false, false, false, "none", "color", "none"), false));
        Assert.assertEquals(14073, ak47.resolveVisualModelData(WeaponVisualState.IDLE,
                WeaponVisualVariantResolver.candidates(false, true, false, "anime", "color", "none"), true));
        Assert.assertEquals(15071, ak47.resolveVisualModelData(WeaponVisualState.AIMING,
                WeaponVisualVariantResolver.candidates(false, false, false, "anime", "color", "none"), false));
        Assert.assertEquals(16073, ak47.resolveVisualModelData(WeaponVisualState.RELOADING,
                WeaponVisualVariantResolver.candidates(false, true, false, "anime", "color", "none"), true));
    }

    @Test
    public void ppkColorAndSkinVariantsResolveBeforeBase() {
        Map<WeaponVisualState, Integer> visualStates = new EnumMap<>(WeaponVisualState.class);
        visualStates.put(WeaponVisualState.IDLE, 5301);
        visualStates.put(WeaponVisualState.AIMING, 6301);

        Map<WeaponVisualState, Map<String, Integer>> variants = new EnumMap<>(WeaponVisualState.class);
        variants.put(WeaponVisualState.IDLE, Map.ofEntries(
                Map.entry("empty", 5301),
                Map.entry("grip", 5304),
                Map.entry("optic", 5309),
                Map.entry("optic-grip", 5312),
                Map.entry("color", 9321),
                Map.entry("grip-color", 9324),
                Map.entry("optic-color", 9329),
                Map.entry("optic-grip-color", 9332),
                Map.entry("skin-gold-reserve", 9341),
                Map.entry("grip-skin-gold-reserve", 9344),
                Map.entry("optic-skin-gold-reserve", 9349),
                Map.entry("optic-grip-skin-gold-reserve", 9352),
                Map.entry("skin-sugarline-bakery", 9361),
                Map.entry("grip-skin-sugarline-bakery", 9364),
                Map.entry("optic-skin-sugarline-bakery", 9369),
                Map.entry("optic-grip-skin-sugarline-bakery", 9372)));
        variants.put(WeaponVisualState.AIMING, Map.of(
                "empty", 6301,
                "color", 10321,
                "skin-gold-reserve", 10341,
                "skin-sugarline-bakery", 10361));

        WeaponDefinition ppk = new WeaponDefinition("ppk", "PPK", WeaponCategory.PISTOL,
                Material.CROSSBOW, 5301, visualStates, variants, 2,
                4.0, 1.3, 3, 40, 12, 55,
                "9mm", "shoot", "reload", true,
                List.of(), null, 0.01, 1,
                3.0, 0.35, 1.4, 0.5, 3.0, 40, 80, 0.5);

        Assert.assertEquals(9321, ppk.resolveVisualModelData(WeaponVisualState.IDLE,
                WeaponVisualVariantResolver.candidates(false, false, false, "none", "color", "none"), false));
        Assert.assertEquals(9332, ppk.resolveVisualModelData(WeaponVisualState.IDLE,
                WeaponVisualVariantResolver.candidates(true, false, true, "none", "color", "none"), false));
        Assert.assertEquals(9372, ppk.resolveVisualModelData(WeaponVisualState.IDLE,
                WeaponVisualVariantResolver.candidates(true, false, true, "none", "color", "sugarline-bakery"), false));
        Assert.assertEquals(10341, ppk.resolveVisualModelData(WeaponVisualState.AIMING,
                WeaponVisualVariantResolver.candidates(false, false, false, "none", "color", "gold-reserve"), false));
    }

    @Test
    public void mp5ColorAndSkinVariantsResolveAcrossStates() {
        Map<WeaponVisualState, Integer> visualStates = new EnumMap<>(WeaponVisualState.class);
        visualStates.put(WeaponVisualState.IDLE, 81);
        visualStates.put(WeaponVisualState.AIMING, 1081);
        visualStates.put(WeaponVisualState.RELOADING, 3081);

        Map<WeaponVisualState, Map<String, Integer>> variants = new EnumMap<>(WeaponVisualState.class);
        variants.put(WeaponVisualState.IDLE, Map.ofEntries(
                Map.entry("empty", 81),
                Map.entry("magazine", 83),
                Map.entry("optic-magazine-grip", 95),
                Map.entry("color", 9401),
                Map.entry("magazine-color", 9403),
                Map.entry("optic-magazine-grip-color", 9415),
                Map.entry("skin-gold-reserve", 9421),
                Map.entry("magazine-skin-gold-reserve", 9423),
                Map.entry("optic-magazine-grip-skin-gold-reserve", 9435),
                Map.entry("skin-sugarline-bakery", 9441),
                Map.entry("magazine-skin-sugarline-bakery", 9443),
                Map.entry("optic-magazine-grip-skin-sugarline-bakery", 9455)));
        variants.put(WeaponVisualState.AIMING, Map.ofEntries(
                Map.entry("empty", 1081),
                Map.entry("color", 10401),
                Map.entry("optic-magazine-grip-color", 10415),
                Map.entry("skin-gold-reserve", 10421),
                Map.entry("skin-sugarline-bakery", 10441),
                Map.entry("optic-magazine-grip-skin-sugarline-bakery", 10455)));
        variants.put(WeaponVisualState.RELOADING, Map.ofEntries(
                Map.entry("empty", 3081),
                Map.entry("color", 12401),
                Map.entry("optic-magazine-grip-color", 12415),
                Map.entry("skin-gold-reserve", 12421),
                Map.entry("optic-magazine-grip-skin-gold-reserve", 12435),
                Map.entry("skin-sugarline-bakery", 12441)));

        WeaponDefinition mp5 = new WeaponDefinition("mp5", "HK MP5", WeaponCategory.SMG,
                Material.CROSSBOW, 81, visualStates, variants, 2,
                4.2, 1.7, 2, 45, 30, 50,
                "9mm", "shoot", "reload", true,
                List.of(), null, 0.008, 1,
                4.0, 0.45, 1.75, 0.75, 4.0, 25, 60, 0.55);

        Assert.assertEquals(9401, mp5.resolveVisualModelData(WeaponVisualState.IDLE,
                WeaponVisualVariantResolver.candidates(false, false, false, "none", "color", "none"), false));
        Assert.assertEquals(9415, mp5.resolveVisualModelData(WeaponVisualState.IDLE,
                WeaponVisualVariantResolver.candidates(true, true, true, "none", "color", "none"), true));
        Assert.assertEquals(9455, mp5.resolveVisualModelData(WeaponVisualState.IDLE,
                WeaponVisualVariantResolver.candidates(true, true, true, "none", "color", "sugarline-bakery"), true));
        Assert.assertEquals(10455, mp5.resolveVisualModelData(WeaponVisualState.AIMING,
                WeaponVisualVariantResolver.candidates(true, true, true, "none", "color", "sugarline-bakery"), true));
        Assert.assertEquals(12435, mp5.resolveVisualModelData(WeaponVisualState.RELOADING,
                WeaponVisualVariantResolver.candidates(true, true, true, "none", "color", "gold-reserve"), true));
    }

    @Test
    public void remingtonColorAndSkinVariantsResolveAcrossStates() {
        Map<WeaponVisualState, Integer> visualStates = new EnumMap<>(WeaponVisualState.class);
        visualStates.put(WeaponVisualState.IDLE, 441);
        visualStates.put(WeaponVisualState.AIMING, 1441);
        visualStates.put(WeaponVisualState.RELOADING, 3441);

        Map<WeaponVisualState, Map<String, Integer>> variants = new EnumMap<>(WeaponVisualState.class);
        variants.put(WeaponVisualState.IDLE, Map.ofEntries(
                Map.entry("empty", 441),
                Map.entry("optic-grip", 452),
                Map.entry("color", 9461),
                Map.entry("optic-grip-color", 9472),
                Map.entry("skin-gold-reserve", 9481),
                Map.entry("optic-grip-skin-gold-reserve", 9492),
                Map.entry("skin-sugarline-bakery", 9501),
                Map.entry("optic-grip-skin-sugarline-bakery", 9512)));
        variants.put(WeaponVisualState.AIMING, Map.ofEntries(
                Map.entry("empty", 1441),
                Map.entry("optic-grip-color", 10472),
                Map.entry("skin-gold-reserve", 10481),
                Map.entry("optic-grip-skin-sugarline-bakery", 10512)));
        variants.put(WeaponVisualState.RELOADING, Map.ofEntries(
                Map.entry("empty", 3441),
                Map.entry("color", 12461),
                Map.entry("optic-grip-color", 12472),
                Map.entry("optic-grip-skin-gold-reserve", 12492),
                Map.entry("skin-sugarline-bakery", 12501)));

        WeaponDefinition remington = new WeaponDefinition("remington_870", "Remington 870", WeaponCategory.SHOTGUN,
                Material.CROSSBOW, 441, visualStates, variants, 0,
                16.0, 1.3, 20, 75, 6, 24,
                "12gauge", "shoot", "reload", false,
                List.of(), null, 0.026, 8,
                7.0, 4.5, 1.75, 0.75, 4.0, 8, 24, 0.3);

        Assert.assertEquals(9461, remington.resolveVisualModelData(WeaponVisualState.IDLE,
                WeaponVisualVariantResolver.candidates(false, false, false, "none", "color", "none"), false));
        Assert.assertEquals(9472, remington.resolveVisualModelData(WeaponVisualState.IDLE,
                WeaponVisualVariantResolver.candidates(true, false, true, "none", "color", "none"), false));
        Assert.assertEquals(9512, remington.resolveVisualModelData(WeaponVisualState.IDLE,
                WeaponVisualVariantResolver.candidates(true, false, true, "none", "color", "sugarline-bakery"), false));
        Assert.assertEquals(10512, remington.resolveVisualModelData(WeaponVisualState.AIMING,
                WeaponVisualVariantResolver.candidates(true, false, true, "none", "color", "sugarline-bakery"), false));
        Assert.assertEquals(12492, remington.resolveVisualModelData(WeaponVisualState.RELOADING,
                WeaponVisualVariantResolver.candidates(true, false, true, "none", "color", "gold-reserve"), false));
    }

    @Test
    public void ppkCosmeticConfigDisablesLedAndSharesSugarlineSounds() throws Exception {
        try (InputStream input = getClass().getClassLoader().getResourceAsStream("weapon_cosmetics.yml")) {
            Assert.assertNotNull(input);
            YamlConfiguration config = YamlConfiguration.loadConfiguration(
                    new InputStreamReader(input, StandardCharsets.UTF_8));

            Assert.assertTrue(config.getBoolean("weapons.ppk.enabled"));
            Assert.assertTrue(config.getBoolean("weapons.ppk.skin"));
            Assert.assertTrue(config.getBoolean("weapons.ppk.color"));
            Assert.assertFalse(config.getBoolean("weapons.ppk.led"));
            Assert.assertEquals("PPK Riserva Aurum", config.getString("skins.ppk.gold-reserve.display-name"));
            Assert.assertEquals("Riserva Aurum", config.getString("skins.ppk.gold-reserve.name-suffix"));
            Assert.assertEquals("PPK Pasticceria Sugarline", config.getString("skins.ppk.sugarline-bakery.display-name"));
            Assert.assertEquals("Pasticceria Sugarline", config.getString("skins.ppk.sugarline-bakery.name-suffix"));
            Assert.assertEquals("weapons.ak47.sugarline.fire",
                    config.getString("skins.ppk.sugarline-bakery.sound-fire"));
            Assert.assertEquals("weapons.ak47.sugarline.hit",
                    config.getString("skins.ppk.sugarline-bakery.sound-hit"));
            Assert.assertEquals("weapons.ak47.sugarline.headshot",
                    config.getString("skins.ppk.sugarline-bakery.sound-headshot"));
            Assert.assertEquals("weapons.ak47.sugarline.reload",
                    config.getString("skins.ppk.sugarline-bakery.sound-reload"));
        }
    }

    @Test
    public void m4a1CosmeticConfigEnablesRoyalMasqueradeSounds() throws Exception {
        try (InputStream input = getClass().getClassLoader().getResourceAsStream("weapon_cosmetics.yml")) {
            Assert.assertNotNull(input);
            YamlConfiguration config = YamlConfiguration.loadConfiguration(
                    new InputStreamReader(input, StandardCharsets.UTF_8));

            Assert.assertTrue(config.getBoolean("weapons.m4a1.enabled"));
            Assert.assertTrue(config.getBoolean("weapons.m4a1.skin"));
            Assert.assertTrue(config.getBoolean("weapons.m4a1.color"));
            Assert.assertTrue(config.getBoolean("weapons.m4a1.led"));
            Assert.assertEquals("M4A1 Riserva Aurum",
                    config.getString("skins.m4a1.gold-reserve.display-name"));
            Assert.assertEquals("M4A1 Mascherata Reale",
                    config.getString("skins.m4a1.royal-masquerade.display-name"));
            Assert.assertEquals("Mascherata Reale",
                    config.getString("skins.m4a1.royal-masquerade.name-suffix"));
            Assert.assertEquals("weapons.m4a1.royal.fire",
                    config.getString("skins.m4a1.royal-masquerade.sound-fire"));
            Assert.assertEquals("weapons.m4a1.royal.fire",
                    config.getString("skins.m4a1.royal-masquerade.sound-hit"));
            Assert.assertEquals("weapons.m4a1.royal.fire",
                    config.getString("skins.m4a1.royal-masquerade.sound-headshot"));
            Assert.assertEquals("weapons.m4a1.royal.reload",
                    config.getString("skins.m4a1.royal-masquerade.sound-reload"));
            Assert.assertNull(config.getString("skins.m4a1.royal-masquerade.sound-automatic"));
        }
    }

    @Test
    public void mp5CosmeticConfigDisablesLedAndSharesSugarlineSounds() throws Exception {
        try (InputStream input = getClass().getClassLoader().getResourceAsStream("weapon_cosmetics.yml")) {
            Assert.assertNotNull(input);
            YamlConfiguration config = YamlConfiguration.loadConfiguration(
                    new InputStreamReader(input, StandardCharsets.UTF_8));

            Assert.assertTrue(config.getBoolean("weapons.mp5.enabled"));
            Assert.assertTrue(config.getBoolean("weapons.mp5.skin"));
            Assert.assertTrue(config.getBoolean("weapons.mp5.color"));
            Assert.assertFalse(config.getBoolean("weapons.mp5.led"));
            Assert.assertEquals("MP5 Riserva Aurum", config.getString("skins.mp5.gold-reserve.display-name"));
            Assert.assertEquals("Riserva Aurum", config.getString("skins.mp5.gold-reserve.name-suffix"));
            Assert.assertEquals("MP5 Pasticceria Sugarline", config.getString("skins.mp5.sugarline-bakery.display-name"));
            Assert.assertEquals("Pasticceria Sugarline", config.getString("skins.mp5.sugarline-bakery.name-suffix"));
            Assert.assertEquals("weapons.ak47.sugarline.fire",
                    config.getString("skins.mp5.sugarline-bakery.sound-fire"));
            Assert.assertEquals("weapons.ak47.sugarline.hit",
                    config.getString("skins.mp5.sugarline-bakery.sound-hit"));
            Assert.assertEquals("weapons.ak47.sugarline.headshot",
                    config.getString("skins.mp5.sugarline-bakery.sound-headshot"));
            Assert.assertEquals("weapons.ak47.sugarline.reload",
                    config.getString("skins.mp5.sugarline-bakery.sound-reload"));
        }
    }

    @Test
    public void remingtonCosmeticConfigDisablesLedAndSharesSugarlineSounds() throws Exception {
        try (InputStream input = getClass().getClassLoader().getResourceAsStream("weapon_cosmetics.yml")) {
            Assert.assertNotNull(input);
            YamlConfiguration config = YamlConfiguration.loadConfiguration(
                    new InputStreamReader(input, StandardCharsets.UTF_8));

            Assert.assertTrue(config.getBoolean("weapons.remington_870.enabled"));
            Assert.assertTrue(config.getBoolean("weapons.remington_870.skin"));
            Assert.assertTrue(config.getBoolean("weapons.remington_870.color"));
            Assert.assertFalse(config.getBoolean("weapons.remington_870.led"));
            Assert.assertEquals("Remington 870 Riserva Aurum",
                    config.getString("skins.remington_870.gold-reserve.display-name"));
            Assert.assertEquals("Riserva Aurum",
                    config.getString("skins.remington_870.gold-reserve.name-suffix"));
            Assert.assertEquals("Remington 870 Pasticceria Sugarline",
                    config.getString("skins.remington_870.sugarline-bakery.display-name"));
            Assert.assertEquals("Pasticceria Sugarline",
                    config.getString("skins.remington_870.sugarline-bakery.name-suffix"));
            Assert.assertEquals("weapons.ak47.sugarline.fire",
                    config.getString("skins.remington_870.sugarline-bakery.sound-fire"));
            Assert.assertEquals("weapons.ak47.sugarline.hit",
                    config.getString("skins.remington_870.sugarline-bakery.sound-hit"));
            Assert.assertEquals("weapons.ak47.sugarline.headshot",
                    config.getString("skins.remington_870.sugarline-bakery.sound-headshot"));
            Assert.assertEquals("weapons.ak47.sugarline.reload",
                    config.getString("skins.remington_870.sugarline-bakery.sound-reload"));
        }
    }
}
