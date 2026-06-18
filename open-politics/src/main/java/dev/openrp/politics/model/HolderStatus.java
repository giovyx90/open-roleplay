package dev.openrp.politics.model;

/** Lifecycle of a charge holder. A removed/expired holder is kept for history, never deleted. */
public enum HolderStatus {
    ACTIVE,
    EXPIRED,
    REMOVED
}
