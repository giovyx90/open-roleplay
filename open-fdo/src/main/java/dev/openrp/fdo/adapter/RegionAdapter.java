package dev.openrp.fdo.adapter;

import java.util.Set;
import java.util.UUID;

/**
 * Minimal region abstraction. The core never assumes WorldGuard: it only asks whether a player is
 * inside a named region (evidence locker, intercept zone) and who is currently inside. A WorldGuard
 * implementation ships as the default; servers without it get a no-op that simply reports "not
 * inside", which degrades the locker/intercept features gracefully.
 */
public interface RegionAdapter {

    String id();

    boolean isInside(UUID player, String regionId);

    Set<UUID> playersInside(String regionId);

    /** Whether the named region is known to the backend. */
    boolean exists(String regionId);
}
