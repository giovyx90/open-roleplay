package dev.openrp.jobs.core;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import dev.openrp.jobs.OpenJobsPlugin;
import dev.openrp.jobs.config.Job;
import dev.openrp.jobs.config.LocationType;
import dev.openrp.jobs.config.Transformation;
import dev.openrp.jobs.model.ActivityEntry;
import dev.openrp.jobs.model.PayoutBreakdown;
import dev.openrp.jobs.model.Season;
import dev.openrp.jobs.model.SessionStatus;
import dev.openrp.jobs.model.WorkLocation;
import dev.openrp.jobs.model.WorkRecord;
import dev.openrp.jobs.model.WorkSession;

/**
 * The heart of Open Jobs: it starts, tracks, pauses and ends work sessions, and pays out at the end.
 * RP First - it records the activity physically done (blocks broken, fish caught, items crafted), never
 * the time merely spent. Leaving the region pauses a session (clock stopped); returning resumes it;
 * staying out too long abandons it. One session per player at a time.
 */
public final class SessionManager {

    private final OpenJobsPlugin plugin;
    private final java.util.Map<UUID, WorkSession> byPlayer = new ConcurrentHashMap<>();

    public SessionManager(OpenJobsPlugin plugin) {
        this.plugin = plugin;
    }

    public void loadAll() {
        byPlayer.clear();
        for (WorkSession session : plugin.adapters().storage().loadSessions()) {
            if (session.status() == SessionStatus.ACTIVE || session.status() == SessionStatus.PAUSED) {
                // A recovered session always starts paused: it resumes only when the worker is back in
                // the region, so downtime since the crash is never paid.
                session.setStatus(SessionStatus.PAUSED);
                byPlayer.put(session.player(), session);
            }
        }
    }

    public Optional<WorkSession> byPlayer(UUID player) {
        return Optional.ofNullable(byPlayer.get(player));
    }

    public List<WorkSession> active() {
        return new ArrayList<>(byPlayer.values());
    }

    // --- start --------------------------------------------------------------------------------

    public JobResult start(Player player) {
        if (byPlayer.containsKey(player.getUniqueId())) {
            return JobResult.fail("session.already_working");
        }
        if (plugin.settings().requireRegionBackend() && !plugin.adapters().region().available()) {
            return JobResult.fail("session.requires_region_backend");
        }
        Optional<String> region = plugin.adapters().region().regionAt(player.getLocation());
        if (region.isEmpty()) {
            return JobResult.fail("session.no_region");
        }
        WorkLocation location = plugin.locations().byRegion(region.get()).orElse(null);
        if (location == null) {
            return JobResult.fail("session.not_a_work_location");
        }
        Job job = plugin.config().jobs().get(location.jobId()).orElse(null);
        if (job == null) {
            return JobResult.fail("session.job_gone", "job", location.jobId());
        }
        // Region tag must match the job's location type (a real region backend can enforce this).
        LocationType type = plugin.config().locationTypes().get(job.locationType()).orElse(null);
        if (type != null && !plugin.adapters().region().hasTag(region.get(), type.regionTag())) {
            return JobResult.fail("session.wrong_location", "type", type.displayName());
        }
        if (!location.unlimited() && activeInLocation(location.id()) >= location.capacity()) {
            return JobResult.fail("session.location_full", "location", location.displayName());
        }
        if (job.requiresLicense() && !plugin.licenses().hasActive(player.getUniqueId(), job.id())) {
            if (job.license().autoIssue()) {
                plugin.licenses().issue(player, player.getUniqueId(), job, "system");
            } else {
                return JobResult.fail("session.need_license", "job", job.displayName());
            }
        }
        if (job.tool().enabled() && !hasTool(player, job.tool().material())) {
            return JobResult.fail("session.need_tool", "tool", pretty(job.tool().material()));
        }

        WorkSession session = new WorkSession(Ids.prefixed("ses"), player.getUniqueId(), job.id(),
                location.id(), System.currentTimeMillis());
        plugin.records().getOrCreate(player.getUniqueId(), job.id());
        byPlayer.put(player.getUniqueId(), session);
        plugin.adapters().storage().saveSession(session);
        return JobResult.ok("session.started", "job", job.displayName(), "location", location.displayName());
    }

    // --- end ----------------------------------------------------------------------------------

    public JobResult end(Player player) {
        WorkSession session = byPlayer.get(player.getUniqueId());
        if (session == null) {
            return JobResult.fail("session.not_working");
        }
        Job job = plugin.config().jobs().get(session.jobId()).orElse(null);
        if (job == null) {
            byPlayer.remove(player.getUniqueId());
            plugin.adapters().storage().deleteSession(session.id());
            return JobResult.fail("session.job_gone", "job", session.jobId());
        }
        // Delivery jobs only pay once the worker reaches the delivery point.
        if (plugin.payment().isDelivery(job) && !job.isTransformative() && !isAtDelivery(player, job)) {
            return JobResult.fail("session.not_delivered", "location", job.payment().deliveryLocation());
        }
        return finalize(player, session, job, false);
    }

