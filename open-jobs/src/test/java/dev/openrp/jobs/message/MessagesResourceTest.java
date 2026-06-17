package dev.openrp.jobs.message;

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
 * of keys resolves in both languages. Also guards the {@code list.license_no} value: an unquoted
 * {@code no} would be coerced to a YAML boolean and silently disappear.
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
                "general.no_permission", "general.reload_done",
                "session.started", "session.ended", "session.not_delivered", "session.need_license",
                "progression.tier_up", "list.entry", "info.header", "status.body",
                "profile.entry", "license.entry", "admin.location_added", "admin.stats_job_body",
                "model.a_produzione", "model.a_consegna", "model.a_sessione");
        for (String file : List.of("messages_it.yml", "messages_en.yml")) {
            YamlConfiguration yaml = load(file);
            for (String key : keys) {
                assertNotNull(file + " missing key: " + key, yaml.getString(key));
            }
        }
    }

    @Test
    public void licenseNoIsStringNotBoolean() throws Exception {
        assertEquals("no", load("messages_it.yml").getString("list.license_no"));
        assertEquals("no", load("messages_en.yml").getString("list.license_no"));
    }
}
