package dev.openrp.access.storage;

import dev.openrp.access.model.AccessAction;
import dev.openrp.access.model.AccessPreset;
import dev.openrp.access.model.AccessPrincipal;
import dev.openrp.access.model.AccessProfile;
import dev.openrp.access.model.AccessProfileType;
import dev.openrp.access.model.AccessRule;
import dev.openrp.access.model.AccessRuleScope;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class SqliteAccessStorageTest {
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void createsTablesAndLoadsSavedProfile() throws Exception {
        try (SqliteAccessStorage storage = storage()) {
            storage.createTables();
            AccessProfile profile = profile();

            storage.saveProfile(profile);

            List<AccessProfile> loaded = storage.loadProfiles();
            assertEquals(1, loaded.size());
            assertEquals(profile.getId(), loaded.get(0).getId());
            assertEquals(profile.getRegionId(), loaded.get(0).getRegionId());
            assertEquals(profile.getDefaultPreset(), loaded.get(0).getDefaultPreset());
        }
    }

    @Test
    public void savesLoadsAndDeletesBlockRules() throws Exception {
        try (SqliteAccessStorage storage = storage()) {
            storage.createTables();
            AccessProfile profile = profile();
            storage.saveProfile(profile);
            AccessRule blockRule = new AccessRule("rule-block", profile.getId(), AccessRuleScope.BLOCK,
                    "world", 10, 64, 10, AccessPrincipal.publicAccess(),
                    EnumSet.of(AccessAction.CONTAINER), true, Instant.now());

            storage.saveRule(blockRule);

            assertEquals(1, storage.loadRules().size());
            storage.deleteBlockRules(profile.getId(), "world", 10, 64, 10);
            assertTrue(storage.loadRules().isEmpty());
        }
    }

    @Test
    public void insertsAuditLogs() throws Exception {
        try (SqliteAccessStorage storage = storage()) {
            storage.createTables();
            AccessProfile profile = profile();
            storage.saveProfile(profile);

            storage.audit(profile.getId(), "TEST_ACTION", UUID.randomUUID(), "Tester",
                    "world", 1, 2, 3, "{\"ok\":true}");

            assertEquals(1L, storage.countAuditLogs());
        }
    }

    private SqliteAccessStorage storage() throws Exception {
        File database = temporaryFolder.newFile("open_access.db");
        return new SqliteAccessStorage(database);
    }

    private AccessProfile profile() {
        return new AccessProfile("profile-test", AccessProfileType.PROPERTY, "world", "home",
                UUID.randomUUID(), "Tester", null, "Casa Tester",
                AccessPreset.PRIVATE, true, Instant.now(), Instant.now());
    }
}
