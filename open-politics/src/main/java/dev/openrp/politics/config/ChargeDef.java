package dev.openrp.politics.config;

import java.util.Set;
import dev.openrp.politics.capability.PoliticalCapability;

/**
 * A configured charge. <strong>This is where the institution lives</strong>: the display name, the
 * ordinal authority level, the holder cap, the term length, the granted capabilities and the
 * assignment mechanism are all config. The core only ever handles "a charge with this id, this
 * authority and these capabilities" - never "Sindaco" or "Re".
 *
 * @param id              stable config key, e.g. {@code sindaco}
 * @param displayName     human name shown to players
 * @param governmentId    the government this charge belongs to
 * @param authorityLevel  ordinal - higher means more authority (used for tie-breaks and display order)
 * @param maxHolders      how many players can hold it at once (a collegiate body has &gt; 1)
 * @param termDurationDays mandate length in in-game days, 0 = unlimited
 * @param capabilities    the political capabilities this charge grants its holders
 * @param mechanism       how the charge is assigned
 * @param collegiate      internal voting rules when this charge is a collegiate body
 */
public record ChargeDef(String id, String displayName, String governmentId, int authorityLevel,
                        int maxHolders, int termDurationDays, Set<PoliticalCapability> capabilities,
                        AssignmentMechanism mechanism, CollegiateConfig collegiate) {

    public ChargeDef {
        authorityLevel = Math.max(0, authorityLevel);
        maxHolders = Math.max(1, maxHolders);
        termDurationDays = Math.max(0, termDurationDays);
        capabilities = capabilities == null ? Set.of() : Set.copyOf(capabilities);
    }

    public boolean grants(PoliticalCapability capability) {
        return capabilities.contains(capability) || capabilities.contains(PoliticalCapability.ALL);
    }

    public boolean isCollegiate() {
        return maxHolders > 1;
    }

    public boolean hasTerm() {
        return termDurationDays > 0;
    }
}
