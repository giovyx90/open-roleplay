package dev.openrp.politics.model;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * An internal vote of a collegiate body on an act. Opens for a configured window; a quorum of the
 * body's members must cast a ballot and a majority of those cast must be YES for it to carry. The
 * result gates whether the act can be promulgated; like everything else, the plugin only records it.
 *
 * <p>Bukkit-free, mutated under the manager lock.</p>
 */
public final class CollegiateVote {

    /** OPEN while collecting ballots; then APPROVED, REJECTED, or EXPIRED if quorum was never met. */
    public enum Status {
        OPEN,
        APPROVED,
        REJECTED,
        EXPIRED
    }

    private final String id;
    private final String actId;
    private final String chargeId;
    private final long openedAt;
    private final long closesAt;
    private Status status = Status.OPEN;
    private final Map<UUID, BallotChoice> ballots = new LinkedHashMap<>();

    public CollegiateVote(String id, String actId, String chargeId, long openedAt, long closesAt) {
        this.id = Objects.requireNonNull(id, "id");
        this.actId = Objects.requireNonNull(actId, "actId");
        this.chargeId = Objects.requireNonNull(chargeId, "chargeId");
        this.openedAt = openedAt;
        this.closesAt = closesAt;
    }

    public String id() {
        return id;
    }

    public String actId() {
        return actId;
    }

    public String chargeId() {
        return chargeId;
    }

    public long openedAt() {
        return openedAt;
    }

    public long closesAt() {
        return closesAt;
    }

    public Status status() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = Objects.requireNonNull(status, "status");
    }

    public boolean isOpen() {
        return status == Status.OPEN;
    }

    public boolean hasVoted(UUID member) {
        return member != null && ballots.containsKey(member);
    }

    public void cast(UUID member, BallotChoice choice) {
        ballots.put(member, choice);
    }

    public Map<UUID, BallotChoice> ballots() {
        return Collections.unmodifiableMap(ballots);
    }

    public long count(BallotChoice choice) {
        return ballots.values().stream().filter(value -> value == choice).count();
    }

    public int castCount() {
        return ballots.size();
    }
}
