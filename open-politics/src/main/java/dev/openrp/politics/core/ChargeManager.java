package dev.openrp.politics.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import dev.openrp.politics.adapter.AdapterRegistry;
import dev.openrp.politics.capability.PoliticalCapability;
import dev.openrp.politics.config.ChargeDef;
import dev.openrp.politics.config.PoliticsConfig;
import dev.openrp.politics.model.ChargeHolder;
import dev.openrp.politics.model.HolderStatus;

/**
 * Owns charge holders: assignment, removal, capability resolution, term expiry and hereditary
 * succession. The charges themselves are config ({@link ChargeDef}); this manager only tracks who
 * occupies them. A player may hold several charges at once. All mutating methods are synchronized so
 * two concurrent commands cannot race; reads use a {@link ConcurrentHashMap} so the public API can
 * safely query off the main thread.
 *
 * <p>The plugin certifies, it does not govern: assigning a charge grants its capabilities (which only
 * gate commands) and, when an identity backend is present, a physical credential. Nothing else.</p>
 */
public final class ChargeManager {

    private final PoliticsConfig config;
    private final AdapterRegistry adapters;
    private final java.util.Map<String, ChargeHolder> holders = new ConcurrentHashMap<>();

    public ChargeManager(PoliticsConfig config, AdapterRegistry adapters) {
        this.config = config;
        this.adapters = adapters;
    }

    public synchronized void loadAll() {
        holders.clear();
        for (ChargeHolder holder : adapters.storage().loadHolders()) {
            holders.put(holder.id(), holder);
        }
    }

    // --- lookups -----------------------------------------------------------------------------

    public Optional<ChargeHolder> holder(String holderId) {
        return Optional.ofNullable(holderId == null ? null : holders.get(holderId));
    }

    public Collection<ChargeHolder> allHolders() {
        return Collections.unmodifiableCollection(holders.values());
    }

    /** Active holders of a charge, in assignment order. */
    public List<ChargeHolder> activeHoldersOf(String chargeId) {
        List<ChargeHolder> result = new ArrayList<>();
        for (ChargeHolder holder : holders.values()) {
            if (holder.isActive() && holder.chargeId().equals(chargeId)) {
                result.add(holder);
            }
        }
        result.sort((a, b) -> Long.compare(a.assignedAt(), b.assignedAt()));
        return result;
    }

    public boolean isVacant(String chargeId) {
        return activeHoldersOf(chargeId).isEmpty();
    }

    /** The charges (definitions) a player actively holds. */
    public List<ChargeDef> chargesOf(UUID player) {
        List<ChargeDef> result = new ArrayList<>();
        for (ChargeHolder holder : activeHoldersOf(player)) {
            config.charges().get(holder.chargeId()).ifPresent(result::add);
        }
        return result;
    }

    /** The active holder records of a player (a player can hold several charges). */
    public List<ChargeHolder> activeHoldersOf(UUID player) {
        List<ChargeHolder> result = new ArrayList<>();
        if (player == null) {
            return result;
        }
        for (ChargeHolder holder : holders.values()) {
            if (holder.isActive() && holder.playerUuid().equals(player)) {
                result.add(holder);
            }
        }
        return result;
    }

    /** Whether the player holds any active charge that grants the capability. */
    public boolean has(UUID player, PoliticalCapability capability) {
        for (ChargeDef charge : chargesOf(player)) {
            if (charge.grants(capability)) {
                return true;
            }
        }
        return false;
    }

    /** Whether the player holds the capability through a charge of the named government. */
    public boolean hasInGovernment(UUID player, PoliticalCapability capability, String governmentId) {
        for (ChargeHolder holder : activeHoldersOf(player)) {
            if (!holder.governmentId().equals(governmentId)) {
                continue;
            }
            if (config.charges().get(holder.chargeId()).map(c -> c.grants(capability)).orElse(false)) {
                return true;
            }
        }
        return false;
    }

    /** Charge ids of a government whose holders carry the capability - used by the public API. */
    public List<String> chargesWithCapability(String governmentId, PoliticalCapability capability) {
        return config.charges().ofGovernment(governmentId).stream()
                .filter(charge -> charge.grants(capability))
                .map(ChargeDef::id)
                .toList();
    }

