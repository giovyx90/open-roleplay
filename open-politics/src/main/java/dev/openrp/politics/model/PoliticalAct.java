package dev.openrp.politics.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * An official document signed by a charge holder. The body is written by the player; the plugin
 * stamps, registers and certifies it. The core never judges its content - it records that a holder of
 * the right capability produced it. An act with a {@code displayId} (the configurable, human-facing
 * number) is the citable reference; {@code id} is the stable storage key.
 *
 * <p>Bukkit-free, mutated under the manager lock.</p>
 */
public final class PoliticalAct {

    private final String id;
    private final String displayId;
    private final String typeId;
    private final String governmentId;
    private final UUID authorUuid;
    private final String chargeId;
    private final String title;
    private final List<String> body = new ArrayList<>();
    private final long signedAt;
    private ActStatus status = ActStatus.SIGNED;
    private String relatedLawId;
    private long vetoDeadline;       // 0 = not vetoable
    private String collegiateVoteId; // set when the act is submitted to a collegiate body

    public PoliticalAct(String id, String displayId, String typeId, String governmentId, UUID authorUuid,
                        String chargeId, String title, List<String> body, long signedAt) {
        this.id = Objects.requireNonNull(id, "id");
        this.displayId = displayId == null ? id : displayId;
        this.typeId = Objects.requireNonNull(typeId, "typeId");
        this.governmentId = governmentId == null ? "" : governmentId;
        this.authorUuid = authorUuid;
        this.chargeId = chargeId == null ? "" : chargeId;
        this.title = title == null ? "" : title;
        this.signedAt = signedAt;
        if (body != null) {
            this.body.addAll(body);
        }
    }

    public String id() {
        return id;
    }

    public String displayId() {
        return displayId;
    }

    public String typeId() {
        return typeId;
    }

    public String governmentId() {
        return governmentId;
    }

    public UUID authorUuid() {
        return authorUuid;
    }

    public String chargeId() {
        return chargeId;
    }

    public String title() {
        return title;
    }

    public List<String> body() {
        return List.copyOf(body);
    }

    public long signedAt() {
        return signedAt;
    }

    public ActStatus status() {
        return status;
    }

    public void setStatus(ActStatus status) {
        this.status = Objects.requireNonNull(status, "status");
    }

    public String relatedLawId() {
        return relatedLawId;
    }

    public void setRelatedLawId(String relatedLawId) {
        this.relatedLawId = relatedLawId;
    }

    public long vetoDeadline() {
        return vetoDeadline;
    }

    public void setVetoDeadline(long vetoDeadline) {
        this.vetoDeadline = Math.max(0, vetoDeadline);
    }

    public boolean vetoableNow(long now) {
        return vetoDeadline > 0 && now <= vetoDeadline;
    }

    public String collegiateVoteId() {
        return collegiateVoteId;
    }

    public void setCollegiateVoteId(String collegiateVoteId) {
        this.collegiateVoteId = collegiateVoteId;
    }
}
