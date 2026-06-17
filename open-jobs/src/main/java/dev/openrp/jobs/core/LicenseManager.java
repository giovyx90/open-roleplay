package dev.openrp.jobs.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.entity.Player;
import dev.openrp.jobs.OpenJobsPlugin;
import dev.openrp.jobs.config.Job;
import dev.openrp.jobs.model.LicenseStatus;
import dev.openrp.jobs.model.WorkLicense;

/**
 * Issues, revokes and looks up professional licences - one per (player, job). The DB record is
 * authoritative: a revoked licence blocks new sessions even if the item is still held, and a lost item
 * can be reissued because the record stands. With an Identity adapter the licence is also a physical
 * NBT document that carries the live progression tier.
 */
public final class LicenseManager {

    private final OpenJobsPlugin plugin;
    private final java.util.Map<String, WorkLicense> byKey = new ConcurrentHashMap<>();

    public LicenseManager(OpenJobsPlugin plugin) {
        this.plugin = plugin;
    }

    private static String key(UUID player, String jobId) {
        return player + "|" + jobId;
    }

    public void loadAll() {
        byKey.clear();
        for (WorkLicense license : plugin.adapters().storage().loadLicenses()) {
            byKey.put(key(license.player(), license.jobId()), license);
        }
    }

    public Optional<WorkLicense> get(UUID player, String jobId) {
        return Optional.ofNullable(byKey.get(key(player, jobId)));
    }

    public boolean hasActive(UUID player, String jobId) {
        WorkLicense license = byKey.get(key(player, jobId));
        return license != null && license.isActive();
    }

    public List<WorkLicense> forPlayer(UUID player) {
        List<WorkLicense> result = new ArrayList<>();
        for (WorkLicense license : byKey.values()) {
            if (license.player().equals(player)) {
                result.add(license);
            }
        }
        return result;
    }

    /**
     * Issues (or reactivates) the licence for a player and job. A revoked licence is reactivated rather
     * than duplicated, preserving the original issue data and uniqueness per (player, job). When the
     * worker is online and an Identity adapter is present, the physical item is (re)given.
     */
    public WorkLicense issue(Player online, UUID player, Job job, String issuedBy) {
        WorkLicense license = byKey.get(key(player, job.id()));
        if (license == null) {
            license = new WorkLicense(Ids.prefixed("lic"), player, job.id(), System.currentTimeMillis(), issuedBy);
            byKey.put(key(player, job.id()), license);
        } else {
            license.setStatus(LicenseStatus.ACTIVE);
        }
        giveItem(online, license, job);
        plugin.adapters().storage().saveLicense(license);
        return license;
    }

    /** Revokes a licence; returns false if the player had none. Does not itself end running sessions. */
    public boolean revoke(UUID player, String jobId) {
        WorkLicense license = byKey.get(key(player, jobId));
        if (license == null) {
            return false;
        }
        license.setStatus(LicenseStatus.REVOKED);
        if (license.itemUuid() != null) {
            plugin.adapters().identity().revokeItem(license.itemUuid());
        }
        plugin.adapters().storage().saveLicense(license);
        return true;
    }

    /** Re-stamps the licence item with the worker's current tier, if it is a physical document. */
    public void refreshItemTier(WorkLicense license, String tierDisplay) {
        if (license != null && license.itemUuid() != null) {
            plugin.adapters().identity().updateTier(license.itemUuid(), tierDisplay);
        }
    }

    private void giveItem(Player online, WorkLicense license, Job job) {
        if (online == null || !plugin.adapters().identity().available()) {
            return;
        }
        String tier = plugin.records().get(license.player(), job.id()) == null
                ? "" : plugin.records().get(license.player(), job.id()).currentTier();
        plugin.adapters().identity().giveLicenseItem(online, license, job, tier)
                .ifPresent(license::setItemUuid);
    }
}