    /** Finalises a session: computes pay, delivers it, updates the record and progression, persists. */
    private JobResult finalize(Player player, WorkSession session, Job job, boolean abandoned) {
        long now = System.currentTimeMillis();
        session.bankActiveTime(now);
        session.setStatus(abandoned ? SessionStatus.ABANDONED : SessionStatus.COMPLETED);
        session.setEndedAt(now);

        PayoutBreakdown breakdown = plugin.payment().compute(job, session, now, context(player, job, session, now));
        double amount = breakdown.total();
        if (abandoned && !plugin.settings().payoutPartialAbandoned()) {
            amount = 0.0;
        }

        WorkRecord record = plugin.records().getOrCreate(player.getUniqueId(), job.id());
        record.recordSession(session.totalProduced(), amount, now);
        boolean tierChanged = plugin.progression().refreshTier(record, now);
        session.setProgressionTier(record.currentTier());
        plugin.records().save(record);

        String method = amount > 0 ? deliverPayout(player, job, amount) : "none";

        byPlayer.remove(player.getUniqueId());
        plugin.adapters().storage().deleteSession(session.id());

        if (tierChanged && job.progressionEnabled()) {
            plugin.licenses().get(player.getUniqueId(), job.id())
                    .ifPresent(license -> plugin.licenses().refreshItemTier(license, tierDisplay(record)));
            plugin.adapters().notification().send(player, plugin.messages().prefixed(player,
                    "progression.tier_up", "job", job.displayName(), "tier", tierDisplay(record)));
        }

        String key = abandoned ? "session.abandoned_paid" : "session.ended";
        return JobResult.ok(key,
                "job", job.displayName(),
                "produced", String.valueOf(session.totalProduced()),
                "amount", money(amount),
                "tier", tierDisplay(record),
                "method", method);
    }

    /** Pays the worker: redirect to employer, else economy, else degrade to a recorded-only payout. */
    private String deliverPayout(Player player, Job job, double amount) {
        if (plugin.adapters().company().available()
                && plugin.adapters().company().shouldRedirectPayout(player.getUniqueId(), job.id())) {
            plugin.adapters().company().notifyPayout(player.getUniqueId(), job.id(), amount);
            return "company";
        }
        if (plugin.adapters().economy().available()
                && plugin.adapters().economy().pay(player.getUniqueId(), amount, "JOB_PAYOUT:" + job.id())) {
            return "economy";
        }
        // No economy backend: the amount is recorded in the work record and the worker is told. A
        // currency-item bridge can deliver physical pay; the setting-neutral core has no money item.
        plugin.adapters().company().notifyPayout(player.getUniqueId(), job.id(), amount);
        return "recorded";
    }

    // --- activity tracking --------------------------------------------------------------------

    public void recordBlockBreak(Player player, Material material, int x, int y, int z) {
        record(player, dev.openrp.jobs.model.ActivityDetection.BLOCK_BREAK, material, 1, x, y, z);
    }

    public void recordFishing(Player player, Material material) {
        org.bukkit.Location at = player.getLocation();
        record(player, dev.openrp.jobs.model.ActivityDetection.FISHING, material, 1,
                at.getBlockX(), at.getBlockY(), at.getBlockZ());
    }

    private void record(Player player, dev.openrp.jobs.model.ActivityDetection detection,
                        Material material, int quantity, int x, int y, int z) {
        WorkSession session = byPlayer.get(player.getUniqueId());
        if (session == null || !session.isActive()) {
            return;
        }
        Job job = plugin.config().jobs().get(session.jobId()).orElse(null);
        LocationType type = job == null ? null : plugin.config().locationTypes().get(job.locationType()).orElse(null);
        if (type == null || type.activityDetection() != detection || !type.accepts(material.name())) {
            return;
        }
        long now = System.currentTimeMillis();
        session.record(new ActivityEntry(now, detection.name().toLowerCase(Locale.ROOT), material.name(), quantity, x, y, z), now);
        plugin.adapters().storage().saveSession(session);
    }

