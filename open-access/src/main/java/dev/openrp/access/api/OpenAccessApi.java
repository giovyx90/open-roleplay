package dev.openrp.access.api;

import dev.openrp.access.model.AccessAction;
import dev.openrp.access.model.AccessCheckResult;
import dev.openrp.access.model.AccessProfile;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public interface OpenAccessApi {
    AccessCheckResult resolve(Player player, Location location, AccessAction action);

    boolean canManage(Player player, AccessProfile profile);

    Optional<AccessProfile> findProfileAt(Location location);

    Optional<AccessProfile> findProfileByRegion(String world, String regionId);

    Collection<AccessProfile> profiles();

    CompletableFuture<Void> refreshCache();

    void openEditor(Player player, AccessProfile profile, Location blockLocation);

    void registerOwnerProvider(OpenAccessOwnerProvider provider);

    void unregisterOwnerProvider(String namespace);
}
