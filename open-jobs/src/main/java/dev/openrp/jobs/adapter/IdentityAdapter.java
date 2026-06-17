package dev.openrp.jobs.adapter;

import java.util.Optional;
import org.bukkit.entity.Player;
import dev.openrp.jobs.config.Job;
import dev.openrp.jobs.model.WorkLicense;

/**
 * Bridge to an identity-document system (Open Identity). When present, a professional licence becomes a
 * physical NBT item showing the trade, the current progression tier and the issue data; the tier is
 * re-stamped as the worker advances. The bundled default is a no-op: the licence stays purely a record,
 * with no physical item. The DB record is always authoritative regardless of the item.
 */
public interface IdentityAdapter {

    String id();

    /** Whether a real identity backend is present (licences can be physical items). */
    boolean available();

    /** Gives the player the licence item; returns the item's tracking uuid if one was created. */
    Optional<String> giveLicenseItem(Player player, WorkLicense license, Job job, String tierDisplay);

    /** Re-stamps the tier shown on an existing licence item. No-op if unsupported. */
    void updateTier(String itemUuid, String tierDisplay);

    /** Invalidates the physical item backing a revoked licence. No-op if unsupported. */
    void revokeItem(String itemUuid);
}
