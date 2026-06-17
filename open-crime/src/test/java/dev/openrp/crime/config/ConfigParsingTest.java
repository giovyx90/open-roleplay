package dev.openrp.crime.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.Test;

public class ConfigParsingTest {

    @Test
    public void goodsCatalogParsesSetting() throws InvalidConfigurationException {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.loadFromString(String.join("\n",
                "goods:",
                "  cocaina:",
                "    display_name: Cocaina",
                "    category: narcotic",
                "    danger_level: 4",
                "    street_value: 800",
                "    item_material: SUGAR"));
        GoodsCatalog catalog = new GoodsCatalog();
        catalog.load(yaml.getConfigurationSection("goods"));
        assertTrue(catalog.exists("cocaina"));
        Good good = catalog.get("cocaina").orElseThrow();
        assertEquals("Cocaina", good.displayName());
        assertEquals(4, good.dangerLevel());
        assertEquals(800L, good.streetValue());
    }

    @Test
    public void settingsTimeScaleConvertsDurations() throws InvalidConfigurationException {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.loadFromString(String.join("\n",
                "time:",
                "  scale: 0.5",
                "discovery:",
                "  denuncia_event_window: 10",
                "modules:",
                "  racket: false"));
        CrimeSettings settings = new CrimeSettings();
        settings.load(yaml);
        assertEquals(0.5, settings.timeScale(), 0.0001);
        // 60 minutes at scale 0.5 -> 30 real minutes -> 1_800_000 ms.
        assertEquals(1_800_000L, settings.realMillisFromHours(1));
        assertEquals(600_000L, settings.denunciaEventWindowMillis());
        assertFalse(settings.moduleRacket());
        assertTrue(settings.moduleSyndicate());
    }

    @Test
    public void launderingLossIsApplied() {
        LaunderingMethod method = new LaunderingMethod("m", "Method", "", 20, 50000, 15, 24);
        assertEquals(800L, method.cleanFrom(1000L));
        assertEquals(0L, method.cleanFrom(0L));
    }
}
