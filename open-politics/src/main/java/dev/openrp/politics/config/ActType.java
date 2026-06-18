package dev.openrp.politics.config;

import dev.openrp.politics.capability.PoliticalCapability;

/**
 * A configured type of act. The core hardcodes no type: a decree, a bill, an appointment, a royal
 * edict are all just rows here. {@code requiresVote} routes the act through a collegiate body before
 * it can be promulgated; {@code canBecomeLaw} lets a completed act become a {@link
 * dev.openrp.politics.model.Law}. The plugin tracks this iter, it never forces it.
 *
 * @param id              stable config key, e.g. {@code proposta_legge}
 * @param displayName     human name shown to players
 * @param required        capability needed to sign this type of act
 * @param canBecomeLaw    whether a completed act of this type is promulgated into a law
 * @param requiresVote    whether it must pass a collegiate vote before promulgation
 * @param submitTo        the collegiate charge id that votes on it (when requiresVote)
 * @param vetoAllowed     whether the act can be vetoed
 * @param vetoCapability  capability needed to veto it
 * @param vetoWindowHours hours after signing/approval during which a veto is valid
 * @param promulgatedBy   capability needed to promulgate the law
 * @param lawCategory     default registry category of the resulting law
 */
public record ActType(String id, String displayName, PoliticalCapability required, boolean canBecomeLaw,
                      boolean requiresVote, String submitTo, boolean vetoAllowed,
                      PoliticalCapability vetoCapability, int vetoWindowHours,
                      PoliticalCapability promulgatedBy, String lawCategory) {

    public ActType {
        required = required == null ? PoliticalCapability.SIGN_ACT : required;
        vetoCapability = vetoCapability == null ? PoliticalCapability.VETO : vetoCapability;
        promulgatedBy = promulgatedBy == null ? PoliticalCapability.SIGN_LAW : promulgatedBy;
        vetoWindowHours = Math.max(0, vetoWindowHours);
        lawCategory = lawCategory == null ? "" : lawCategory;
    }
}