    /** Records a completed workshop transformation, gated by its minimum craft time. */
    public void recordTransformation(Player player, ItemStack crafted) {
        WorkSession session = byPlayer.get(player.getUniqueId());
        if (session == null || !session.isActive() || crafted == null) {
            return;
        }
        Job job = plugin.config().jobs().get(session.jobId()).orElse(null);
        LocationType type = job == null ? null : plugin.config().locationTypes().get(job.locationType()).orElse(null);
        if (job == null || type == null || type.activityDetection() != dev.openrp.jobs.model.ActivityDetection.CRAFTING
                || !job.isTransformative()) {
            return;
        }
        long now = System.currentTimeMillis();
        List<Transformation> transformations = job.transformations();
        for (int i = 0; i < transformations.size(); i++) {
            Transformation transformation = transformations.get(i);
            if (!producesMaterial(transformation, crafted.getType())) {
                continue;
            }
            long elapsed = now - session.lastTransformAt(i);
            if (elapsed < transformation.craftTimeSeconds() * 1000L) {
                continue;
            }
            session.markTransform(i, now);
            session.addTransformationEarnings(transformation.payout());
            org.bukkit.Location at = player.getLocation();
            session.record(new ActivityEntry(now, "crafting", crafted.getType().name(), crafted.getAmount(),
                    at.getBlockX(), at.getBlockY(), at.getBlockZ()), now);
            plugin.adapters().storage().saveSession(session);
            return;
        }
    }

    // --- pause / resume / abandon -------------------------------------------------------------

    /** Re-evaluates whether the player is still in their session's location; pauses or resumes as needed. */
    public void refreshPresence(Player player) {
        WorkSession session = byPlayer.get(player.getUniqueId());
        if (session == null) {
            return;
        }
        WorkLocation location = plugin.locations().get(session.locationId()).orElse(null);
        boolean inside = location != null && plugin.adapters().region().regionAt(player.getLocation())
                .map(region -> region.equals(location.regionId())).orElse(false);
        long now = System.currentTimeMillis();
        if (inside && session.status() == SessionStatus.PAUSED) {
            session.setStatus(SessionStatus.ACTIVE);
            session.resumeClock(now);
            session.setLeftRegionAt(0L);
            plugin.adapters().storage().saveSession(session);
            plugin.adapters().notification().actionBar(player, plugin.messages().prefixed(player, "session.resumed"));
        } else if (!inside && session.status() == SessionStatus.ACTIVE) {
            session.bankActiveTime(now);
            session.setStatus(SessionStatus.PAUSED);
            session.setLeftRegionAt(now);
            plugin.adapters().storage().saveSession(session);
            plugin.adapters().notification().actionBar(player, plugin.messages().prefixed(player, "session.paused"));
        }
    }

    /** Pauses without abandoning when the worker logs out, so they can resume on return. */
    public void pauseForQuit(Player player) {
        WorkSession session = byPlayer.get(player.getUniqueId());
        if (session != null && session.status() == SessionStatus.ACTIVE) {
            session.bankActiveTime(System.currentTimeMillis());
            session.setStatus(SessionStatus.PAUSED);
            session.setLeftRegionAt(System.currentTimeMillis());
            plugin.adapters().storage().saveSession(session);
        }
    }

    /** Ends paused sessions whose worker has been away longer than the abandon window. */
    public void checkAbandoned() {
        long now = System.currentTimeMillis();
        long limit = plugin.settings().sessionAbandonedMillis();
        for (WorkSession session : new ArrayList<>(byPlayer.values())) {
            if (session.status() != SessionStatus.PAUSED || session.leftRegionAt() <= 0L) {
                continue;
            }
            if (now - session.leftRegionAt() < limit) {
                continue;
            }
            Player player = plugin.getServer().getPlayer(session.player());
            Job job = plugin.config().jobs().get(session.jobId()).orElse(null);
            if (job == null) {
                byPlayer.remove(session.player());
                plugin.adapters().storage().deleteSession(session.id());
                continue;
            }
            if (player != null) {
                JobResult result = finalize(player, session, job, true);
                plugin.messages().warning(player, result.messageKey(), result.placeholders());
            } else {
                finalizeOffline(session, job);
            }
        }
    }

    /** Abandon path for an offline worker: record the session and pay only if partial pay is on and an economy exists. */
    private void finalizeOffline(WorkSession session, Job job) {
        long now = System.currentTimeMillis();
        session.bankActiveTime(now);
        session.setStatus(SessionStatus.ABANDONED);
        double amount = plugin.settings().payoutPartialAbandoned()
                ? offlineBase(job, session, now)
                : 0.0;
        WorkRecord record = plugin.records().getOrCreate(session.player(), job.id());
        record.recordSession(session.totalProduced(), amount, now);
        plugin.progression().refreshTier(record, now);
        plugin.records().save(record);
        if (amount > 0 && plugin.adapters().economy().available()) {
            plugin.adapters().economy().pay(session.player(), amount, "JOB_PAYOUT:" + job.id());
        }
        byPlayer.remove(session.player());
        plugin.adapters().storage().deleteSession(session.id());
    }

