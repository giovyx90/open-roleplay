package dev.openrp.politics.model;

/** Status of a law in the public registry. Repealed laws stay in the archive for RP retro-application. */
public enum LawStatus {
    ACTIVE,
    REPEALED,
    SUSPENDED
}
