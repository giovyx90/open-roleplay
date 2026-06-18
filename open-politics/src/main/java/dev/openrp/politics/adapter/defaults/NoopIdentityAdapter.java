package dev.openrp.politics.adapter.defaults;

import java.util.UUID;
import dev.openrp.politics.adapter.IdentityAdapter;

/** No-op identity adapter: reports unavailable; charges simply produce no physical credential. */
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
    public void issueCredential(UUID player, String chargeDisplayName, String governmentName,
                                long assignedAt, long expiresAt) {
        // No identity backend: nothing to issue.
    }

    @Override
    public void revokeCredential(UUID player, String chargeId) {
        // No identity backend: nothing to revoke.
    }
}
