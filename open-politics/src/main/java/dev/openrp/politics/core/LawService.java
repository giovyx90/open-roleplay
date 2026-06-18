package dev.openrp.politics.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import dev.openrp.politics.OpenPoliticsPlugin;
import dev.openrp.politics.capability.PoliticalCapability;
import dev.openrp.politics.model.Law;
import dev.openrp.politics.model.LawStatus;
import dev.openrp.politics.model.PoliticalAct;

/**
 * Owns the public law registry. A law is an act that completed its iter; this service registers it,
 * exposes it and archives it on repeal - it <strong>never executes it</strong>. The active registry is
 * what an authority bridge reads to attach charges to a dossier; the historical archive lets a judge
 * apply the law that was in force when a fact occurred, even after repeal.
 */
public final class LawService {

    private final OpenPoliticsPlugin plugin;
    private final java.util.Map<String, Law> laws = new ConcurrentHashMap<>();

    public LawService(OpenPoliticsPlugin plugin) {
        this.plugin = plugin;
    }

    public synchronized void loadAll() {
        laws.clear();
        for (Law law : plugin.adapters().storage().loadLaws()) {
            laws.put(law.id(), law);
        }
    }

    // --- lookups -----------------------------------------------------------------------------

    public Optional<Law> get(String id) {
        return Optional.ofNullable(id == null ? null : laws.get(id));
    }

    public List<Law> all() {
        return new ArrayList<>(laws.values());
    }

    /** Active laws of a government (or all active laws when {@code governmentId} is null/blank). */
    public List<Law> active(String governmentId) {
        return laws.values().stream()
                .filter(Law::isActive)
                .filter(law -> governmentId == null || governmentId.isBlank()
                        || law.governmentId().equals(governmentId))
                .toList();
    }

    public List<Law> archived() {
        return laws.values().stream().filter(law -> !law.isActive()).toList();
    }

    public boolean wasActiveDuring(String lawId, long moment) {
        return get(lawId).map(law -> law.wasActiveDuring(moment)).orElse(false);
    }

    // --- enactment ---------------------------------------------------------------------------

    /**
     * Promulgates a completed act into a law and files it under the given category. Called by the act
     * service once an act has cleared its iter (vote and/or veto window); never directly by a command.
     */
    public synchronized Law enactFromAct(PoliticalAct act, String category) {
        Law law = new Law(Ids.prefixed("law"), act.id(), act.governmentId(), act.title(),
                act.body(), category, System.currentTimeMillis());
        laws.put(law.id(), law);
        plugin.adapters().storage().saveLaw(law);
        return law;
    }

    // --- repeal ------------------------------------------------------------------------------

    public synchronized PoliticsResult repeal(UUID actor, boolean admin, String lawId) {
        Law law = laws.get(lawId);
        if (law == null) {
            return PoliticsResult.fail("law.unknown", "id", lawId);
        }
        if (!law.isActive()) {
            return PoliticsResult.fail("law.not_active", "id", lawId);
        }
        if (!admin && !plugin.charges().hasInGovernment(actor, PoliticalCapability.SIGN_LAW, law.governmentId())) {
            return PoliticsResult.fail("general.no_capability");
        }
        String chargeId = plugin.charges().activeHoldersOf(actor).stream()
                .filter(h -> h.governmentId().equals(law.governmentId()))
                .map(h -> h.chargeId())
                .findFirst().orElse(admin ? "admin" : "");
        law.setStatus(LawStatus.REPEALED);
        law.setRepealedAt(System.currentTimeMillis());
        law.setRepealedByUuid(actor);
        law.setRepealedByCharge(chargeId);
        plugin.adapters().storage().saveLaw(law);
        return PoliticsResult.ok("law.repealed", "title", law.title()).withPayload(law);
    }

    public synchronized PoliticsResult adminRepeal(String lawId) {
        return repeal(null, true, lawId);
    }
}
