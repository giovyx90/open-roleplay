package dev.openrp.politics.core;

import java.time.Year;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import dev.openrp.politics.OpenPoliticsPlugin;
import dev.openrp.politics.capability.PoliticalCapability;
import dev.openrp.politics.config.ActType;
import dev.openrp.politics.config.ChargeDef;
import dev.openrp.politics.config.CollegiateConfig;
import dev.openrp.politics.model.ActStatus;
import dev.openrp.politics.model.BallotChoice;
import dev.openrp.politics.model.ChargeHolder;
import dev.openrp.politics.model.CollegiateVote;
import dev.openrp.politics.model.Law;
import dev.openrp.politics.model.PoliticalAct;

/**
 * Owns acts and the legislative iter. A holder of the required capability signs an act; the plugin
 * stamps and records it. If the type requires a vote, the act is submitted to a collegiate body; if it
 * allows a veto, a veto window opens. When the iter clears and the type can become law, the act is
 * promulgated through the {@link LawService}. The plugin tracks the iter - it never forces it: a step
 * skipped is recorded, not punished.
 */
public final class ActService {

    private final OpenPoliticsPlugin plugin;
    private final Map<String, PoliticalAct> acts = new ConcurrentHashMap<>();
    private final Map<String, CollegiateVote> votes = new ConcurrentHashMap<>();
    private final Map<String, Long> counters = new ConcurrentHashMap<>();

    public ActService(OpenPoliticsPlugin plugin) {
        this.plugin = plugin;
    }

    public synchronized void loadAll() {
        acts.clear();
        votes.clear();
        counters.clear();
        for (PoliticalAct act : plugin.adapters().storage().loadActs()) {
            acts.put(act.id(), act);
        }
        for (CollegiateVote vote : plugin.adapters().storage().loadCollegiateVotes()) {
            votes.put(vote.id(), vote);
        }
        counters.putAll(plugin.adapters().storage().loadCounters());
    }

    // --- lookups -----------------------------------------------------------------------------

    public Optional<PoliticalAct> get(String id) {
        if (id == null) {
            return Optional.empty();
        }
        PoliticalAct byKey = acts.get(id);
        if (byKey != null) {
            return Optional.of(byKey);
        }
        return acts.values().stream().filter(act -> act.displayId().equals(id)).findFirst();
    }

    public List<PoliticalAct> all() {
        return new ArrayList<>(acts.values());
    }

    /** Recent acts, newest first, capped at {@code limit}. */
    public List<PoliticalAct> recent(int limit) {
        return acts.values().stream()
                .sorted((a, b) -> Long.compare(b.signedAt(), a.signedAt()))
                .limit(Math.max(1, limit))
                .toList();
    }

    public Optional<CollegiateVote> vote(String id) {
        return Optional.ofNullable(id == null ? null : votes.get(id));
    }

    // --- signing -----------------------------------------------------------------------------

    /**
     * Signs an act. The author must hold a charge granting the type's required capability (unless
     * admin); that charge and its government stamp the act. The iter then routes by type: a vote opens
     * for {@code requires_vote}, a veto window for {@code veto_allowed}, otherwise the act is final and
     * promulgated at once when it can become law.
     */
    public synchronized PoliticsResult sign(UUID author, boolean admin, String typeId,
                                            String title, List<String> body) {
        ActType type = plugin.config().actTypes().get(typeId).orElse(null);
        if (type == null) {
            return PoliticsResult.fail("act.unknown_type", "type", typeId);
        }
        if (title == null || title.isBlank()) {
            return PoliticsResult.fail("act.title_required");
        }
        ChargeHolder signing = signingCharge(author, type.required());
        if (signing == null && !admin) {
            return PoliticsResult.fail("general.no_capability");
        }
        String governmentId = signing != null ? signing.governmentId()
                : plugin.config().governments().ids().stream().findFirst().orElse("");
        String chargeId = signing != null ? signing.chargeId() : "admin";
        long now = System.currentTimeMillis();
        String storageId = Ids.prefixed("act");
        String displayId = displayId(governmentId);
        PoliticalAct act = new PoliticalAct(storageId, displayId, typeId, governmentId, author,
                chargeId, title, body, now);

        if (type.requiresVote()) {
            CollegiateVote vote = openCollegiateVote(act, type, now);
            if (vote == null) {
                return PoliticsResult.fail("act.no_collegiate_body", "charge", type.submitTo());
            }
            act.setStatus(ActStatus.DRAFT);
            act.setCollegiateVoteId(vote.id());
        } else if (type.vetoAllowed()) {
            act.setStatus(ActStatus.SIGNED);
            act.setVetoDeadline(now + plugin.config().settings().realMillisFromHours(type.vetoWindowHours()));
        } else {
            act.setStatus(ActStatus.SIGNED);
        }

        acts.put(act.id(), act);
        plugin.adapters().storage().saveAct(act);

        // An act with no vote and no veto window that can become law is promulgated immediately.
        if (act.status() == ActStatus.SIGNED && type.canBecomeLaw() && act.vetoDeadline() == 0) {
            promulgate(act, type);
        }
        return PoliticsResult.ok("act.signed", "id", act.displayId(), "type", type.displayName())
                .withPayload(act);
    }

