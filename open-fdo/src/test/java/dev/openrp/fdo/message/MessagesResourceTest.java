package dev.openrp.fdo.message;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.Test;

/**
 * Guards the bundled message bundles. Notably {@code duty.on}/{@code duty.off}: unquoted {@code on}
 * and {@code off} are coerced to booleans by SnakeYAML (YAML 1.1), which would silently move them to
 * {@code duty.true}/{@code duty.false} and make the command show the raw key. The keys must stay
 * reachable as strings in both languages.
 */
public class MessagesResourceTest {

    private YamlConfiguration load(String resource) throws Exception {
        YamlConfiguration config = new YamlConfiguration();
        try (InputStream in = getClass().getResourceAsStream(resource)) {
            assertNotNull("missing bundled resource " + resource, in);
            config.load(new InputStreamReader(in, StandardCharsets.UTF_8));
        }
        return config;
    }

    @Test
    public void dutyOnOffKeysResolveInEnglish() throws Exception {
        YamlConfiguration en = load("/messages_en.yml");
        assertEquals("on duty", en.getString("duty.on"));
        assertEquals("off duty", en.getString("duty.off"));
    }

    @Test
    public void dutyOnOffKeysResolveInItalian() throws Exception {
        YamlConfiguration it = load("/messages_it.yml");
        assertEquals("in servizio", it.getString("duty.on"));
        assertEquals("fuori servizio", it.getString("duty.off"));
    }

    @Test
    public void coreKeysPresentInBothLanguages() throws Exception {
        for (String resource : new String[] {"/messages_en.yml", "/messages_it.yml"}) {
            YamlConfiguration config = load(resource);
            for (String key : new String[] {"act.produced", "dossier.opened", "verdict.signed",
                    "wanted.flagged", "detention.begun", "general.no_capability"}) {
                assertNotNull(resource + " missing " + key, config.getString(key));
            }
        }
    }
}
