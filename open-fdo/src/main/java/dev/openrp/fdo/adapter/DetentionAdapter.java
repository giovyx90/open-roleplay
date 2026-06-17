package dev.openrp.fdo.adapter;

import java.util.UUID;
import dev.openrp.fdo.model.DetentionEndReason;
import dev.openrp.fdo.model.DetentionOrder;

/**
 * Executes the physical side of a sentence. This is the most important adapter for setting-neutrality:
 * the core decides <em>that</em> there is a conviction; the adapter decides what detention
 * <em>physically</em> means. A realistic prison module, a fantasy "dungeon" teleport and a sci-fi
 * cryo-cell all implement this same contract. When no adapter is registered the conviction is still
 * recorded in the dossier, but physical execution is left to manual roleplay.
 */
public interface DetentionAdapter {

    String id();

    /** Starts detention. The core passes only abstract data. */
    void beginDetention(DetentionOrder order);

    /** Ends detention (sentence served, early release, confirmed escape, transfer). */
    void endDetention(UUID inmate, DetentionEndReason reason);

    /** Whether the inmate is currently where they should be (for escape detection). */
    boolean isContained(UUID inmate);
}