    // --- veto / annul ------------------------------------------------------------------------

    public synchronized PoliticsResult veto(UUID actor, boolean admin, String actId) {
        PoliticalAct act = get(actId).orElse(null);
        if (act == null) {
            return PoliticsResult.fail("act.unknown", "id", actId);
        }
        ActType type = plugin.config().actTypes().get(act.typeId()).orElse(null);
        if (type == null || !type.vetoAllowed()) {
            return PoliticsResult.fail("act.not_vetoable", "id", act.displayId());
        }
        if (!act.vetoableNow(System.currentTimeMillis())) {
            return PoliticsResult.fail("act.veto_window_closed", "id", act.displayId());
        }
        if (!admin && !plugin.charges().hasInGovernment(actor, type.vetoCapability(), act.governmentId())) {
            return PoliticsResult.fail("general.no_capability");
        }
        act.setStatus(ActStatus.VETOED);
        plugin.adapters().storage().saveAct(act);
        return PoliticsResult.ok("act.vetoed", "id", act.displayId()).withPayload(act);
    }

    public synchronized PoliticsResult annul(String actId) {
        PoliticalAct act = get(actId).orElse(null);
        if (act == null) {
            return PoliticsResult.fail("act.unknown", "id", actId);
        }
        act.setStatus(ActStatus.ANNULLED);
        plugin.adapters().storage().saveAct(act);
        return PoliticsResult.ok("act.annulled", "id", act.displayId()).withPayload(act);
    }

    // --- collegiate voting -------------------------------------------------------------------

    public synchronized PoliticsResult castCollegiate(UUID member, String actId, BallotChoice choice) {
        PoliticalAct act = get(actId).orElse(null);
        if (act == null || act.collegiateVoteId() == null) {
            return PoliticsResult.fail("act.no_open_vote", "id", actId);
        }
        CollegiateVote vote = votes.get(act.collegiateVoteId());
        if (vote == null || !vote.isOpen()) {
            return PoliticsResult.fail("act.no_open_vote", "id", act.displayId());
        }
        boolean memberOfBody = plugin.charges().activeHoldersOf(vote.chargeId()).stream()
                .anyMatch(h -> h.playerUuid().equals(member));
        if (!memberOfBody) {
            return PoliticsResult.fail("act.not_in_body");
        }
        if (vote.hasVoted(member)) {
            return PoliticsResult.fail("act.already_voted");
        }
        vote.cast(member, choice);
        plugin.adapters().storage().saveCollegiateVote(vote);
        return PoliticsResult.ok("act.ballot_cast", "id", act.displayId()).withPayload(act);
    }

    // --- lifecycle ---------------------------------------------------------------------------

    /** Closes due collegiate votes and promulgates acts whose veto window has elapsed. */
    public synchronized void tick(long now) {
        for (CollegiateVote vote : new ArrayList<>(votes.values())) {
            if (vote.isOpen() && now >= vote.closesAt()) {
                closeCollegiate(vote, now);
            }
        }
        for (PoliticalAct act : new ArrayList<>(acts.values())) {
            if (act.status() == ActStatus.SIGNED && act.relatedLawId() == null
                    && act.vetoDeadline() > 0 && now > act.vetoDeadline()) {
                plugin.config().actTypes().get(act.typeId()).ifPresent(type -> {
                    if (type.canBecomeLaw()) {
                        promulgate(act, type);
                    }
                });
            }
        }
    }

