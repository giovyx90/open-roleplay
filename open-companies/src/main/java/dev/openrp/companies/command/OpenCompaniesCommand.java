package dev.openrp.companies.command;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import dev.openrp.companies.OpenCompaniesPlugin;
import dev.openrp.companies.core.CompanyResult;
import dev.openrp.companies.model.Company;
import dev.openrp.companies.model.CompanyApplication;
import dev.openrp.companies.model.CompanyAsset;
import dev.openrp.companies.model.CompanyLicenseStatus;
import dev.openrp.companies.model.CompanyLicenseType;
import dev.openrp.companies.model.CompanyMember;
import dev.openrp.companies.model.CompanyRole;
import dev.openrp.companies.model.CompanyStatus;

/** {@code /company} (aliases {@code /opencompanies}, {@code /ocompanies}, {@code /companies}). */
public final class OpenCompaniesCommand implements CommandExecutor, TabCompleter {

    private static final List<String> SUBCOMMANDS = List.of("help", "create", "apply", "list", "info",
            "members", "invite", "fire", "role", "leave", "licenses", "assets", "reload", "admin");
    private static final List<String> ADMIN_SUBCOMMANDS = List.of("create", "delete", "setstatus", "setowner",
            "license", "sethq", "applications", "approve", "deny", "reload");
    private static final List<String> PLAYER_HELP_KEYS = List.of("help", "create", "apply", "list", "info",
            "members", "invite", "fire", "role", "leave", "licenses", "assets");

    private final OpenCompaniesPlugin plugin;

    public OpenCompaniesCommand(OpenCompaniesPlugin plugin) {
        this.plugin = plugin;
    }

