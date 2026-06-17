package dev.openrp.fdo.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.logging.Logger;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.Before;
import org.junit.Test;
import dev.openrp.fdo.capability.Capability;

public class ActCatalogTest {

    private final ActCatalog catalog = new ActCatalog(Logger.getLogger("test"));

    @Before
    public void setUp() throws InvalidConfigurationException {
        String yaml = """
                acts:
                  arresto:
                    capability: ARREST
                    opens_dossier: true
                    starts_custody: true
                    custody_hours: default
                  fermo:
                    capability: DETAIN_TEMPORARY
                    custody_hours: 6
                  audit:
                    capability: ECONOMIC_AUDIT
                    requires_adapter: ECONOMY_AUDIT
                  broken:
                    capability: NOT_A_CAPABILITY
                """;
        YamlConfiguration config = new YamlConfiguration();
        config.loadFromString(yaml);
        catalog.load(config.getConfigurationSection("acts"));
    }

    @Test
    public void unknownCapabilityActsAreSkipped() {
        assertTrue(catalog.get("broken").isEmpty());
        assertEquals(3, catalog.all().size());
    }

    @Test
    public void defaultCustodyHoursResolvesToMinusOne() {
        assertEquals(-1, catalog.get("arresto").orElseThrow().custodyHours());
        assertEquals(6, catalog.get("fermo").orElseThrow().custodyHours());
    }

    @Test
    public void effectiveAdapterFallsBackToCapabilityRequirement() {
        ActDefinition audit = catalog.get("audit").orElseThrow();
        assertEquals("ECONOMY_AUDIT", audit.effectiveRequiredAdapter());
        assertEquals(Capability.ECONOMIC_AUDIT, audit.capability());
    }

    @Test
    public void selfContainedActsRequireNoAdapter() {
        assertFalse(catalog.get("arresto").orElseThrow().effectiveRequiredAdapter() != null);
    }
}
