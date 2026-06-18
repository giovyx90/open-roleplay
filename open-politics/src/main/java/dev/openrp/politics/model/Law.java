package dev.openrp.politics.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * A law: an act that completed its iter and produces a narrative constraint. The plugin registers,
 * exposes and archives it - <strong>it never executes it</strong>. A law that bans weapons downtown is
 * a signed reference the authorities cite in RP; the plugin touches nothing. Its time span
 * ({@code enactedAt}..{@code repealedAt}) lets a judge apply the law that was in force when a fact
 * occurred, even after repeal.
 *
 * <p>Bukkit-free, mutated under the manager lock.</p>
 */
public final class Law {

    private final String id;
    private final String actId;
    private final String governmentId;
    private final String title;
    private final List<String> body = new ArrayList<>();
    private final String category;
    private LawStatus status = LawStatus.ACTIVE;
    private final long enactedAt;
    private long repealedAt;         // 0 = still active
    private UUID repealedByUuid;
    private String repealedByCharge;

    public Law(String id, String actId, String governmentId, String title, List<String> body,
               String category, long enactedAt) {
        this.id = Objects.requireNonNull(id, "id");
        this.actId = actId == null ? "" : actId;
        this.governmentId = governmentId == null ? "" : governmentId;
        this.title = title == null ? "" : title;
        this.category = category == null ? "" : category;
        this.enactedAt = enactedAt;
        if (body != null) {
            this.body.addAll(body);
        }
    }

    public String id() {
        return id;
    }

    public String actId() {
        return actId;
    }

    public String governmentId() {
        return governmentId;
    }

    public String title() {
        return title;
    }

    public List<String> body() {
        return List.copyOf(body);
    }

    public String category() {
        return category;
    }

    public LawStatus status() {
        return status;
    }

    public void setStatus(LawStatus status) {
        this.status = Objects.requireNonNull(status, "status");
    }

    public boolean isActive() {
        return status == LawStatus.ACTIVE;
    }

    public long enactedAt() {
        return enactedAt;
    }

    public long repealedAt() {
        return repealedAt;
    }

    public void setRepealedAt(long repealedAt) {
        this.repealedAt = Math.max(0, repealedAt);
    }

    public UUID repealedByUuid() {
        return repealedByUuid;
    }

    public void setRepealedByUuid(UUID repealedByUuid) {
        this.repealedByUuid = repealedByUuid;
    }

    public String repealedByCharge() {
        return repealedByCharge;
    }

    public void setRepealedByCharge(String repealedByCharge) {
        this.repealedByCharge = repealedByCharge;
    }

    /**
     * Whether the law was in force at {@code moment}: enacted at or before it, and either still active
     * or repealed strictly after it. This is what an authority bridge calls to apply the law of the
     * time of a fact.
     */
    public boolean wasActiveDuring(long moment) {
        if (moment < enactedAt) {
            return false;
        }
        return repealedAt == 0 || moment < repealedAt;
    }
}
