package dev.openrp.fdo.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import dev.openrp.fdo.OpenFdoPlugin;
import dev.openrp.fdo.adapter.DetentionAdapter;
import dev.openrp.fdo.model.DetentionEndReason;
import dev.openrp.fdo.model.DetentionOrder;

/**
 * Owns the sentence timer and the release; delegates the <em>physical</em> meaning of detention to a
 * {@link DetentionAdapter} when one is registered, and otherwise records the conviction in the
 * dossier and leaves execution to manual roleplay. The core never builds a prison - it times the
 * sentence and signals the adapter.
 */
public final class DetentionManager {

    private final OpenFdoPlugin plugin;
    private final Map<UUID, DetentionOrder> active = new HashMap<>();
    private final Map<UUID, BukkitTask> tasks = new HashMap<>();

    public DetentionManager(OpenFdoPlugin plugin) {
        this.plugin = plugin;
    }

    /** Loads persisted orders and reschedules their release timers (does not re-trigger the adapter). */
    public synchronized void loadAll() {
        cancelTasks();
        active.clear();
        for (DetentionOrder order : plugin.adapters().storage().loadDetentions()) {
            active.put(order.inmate(), order);
        }
        long now = System.currentTimeMillis();
        for (DetentionOrder order : new ArrayList<>(active.values())) {
            if (order.releaseAt() <= now) {
                purgeExpired(order);
            } else {
                scheduleRelease(order);
            }
        }
    }

    /**
     * Drops a persisted order whose sentence already elapsed while the server was offline, WITHOUT
     * signalling the detention adapter or writing a dossier note: the adapter has its own persistence
     * and the inmate was released (or is offline), so re-firing a physical release on load would be a
     * spurious side effect. Still-running orders go through {@link #scheduleRelease} instead.
     */
    private void purgeExpired(DetentionOrder order) {
        active.remove(order.inmate());
        cancelTask(order.inmate());
        plugin.adapters().storage().deleteDetention(order.inmate());
        plugin.adapters().logging().log("detention purge-on-load inmate=" + order.inmate()
                + " dossier=" + order.dossierId() + " (sentence already elapsed offline)");
    }

    public Collection<DetentionOrder> active() {
        return new ArrayList<>(active.values());
    }

    public Optional<DetentionOrder> find(UUID inmate) {
        return Optional.ofNullable(inmate == null ? null : active.get(inmate));
    }

    public boolean isDetained(UUID inmate) {
        return inmate != null && active.containsKey(inmate);
    }

    /** Starts detention: persists the order, signals the adapter (if any) and arms the release timer. */
    public synchronized FdoResult begin(DetentionOrder order) {
        if (order == null) {
            return FdoResult.fail("detention.invalid");
        }
        active.put(order.inmate(), order);
        plugin.adapters().storage().saveDetention(order);
        Optional<DetentionAdapter> adapter = plugin.adapters().detention();
        adapter.ifPresent(detention -> safeAdapter(() -> detention.beginDetention(order), "beginDetention"));
        scheduleRelease(order);
        if (order.dossierId() != null) {
            plugin.dossiers().addNote(order.dossierId(),
                    "Detention started (" + order.sentenceSeconds() + "s, level " + order.securityLevel()
                            + (adapter.isPresent() ? ", adapter " + adapter.get().id() : ", recorded only") + ").");
        }
        plugin.adapters().logging().log("detention begin inmate=" + order.inmate()
                + " dossier=" + order.dossierId() + " seconds=" + order.sentenceSeconds());
        return FdoResult.ok("detention.begun", "name", order.inmateName());
    }

    /** Ends detention for any reason: cancels the timer, signals the adapter and notes the dossier. */
    public synchronized FdoResult end(UUID inmate, DetentionEndReason reason) {
        DetentionOrder order = active.remove(inmate);
        if (order == null) {
            return FdoResult.fail("detention.not_detained");
        }
        cancelTask(inmate);
        plugin.adapters().storage().deleteDetention(inmate);
        plugin.adapters().detention().ifPresent(detention ->
                safeAdapter(() -> detention.endDetention(inmate, reason), "endDetention"));
        if (order.dossierId() != null) {
            plugin.dossiers().addNote(order.dossierId(), "Detention ended: " + reason.name().toLowerCase() + ".");
        }
        plugin.adapters().logging().log("detention end inmate=" + inmate + " reason=" + reason.name());
        return FdoResult.ok("detention.ended", "name", order.inmateName(),
                "reason", plugin.messages().text(plugin.getServer().getConsoleSender(), reason.messageKey()));
    }

    /**
     * Hook for the detention adapter (and the public API): reports a confirmed escape. Updates the
     * dossier, alerts on-duty members and proposes - but does not auto-apply - wanted status (a member
     * with {@code FLAG_WANTED} confirms it manually). Ends the order with reason ESCAPE.
     */
    public synchronized FdoResult reportEscape(UUID inmate, String world, double x, double y, double z) {
        DetentionOrder order = active.get(inmate);
        if (order == null) {
            return FdoResult.fail("detention.not_detained");
        }
        if (order.dossierId() != null) {
            plugin.dossiers().addNote(order.dossierId(),
                    "Escape reported at " + world + " " + (int) x + "," + (int) y + "," + (int) z + ".");
        }
        notifyOnDuty("detention.escape_alert", "name", order.inmateName(),
                "where", world + " " + (int) x + "," + (int) y + "," + (int) z);
        plugin.adapters().logging().log("detention escape inmate=" + inmate + " at=" + world + ":" + (int) x + "," + (int) y + "," + (int) z);
        end(inmate, DetentionEndReason.ESCAPE);
        return FdoResult.ok("detention.escape_recorded", "name", order.inmateName());
    }

    /** Cancels every pending release task. Called on disable/reload before rescheduling. */
    public synchronized void shutdown() {
        cancelTasks();
    }

    private void scheduleRelease(DetentionOrder order) {
        cancelTask(order.inmate());
        long remainingMs = order.releaseAt() - System.currentTimeMillis();
        long ticks = Math.max(1L, remainingMs / 50L);
        BukkitTask task = plugin.getServer().getScheduler().runTaskLater(plugin,
                () -> end(order.inmate(), DetentionEndReason.SENTENCE_SERVED), ticks);
        tasks.put(order.inmate(), task);
    }

    private void cancelTask(UUID inmate) {
        BukkitTask task = tasks.remove(inmate);
        if (task != null) {
            task.cancel();
        }
    }

    private void cancelTasks() {
        for (BukkitTask task : tasks.values()) {
            task.cancel();
        }
        tasks.clear();
    }

    private void notifyOnDuty(String key, Object... placeholders) {
        for (UUID uuid : plugin.duty().onDutyEverywhere()) {
            Player player = plugin.getServer().getPlayer(uuid);
            if (player != null) {
                plugin.adapters().notification().notify(player,
                        plugin.messages().prefixed(player, key, placeholders));
            }
        }
    }

    private void safeAdapter(Runnable action, String label) {
        try {
            action.run();
        } catch (RuntimeException | LinkageError error) {
            plugin.getLogger().warning("[OpenFDO] Detention adapter " + label + " failed: " + error.getMessage());
        }
    }
}
