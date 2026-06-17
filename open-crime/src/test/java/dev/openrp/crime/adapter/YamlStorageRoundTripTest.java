package dev.openrp.crime.adapter;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;
import org.junit.Before;
import org.junit.Test;
import dev.openrp.crime.adapter.defaults.YamlStorageAdapter;
import dev.openrp.crime.model.CrimeEvent;
import dev.openrp.crime.model.CrimeEventType;
import dev.openrp.crime.model.Discovery;
import dev.openrp.crime.model.DiscoveryType;
import dev.openrp.crime.model.IllegalOrg;
import dev.openrp.crime.model.OrgMember;
import dev.openrp.crime.model.Territory;

public class YamlStorageRoundTripTest {

    private File file;
    private final Logger logger = Logger.getAnonymousLogger();

    @Before
    public void setUp() throws Exception {
        file = File.createTempFile("opencrime-data", ".yml");
        file.delete();
        file.deleteOnExit();
    }

    @Test
    public void orgRoundTrip() {
        UUID founder = UUID.randomUUID();
        UUID member = UUID.randomUUID();
        UUID treasury = UUID.randomUUID();

        YamlStorageAdapter out = new YamlStorageAdapter(file, logger);
        out.init();
        IllegalOrg org = new IllegalOrg("clan", "Cosa Nostra", "famiglia", founder, 1000L, treasury);
        org.addMember(founder, "Tony", "boss", 1000L);
        OrgMember soldier = org.addMember(member, "Vito", "soldato", 2000L);
        soldier.setInformant(true);
        org.addTerritory("region1");
        out.saveOrg(org);
        out.close();

        YamlStorageAdapter in = new YamlStorageAdapter(file, logger);
        in.init();
        List<IllegalOrg> loaded = List.copyOf(in.loadOrgs());
        assertEquals(1, loaded.size());
        IllegalOrg back = loaded.get(0);
        assertEquals("Cosa Nostra", back.name());
        assertEquals(treasury, back.treasury());
        assertEquals(2, back.memberCount());
        assertTrue(back.controls("region1"));
        assertTrue(back.member(member).orElseThrow().isInformant());
        assertEquals("boss", back.member(founder).orElseThrow().roleId());
    }

    @Test
    public void eventAndDiscoveryRoundTrip() {
        YamlStorageAdapter out = new YamlStorageAdapter(file, logger);
        out.init();
        CrimeEvent event = new CrimeEvent("evt1", CrimeEventType.TRAFFIC, "clan",
                List.of(UUID.randomUUID()), List.of("item-uuid-1"), "world", 10, 64, 20, 5000L, "DOSS-1");
        out.saveEvent(event);
        Discovery discovery = new Discovery("disc1", "evt1", DiscoveryType.ARRESTO, UUID.randomUUID(),
                6000L, "world", 10, 64, 20, "DOSS-1");
        out.saveDiscovery(discovery);
        out.close();

        YamlStorageAdapter in = new YamlStorageAdapter(file, logger);
        in.init();
        List<CrimeEvent> events = List.copyOf(in.loadEvents());
        assertEquals(1, events.size());
        assertEquals("DOSS-1", events.get(0).dossierId());
        assertEquals(CrimeEventType.TRAFFIC, events.get(0).type());
        assertEquals(List.of("item-uuid-1"), events.get(0).goodItemUuids());

        List<Discovery> discoveries = List.copyOf(in.loadDiscoveries());
        assertEquals(1, discoveries.size());
        assertEquals(DiscoveryType.ARRESTO, discoveries.get(0).type());
        assertEquals("evt1", discoveries.get(0).crimeEventId());
    }

    @Test
    public void regionKeysWithDotsDoNotCollide() {
        YamlStorageAdapter out = new YamlStorageAdapter(file, logger);
        out.init();
        // These two region ids differ only by '.' vs '_' - with lossy escaping they would collide.
        out.saveTerritory(new Territory("w.a@0,0", "clan", false, 1L));
        out.saveTerritory(new Territory("w_a@0,0", "clan", false, 2L));
        out.close();

        YamlStorageAdapter in = new YamlStorageAdapter(file, logger);
        in.init();
        List<String> ids = in.loadTerritories().stream().map(Territory::regionId).sorted().toList();
        assertEquals(List.of("w.a@0,0", "w_a@0,0"), ids);
    }

    @Test
    public void treasuryRoundTrip() {
        YamlStorageAdapter out = new YamlStorageAdapter(file, logger);
        out.init();
        UUID treasury = UUID.randomUUID();
        out.saveTreasury(treasury, 1500L, 800L);
        out.close();

        YamlStorageAdapter in = new YamlStorageAdapter(file, logger);
        in.init();
        Map<UUID, long[]> balances = in.loadTreasuries();
        assertTrue(balances.containsKey(treasury));
        assertArrayEquals(new long[]{1500L, 800L}, balances.get(treasury));
    }
}
