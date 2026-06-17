package dev.openrp.crime.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.Test;
import dev.openrp.crime.capability.Capability;

public class HierarchyTest {

    private Hierarchy load() throws InvalidConfigurationException {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.loadFromString(String.join("\n",
                "hierarchy:",
                "  - id: soldato",
                "    display_name: Soldato",
                "    order: 0",
                "    capabilities: [PRODUCE]",
                "  - id: capo",
                "    order: 1",
                "    capabilities: [INVITE, EXPEL]",
                "  - id: boss",
                "    order: 2",
                "    apical: true",
                "    capabilities: [ALL]",
                "founding:",
                "  min_members: 2"));
        Hierarchy hierarchy = new Hierarchy();
        hierarchy.load(yaml);
        return hierarchy;
    }

    @Test
    public void ordersAndResolvesRanks() throws InvalidConfigurationException {
        Hierarchy hierarchy = load();
        assertEquals(3, hierarchy.ranks().size());
        assertEquals("soldato", hierarchy.defaultRank().orElseThrow().id());
        assertEquals("boss", hierarchy.apicalRank().orElseThrow().id());
        assertEquals("capo", hierarchy.nextRank("soldato").orElseThrow().id());
        assertEquals("capo", hierarchy.previousRank("boss").orElseThrow().id());
        assertEquals(2, hierarchy.minMembers());
    }

    @Test
    public void capabilitiesAndWildcard() throws InvalidConfigurationException {
        Hierarchy hierarchy = load();
        assertTrue(hierarchy.rank("soldato").orElseThrow().has(Capability.PRODUCE));
        assertFalse(hierarchy.rank("soldato").orElseThrow().has(Capability.INVITE));
        assertTrue(hierarchy.rank("capo").orElseThrow().has(Capability.INVITE));
        // ALL is a wildcard: the apical rank passes every check.
        assertTrue(hierarchy.rank("boss").orElseThrow().has(Capability.DISSOLVE));
        assertTrue(hierarchy.rank("boss").orElseThrow().has(Capability.LAUNDER));
    }
}
