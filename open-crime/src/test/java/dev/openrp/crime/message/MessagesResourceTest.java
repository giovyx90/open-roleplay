package dev.openrp.crime.message;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.Test;

/**
 * Loads the shipped language files exactly as Bukkit will at runtime and asserts a representative set
 * of keys resolves. Notably guards {@code territory.yes}/{@code territory.no}: unquoted {@code yes:}
 * and {@code no:} keys would be coerced to YAML booleans and silently disappear.
 */
public class MessagesResourceTest {

    private YamlConfiguration load(String resource) throws Exception {
        try (InputStream in = getClass().getClassLoader().getResourceAsStream(resource)) {
            assertNotNull("resource missing: " + resource, in);
            return YamlConfiguration.loadConfiguration(new InputStreamReader(in, StandardCharsets.UTF_8));
        }
    }

    @Test
    public void bothLanguagesResolveKeyMessageKeys() throws Exception {
        List<String> keys = List.of(
                "general.no_capability", "syndicate.founded", "territory.claimed",
                "territory.yes", "territory.no", "production.started", "traffic.delivered",
                "laundering.started", "racket.imposed", "denuncia.filed", "informatore.done");
        for (String file : List.of("messages_it.yml", "messages_en.yml")) {
            YamlConfiguration yaml = load(file);
            for (String key : keys) {
                assertNotNull(file + " missing key: " + key, yaml.getString(key));
            }
        }
    }

    @Test
    public void yesNoAreStringsNotBooleans() throws Exception {
        YamlConfiguration it = load("messages_it.yml");
        assertEquals("si", it.getString("territory.yes"));
        assertEquals("no", it.getString("territory.no"));
    }
}
