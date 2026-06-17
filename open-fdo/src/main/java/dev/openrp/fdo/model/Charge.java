package dev.openrp.fdo.model;

import java.util.UUID;

/**
 * One charge added to a dossier. {@code crimeId} references an entry in the configured crime
 * catalogue ({@code crimes.yml}); the core never stores the crime label, only its id, so renaming a
 * crime in config updates every dossier at once.
 */
public record Charge(String crimeId, UUID addedBy, long addedAt) {
}
