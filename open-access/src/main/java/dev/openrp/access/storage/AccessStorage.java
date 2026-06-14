package dev.openrp.access.storage;

import dev.openrp.access.model.AccessProfile;
import dev.openrp.access.model.AccessRule;

import java.util.List;
import java.util.UUID;

public interface AccessStorage extends AutoCloseable {
    void createTables();

    List<AccessProfile> loadProfiles();

    List<AccessRule> loadRules();

    void saveProfile(AccessProfile profile);

    void deleteProfile(String profileId);

    void saveRule(AccessRule rule);

    void deleteBlockRules(String profileId, String world, int x, int y, int z);

    void deletePlayerRules(String profileId, String playerUuid);

    void audit(String profileId, String action, UUID actorUuid, String actorName,
               String world, Integer x, Integer y, Integer z, String detailsJson);

    long countAuditLogs();

    @Override
    void close();
}
