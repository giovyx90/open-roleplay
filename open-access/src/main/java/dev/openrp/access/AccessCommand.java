package dev.openrp.access;

import dev.openrp.access.model.AccessAction;
import dev.openrp.access.model.AccessPreset;
import dev.openrp.access.model.AccessProfile;
import dev.openrp.access.model.AccessProfileType;
import dev.openrp.access.util.AccessMessages;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public class AccessCommand implements CommandExecutor, TabCompleter {

    private final AccessService service;

    public AccessCommand(AccessService service) {
        this.service = service;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            openCurrent(sender);
            return true;
        }
        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "region" -> handleRegion(sender, args);
            case "trust" -> handleTrust(sender, args);
            case "untrust" -> handleUntrust(sender, args);
            case "player" -> handlePlayer(sender, args);
            case "preset" -> handlePreset(sender, args);
            case "reload" -> handleReload(sender);
            case "debug" -> handleDebug(sender);
            default -> sendHelp(sender, label);
        }
        return true;
    }

    private void openCurrent(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(AccessMessages.error("Access", "Solo i player possono aprire l'editor accessi."));
            return;
        }
        Location location = targetOrCurrent(player);
        Optional<AccessProfile> profile = service.findProfileAt(location);
        if (profile.isEmpty()) {
            player.sendMessage(AccessMessages.warning("Access", "Nessun profilo accesso copre questa posizione."));
            return;
        }
        if (!service.canManage(player, profile.get())) {
            player.sendMessage(AccessMessages.error("Access", "Non puoi gestire gli accessi qui."));
            return;
        }
        service.openEditor(player, profile.get(), targetBlockLocation(player));
    }

    private void handleRegion(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(AccessMessages.info("Access", "/access region <link|unlink|info>"));
            return;
        }
        switch (args[1].toLowerCase(Locale.ROOT)) {
            case "link" -> linkRegion(sender, args);
            case "unlink" -> unlinkRegion(sender, args);
            case "info" -> regionInfo(sender, args);
            default -> sender.sendMessage(AccessMessages.info("Access", "/access region <link|unlink|info>"));
        }
    }

    private void linkRegion(CommandSender sender, String[] args) {
        if (!AccessPermissions.hasRegionManage(sender)) {
            sender.sendMessage(AccessMessages.error("Access", "Non puoi collegare regioni accesso."));
            return;
        }
        if (args.length < 5) {
            sender.sendMessage(AccessMessages.info("Access", "/access region link <PROPERTY|COMPANY|HOTEL_ROOM|REGION> <wgRegion> <owner> [world]"));
            return;
        }
        World world = worldFrom(sender, args.length >= 6 ? args[5] : null);
        if (world == null) {
            sender.sendMessage(AccessMessages.error("Access", "Da console devi indicare il mondo."));
            return;
        }
        AccessProfileType type;
        try {
            type = AccessProfileType.parse(args[2]);
        } catch (IllegalArgumentException error) {
            sender.sendMessage(AccessMessages.error("Access", "Tipo profilo sconosciuto."));
            return;
        }
        UUID actorUuid = sender instanceof Player player ? player.getUniqueId() : null;
        service.linkRegion(type, world, args[3], args[4], actorUuid, sender.getName())
                .whenComplete((profile, error) -> Bukkit.getScheduler().runTask(service.core(), () -> {
                    if (error != null) {
                        sender.sendMessage(AccessMessages.error("Access", rootMessage(error)));
                        return;
                    }
                    service.applyWorldGuardFlags(world, profile.getRegionId());
                    sender.sendMessage(AccessMessages.success("Access",
                            "Profilo " + profile.getType().name() + " collegato a " + world.getName() + "/" + profile.getRegionId() + "."));
                }));
    }

    private void unlinkRegion(CommandSender sender, String[] args) {
        if (!AccessPermissions.hasRegionManage(sender)) {
            sender.sendMessage(AccessMessages.error("Access", "Non puoi scollegare regioni accesso."));
            return;
        }
        if (args.length < 3) {
            sender.sendMessage(AccessMessages.info("Access", "/access region unlink <wgRegion> [world]"));
            return;
        }
        World world = worldFrom(sender, args.length >= 4 ? args[3] : null);
        if (world == null) {
            sender.sendMessage(AccessMessages.error("Access", "Da console devi indicare il mondo."));
            return;
        }
        AccessProfile profile = service.findProfileByRegion(world.getName(), args[2]).orElse(null);
        if (profile == null) {
            sender.sendMessage(AccessMessages.warning("Access", "Nessun profilo accesso collegato a quella regione."));
            return;
        }
        UUID actorUuid = sender instanceof Player player ? player.getUniqueId() : null;
        service.unlinkRegion(profile, actorUuid, sender.getName())
                .whenComplete((ignored, error) -> Bukkit.getScheduler().runTask(service.core(), () -> {
                    if (error != null) {
                        sender.sendMessage(AccessMessages.error("Access", rootMessage(error)));
                        return;
                    }
                    sender.sendMessage(AccessMessages.success("Access", "Profilo accesso scollegato."));
                }));
    }

    private void regionInfo(CommandSender sender, String[] args) {
        World world = worldFrom(sender, args.length >= 4 ? args[3] : null);
        Optional<AccessProfile> profile;
        if (args.length >= 3 && world != null) {
            profile = service.findProfileByRegion(world.getName(), args[2]);
        } else if (sender instanceof Player player) {
            profile = service.findProfileAt(player.getLocation());
        } else {
            sender.sendMessage(AccessMessages.info("Access", "/access region info <wgRegion> <world>"));
            return;
        }
        if (profile.isEmpty()) {
            sender.sendMessage(AccessMessages.warning("Access", "Nessun profilo accesso trovato."));
            return;
        }
        AccessProfile p = profile.get();
        sender.sendMessage(AccessMessages.info("Access",
                p.getType().name() + " " + p.getWorld() + "/" + p.getRegionId()
                        + " | preset=" + p.getDefaultPreset().name()
                        + " | owner=" + (p.getOwnerName() == null ? p.getOwnerKey() : p.getOwnerName())));
    }

    private void handleTrust(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(AccessMessages.error("Access", "Solo i player possono autorizzare altri player sul posto."));
            return;
        }
        if (args.length < 2) {
            player.sendMessage(AccessMessages.info("Access", "/access trust <onlinePlayer|uuid> [manage]"));
            return;
        }
        AccessProfile profile = manageableProfile(player);
        if (profile == null) {
            return;
        }
        UUID targetUuid = playerUuid(args[1]);
        if (targetUuid == null) {
            player.sendMessage(AccessMessages.error("Access", "Il target deve essere online o un UUID valido."));
            return;
        }
        Set<AccessAction> actions = trustActions(args, 2);
        service.addPlayerRule(profile, targetUuid, args[1], actions, player.getUniqueId(), player.getName())
                .whenComplete((ignored, error) -> Bukkit.getScheduler().runTask(service.core(), () -> {
                    if (error != null) {
                        player.sendMessage(AccessMessages.error("Access", rootMessage(error)));
                        return;
                    }
                    String suffix = actions.contains(AccessAction.MANAGE) ? " con gestione." : ".";
                    player.sendMessage(AccessMessages.success("Access", "Player autorizzato" + suffix));
                }));
    }

    private void handleUntrust(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(AccessMessages.error("Access", "Solo i player possono rimuovere autorizzazioni sul posto."));
            return;
        }
        if (args.length < 2) {
            player.sendMessage(AccessMessages.info("Access", "/access untrust <onlinePlayer|uuid>"));
            return;
        }
        AccessProfile profile = manageableProfile(player);
        if (profile == null) {
            return;
        }
        UUID targetUuid = playerUuid(args[1]);
        if (targetUuid == null) {
            player.sendMessage(AccessMessages.error("Access", "Il target deve essere online o un UUID valido."));
            return;
        }
        service.removePlayerRule(profile, targetUuid, player.getUniqueId(), player.getName())
                .whenComplete((ignored, error) -> Bukkit.getScheduler().runTask(service.core(), () -> {
                    if (error != null) {
                        player.sendMessage(AccessMessages.error("Access", rootMessage(error)));
                        return;
                    }
                    player.sendMessage(AccessMessages.success("Access", "Autorizzazione rimossa."));
                }));
    }

    private void handlePlayer(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(AccessMessages.error("Access", "Solo i player possono modificare accessi custom sul posto."));
            return;
        }
        if (args.length < 3) {
            sender.sendMessage(AccessMessages.info("Access", "/access player <add|remove> <onlinePlayer|uuid> [open|manage|all]"));
            return;
        }
        AccessProfile profile = manageableProfile(player);
        if (profile == null) {
            return;
        }
        UUID targetUuid = playerUuid(args[2]);
        if (targetUuid == null) {
            player.sendMessage(AccessMessages.error("Access", "Il target deve essere online o un UUID valido."));
            return;
        }
        if (args[1].equalsIgnoreCase("add")) {
            Set<AccessAction> actions = parseActions(args, 3);
            service.addPlayerRule(profile, targetUuid, args[2], actions, player.getUniqueId(), player.getName())
                    .whenComplete((ignored, error) -> Bukkit.getScheduler().runTask(service.core(), () -> {
                        if (error != null) {
                            player.sendMessage(AccessMessages.error("Access", rootMessage(error)));
                            return;
                        }
                        String suffix = actions.contains(AccessAction.MANAGE) ? " con gestione." : ".";
                        player.sendMessage(AccessMessages.success("Access", "Player autorizzato" + suffix));
                    }));
        } else if (args[1].equalsIgnoreCase("remove")) {
            service.removePlayerRule(profile, targetUuid, player.getUniqueId(), player.getName())
                    .whenComplete((ignored, error) -> Bukkit.getScheduler().runTask(service.core(), () -> {
                        if (error != null) {
                            player.sendMessage(AccessMessages.error("Access", rootMessage(error)));
                            return;
                        }
                        player.sendMessage(AccessMessages.success("Access", "Autorizzazione rimossa."));
                    }));
        } else {
            player.sendMessage(AccessMessages.info("Access", "/access player <add|remove> <onlinePlayer|uuid> [open|manage|all]"));
        }
    }

    private void handlePreset(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(AccessMessages.error("Access", "Solo i player possono cambiare preset sul posto."));
            return;
        }
        if (args.length < 2) {
            player.sendMessage(AccessMessages.info("Access", "/access preset <private|members|managers|public|custom> [region|block]"));
            return;
        }
        AccessProfile profile = service.findProfileAt(targetOrCurrent(player)).orElse(null);
        if (profile == null) {
            player.sendMessage(AccessMessages.warning("Access", "Nessun profilo accesso copre questa posizione."));
            return;
        }
        if (!service.canManage(player, profile)) {
            player.sendMessage(AccessMessages.error("Access", "Non puoi gestire gli accessi qui."));
            return;
        }
        AccessPreset preset;
        try {
            preset = AccessPreset.parse(args[1]);
        } catch (IllegalArgumentException error) {
            player.sendMessage(AccessMessages.error("Access", "Preset sconosciuto."));
            return;
        }
        boolean block = args.length >= 3 && args[2].equalsIgnoreCase("block");
        if (block) {
            Location blockLocation = targetBlockLocation(player);
            if (blockLocation == null) {
                player.sendMessage(AccessMessages.warning("Access", "Guarda prima un blocco."));
                return;
            }
            service.setBlockPreset(profile, blockLocation, preset, player.getUniqueId(), player.getName())
                    .whenComplete((ignored, error) -> sendPresetResult(player, error, "Override del blocco aggiornato."));
        } else {
            service.setRegionPreset(profile, preset, player.getUniqueId(), player.getName())
                    .whenComplete((ignored, error) -> sendPresetResult(player, error, "Preset regione aggiornato."));
        }
    }

    private void sendPresetResult(Player player, Throwable error, String success) {
        Bukkit.getScheduler().runTask(service.core(), () -> {
            if (error != null) {
                player.sendMessage(AccessMessages.error("Access", rootMessage(error)));
                return;
            }
            player.sendMessage(AccessMessages.success("Access", success));
        });
    }

    private void handleReload(CommandSender sender) {
        if (!AccessPermissions.hasReload(sender)) {
            sender.sendMessage(AccessMessages.error("Access", "Non puoi ricaricare i dati accesso."));
            return;
        }
        service.refreshCache().whenComplete((ignored, error) -> Bukkit.getScheduler().runTask(service.core(), () -> {
            if (error != null) {
                sender.sendMessage(AccessMessages.error("Access", rootMessage(error)));
                return;
            }
            sender.sendMessage(AccessMessages.success("Access", "Cache accessi ricaricata."));
        }));
    }

    private void handleDebug(CommandSender sender) {
        if (!AccessPermissions.hasDebug(sender)) {
            sender.sendMessage(AccessMessages.error("Access", "Non puoi usare il debug accessi."));
            return;
        }
        sender.sendMessage(AccessMessages.info("Access",
                "Profili caricati: " + service.profiles().size()));
    }

    private void sendHelp(CommandSender sender, String label) {
        sender.sendMessage(AccessMessages.info("Access",
                "/" + label + " | /" + label + " trust <player> [manage] | /" + label + " untrust <player> | /" + label + " region link <type> <wgRegion> <owner> [world]"));
    }

    private World worldFrom(CommandSender sender, String raw) {
        if (raw != null && !raw.isBlank()) {
            return Bukkit.getWorld(raw);
        }
        return sender instanceof Player player ? player.getWorld() : null;
    }

    private Location targetOrCurrent(Player player) {
        Location target = targetBlockLocation(player);
        return target == null ? player.getLocation() : target;
    }

    private Location targetBlockLocation(Player player) {
        Block block = player.getTargetBlockExact(5);
        return block == null ? null : block.getLocation();
    }

    private UUID playerUuid(String raw) {
        try {
            return UUID.fromString(raw);
        } catch (IllegalArgumentException ignored) {
            Player online = Bukkit.getPlayerExact(raw);
            return online == null ? null : online.getUniqueId();
        }
    }

    private AccessProfile manageableProfile(Player player) {
        AccessProfile profile = service.findProfileAt(targetOrCurrent(player)).orElse(null);
        if (profile == null) {
            player.sendMessage(AccessMessages.warning("Access", "Nessun profilo accesso copre questa posizione."));
            return null;
        }
        if (!service.canManage(player, profile)) {
            player.sendMessage(AccessMessages.error("Access", "Non puoi gestire gli accessi qui."));
            return null;
        }
        return profile;
    }

    private Set<AccessAction> trustActions(String[] args, int start) {
        if (args.length <= start) {
            return EnumSet.copyOf(AccessAction.USE_ACTIONS);
        }
        EnumSet<AccessAction> actions = EnumSet.copyOf(AccessAction.USE_ACTIONS);
        for (int i = start; i < args.length; i++) {
            String token = args[i].toLowerCase(Locale.ROOT);
            if (token.equals("manage") || token.equals("all")) {
                actions.add(AccessAction.MANAGE);
            }
        }
        return actions;
    }

    private Set<AccessAction> parseActions(String[] args, int start) {
        if (args.length <= start) {
            return EnumSet.copyOf(AccessAction.USE_ACTIONS);
        }
        EnumSet<AccessAction> actions = EnumSet.noneOf(AccessAction.class);
        for (int i = start; i < args.length; i++) {
            String token = args[i].toLowerCase(Locale.ROOT);
            if (token.equals("all")) {
                actions.addAll(AccessAction.USE_ACTIONS);
                actions.add(AccessAction.MANAGE);
                continue;
            }
            if (token.equals("open") || token.equals("use")) {
                actions.addAll(AccessAction.USE_ACTIONS);
                continue;
            }
            if (token.equals("manage")) {
                actions.addAll(AccessAction.USE_ACTIONS);
                actions.add(AccessAction.MANAGE);
                continue;
            }
            try {
                actions.add(AccessAction.parse(args[i]));
            } catch (IllegalArgumentException ignored) {
            }
        }
        return actions.isEmpty() ? EnumSet.copyOf(AccessAction.USE_ACTIONS) : actions;
    }

    private String rootMessage(Throwable error) {
        Throwable cause = error != null && error.getCause() != null ? error.getCause() : error;
        return cause != null && cause.getMessage() != null ? cause.getMessage()
                : cause == null ? "Errore sconosciuto" : cause.getClass().getSimpleName();
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return suggest(sender, args);
    }

    public List<String> suggest(CommandSender sender, String[] args) {
        if (args.length == 0) {
            return rootSuggestions(sender, "");
        }
        if (args.length == 1) {
            return rootSuggestions(sender, args[0]);
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("region")) {
            if (!canManageRegions(sender)) {
                return List.of();
            }
            return starts(args[1], "link", "unlink", "info");
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("region") && args[1].equalsIgnoreCase("link")) {
            if (!canManageRegions(sender)) {
                return List.of();
            }
            return starts(args[2], Arrays.stream(AccessProfileType.values()).map(Enum::name).toArray(String[]::new));
        }
        if (args.length == 2 && (args[0].equalsIgnoreCase("trust") || args[0].equalsIgnoreCase("untrust"))) {
            return canManageCurrent(sender) ? onlinePlayers(args[1]) : List.of();
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("trust")) {
            return canManageCurrent(sender) ? starts(args[2], "manage") : List.of();
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("player")) {
            return canManageCurrent(sender) ? starts(args[1], "add", "remove") : List.of();
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("player")) {
            return canManageCurrent(sender) ? onlinePlayers(args[2]) : List.of();
        }
        if (args.length >= 4 && args[0].equalsIgnoreCase("player") && args[1].equalsIgnoreCase("add")) {
            return canManageCurrent(sender) ? starts(args[args.length - 1], "open", "manage") : List.of();
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("preset")) {
            return canManageCurrent(sender) ? starts(args[1], "private", "members", "managers", "public", "custom") : List.of();
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("preset")) {
            return canManageCurrent(sender) ? starts(args[2], "region", "block") : List.of();
        }
        return List.of();
    }

    public boolean canUseRoot(CommandSender sender) {
        return canManageCurrent(sender) || canManageRegions(sender) || canReload(sender) || canDebug(sender);
    }

    private boolean canManageCurrent(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            return false;
        }
        return service.findProfileAt(targetOrCurrent(player))
                .map(profile -> service.canManage(player, profile))
                .orElse(false);
    }

    private boolean canManageRegions(CommandSender sender) {
        return AccessPermissions.hasRegionManage(sender);
    }

    private boolean canReload(CommandSender sender) {
        return AccessPermissions.hasReload(sender);
    }

    private boolean canDebug(CommandSender sender) {
        return AccessPermissions.hasDebug(sender);
    }

    private List<String> rootSuggestions(CommandSender sender, String token) {
        List<String> roots = new ArrayList<>();
        if (canManageCurrent(sender)) {
            roots.addAll(List.of("trust", "untrust", "preset", "player"));
        }
        if (canManageRegions(sender)) {
            roots.add("region");
        }
        if (canReload(sender)) {
            roots.add("reload");
        }
        if (canDebug(sender)) {
            roots.add("debug");
        }
        return starts(token, roots);
    }

    private List<String> starts(String token, String... values) {
        return starts(token, Arrays.asList(values));
    }

    private List<String> starts(String token, List<String> values) {
        String prefix = token == null ? "" : token.toLowerCase(Locale.ROOT);
        List<String> out = new ArrayList<>();
        for (String value : values) {
            if (value.toLowerCase(Locale.ROOT).startsWith(prefix)) {
                out.add(value);
            }
        }
        return out;
    }

    private List<String> onlinePlayers(String token) {
        String prefix = token == null ? "" : token.toLowerCase(Locale.ROOT);
        List<String> out = new ArrayList<>();
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getName().toLowerCase(Locale.ROOT).startsWith(prefix)) {
                out.add(player.getName());
            }
        }
        return out;
    }
}
