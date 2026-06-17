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
import dev.openrp.crime.capability.Capability;
import dev.openrp.crime.config.Hierarchy;

public class OrgManagerTest {

    private OrgManager orgs;
    private Hierarchy hierarchy;
    private AdapterRegistry adapters;

    @Before
    public void setUp() throws InvalidConfigurationException {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.loadFromString(String.join("\n",
                "hierarchy:",
                "  - id: soldato",
                "    order: 0",
                "    capabilities: [PRODUCE]",
                "  - id: capo",
                "    order: 1",
                "    capabilities: [INVITE, PROMOTE, DEMOTE, EXPEL]",
                "  - id: boss",
                "    order: 2",
                "    apical: true",
                "    capabilities: [ALL]",
                "founding:",
                "  min_members: 1"));
        hierarchy = new Hierarchy();
        hierarchy.load(yaml);
        adapters = new AdapterRegistry();
        adapters.setStorage(new MemoryStorageAdapter());
        orgs = new OrgManager(hierarchy, adapters);
        orgs.loadAll();
    }

    @Test
    public void foundsWithFounderAsApical() {
        UUID founder = UUID.randomUUID();
        CrimeResult result = orgs.found(founder, "Tony", "Cosa Nostra", "famiglia", Map.of());
        assertTrue(result.success());
        assertTrue(orgs.byMember(founder).isPresent());
        assertTrue(orgs.isApical(founder));
        // The apical rank holds ALL, so every capability passes.
        assertTrue(orgs.has(founder, Capability.DISSOLVE));
        assertTrue(orgs.has(founder, Capability.LAUNDER));
    }

    @Test
    public void rejectsDuplicateNameAndDoubleMembership() {
        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();
        assertTrue(orgs.found(a, "A", "Family", "", Map.of()).success());
        assertFalse("duplicate name", orgs.found(b, "B", "family", "", Map.of()).success());
        assertFalse("already a member", orgs.found(a, "A", "Other", "", Map.of()).success());
    }

    @Test
    public void inviteAcceptAndCapabilityInheritance() {
        UUID founder = UUID.randomUUID();
        UUID recruit = UUID.randomUUID();
        orgs.found(founder, "Boss", "Clan", "", Map.of());

        assertTrue(orgs.invite(founder, recruit, "Rookie").success());
        assertTrue(orgs.accept(recruit, "Rookie").success());
        assertTrue(orgs.isMember(recruit, orgs.byMember(recruit).orElseThrow().id()));

        // recruit joins at the base rank (soldato): has PRODUCE, not INVITE.
        assertTrue(orgs.has(recruit, Capability.PRODUCE));
        assertFalse(orgs.has(recruit, Capability.INVITE));

        // promote to capo: inherits PRODUCE from below and gains INVITE.
        assertTrue(orgs.promote(founder, recruit, true).success());
        assertTrue(orgs.has(recruit, Capability.PRODUCE));
        assertTrue(orgs.has(recruit, Capability.INVITE));
        assertFalse(orgs.has(recruit, Capability.DISSOLVE));
    }

    @Test
    public void dissolveRemovesMembership() {
        UUID founder = UUID.randomUUID();
        orgs.found(founder, "Boss", "Clan", "", Map.of());
        assertTrue(orgs.dissolve(founder).success());
        assertFalse(orgs.byMember(founder).isPresent());
    }

    @Test
    public void cannotExpelFounder() {
        UUID founder = UUID.randomUUID();
        orgs.found(founder, "Boss", "Clan", "", Map.of());
        assertFalse(orgs.expel(founder, founder).success());
    }

    @Test
    public void dissolveSurvivesReloadAndFreesMembers() {
        UUID founder = UUID.randomUUID();
        orgs.found(founder, "Boss", "Clan", "", Map.of());
        assertTrue(orgs.dissolve(founder).success());

        // Simulate a server restart / reload: a fresh manager over the same storage must NOT re-trap
        // the founder by re-indexing the dissolved org's roster.
        OrgManager reloaded = new OrgManager(hierarchy, adapters);
        reloaded.loadAll();
        assertFalse(reloaded.byMember(founder).isPresent());
        assertTrue(reloaded.found(founder, "Boss", "New Clan", "", Map.of()).success());
    }
}
