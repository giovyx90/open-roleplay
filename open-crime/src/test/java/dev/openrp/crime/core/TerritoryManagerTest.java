package dev.openrp.crime.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Map;
import java.util.UUID;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.Before;
import org.junit.Test;
import dev.openrp.crime.adapter.AdapterRegistry;
import dev.openrp.crime.adapter.defaults.MemoryStorageAdapter;
import dev.openrp.crime.config.Hierarchy;

public class TerritoryManagerTest {

    private OrgManager orgs;
    private TerritoryManager territories;

    @Before
    public void setUp() throws InvalidConfigurationException {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.loadFromString(String.join("\n",
                "hierarchy:",
                "  - id: boss",
                "    order: 0",
                "    apical: true",
                "    capabilities: [ALL]",
                "founding:",
                "  min_members: 1"));
        Hierarchy hierarchy = new Hierarchy();
        hierarchy.load(yaml);
        AdapterRegistry adapters = new AdapterRegistry();
        adapters.setStorage(new MemoryStorageAdapter());
        orgs = new OrgManager(hierarchy, adapters);
        orgs.loadAll();
        territories = new TerritoryManager(adapters, orgs);
        territories.loadAll();
    }

    @Test
    public void claimUncontrolledThenContest() {
        UUID bossA = UUID.randomUUID();
        UUID bossB = UUID.randomUUID();
        orgs.found(bossA, "A", "ClanA", "", Map.of());
        orgs.found(bossB, "B", "ClanB", "", Map.of());
        String orgA = orgs.byMember(bossA).orElseThrow().id();

        assertTrue(territories.claim(bossA, "region1").success());
        assertEquals(orgA, territories.controller("region1").orElseThrow().id());
        assertFalse(territories.isContested("region1"));

        // B claims A's region -> contested, no transfer.
        assertTrue(territories.claim(bossB, "region1").success());
        assertTrue(territories.isContested("region1"));
        assertEquals(orgA, territories.controller("region1").orElseThrow().id());
    }

    @Test
    public void abandonReleasesRegion() {
        UUID boss = UUID.randomUUID();
        orgs.found(boss, "A", "ClanA", "", Map.of());
        territories.claim(boss, "region1");
        assertTrue(territories.abandon(boss, "region1").success());
        assertFalse(territories.controller("region1").isPresent());
    }

    @Test
    public void cannotClaimWithoutOrg() {
        assertFalse(territories.claim(UUID.randomUUID(), "region1").success());
    }

    @Test
    public void dissolvedOrgFreesItsTerritory() {
        UUID bossA = UUID.randomUUID();
        UUID bossB = UUID.randomUUID();
        orgs.found(bossA, "A", "ClanA", "", Map.of());
        orgs.found(bossB, "B", "ClanB", "", Map.of());
        territories.claim(bossA, "region1");
        orgs.dissolve(bossA);

        // A dead org cannot lock a region: B claims it cleanly (not contested), no longer controlled by A.
        assertTrue(territories.claim(bossB, "region1").success());
        assertFalse(territories.isContested("region1"));
        assertEquals(orgs.byMember(bossB).orElseThrow().id(),
                territories.controller("region1").orElseThrow().id());
    }
}
