package dev.openrp.crime.adapter;

import java.util.Optional;
import java.util.UUID;

/**
 * Bridge to a company system (Open Companies). Extortion targets are companies; shell-company
 * laundering routes money through one. The bundled default is a no-op reporting no companies, so the
 * racket subsystem degrades cleanly when no company plugin is present.
 */
public interface CompanyAdapter {

    String id();

    /** Whether a real company backend is present. */
    boolean available();

    /** The id of a company owned by the player, if any. */
    Optional<String> companyOwnedBy(UUID owner);

    boolean isOwner(UUID player, String companyId);

    String companyName(String companyId);

    /** Applies a reputation malus to the company (escalation effect). No-op if unsupported. */
    void applyReputationMalus(String companyId, int malus);

    /** Debits the company's funds to pay protection; returns {@code false} if it cannot pay. */
    boolean chargeCompany(String companyId, long amount);
}
