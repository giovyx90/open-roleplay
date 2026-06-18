package dev.openrp.politics.model;

/** Lifecycle of an act: a draft awaiting a vote, signed, vetoed within its window, or annulled. */
public enum ActStatus {
    DRAFT,
    SIGNED,
    VETOED,
    ANNULLED
}
