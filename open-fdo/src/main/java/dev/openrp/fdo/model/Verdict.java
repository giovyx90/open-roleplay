package dev.openrp.fdo.model;

import java.util.UUID;

/**
 * Section C of a dossier: the final outcome, signed by the judging authority and immutable
 * thereafter. For a {@link VerdictOutcome#GUILTY} verdict {@code sentenceSeconds} and
 * {@code securityLevel} feed the abstract {@link DetentionOrder} handed to the detention adapter;
 * for any other outcome they are {@code 0}.
 */
public record Verdict(VerdictOutcome outcome, long sentenceSeconds, int securityLevel,
                      UUID judge, long signedAt, String note) {
}
