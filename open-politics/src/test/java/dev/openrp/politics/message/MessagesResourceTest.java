package dev.openrp.politics.message;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.TreeSet;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.Test;

/** Guards that the two bundled message files parse and stay in sync (no missing translation). */
public class MessagesResourceTest {

    private YamlConfiguration load(String resource) throws Exception {
        try (InputStream stream = getClass().getResourceAsStream(resource)) {
            assertNotNull("missing resource " + resource, stream);
            return YamlConfiguration.loadConfiguration(new InputStreamReader(stream, StandardCharsets.UTF_8));
        }
    }

    @Test
    public void englishAndItalianHaveTheSameKeys() throws Exception {
        Set<String> en = new TreeSet<>(load("/messages_en.yml").getKeys(true));
        Set<String> it = new TreeSet<>(load("/messages_it.yml").getKeys(true));
        assertEquals("the two message files must define the same keys", en, it);
        assertTrue("a few core keys must exist", en.contains("general.no_capability"));
        assertTrue(en.contains("law.promulgated"));
        assertTrue(en.contains("election.announce_results"));
    }
}