    // --- assignment --------------------------------------------------------------------------

    /**
     * Core assignment used by elections, appointment, conquest and admin. Validates the charge exists,
     * the holder cap, and computes the term. When {@code replaceWhenFull} and a single-holder charge is
     * full, the incumbent is vacated first (an election winner or a fresh appointment supersedes).
     */
    public synchronized PoliticsResult assign(UUID player, String chargeId, String assignedBy,
                                              boolean replaceWhenFull) {
        ChargeDef charge = config.charges().get(chargeId).orElse(null);
        if (charge == null) {
            return PoliticsResult.fail("charge.unknown", "id", chargeId);
        }
        for (ChargeHolder existing : activeHoldersOf(chargeId)) {
            if (existing.playerUuid().equals(player)) {
                return PoliticsResult.fail("charge.already_held");
            }
        }
        List<ChargeHolder> current = activeHoldersOf(chargeId);
        if (current.size() >= charge.maxHolders()) {
            if (!replaceWhenFull) {
                return PoliticsResult.fail("charge.full", "charge", charge.displayName());
            }
            // Make room: vacate the oldest incumbent.
            vacate(current.get(0), HolderStatus.REMOVED);
        }
        long now = System.currentTimeMillis();
        long expiresAt = charge.hasTerm()
                ? now + config.settings().realMillisFromDays(charge.termDurationDays())
                : 0L;
        ChargeHolder holder = new ChargeHolder(Ids.prefixed("holder"), player, chargeId,
                charge.governmentId(), now, expiresAt, assignedBy);
        holders.put(holder.id(), holder);
        adapters.storage().saveHolder(holder);
        issueCredential(holder, charge);
        return PoliticsResult.ok("charge.assigned", "charge", charge.displayName()).withPayload(holder);
    }

    /** Appointment by a holder of APPOINT (or admin). The mechanism must be appointment. */
    public synchronized PoliticsResult appoint(UUID actor, boolean admin, UUID target, String chargeId) {
        ChargeDef charge = config.charges().get(chargeId).orElse(null);
        if (charge == null) {
            return PoliticsResult.fail("charge.unknown", "id", chargeId);
        }
        if (!charge.mechanism().is("appointment") && !admin) {
            return PoliticsResult.fail("charge.not_appointment", "charge", charge.displayName());
        }
        if (!admin && !hasInGovernment(actor, PoliticalCapability.APPOINT, charge.governmentId())) {
            return PoliticsResult.fail("general.no_capability");
        }
        return assign(target, chargeId, actor == null ? "admin" : actor.toString(), true);
    }

    /** Removal by a holder of REMOVE (or admin). */
    public synchronized PoliticsResult remove(UUID actor, boolean admin, UUID target, String chargeId) {
        ChargeDef charge = config.charges().get(chargeId).orElse(null);
        if (charge == null) {
            return PoliticsResult.fail("charge.unknown", "id", chargeId);
        }
        if (!admin && !hasInGovernment(actor, PoliticalCapability.REMOVE, charge.governmentId())) {
            return PoliticsResult.fail("general.no_capability");
        }
        ChargeHolder holder = activeHoldersOf(chargeId).stream()
                .filter(h -> h.playerUuid().equals(target))
                .findFirst().orElse(null);
        if (holder == null) {
            return PoliticsResult.fail("charge.not_held_by_target", "charge", charge.displayName());
        }
        vacate(holder, HolderStatus.REMOVED);
        return PoliticsResult.ok("charge.removed", "charge", charge.displayName());
    }

    /** Admin force-assign, bypassing capability and mechanism checks. */
    public synchronized PoliticsResult adminAssign(UUID target, String chargeId) {
        return assign(target, chargeId, "admin", true);
    }

