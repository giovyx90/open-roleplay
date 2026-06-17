package dev.openrp.crime.command;

import java.util.Optional;
import org.bukkit.entity.Player;
import dev.openrp.crime.adapter.AuthorityAdapter;

/** Finds the nearest authority agent to a player - the physical-presence requirement of the discovery commands. */
final class Proximity {

    private Proximity() {
    }

    /**
     * The nearest agent within {@code radius} blocks who satisfies the authority check. {@code
     * needInformantCap} additionally requires the RECEIVE_INFORMANT capability. The reporter is never
     * matched against themselves.
     */
    static Optional<Player> nearestAgent(AuthorityAdapter authority, Player from, double radius,
                                         boolean needInformantCap) {
        Player best = null;
        double bestSq = Double.MAX_VALUE;
        for (Player candidate : from.getWorld().getNearbyPlayers(from.getLocation(), radius)) {
            if (candidate.equals(from) || !authority.isAgent(candidate)) {
                continue;
            }
            if (needInformantCap && !authority.canReceiveInformant(candidate)) {
                continue;
            }
            double distanceSq = candidate.getLocation().distanceSquared(from.getLocation());
            if (distanceSq < bestSq) {
                bestSq = distanceSq;
                best = candidate;
            }
        }
        return Optional.ofNullable(best);
    }
}
