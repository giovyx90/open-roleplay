package dev.openrp.fdo.model;

import java.util.UUID;

/**
 * The abstract order the core hands to a {@code DetentionAdapter} when a guilty verdict is signed.
 * The core knows only the inmate, the linked dossier, the sentence duration and an abstract security
 * level; the adapter decides what detention <em>physically</em> means (a prison region, a dungeon
 * teleport, criostasis - or nothing, leaving execution to manual roleplay).
 *
 * @param sentenceSeconds abstract sentence length; the adapter maps it onto its own time scale
 * @param securityLevel   abstract level (1..n); the adapter interprets it
 */
public record DetentionOrder(UUID inmate, String inmateName, String dossierId,
                             long sentenceSeconds, int securityLevel, long startedAt) {

    /** Wall-clock millisecond at which the sentence is fully served. */
    public long releaseAt() {
        return startedAt + sentenceSeconds * 1000L;
    }
}
