package dev.openrp.crime.adapter;

import java.util.Optional;
import java.util.UUID;
import org.bukkit.entity.Player;

/**
 * Bridge to the authorities (Open FDO). This is how a {@link dev.openrp.crime.model.Discovery}
 * reaches a dossier. The bundled default recognises agents and capabilities through Bukkit
 * permission nodes and mints a synthetic local dossier id, so the discovery system works standalone;
 * an Open FDO bridge registers a real implementation that opens an actual dossier and links the
 * discovered events to it.
 */
public interface AuthorityAdapter {

    String id();

    /** Whether a real authority backend (e.g. Open FDO) is present. */
    boolean available();

    /** Whether the player is an authority agent able to receive reports right now (e.g. on duty). */
    boolean isAgent(Player player);

    /** Whether the agent holds the capability to take in an informant. */
    boolean canReceiveInformant(Player agent);

    /** Whether the agent holds the capability to open/lead an investigation. */
    boolean canInvestigate(Player agent);

    /**
     * Opens (or reuses) a dossier the discovery can be attached to, returning its id. Empty means no
     * dossier could be opened (the discovery is still recorded, just not yet linked to a case).
     */
    Optional<String> openDossier(Player agent, UUID subject, String subjectName);
}
