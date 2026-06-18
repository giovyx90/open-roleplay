package dev.openrp.politics.api;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import dev.openrp.politics.adapter.AdapterRegistry;
import dev.openrp.politics.capability.PoliticalCapability;
import dev.openrp.politics.config.ChargeDef;
import dev.openrp.politics.config.Government;
import dev.openrp.politics.core.ActService;
import dev.openrp.politics.core.ChargeManager;
import dev.openrp.politics.core.ElectionService;
import dev.openrp.politics.core.GovernmentManager;
import dev.openrp.politics.core.LawService;
import dev.openrp.politics.model.ChargeHolder;
import dev.openrp.politics.model.Election;
import dev.openrp.politics.model.Law;
import dev.openrp.politics.model.PoliticalAct;

/**
 * Public API, registered with the Bukkit ServicesManager. It is the institutional registry of the
 * server: who holds which charge, what they may do, what they have signed, and - most importantly for
 * other modules - the public law registry. Open FDO reads {@link #getActiveLaws(String)} to attach
 * charges to a dossier and {@link #wasActiveDuring(String, Instant)} to apply the law of the time of a
 * fact; Open Companies reads the licence-revocation charges; the gestionale reads the structure.
 * Retrieve it with {@code Bukkit.getServicesManager().load(OpenPoliticsApi.class)}.
 */
public interface OpenPoliticsApi {

    /** The live adapter set; register your economy/company/identity/region/authority adapter here. */
    AdapterRegistry adapters();

    // --- governments -------------------------------------------------------------------------

    List<Government> governments();

    boolean isGovernmentActive(String governmentId);

    // --- charges -----------------------------------------------------------------------------

    List<ChargeDef> charges();

    Optional<ChargeDef> getCharge(String chargeId);

    /** Active holders of a charge (a collegiate body has several). */
    List<ChargeHolder> holdersOf(String chargeId);

    boolean isVacant(String chargeId);

    /** The charges a player actively holds. */
    List<ChargeDef> chargesOf(UUID player);

    /** Whether the player holds any active charge granting the capability. */
    boolean hasCapability(UUID player, PoliticalCapability capability);

    /** Charge ids of a government whose holders carry the capability (e.g. who can DECLARE_EMERGENCY). */
    List<String> chargesWithCapability(String governmentId, PoliticalCapability capability);

    // --- acts --------------------------------------------------------------------------------

    Optional<PoliticalAct> getAct(String actId);

    List<PoliticalAct> recentActs(int limit);

    // --- laws: the bridge for the authorities ------------------------------------------------

    /** Active laws of a government - the catalogue Open FDO attaches to a dossier. */
    List<Law> getActiveLaws(String governmentId);

    Optional<Law> getLaw(String lawId);

    /** Whether a law was in force at a given moment, so a fact is judged by the law of its time. */
    boolean wasActiveDuring(String lawId, Instant moment);

    // --- elections ---------------------------------------------------------------------------

    List<Election> openElections();

    Optional<Election> getElection(String electionId);

    // --- manager access for richer integrations ----------------------------------------------

    GovernmentManager governmentManager();

    ChargeManager chargeManager();

    ActService actService();

    LawService lawService();

    ElectionService electionService();
}
