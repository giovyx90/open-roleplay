package dev.openrp.fdo.config;

/**
 * A configured charge from {@code crimes.yml}. The core stores only the {@code id} on a dossier; the
 * {@code label} (e.g. an article of a penal code, or "Forbidden sorcery") lives in config so the
 * same engine serves a realistic and a fantasy server unchanged. {@code gravity} is an abstract hint
 * an adapter or config may use; the core does not interpret it.
 */
public record Crime(String id, String label, int gravity) {
}
