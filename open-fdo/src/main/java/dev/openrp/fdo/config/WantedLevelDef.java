package dev.openrp.fdo.config;

/**
 * A configured wanted level from {@code wanted.yml}. How many levels exist and what they mean is
 * entirely config-driven; the core assumes no fixed scale (no hardcoded "L1/L2/L3"). {@code color}
 * is a MiniMessage colour name used when rendering the register.
 */
public record WantedLevelDef(int level, String label, String color) {
}
