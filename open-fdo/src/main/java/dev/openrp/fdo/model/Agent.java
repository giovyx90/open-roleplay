package dev.openrp.fdo.model;

import java.util.Objects;
import java.util.UUID;

/**
 * An enrolled authority member. The core stores only ids: which configured corps and rank the member
 * belongs to, plus the badge number ({@code matricola}). The corps and rank <em>labels</em> are
 * resolved from config at render time, so renaming a corps updates every member and every badge at
 * once. Capabilities are never stored here - they are derived from the rank via the configuration.
 */
public final class Agent {

    private final UUID uuid;
    private String name;
    private String corpsId;
    private String rankId;
    private final String matricola;
    private final long enrolledAt;

    public Agent(UUID uuid, String name, String corpsId, String rankId, String matricola, long enrolledAt) {
        this.uuid = Objects.requireNonNull(uuid, "uuid");
        this.name = name == null ? "" : name;
        this.corpsId = Objects.requireNonNull(corpsId, "corpsId");
        this.rankId = Objects.requireNonNull(rankId, "rankId");
        this.matricola = matricola == null ? "" : matricola;
        this.enrolledAt = enrolledAt;
    }

    public UUID uuid() {
        return uuid;
    }

    public String name() {
        return name;
    }

    public void setName(String name) {
        if (name != null && !name.isBlank()) {
            this.name = name;
        }
    }

    public String corpsId() {
        return corpsId;
    }

    public void setCorpsId(String corpsId) {
        this.corpsId = Objects.requireNonNull(corpsId, "corpsId");
    }

    public String rankId() {
        return rankId;
    }

    public void setRankId(String rankId) {
        this.rankId = Objects.requireNonNull(rankId, "rankId");
    }

    public String matricola() {
        return matricola;
    }

    public long enrolledAt() {
        return enrolledAt;
    }
}
