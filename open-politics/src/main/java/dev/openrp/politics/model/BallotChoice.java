package dev.openrp.politics.model;

import java.util.Locale;
import java.util.Optional;

/** A member's choice in a collegiate vote. Abstentions count toward quorum but not toward approval. */
public enum BallotChoice {
    YES,
    NO,
    ABSTAIN;

    public static Optional<BallotChoice> fromString(String value) {
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(BallotChoice.valueOf(value.trim().toUpperCase(Locale.ROOT)));
        } catch (IllegalArgumentException unknown) {
            return Optional.empty();
        }
    }
}