    /** Admin: vacate every active holder of a charge. */
    public synchronized PoliticsResult adminVacate(String chargeId) {
        ChargeDef charge = config.charges().get(chargeId).orElse(null);
        if (charge == null) {
            return PoliticsResult.fail("charge.unknown", "id", chargeId);
        }
        List<ChargeHolder> current = activeHoldersOf(chargeId);
        for (ChargeHolder holder : current) {
            vacate(holder, HolderStatus.REMOVED);
        }
        return PoliticsResult.ok("charge.vacated", "charge", charge.displayName(),
                "count", String.valueOf(current.size()));
    }

    // --- hereditary succession ---------------------------------------------------------------

    /** A hereditary holder designates their successor. */
    public synchronized PoliticsResult designateSuccessor(UUID actor, UUID successor) {
        ChargeHolder holder = activeHoldersOf(actor).stream()
                .filter(h -> config.charges().get(h.chargeId())
                        .map(c -> c.mechanism().is("hereditary")).orElse(false))
                .findFirst().orElse(null);
        if (holder == null) {
            return PoliticsResult.fail("charge.no_hereditary");
        }
        holder.setSuccessor(successor);
        adapters.storage().saveHolder(holder);
        return PoliticsResult.ok("charge.successor_set");
    }

    // --- lifecycle ---------------------------------------------------------------------------

    /**
     * Expires every active holder whose term has elapsed. Returns the affected records so the caller
     * (the lifecycle task) can react: re-run an election, apply a succession, or just leave it vacant.
     */
    public synchronized List<ChargeHolder> expireDue(long now) {
        List<ChargeHolder> expired = new ArrayList<>();
        for (ChargeHolder holder : new ArrayList<>(holders.values())) {
            if (holder.isActive() && holder.isExpired(now)) {
                vacate(holder, HolderStatus.EXPIRED);
                expired.add(holder);
            }
        }
        return expired;
    }

    /**
     * Applies a hereditary succession when a holder leaves a hereditary charge: if a successor was
     * designated and is not already a holder, they take the charge. Returns the new holder, if any.
     */
    public synchronized Optional<ChargeHolder> applySuccession(ChargeHolder vacated) {
        ChargeDef charge = config.charges().get(vacated.chargeId()).orElse(null);
        if (charge == null || !charge.mechanism().is("hereditary") || vacated.successor() == null) {
            return Optional.empty();
        }
        PoliticsResult result = assign(vacated.successor(), vacated.chargeId(), "succession", true);
        return result.success() && result.payload() instanceof ChargeHolder heir
                ? Optional.of(heir) : Optional.empty();
    }

    /**
     * Reconciles a conquest charge with the region it tracks: if the controller changed, the new
     * controller takes the charge and the previous holder is vacated. No region backend → no change.
     */
    public synchronized void reconcileConquest(String chargeId) {
        ChargeDef charge = config.charges().get(chargeId).orElse(null);
        if (charge == null || !charge.mechanism().is("conquest") || !adapters.region().available()) {
            return;
        }
        String region = charge.mechanism().string("control_region", "");
        if (region.isBlank()) {
            return;
        }
        UUID controller = adapters.region().controller(region).orElse(null);
        if (controller == null) {
            return;
        }
        boolean alreadyHeld = activeHoldersOf(chargeId).stream()
                .anyMatch(h -> h.playerUuid().equals(controller));
        if (!alreadyHeld) {
            assign(controller, chargeId, "conquest", true);
        }
    }

    // --- internal ----------------------------------------------------------------------------

    private void vacate(ChargeHolder holder, HolderStatus status) {
        holder.setStatus(status);
        adapters.storage().saveHolder(holder);
        config.charges().get(holder.chargeId())
                .ifPresent(charge -> revokeCredential(holder, charge));
    }

    private void issueCredential(ChargeHolder holder, ChargeDef charge) {
        if (adapters.identity().available()) {
            String governmentName = config.governments().get(charge.governmentId())
                    .map(g -> g.displayName()).orElse(charge.governmentId());
            adapters.identity().issueCredential(holder.playerUuid(), charge.displayName(),
                    governmentName, holder.assignedAt(), holder.expiresAt());
        }
    }

    private void revokeCredential(ChargeHolder holder, ChargeDef charge) {
        if (adapters.identity().available()) {
            adapters.identity().revokeCredential(holder.playerUuid(), charge.id());
        }
    }
}
