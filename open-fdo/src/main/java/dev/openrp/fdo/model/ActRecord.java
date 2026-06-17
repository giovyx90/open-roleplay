package dev.openrp.fdo.model;

import java.util.UUID;

/**
 * An append-only log line for an act produced by an authority member. This is the raw material the
 * service sheet is derived from: who did what, to whom, where and when, and which dossier it touched.
 *
 * @param target    the subject of the act, or {@code null} when the act has no person target
 * @param dossierId the dossier the act opened or touched, or {@code null}
 */
public record ActRecord(UUID id, String type, UUID author, String authorName, UUID target,
                        String targetName, String world, double x, double y, double z,
                        long timestamp, String dossierId) {
}
