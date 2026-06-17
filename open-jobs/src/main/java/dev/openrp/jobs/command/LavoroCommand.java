package dev.openrp.jobs.command;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import dev.openrp.jobs.OpenJobsPlugin;
import dev.openrp.jobs.config.Job;
import dev.openrp.jobs.core.JobResult;
import dev.openrp.jobs.core.SessionManager;
import dev.openrp.jobs.model.PayoutBreakdown;
import dev.openrp.jobs.model.WorkLicense;
import dev.openrp.jobs.model.WorkLocation;
import dev.openrp.jobs.model.WorkRecord;
import dev.openrp.jobs.model.WorkSession;

/**
 * {@code /lavoro} - everything a worker does, plus the admin subtree behind a separate permission.
 * The work itself is physical: the player goes to a location, runs {@code inizia}, does the work, and
 * runs {@code fine} to be paid. Nothing here is a menu; the command only starts, ends and reports.
 */
public final class LavoroCommand extends BaseJobsCommand {

    private static final List<String> PLAYER_SUBS =
            List.of("lista", "info", "inizia", "fine", "stato", "profilo", "licenza");
    private static final List<String> ADMIN_SUBS =
            List.of("licenza", "sessione", "location", "stats", "reload");
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(ZoneId.systemDefault());

    public LavoroCommand(OpenJobsPlugin plugin) {
        super(plugin);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String sub = args.length == 0 ? "lista" : args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "lista" -> list(sender);
            case "info" -> info(sender, args);
            case "inizia", "start" -> startSession(sender);
            case "fine", "stop" -> endSession(sender);
            case "stato", "status" -> status(sender);
            case "profilo", "profile" -> profile(sender);
            case "licenza", "license" -> licenses(sender);
            case "admin" -> admin(sender, args);
            default -> plugin.messages().warning(sender, "general.unknown_subcommand");
        }
        return true;
    }

    // --- player ------------------------------------------------------------------------------

    private void list(CommandSender sender) {
        plugin.messages().info(sender, "list.header");
        if (plugin.config().jobs().all().isEmpty()) {
            plugin.messages().plain(sender, "list.empty");
            return;
        }
        for (Job job : plugin.config().jobs().all()) {
            plugin.messages().plain(sender, "list.entry",
                    "job", job.displayName(),
                    "id", job.id(),
                    "model", model(sender, job),
                    "license", job.requiresLicense() ? plugin.messages().text(sender, "list.license_yes")
                            : plugin.messages().text(sender, "list.license_no"),
                    "locations", String.valueOf(plugin.locations().forJob(job.id()).size()));
        }
    }

    private void info(CommandSender sender, String[] args) {
        if (args.length < 2) {
            plugin.messages().warning(sender, "info.usage");
            return;
        }
        Job job = plugin.config().jobs().get(args[1]).orElse(null);
        if (job == null) {
            plugin.messages().warning(sender, "info.unknown", "job", args[1]);
            return;
        }
        plugin.messages().info(sender, "info.header", "job", job.displayName());
        plugin.messages().plain(sender, "info.basics",
                "category", job.category(),
                "type", job.locationType(),
                "model", model(sender, job));
        plugin.messages().plain(sender, "info.license",
                "license", job.requiresLicense() ? plugin.messages().text(sender, "list.license_yes")
                        : plugin.messages().text(sender, "list.license_no"));
        List<WorkLocation> locations = plugin.locations().forJob(job.id());
        if (locations.isEmpty()) {
            plugin.messages().plain(sender, "info.no_locations");
        } else {
            for (WorkLocation location : locations) {
                plugin.messages().plain(sender, "info.location",
                        "location", location.displayName(),
                        "capacity", location.unlimited() ? "-" : String.valueOf(location.capacity()));
            }
        }
        if (job.progressionEnabled() && !plugin.config().progression().isEmpty()) {
            StringBuilder tiers = new StringBuilder();
            plugin.config().progression().tiers().forEach(tier -> tiers.append(tier.displayName())
                    .append(" (").append(tier.sessionsRequired()).append(") "));
            plugin.messages().plain(sender, "info.progression", "tiers", tiers.toString().trim());
        }
    }

    private void startSession(CommandSender sender) {
        Player player = requirePlayer(sender);
        if (player != null) {
            send(player, plugin.sessions().start(player));
        }
    }

    private void endSession(CommandSender sender) {
        Player player = requirePlayer(sender);
        if (player != null) {
            send(player, plugin.sessions().end(player));
        }
    }

    private void status(CommandSender sender) {
        Player player = requirePlayer(sender);
        if (player == null) {
            return;
        }
        WorkSession session = plugin.sessions().byPlayer(player.getUniqueId()).orElse(null);
        if (session == null) {
            plugin.messages().warning(player, "session.not_working");
            return;
        }
        Job job = plugin.config().jobs().get(session.jobId()).orElse(null);
        WorkLocation location = plugin.locations().get(session.locationId()).orElse(null);
        PayoutBreakdown estimate = plugin.sessions().estimate(player, session);
        long minutes = session.activeMillis(System.currentTimeMillis()) / 60_000L;
        plugin.messages().info(player, "status.header",
                "job", job == null ? session.jobId() : job.displayName());
        plugin.messages().plain(player, "status.body",
                "location", location == null ? "-" : location.displayName(),
                "state", plugin.messages().text(player, "status.state_" + session.status().name().toLowerCase(Locale.ROOT)),
                "minutes", String.valueOf(minutes),
                "produced", String.valueOf(session.totalProduced()),
                "estimate", SessionManager.money(estimate.total()));
    }

    private void profile(CommandSender sender) {
        Player player = requirePlayer(sender);
        if (player != null) {
            showRecords(player, player.getUniqueId(), player.getName());
        }
    }

    private void showRecords(CommandSender viewer, UUID target, String targetName) {
        List<WorkRecord> records = plugin.records().forPlayer(target);
        plugin.messages().info(viewer, "profile.header", "player", targetName);
        if (records.isEmpty()) {
            plugin.messages().plain(viewer, "profile.empty");
            return;
        }
        long now = System.currentTimeMillis();
        for (WorkRecord record : records) {
            Job job = plugin.config().jobs().get(record.jobId()).orElse(null);
            String tier = plugin.config().progression().byId(record.currentTier())
                    .map(dev.openrp.jobs.config.ProgressionTier::displayName).orElse(record.currentTier());
            String next = plugin.progression().sessionsToNextTier(record, now)
                    .map(String::valueOf).orElse("-");
            plugin.messages().plain(viewer, "profile.entry",
                    "job", job == null ? record.jobId() : job.displayName(),
                    "tier", tier.isEmpty() ? "-" : tier,
                    "sessions", String.valueOf(record.totalSessions()),
                    "produced", String.valueOf(record.totalProduced()),
                    "payout", SessionManager.money(record.totalPayout()),
                    "next", next);
        }
    }

    private void licenses(CommandSender sender) {
        Player player = requirePlayer(sender);
        if (player == null) {
            return;
        }
        List<WorkLicense> licenses = plugin.licenses().forPlayer(player.getUniqueId());
        plugin.messages().info(player, "license.header");
        if (licenses.isEmpty()) {
            plugin.messages().plain(player, "license.empty");
            return;
        }
        for (WorkLicense license : licenses) {
            Job job = plugin.config().jobs().get(license.jobId()).orElse(null);
            plugin.messages().plain(player, "license.entry",
                    "job", job == null ? license.jobId() : job.displayName(),
                    "status", plugin.messages().text(player, "license.status_" + license.status().name().toLowerCase(Locale.ROOT)),
                    "date", DATE.format(Instant.ofEpochMilli(license.issuedAt())));
        }
    }

    // --- admin -------------------------------------------------------------------------------

    private void admin(CommandSender sender, String[] args) {
        if (!isAdmin(sender)) {
            plugin.messages().warning(sender, "general.no_permission");
            return;
        }
        String sub = args.length < 2 ? "" : args[1].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "licenza", "license" -> adminLicense(sender, args);
            case "sessione", "session" -> adminSession(sender, args);
            case "location" -> adminLocation(sender, args);
            case "stats" -> adminStats(sender, args);
            case "reload" -> adminReload(sender);
            default -> plugin.messages().warning(sender, "admin.usage");
        }
    }

    private void adminLicense(CommandSender sender, String[] args) {
        // /lavoro admin licenza <emetti|revoca> <player> <job>
        if (args.length < 5) {
            plugin.messages().warning(sender, "admin.license_usage");
            return;
        }
        String action = args[2].toLowerCase(Locale.ROOT);
        OfflinePlayer target = resolvePlayer(args[3]);
        if (target == null || target.getUniqueId() == null) {
            plugin.messages().warning(sender, "general.player_not_found", "player", args[3]);
            return;
        }
        Job job = plugin.config().jobs().get(args[4]).orElse(null);
        if (job == null) {
            plugin.messages().warning(sender, "info.unknown", "job", args[4]);
            return;
        }
        if (action.equals("emetti") || action.equals("issue")) {
            Player online = target.getPlayer();
            plugin.licenses().issue(online, target.getUniqueId(), job, sender.getName());
            plugin.messages().success(sender, "admin.license_issued", "player", args[3], "job", job.displayName());
        } else if (action.equals("revoca") || action.equals("revoke")) {
            if (plugin.licenses().revoke(target.getUniqueId(), job.id())) {
                plugin.sessions().byPlayer(target.getUniqueId())
                        .filter(session -> session.jobId().equals(job.id()))
                        .ifPresent(session -> plugin.sessions().forceEnd(target.getUniqueId()));
                plugin.messages().success(sender, "admin.license_revoked", "player", args[3], "job", job.displayName());
            } else {
                plugin.messages().warning(sender, "admin.license_absent", "player", args[3], "job", job.displayName());
            }
        } else {
            plugin.messages().warning(sender, "admin.license_usage");
        }
    }

    private void adminSession(CommandSender sender, String[] args) {
        // /lavoro admin sessione termina <player>
        if (args.length < 4 || !args[2].equalsIgnoreCase("termina") && !args[2].equalsIgnoreCase("terminate")) {
            plugin.messages().warning(sender, "admin.session_usage");
            return;
        }
        OfflinePlayer target = resolvePlayer(args[3]);
        if (target == null || target.getUniqueId() == null) {
            plugin.messages().warning(sender, "general.player_not_found", "player", args[3]);
            return;
        }
        JobResult result = plugin.sessions().forceEnd(target.getUniqueId());
        send(sender, result);
    }

    private void adminLocation(CommandSender sender, String[] args) {
        // /lavoro admin location add <job> <region> [capacity]  | location remove <id>
        if (args.length < 3) {
            plugin.messages().warning(sender, "admin.location_usage");
            return;
        }
        String action = args[2].toLowerCase(Locale.ROOT);
        if (action.equals("add") && args.length >= 5) {
            Job job = plugin.config().jobs().get(args[3]).orElse(null);
            if (job == null) {
                plugin.messages().warning(sender, "info.unknown", "job", args[3]);
                return;
            }
            int capacity = args.length >= 6 ? parseInt(args[5], 0) : 0;
            WorkLocation location = plugin.locations().add(job.id(), args[4], args[4], capacity, false);
            plugin.messages().success(sender, "admin.location_added",
                    "id", location.id(), "job", job.displayName(), "region", args[4]);
        } else if (action.equals("remove") && args.length >= 4) {
            if (plugin.locations().remove(args[3])) {
                plugin.messages().success(sender, "admin.location_removed", "id", args[3]);
            } else {
                plugin.messages().warning(sender, "admin.location_absent", "id", args[3]);
            }
        } else {
            plugin.messages().warning(sender, "admin.location_usage");
        }
    }

    private void adminStats(CommandSender sender, String[] args) {
        // /lavoro admin stats <player|job>
        if (args.length < 3) {
            plugin.messages().warning(sender, "admin.stats_usage");
            return;
        }
        String token = args[2];
        Job job = plugin.config().jobs().get(token).orElse(null);
        if (job != null) {
            int sessions = 0;
            long produced = 0;
            double payout = 0;
            int workers = 0;
            for (WorkRecord record : plugin.records().all()) {
                if (record.jobId().equals(job.id())) {
                    sessions += record.totalSessions();
                    produced += record.totalProduced();
                    payout += record.totalPayout();
                    workers++;
                }
            }
            plugin.messages().info(sender, "admin.stats_job_header", "job", job.displayName());
            plugin.messages().plain(sender, "admin.stats_job_body",
                    "workers", String.valueOf(workers),
                    "sessions", String.valueOf(sessions),
                    "produced", String.valueOf(produced),
                    "payout", SessionManager.money(payout));
            return;
        }
        OfflinePlayer target = resolvePlayer(token);
        if (target == null || target.getUniqueId() == null) {
            plugin.messages().warning(sender, "general.player_not_found", "player", token);
            return;
        }
        showRecords(sender, target.getUniqueId(), token);
    }

    private void adminReload(CommandSender sender) {
        plugin.reloadAll();
        plugin.messages().success(sender, "general.reload_done");
    }

    private String model(CommandSender sender, Job job) {
        return plugin.messages().text(sender, "model." + job.paymentModel().name().toLowerCase(Locale.ROOT));
    }

    // --- tab completion ----------------------------------------------------------------------

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> subs = new ArrayList<>(PLAYER_SUBS);
            if (isAdmin(sender)) {
                subs.add("admin");
            }
            return CommandSuggestions.filter(subs, args[0]);
        }
        String sub = args[0].toLowerCase(Locale.ROOT);
        if (args.length == 2 && sub.equals("info")) {
            return CommandSuggestions.filter(jobIds(), args[1]);
        }
        if (sub.equals("admin") && isAdmin(sender)) {
            return adminTab(args);
        }
        return List.of();
    }

    private List<String> adminTab(String[] args) {
        if (args.length == 2) {
            return CommandSuggestions.filter(ADMIN_SUBS, args[1]);
        }
        String section = args[1].toLowerCase(Locale.ROOT);
        if (args.length == 3) {
            return switch (section) {
                case "licenza", "license" -> CommandSuggestions.filter(List.of("emetti", "revoca"), args[2]);
                case "sessione", "session" -> CommandSuggestions.filter(List.of("termina"), args[2]);
                case "location" -> CommandSuggestions.filter(List.of("add", "remove"), args[2]);
                case "stats" -> CommandSuggestions.filter(jobIds(), args[2]);
                default -> List.of();
            };
        }
        if (args.length == 4 && (section.equals("licenza") || section.equals("license")
                || section.equals("sessione") || section.equals("session"))) {
            return CommandSuggestions.filter(onlineNames(), args[3]);
        }
        if (args.length == 4 && section.equals("location") && args[2].equalsIgnoreCase("add")) {
            return CommandSuggestions.filter(jobIds(), args[3]);
        }
        if (args.length == 5 && (section.equals("licenza") || section.equals("license"))) {
            return CommandSuggestions.filter(jobIds(), args[4]);
        }
        return List.of();
    }
}