    private double offlineBase(Job job, WorkSession session, long now) {
        // Offline abandon cannot resolve cooperative/tool/shift context; pay the bare progression-adjusted base.
        WorkRecord record = plugin.records().get(session.player(), job.id());
        double progression = record == null ? 1.0 : plugin.progression().payMultiplier(record, job, now);
        return plugin.payment().basePay(job, session, now) * progression;
    }

    /** Forcibly ends a player's session (admin command, licence revocation). */
    public JobResult forceEnd(UUID player) {
        WorkSession session = byPlayer.get(player);
        if (session == null) {
            return JobResult.fail("admin.no_session");
        }
        Job job = plugin.config().jobs().get(session.jobId()).orElse(null);
        Player online = plugin.getServer().getPlayer(player);
        if (job != null && online != null) {
            finalize(online, session, job, true);
        } else if (job != null) {
            finalizeOffline(session, job);
        } else {
            byPlayer.remove(player);
            plugin.adapters().storage().deleteSession(session.id());
        }
        return JobResult.ok("admin.session_terminated");
    }

    // --- estimates / helpers ------------------------------------------------------------------

    /** The current payout estimate for a live session (for {@code /lavoro stato}). */
    public PayoutBreakdown estimate(Player player, WorkSession session) {
        Job job = plugin.config().jobs().get(session.jobId()).orElse(null);
        if (job == null) {
            return PayoutBreakdown.none();
        }
        long now = System.currentTimeMillis();
        return plugin.payment().compute(job, session, now, context(player, job, session, now));
    }

    public int activeInLocation(String locationId) {
        int count = 0;
        for (WorkSession session : byPlayer.values()) {
            if (session.locationId().equals(locationId) && session.isActive()) {
                count++;
            }
        }
        return count;
    }

    /** Concurrent workers of the same job in the same location, including the given session (>= 1). */
    public int cooperativeParticipants(WorkSession session) {
        int count = 0;
        for (WorkSession other : byPlayer.values()) {
            if (other.isActive() && other.jobId().equals(session.jobId())
                    && other.locationId().equals(session.locationId())) {
                count++;
            }
        }
        return Math.max(1, count);
    }

    private PaymentService.Context context(Player player, Job job, WorkSession session, long now) {
        WorkRecord record = plugin.records().getOrCreate(player.getUniqueId(), job.id());
        double progression = plugin.progression().payMultiplier(record, job, now);
        int participants = cooperativeParticipants(session);
        String tool = bestTool(player, job);
        return new PaymentService.Context(progression, participants, tool, Season.currentReal(), LocalTime.now(),
                plugin.settings().cooperativeEnabled(), plugin.settings().seasonalEnabled());
    }

    /** The highest-bonus tool the player holds for this job, or the held item, for the tool multiplier. */
    private String bestTool(Player player, Job job) {
        if (!job.tool().enabled()) {
            return null;
        }
        String best = null;
        double bestBonus = 1.0;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item == null) {
                continue;
            }
            double bonus = job.tool().bonusFor(item.getType().name());
            if (bonus > bestBonus) {
                bestBonus = bonus;
                best = item.getType().name();
            }
        }
        return best;
    }

    private boolean isAtDelivery(Player player, Job job) {
        String target = job.payment().deliveryLocation();
        if (target == null || target.isBlank() || !plugin.adapters().region().available()) {
            return true;
        }
        String current = plugin.adapters().region().regionAt(player.getLocation()).orElse(null);
        if (current == null) {
            return false;
        }
        if (current.equals(target)) {
            return true;
        }
        WorkLocation location = plugin.locations().get(target).orElse(null);
        return location != null && current.equals(location.regionId());
    }

    private boolean hasTool(Player player, String material) {
        Material tool = Material.matchMaterial(material);
        if (tool == null) {
            return true; // a misconfigured tool material must not lock everyone out of the job
        }
        return player.getInventory().contains(tool);
    }

    private boolean producesMaterial(Transformation transformation, Material material) {
        for (var output : transformation.outputs()) {
            if (material.name().equalsIgnoreCase(output.material()) || material == Material.matchMaterial(output.material())) {
                return true;
            }
        }
        return false;
    }

    private String tierDisplay(WorkRecord record) {
        return plugin.config().progression().byId(record.currentTier())
                .map(dev.openrp.jobs.config.ProgressionTier::displayName)
                .orElse(record.currentTier());
    }

    public static String money(double amount) {
        return String.format(Locale.ROOT, "%.2f", amount);
    }

    public static String pretty(String material) {
        return material == null ? "" : material.toLowerCase(Locale.ROOT).replace('_', ' ');
    }
}
