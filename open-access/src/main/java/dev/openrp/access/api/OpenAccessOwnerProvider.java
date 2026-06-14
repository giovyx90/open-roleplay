package dev.openrp.access.api;

import dev.openrp.access.model.AccessPrincipal;
import dev.openrp.access.model.AccessProfile;

import java.util.Set;
import java.util.UUID;

public interface OpenAccessOwnerProvider {
    String namespace();

    boolean supports(AccessProfile profile);

    Set<AccessPrincipal> principalsFor(AccessProfile profile, UUID playerUuid, String playerName);

    boolean canManage(AccessProfile profile, UUID playerUuid, String playerName);

    default void onUnregister() {
    }
}
