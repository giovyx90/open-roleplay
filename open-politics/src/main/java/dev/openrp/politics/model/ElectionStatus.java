package dev.openrp.politics.model;

/** Lifecycle of an election: campaign -&gt; voting -&gt; closed, or cancelled at any point. */
public enum ElectionStatus {
    CAMPAIGN,
    VOTING,
    CLOSED,
    CANCELLED
}