    private void closeCollegiate(CollegiateVote vote, long now) {
        ChargeDef body = plugin.config().charges().get(vote.chargeId()).orElse(null);
        CollegiateConfig rules = body == null ? null : body.collegiate();
        int members = plugin.charges().activeHoldersOf(vote.chargeId()).size();
        double quorum = rules == null ? 0.5 : rules.quorum();
        double majority = rules == null ? 0.5 : rules.majority();
        boolean quorumMet = members == 0 || vote.castCount() >= Math.ceil(quorum * members);
        long yes = vote.count(BallotChoice.YES);
        boolean approved = quorumMet && vote.castCount() > 0 && yes > majority * vote.castCount();
        vote.setStatus(quorumMet
                ? (approved ? CollegiateVote.Status.APPROVED : CollegiateVote.Status.REJECTED)
                : CollegiateVote.Status.EXPIRED);
        plugin.adapters().storage().saveCollegiateVote(vote);

        PoliticalAct act = acts.get(vote.actId());
        if (act == null) {
            return;
        }
        ActType type = plugin.config().actTypes().get(act.typeId()).orElse(null);
        if (approved && type != null) {
            // The body approved: the act is now signed. Open a veto window if the type allows one,
            // otherwise promulgate straight away when it can become law.
            act.setStatus(ActStatus.SIGNED);
            if (type.vetoAllowed()) {
                act.setVetoDeadline(now + plugin.config().settings().realMillisFromHours(type.vetoWindowHours()));
                plugin.adapters().storage().saveAct(act);
            } else if (type.canBecomeLaw()) {
                plugin.adapters().storage().saveAct(act);
                promulgate(act, type);
            } else {
                plugin.adapters().storage().saveAct(act);
            }
        } else {
            // Rejected or no quorum: the act does not advance. It stays on record as a dead draft.
            plugin.adapters().storage().saveAct(act);
        }
    }

    private CollegiateVote openCollegiateVote(PoliticalAct act, ActType type, long now) {
        ChargeDef body = plugin.config().charges().get(type.submitTo()).orElse(null);
        if (body == null || !body.isCollegiate()) {
            return null;
        }
        long closesAt = now + plugin.config().settings()
                .realMillisFromHours(body.collegiate().durationHours());
        CollegiateVote vote = new CollegiateVote(Ids.prefixed("cvote"), act.id(), body.id(), now, closesAt);
        votes.put(vote.id(), vote);
        plugin.adapters().storage().saveCollegiateVote(vote);
        return vote;
    }

    private void promulgate(PoliticalAct act, ActType type) {
        if (act.relatedLawId() != null) {
            return;
        }
        String category = type.lawCategory();
        Law law = plugin.laws().enactFromAct(act, category);
        act.setRelatedLawId(law.id());
        plugin.adapters().storage().saveAct(act);
        plugin.adapters().notification().broadcast(
                plugin.messages().mini(plugin.getServer().getConsoleSender(),
                        "law.promulgated", "title", law.title(), "id", act.displayId()));
    }

    private ChargeHolder signingCharge(UUID author, PoliticalCapability required) {
        return plugin.charges().activeHoldersOf(author).stream()
                .filter(holder -> plugin.config().charges().get(holder.chargeId())
                        .map(charge -> charge.grants(required)).orElse(false))
                .findFirst().orElse(null);
    }

    private String displayId(String governmentId) {
        int year = Year.now().getValue();
        String key = "act_seq_" + governmentId + "_" + year;
        long next = counters.getOrDefault(key, 0L) + 1;
        counters.put(key, next);
        plugin.adapters().storage().saveCounter(key, next);
        String sigla = plugin.config().governments().get(governmentId)
                .map(g -> g.sigla()).orElse(governmentId);
        return plugin.config().settings().actIdPattern()
                .replace("{anno}", String.valueOf(year))
                .replace("{numero}", String.valueOf(next))
                .replace("{sigla}", sigla);
    }
}
