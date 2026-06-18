package dev.openrp.politics.adapter;

import java.util.UUID;

/**
 * Bridge to an identity backend (e.g. Open Identity). When present, a charge generates a physical
 * institutional credential that decays at the end of the mandate. Open Politics is the source of truth
 * for who holds what; this adapter just renders it as an item. The default is a no-op whose
 * {@link #available()} reports {@code false}.
 */
public interface IdentityAdapter {

    String id();

    boolean available();

    /** Issue (or refresh) the institutional credential for a holder. {@code expiresAt} 0 = unlimited. */
    void issueCredential(UUID player, String chargeDisplayName, String governmentName,
                         long assignedAt, long expiresAt);

    /** Revoke the credential bound to a charge when a holder leaves it. */
    void revokeCredential(UUID player, String chargeId);
}
