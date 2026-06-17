package dev.openrp.fdo.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Set;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.Before;
import org.junit.Test;
import dev.openrp.fdo.capability.Capability;

public class RankRegistryTest {

    private final RankRegistry registry = new RankRegistry();

    @Before
    public void setUp() throws InvalidConfigurationException {
        String yaml = """
                ranks:
                  polizia:
                    - id: agente
                      order: 0
                      capabilities: [ARREST, SEIZE_EVIDENCE]
                    - id: ispettore
                      order: 3
                      capabilities: [REQUEST_WARRANT]
                    - id: questore
                      order: 5
                      apical: true
                      capabilities: [FLAG_WANTED]
                """;
        YamlConfiguration config = new YamlConfiguration();
        config.loadFromString(yaml);
        registry.load(config.getConfigurationSection("ranks"));
    }

    @Test
    public void lowestRankHasOnlyItsOwnCapabilities() {
        Set<Capability> caps = registry.capabilitiesFor("polizia", "agente");
        assertEquals(Set.of(Capability.ARREST, Capability.SEIZE_EVIDENCE), caps);
    }

    @Test
    public void higherRankInheritsLowerCapabilities() {
        Set<Capability> caps = registry.capabilitiesFor("polizia", "questore");
        assertTrue(caps.contains(Capability.ARREST));
        assertTrue(caps.contains(Capability.REQUEST_WARRANT));
        assertTrue(caps.contains(Capability.FLAG_WANTED));
    }

    @Test
    public void middleRankDoesNotHaveHigherCapabilities() {
        assertTrue(registry.has("polizia", "ispettore", Capability.REQUEST_WARRANT));
        assertFalse(registry.has("polizia", "ispettore", Capability.FLAG_WANTED));
    }

    @Test
    public void nextAndPreviousWalkTheChain() {
        assertEquals("ispettore", registry.next("polizia", "agente").orElseThrow().id());
        assertEquals("agente", registry.previous("polizia", "ispettore").orElseThrow().id());
        assertTrue(registry.next("polizia", "questore").isEmpty());
        assertTrue(registry.previous("polizia", "agente").isEmpty());
    }

    @Test
    public void apicalFlagIsParsed() {
        assertTrue(registry.rank("polizia", "questore").orElseThrow().apical());
        assertFalse(registry.rank("polizia", "agente").orElseThrow().apical());
    }

    @Test
    public void unknownCorpsYieldsNoCapabilities() {
        assertTrue(registry.capabilitiesFor("ghost", "x").isEmpty());
    }
}
