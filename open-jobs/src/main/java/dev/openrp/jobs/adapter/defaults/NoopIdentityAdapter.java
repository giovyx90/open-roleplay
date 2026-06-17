package dev.openrp.jobs.adapter.defaults;

import java.util.Optional;
import org.bukkit.entity.Player;
import dev.openrp.jobs.adapter.IdentityAdapter;
import dev.openrp.jobs.config.Job;
import dev.openrp.jobs.model.WorkLicense;

/**
 * Default identity adapter when no identity plugin is present: licences stay purely records, with no
 * physical item. An Open Identity bridge registers a real implementation that mints NBT documents.
 */
public final class NoopIdentityAdapter implements IdentityAdapter {

    @Override
    public String id() {
        return "none";
    }

    @Override
    public boolean available() {
        return false;
    }

    @Override
    public Optional<String> giveLicenseItem(Player player, WorkLicense license, Job job, String tierDisplay) {
        return Optional.empty();
    }

    @Override
    public void updateTier(String itemUuid, String tierDisplay) {
        // no-op
    }

    @Override
    public void revokeItem(String itemUuid) {
        // no-op
    }
}
