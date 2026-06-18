package dev.openrp.politics.adapter;

import java.util.Optional;
import java.util.UUID;

/**
 * Bridge to a region backend (e.g. WorldGuard). Used only by the {@code conquest} assignment
 * mechanism: the charge belongs to whoever physically controls a designated region. The default is a
 * no-op whose {@link #available()} reports {@code false}, so a conquest charge simply stays vacant
 * until a real region backend registers.
 */
public interface RegionAdapter {

    String id();

    boolean available();

    boolean regionExists(String regionId);

    /** The player currently controlling a region, when the backend can determine one. */
    Optional<UUID> controller(String regionId);
}