    /** Exposed for tests. */
    public static List<String> rootSubcommands() {
        return SUBCOMMANDS;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String sub = args.length == 0 ? "help" : args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "help" -> sendHelp(sender);
            case "create" -> create(sender, args);
            case "apply" -> apply(sender, args);
            case "list" -> list(sender);
            case "info" -> info(sender, args);
            case "members" -> members(sender);
            case "invite" -> invite(sender, args);
            case "fire" -> fire(sender, args);
            case "role" -> role(sender, args);
            case "leave" -> leave(sender);
            case "licenses" -> licenses(sender);
            case "assets" -> assets(sender);
            case "reload" -> reload(sender);
            case "admin" -> admin(sender, args);
            default -> plugin.messages().warning(sender, "general.unknown_subcommand");
        }
        return true;
    }

    // --- player commands ---------------------------------------------------------------------

    private void sendHelp(CommandSender sender) {
        plugin.messages().info(sender, "command.header");
        for (String key : PLAYER_HELP_KEYS) {
            plugin.messages().info(sender, "command.help." + key);
        }
        if (permitted(sender, "opencompanies.admin")) {
            plugin.messages().info(sender, "command.help.admin");
        }
    }

    private void create(CommandSender sender, String[] args) {
        Player player = requirePlayer(sender);
        if (player == null || notPermitted(sender, "opencompanies.create")) {
            return;
        }
        if (args.length < 3) {
            plugin.messages().info(sender, "command.help.create");
            return;
        }
        String type = args[args.length - 1];
        String name = join(args, 1, args.length - 1);
        send(sender, plugin.companies().createCompanyForPlayer(player, name, type));
    }

    private void apply(CommandSender sender, String[] args) {
        Player player = requirePlayer(sender);
        if (player == null || notPermitted(sender, "opencompanies.apply")) {
            return;
        }
        if (args.length < 3) {
            plugin.messages().info(sender, "command.help.apply");
            return;
        }
        String name = args[1];
        String type = args[2];
        String description = args.length > 3 ? join(args, 3, args.length) : "";
        send(sender, plugin.chamber().submitApplication(player.getUniqueId(), player.getName(), name, type, description));
    }

    private void list(CommandSender sender) {
        var companies = plugin.companies().allCompanies();
        if (companies.isEmpty()) {
            plugin.messages().info(sender, "list.empty");
            return;
        }
        plugin.messages().info(sender, "list.header", "count", companies.size());
        for (Company company : companies) {
            plugin.messages().plain(sender, "list.entry",
                    "id", company.id(),
                    "name", company.displayName(),
                    "type", company.type(),
                    "members", company.memberCount(),
                    "status", plugin.messages().text(sender, company.status().messageKey()));
        }
    }

    private void info(CommandSender sender, String[] args) {
        Company company = args.length >= 2
                ? plugin.companies().findByName(join(args, 1, args.length)).orElse(null)
                : primaryCompany(sender);
        if (company == null) {
            plugin.messages().warning(sender, args.length >= 2 ? "company.not_found" : "company.none");
            return;
        }
        String ownerName = company.owner().map(CompanyMember::playerName)
                .orElse(company.ownerUuid() == null ? "-" : company.ownerUuid().toString());
        plugin.messages().info(sender, "info.header", "name", company.displayName());
        plugin.messages().plain(sender, "info.id", "id", company.id());
        plugin.messages().plain(sender, "info.type", "type", company.type());
        plugin.messages().plain(sender, "info.owner", "owner", ownerName);
        plugin.messages().plain(sender, "info.status", "status",
                plugin.messages().text(sender, company.status().messageKey()));
        plugin.messages().plain(sender, "info.members", "members", company.memberCount(),
                "max", plugin.settings().maxMembersPerCompany());
        String licenses = company.licenses().entrySet().stream()
                .filter(entry -> entry.getValue() == CompanyLicenseStatus.GRANTED)
                .map(entry -> entry.getKey().key())
                .reduce((a, b) -> a + ", " + b).orElse("-");
        plugin.messages().plain(sender, "info.licenses", "licenses", licenses);
        company.headquarters().ifPresent(hq -> plugin.messages().plain(sender, "info.hq",
                "world", hq.world(), "x", (int) hq.x(), "y", (int) hq.y(), "z", (int) hq.z()));
    }

    private void members(CommandSender sender) {
        Company company = primaryCompany(sender);
        if (company == null) {
            plugin.messages().warning(sender, "company.none");
            return;
        }
        plugin.messages().info(sender, "members.header", "name", company.displayName());
        for (CompanyMember member : company.members()) {
            plugin.messages().plain(sender, "members.entry",
                    "player", member.playerName(),
                    "role", plugin.messages().text(sender, member.role().messageKey()));
        }
    }

    private void invite(CommandSender sender, String[] args) {
        Player player = requirePlayer(sender);
        if (player == null) {
            return;
        }
        if (args.length >= 2 && args[1].equalsIgnoreCase("accept")) {
            send(sender, plugin.companies().acceptInvite(player.getUniqueId(), player.getName()));
            return;
        }
        if (args.length >= 2 && args[1].equalsIgnoreCase("deny")) {
            send(sender, plugin.companies().denyInvite(player.getUniqueId()));
            return;
        }
        if (notPermitted(sender, "opencompanies.invite")) {
            return;
        }
        if (args.length < 3) {
            plugin.messages().info(sender, "command.help.invite");
            return;
        }
        Company company = primaryCompany(sender);
        if (company == null) {
            plugin.messages().warning(sender, "company.none");
            return;
        }
        Player target = plugin.getServer().getPlayerExact(args[1]);
        if (target == null) {
            plugin.messages().warning(sender, "general.player_not_found", "player", args[1]);
            return;
        }
        CompanyRole role = CompanyRole.fromString(args[2]);
        if (role == null) {
            plugin.messages().warning(sender, "role.unknown", "role", args[2]);
            return;
        }
        send(sender, plugin.companies().inviteMember(company.id(), player.getUniqueId(),
                target.getUniqueId(), target.getName(), role));
    }

    private void fire(CommandSender sender, String[] args) {
        Player player = requirePlayer(sender);
        if (player == null || notPermitted(sender, "opencompanies.fire")) {
            return;
        }
        if (args.length < 2) {
            plugin.messages().info(sender, "command.help.fire");
            return;
        }
        Company company = primaryCompany(sender);
        if (company == null) {
            plugin.messages().warning(sender, "company.none");
            return;
        }
        UUID target = memberUuidByName(company, args[1]);
        if (target == null) {
            plugin.messages().warning(sender, "member.not_found");
            return;
        }
        send(sender, plugin.companies().removeMember(company.id(), player.getUniqueId(), target));
    }

    private void role(CommandSender sender, String[] args) {
        Player player = requirePlayer(sender);
        if (player == null || notPermitted(sender, "opencompanies.role")) {
            return;
        }
        if (args.length < 3) {
            plugin.messages().info(sender, "command.help.role");
            return;
        }
        Company company = primaryCompany(sender);
        if (company == null) {
            plugin.messages().warning(sender, "company.none");
            return;
        }
        UUID target = memberUuidByName(company, args[1]);
        if (target == null) {
            plugin.messages().warning(sender, "member.not_found");
            return;
        }
        CompanyRole role = CompanyRole.fromString(args[2]);
        if (role == null) {
            plugin.messages().warning(sender, "role.unknown", "role", args[2]);
            return;
        }
        send(sender, plugin.companies().changeRole(company.id(), player.getUniqueId(), target, role));
    }

    private void leave(CommandSender sender) {
        Player player = requirePlayer(sender);
        if (player == null) {
            return;
        }
        Company company = primaryCompany(sender);
        if (company == null) {
            plugin.messages().warning(sender, "company.none");
            return;
        }
        send(sender, plugin.companies().removeMember(company.id(), player.getUniqueId(), player.getUniqueId()));
    }

    private void licenses(CommandSender sender) {
        Company company = primaryCompany(sender);
        if (company == null) {
            plugin.messages().warning(sender, "company.none");
            return;
        }
        plugin.messages().info(sender, "licenses.header", "name", company.displayName());
        for (CompanyLicenseType type : CompanyLicenseType.values()) {
            CompanyLicenseStatus status = company.licenseStatus(type);
            plugin.messages().plain(sender, "licenses.entry",
                    "license", type.key(),
                    "status", plugin.messages().text(sender, status.messageKey()));
        }
    }

    private void assets(CommandSender sender) {
        Company company = primaryCompany(sender);
        if (company == null) {
            plugin.messages().warning(sender, "company.none");
            return;
        }
        List<CompanyAsset> list = plugin.assets().assetsOf(company.id());
        if (list.isEmpty()) {
            plugin.messages().info(sender, "assets.empty");
            return;
        }
        plugin.messages().info(sender, "assets.header", "name", company.displayName(), "count", list.size());
        for (CompanyAsset asset : list) {
            plugin.messages().plain(sender, "assets.entry",
                    "id", asset.shortId(),
                    "type", asset.type().key(),
                    "world", asset.position().world(),
                    "x", asset.position().x(), "y", asset.position().y(), "z", asset.position().z());
        }
    }

    private void reload(CommandSender sender) {
        if (notPermitted(sender, "opencompanies.reload")) {
            return;
        }
        plugin.reloadAll();
        plugin.messages().success(sender, "general.reload_done");
    }

    // --- admin commands ----------------------------------------------------------------------

    private void admin(CommandSender sender, String[] args) {
        if (!permitted(sender, "opencompanies.admin")
                && !permitted(sender, "opencompanies.create.admin")
                && !permitted(sender, "opencompanies.delete")) {
            plugin.messages().warning(sender, "general.no_permission");
            return;
        }
        String adminSub = args.length >= 2 ? args[1].toLowerCase(Locale.ROOT) : "help";
        switch (adminSub) {
            case "create" -> adminCreate(sender, args);
            case "delete" -> adminDelete(sender, args);
            case "setstatus" -> adminSetStatus(sender, args);
            case "setowner" -> adminSetOwner(sender, args);
            case "license" -> adminLicense(sender, args);
            case "sethq" -> adminSetHq(sender, args);
            case "applications" -> adminApplications(sender);
            case "approve" -> adminApprove(sender, args);
            case "deny" -> adminDeny(sender, args);
            case "reload" -> reload(sender);
            default -> adminHelp(sender);
        }
    }

    private void adminHelp(CommandSender sender) {
        plugin.messages().info(sender, "command.admin.header");
        for (String key : ADMIN_SUBCOMMANDS) {
            plugin.messages().info(sender, "command.admin.help." + key);
        }
    }

    private void adminCreate(CommandSender sender, String[] args) {
        if (!adminPermitted(sender, "opencompanies.create.admin")) {
            return;
        }
        if (args.length < 5) {
            plugin.messages().info(sender, "command.admin.help.create");
            return;
        }
        OfflinePlayer owner = resolvePlayer(args[2]);
        if (owner == null) {
            plugin.messages().warning(sender, "general.player_not_found", "player", args[2]);
            return;
        }
        String type = args[args.length - 1];
        String name = join(args, 3, args.length - 1);
        send(sender, plugin.companies().createCompany(owner.getUniqueId(),
                owner.getName() == null ? args[2] : owner.getName(), name, type));
    }

    private void adminDelete(CommandSender sender, String[] args) {
        if (!adminPermitted(sender, "opencompanies.delete")) {
            return;
        }
        Company company = requireCompanyArg(sender, args, 2);
        if (company == null) {
            return;
        }
        send(sender, plugin.companies().deleteCompany(company.id()));
    }

    private void adminSetStatus(CommandSender sender, String[] args) {
        if (!adminPermitted(sender, "opencompanies.admin")) {
            return;
        }
        if (args.length < 4) {
            plugin.messages().info(sender, "command.admin.help.setstatus");
            return;
        }
        Company company = requireCompanyArg(sender, args, 2);
        if (company == null) {
            return;
        }
        CompanyStatus status = parseEnum(CompanyStatus.class, args[3]);
        if (status == null) {
            plugin.messages().warning(sender, "status.unknown", "status", args[3]);
            return;
        }
        send(sender, plugin.chamber().setStatus(company.id(), status));
    }

    private void adminSetOwner(CommandSender sender, String[] args) {
        if (!adminPermitted(sender, "opencompanies.admin")) {
            return;
        }
        if (args.length < 4) {
            plugin.messages().info(sender, "command.admin.help.setowner");
            return;
        }
        Company company = requireCompanyArg(sender, args, 2);
        if (company == null) {
            return;
        }
        OfflinePlayer owner = resolvePlayer(args[3]);
        if (owner == null) {
            plugin.messages().warning(sender, "general.player_not_found", "player", args[3]);
            return;
        }
        send(sender, plugin.companies().transferOwnership(company.id(), owner.getUniqueId(),
                owner.getName() == null ? args[3] : owner.getName()));
    }

    private void adminLicense(CommandSender sender, String[] args) {
        if (!adminPermitted(sender, "opencompanies.admin")) {
            return;
        }
        if (args.length < 5) {
            plugin.messages().info(sender, "command.admin.help.license");
            return;
        }
        boolean grant = args[2].equalsIgnoreCase("grant");
        boolean revoke = args[2].equalsIgnoreCase("revoke");
        if (!grant && !revoke) {
            plugin.messages().info(sender, "command.admin.help.license");
            return;
        }
        Company company = requireCompanyArg(sender, args, 3);
        if (company == null) {
            return;
        }
        CompanyLicenseType type = CompanyLicenseType.fromString(args[4]);
        if (type == null) {
            plugin.messages().warning(sender, "license.unknown", "license", args[4]);
            return;
        }
        send(sender, grant ? plugin.chamber().grantLicense(company.id(), type)
                : plugin.chamber().revokeLicense(company.id(), type));
    }

    private void adminSetHq(CommandSender sender, String[] args) {
        Player player = requirePlayer(sender);
        if (player == null || !adminPermitted(sender, "opencompanies.admin")) {
            return;
        }
        Company company = requireCompanyArg(sender, args, 2);
        if (company == null) {
            return;
        }
        Location location = player.getLocation();
        send(sender, plugin.chamber().setHeadquarters(company.id(), player,
                location.getWorld() == null ? "world" : location.getWorld().getName(),
                location.getX(), location.getY(), location.getZ(),
                location.getYaw(), location.getPitch()));
    }

    private void adminApplications(CommandSender sender) {
        if (!adminPermitted(sender, "opencompanies.admin")) {
            return;
        }
        var pending = plugin.chamber().pendingApplications();
        if (pending.isEmpty()) {
            plugin.messages().info(sender, "application.list_empty");
            return;
        }
        plugin.messages().info(sender, "application.list_header", "count", pending.size());
        for (CompanyApplication application : pending) {
            plugin.messages().plain(sender, "application.list_entry",
                    "id", application.shortId(),
                    "name", application.requestedName(),
                    "type", application.requestedType(),
                    "player", application.applicantName());
        }
    }

    private void adminApprove(CommandSender sender, String[] args) {
        if (!adminPermitted(sender, "opencompanies.admin")) {
            return;
        }
        if (args.length < 3) {
            plugin.messages().info(sender, "command.admin.help.approve");
            return;
        }
        CompanyApplication application = plugin.chamber().findApplicationByShortId(args[2]).orElse(null);
        if (application == null) {
            plugin.messages().warning(sender, "application.not_found");
            return;
        }
        send(sender, plugin.chamber().approveApplication(application.id()));
    }

    private void adminDeny(CommandSender sender, String[] args) {
        if (!adminPermitted(sender, "opencompanies.admin")) {
            return;
        }
        if (args.length < 3) {
            plugin.messages().info(sender, "command.admin.help.deny");
            return;
        }
        CompanyApplication application = plugin.chamber().findApplicationByShortId(args[2]).orElse(null);
        if (application == null) {
            plugin.messages().warning(sender, "application.not_found");
            return;
        }
        String reason = args.length > 3 ? join(args, 3, args.length) : "";
        send(sender, plugin.chamber().denyApplication(application.id(), reason));
    }

    // --- helpers -----------------------------------------------------------------------------

    private void send(CommandSender sender, CompanyResult result) {
        if (result.success()) {
            plugin.messages().success(sender, result.messageKey(), result.placeholders());
        } else {
            plugin.messages().warning(sender, result.messageKey(), result.placeholders());
        }
    }

    private Company primaryCompany(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            return null;
        }
        List<Company> companies = plugin.companies().findByPlayer(player.getUniqueId());
        return companies.stream()
                .filter(company -> player.getUniqueId().equals(company.ownerUuid()))
                .findFirst()
                .orElseGet(() -> companies.stream().findFirst().orElse(null));
    }

    private Company requireCompanyArg(CommandSender sender, String[] args, int index) {
        if (args.length <= index) {
            plugin.messages().warning(sender, "company.not_found", "company", "?");
            return null;
        }
        Company company = plugin.companies().findByName(args[index]).orElse(null);
        if (company == null) {
            plugin.messages().warning(sender, "company.not_found", "company", args[index]);
        }
        return company;
    }

    private UUID memberUuidByName(Company company, String name) {
        for (CompanyMember member : company.members()) {
            if (member.playerName().equalsIgnoreCase(name)) {
                return member.playerUuid();
            }
        }
        return null;
    }

    private OfflinePlayer resolvePlayer(String name) {
        Player online = plugin.getServer().getPlayerExact(name);
        if (online != null) {
            return online;
        }
        return plugin.getServer().getOfflinePlayerIfCached(name);
    }

    private Player requirePlayer(CommandSender sender) {
        if (sender instanceof Player player) {
            return player;
        }
        plugin.messages().warning(sender, "general.player_only");
        return null;
    }

    private boolean permitted(CommandSender sender, String permission) {
        return plugin.adapters().permission().has(sender, permission);
    }

    private boolean notPermitted(CommandSender sender, String permission) {
        if (permitted(sender, permission)) {
            return false;
        }
        plugin.messages().warning(sender, "general.no_permission");
        return true;
    }

    private boolean adminPermitted(CommandSender sender, String specific) {
        if (permitted(sender, "opencompanies.admin") || permitted(sender, specific)) {
            return true;
        }
        plugin.messages().warning(sender, "general.no_permission");
        return false;
    }

    private static String join(String[] args, int from, int to) {
        return String.join(" ", java.util.Arrays.copyOfRange(args, from, to)).trim();
    }

    private static <E extends Enum<E>> E parseEnum(Class<E> type, String value) {
        if (value == null) {
            return null;
        }
        try {
            return Enum.valueOf(type, value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    // --- tab completion ----------------------------------------------------------------------

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return CommandSuggestions.filter(SUBCOMMANDS, args[0]);
        }
        String sub = args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "create" -> {
                if (args.length == 2) {
                    return CommandSuggestions.filter(plugin.settings().allowedTypes(), args[1]);
                }
            }
            case "apply" -> {
                if (args.length == 3) {
                    return CommandSuggestions.filter(plugin.settings().allowedTypes(), args[2]);
                }
            }
            case "invite" -> {
                if (args.length == 2) {
                    List<String> options = new ArrayList<>(onlinePlayerNames());
                    options.add("accept");
                    options.add("deny");
                    return CommandSuggestions.filter(options, args[1]);
                }
                if (args.length == 3) {
                    return CommandSuggestions.filter(assignableRoleNames(), args[2]);
                }
            }
            case "fire", "role" -> {
                if (args.length == 2) {
                    return CommandSuggestions.filter(memberNames(sender), args[1]);
                }
                if (args.length == 3 && sub.equals("role")) {
                    return CommandSuggestions.filter(assignableRoleNames(), args[2]);
                }
            }
            case "info" -> {
                if (args.length == 2) {
                    return CommandSuggestions.filter(companyIds(), args[1]);
                }
            }
            case "admin" -> {
                return adminTabComplete(args);
            }
            default -> {
                return List.of();
            }
        }
        return List.of();
    }

    private List<String> adminTabComplete(String[] args) {
        if (args.length == 2) {
            return CommandSuggestions.filter(ADMIN_SUBCOMMANDS, args[1]);
        }
        String adminSub = args[1].toLowerCase(Locale.ROOT);
        switch (adminSub) {
            case "delete", "setstatus", "setowner", "sethq" -> {
                if (args.length == 3) {
                    return CommandSuggestions.filter(companyIds(), args[2]);
                }
                if (args.length == 4 && adminSub.equals("setstatus")) {
                    return CommandSuggestions.filter(statusNames(), args[3]);
                }
                if (args.length == 4 && adminSub.equals("setowner")) {
                    return CommandSuggestions.filter(onlinePlayerNames(), args[3]);
                }
            }
            case "create" -> {
                if (args.length == 3) {
                    return CommandSuggestions.filter(onlinePlayerNames(), args[2]);
                }
            }
            case "license" -> {
                if (args.length == 3) {
                    return CommandSuggestions.filter(List.of("grant", "revoke"), args[2]);
                }
                if (args.length == 4) {
                    return CommandSuggestions.filter(companyIds(), args[3]);
                }
                if (args.length == 5) {
                    return CommandSuggestions.filter(licenseNames(), args[4]);
                }
            }
            case "approve", "deny" -> {
                if (args.length == 3) {
                    return CommandSuggestions.filter(pendingApplicationIds(), args[2]);
                }
            }
            default -> {
                return List.of();
            }
        }
        return List.of();
    }

    private List<String> companyIds() {
        return plugin.companies().allCompanies().stream().map(Company::id).toList();
    }

    private List<String> memberNames(CommandSender sender) {
        Company company = primaryCompany(sender);
        if (company == null) {
            return List.of();
        }
        return company.members().stream().map(CompanyMember::playerName).toList();
    }

    private List<String> onlinePlayerNames() {
        return plugin.getServer().getOnlinePlayers().stream().map(Player::getName).toList();
    }

    private List<String> pendingApplicationIds() {
        return plugin.chamber().pendingApplications().stream().map(CompanyApplication::shortId).toList();
    }

    private static List<String> assignableRoleNames() {
        List<String> names = new ArrayList<>();
        for (CompanyRole role : CompanyRole.values()) {
            if (role != CompanyRole.CEO) {
                names.add(role.name().toLowerCase(Locale.ROOT));
            }
        }
        return names;
    }

    private static List<String> licenseNames() {
        return java.util.Arrays.stream(CompanyLicenseType.values()).map(CompanyLicenseType::key).toList();
    }

    private static List<String> statusNames() {
        return java.util.Arrays.stream(CompanyStatus.values())
                .map(status -> status.name().toLowerCase(Locale.ROOT)).toList();
    }
}
