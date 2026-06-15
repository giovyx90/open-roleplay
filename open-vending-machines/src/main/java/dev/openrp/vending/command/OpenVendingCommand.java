package dev.openrp.vending.command;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import dev.openrp.vending.OpenVendingMachinesPlugin;
import dev.openrp.vending.core.Authorization;
import dev.openrp.vending.core.MachineInfoPresenter;
import dev.openrp.vending.model.MachineLocation;
import dev.openrp.vending.model.MachineType;
import dev.openrp.vending.model.ProductDefinition;
import dev.openrp.vending.model.VendingMachine;

/** {@code /openvending} (aliases {@code /ovm}, {@code /vending}) - all player and admin actions. */
public final class OpenVendingCommand implements CommandExecutor, TabCompleter {

    private static final List<String> SUBCOMMANDS =
            List.of("help", "create", "remove", "list", "info", "restock", "withdraw", "reload", "giveitem");

    private final OpenVendingMachinesPlugin plugin;

    public OpenVendingCommand(OpenVendingMachinesPlugin plugin) {
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
            case "reload" -> reload(sender);
            case "create" -> create(sender, args);
            case "remove" -> remove(sender);
            case "list" -> list(sender);
            case "info" -> info(sender);
            case "restock" -> restock(sender);
            case "withdraw" -> withdraw(sender);
            case "giveitem" -> giveItem(sender, args);
            default -> plugin.messages().warning(sender, "general.unknown_subcommand");
        }
        return true;
    }

    private void sendHelp(CommandSender sender) {
        plugin.messages().info(sender, "command.header");
        for (String key : SUBCOMMANDS) {
            plugin.messages().info(sender, "command.help." + key);
        }
    }

    private void reload(CommandSender sender) {
        if (notPermitted(sender, "openvending.reload")) {
            return;
        }
        plugin.reloadAll();
        plugin.messages().success(sender, "general.reload_done");
    }

    private void create(CommandSender sender, String[] args) {
        Player player = requirePlayer(sender);
        if (player == null || notPermitted(sender, "openvending.create")) {
            return;
        }
        if (args.length < 2) {
            plugin.messages().info(sender, "command.help.create");
            return;
        }
        MachineType type = plugin.machineTypes().get(args[1]);
        if (type == null) {
            plugin.messages().warning(sender, "machine.unknown_type", "type", args[1]);
            return;
        }
        String company = args.length >= 3 ? args[2] : null;
        if (company != null && !plugin.adapters().business().companyExists(company)) {
            plugin.messages().warning(sender, "machine.unknown_company", "company", company);
            return;
        }
        Block target = player.getTargetBlockExact(targetRange());
        if (target == null) {
            plugin.messages().warning(sender, "machine.look_at_block");
            return;
        }
        if (plugin.machines().existsAt(target.getLocation())) {
            plugin.messages().warning(sender, "machine.occupied");
            return;
        }
        if (company != null && plugin.machines().isAtLimit(company)) {
            plugin.messages().warning(sender, "machine.limit_reached",
                    "company", company, "limit", plugin.machines().effectiveLimit(company));
            return;
        }
        VendingMachine machine = plugin.machines().create(player, type, MachineLocation.of(target.getLocation()), company);
        if (machine == null) {
            plugin.messages().error(sender, "general.operation_failed");
            return;
        }
        plugin.messages().success(sender, "machine.created", "type", type.id(), "id", machine.shortId());
    }

    private void remove(CommandSender sender) {
        Player player = requirePlayer(sender);
        if (player == null || notPermitted(sender, "openvending.remove")) {
            return;
        }
        VendingMachine machine = targetedMachine(player);
        if (machine == null) {
            plugin.messages().warning(sender, "machine.not_found");
            return;
        }
        if (!Authorization.canRemove(plugin, player, machine)) {
            plugin.messages().warning(sender, "general.no_permission");
            return;
        }
        if (plugin.machines().remove(player, machine)) {
            plugin.messages().success(sender, "machine.removed", "id", machine.shortId());
        } else {
            plugin.messages().error(sender, "general.operation_failed");
        }
    }

    private void list(CommandSender sender) {
        Player player = requirePlayer(sender);
        if (player == null) {
            return;
        }
        List<VendingMachine> nearby = plugin.machines().nearby(player.getLocation(), 50.0);
        if (nearby.isEmpty()) {
            plugin.messages().info(sender, "machine.list_empty");
            return;
        }
        plugin.messages().info(sender, "machine.list_header");
        for (VendingMachine machine : nearby) {
            MachineLocation location = machine.location();
            plugin.messages().info(sender, "machine.list_entry",
                    "id", machine.shortId(),
                    "type", machine.typeId(),
                    "x", location.x(), "y", location.y(), "z", location.z(),
                    "state", plugin.messages().text(sender, machine.state().messageKey()));
        }
    }

    private void info(CommandSender sender) {
        Player player = requirePlayer(sender);
        if (player == null) {
            return;
        }
        VendingMachine machine = targetedMachine(player);
        if (machine == null) {
            plugin.messages().warning(sender, "machine.not_found");
            return;
        }
        MachineInfoPresenter.send(plugin, sender, machine);
    }

    private void restock(CommandSender sender) {
        Player player = requirePlayer(sender);
        if (player == null || notPermitted(sender, "openvending.restock")) {
            return;
        }
        VendingMachine machine = targetedMachine(player);
        if (machine == null) {
            plugin.messages().warning(sender, "machine.not_found");
            return;
        }
        if (!Authorization.canRestock(plugin, player, machine)) {
            plugin.messages().warning(sender, "restock.denied");
            return;
        }
        plugin.userInterface().openManagement(player, machine);
    }

    private void withdraw(CommandSender sender) {
        Player player = requirePlayer(sender);
        if (player == null || notPermitted(sender, "openvending.withdraw")) {
            return;
        }
        VendingMachine machine = targetedMachine(player);
        if (machine == null) {
            plugin.messages().warning(sender, "machine.not_found");
            return;
        }
        plugin.cash().withdraw(player, machine);
    }

    private void giveItem(CommandSender sender, String[] args) {
        Player player = requirePlayer(sender);
        if (player == null || notPermitted(sender, "openvending.admin")) {
            return;
        }
        if (args.length < 2) {
            plugin.messages().info(sender, "command.help.giveitem");
            return;
        }
        ProductDefinition product = plugin.products().get(args[1]);
        if (product == null) {
            plugin.messages().warning(sender, "giveitem.unknown", "product", args[1]);
            return;
        }
        int amount = 1;
        if (args.length >= 3) {
            try {
                amount = Math.max(1, Integer.parseInt(args[2]));
            } catch (NumberFormatException exception) {
                plugin.messages().warning(sender, "general.number_invalid", "value", args[2]);
                return;
            }
        }
        if (plugin.adapters().inventory().give(player, product, amount)) {
            plugin.messages().success(sender, "giveitem.given", "amount", amount, "product", product.plainName());
        } else {
            plugin.messages().warning(sender, "purchase.fail_inventory_full");
        }
    }

    private int targetRange() {
        return Math.max(1, (int) Math.ceil(plugin.settings().maxInteractionDistance()));
    }

    private VendingMachine targetedMachine(Player player) {
        Block block = player.getTargetBlockExact(targetRange());
        return block == null ? null : plugin.machines().getAt(block.getLocation());
    }

    private Player requirePlayer(CommandSender sender) {
        if (sender instanceof Player player) {
            return player;
        }
        plugin.messages().warning(sender, "general.player_only");
        return null;
    }

    private boolean notPermitted(CommandSender sender, String permission) {
        if (plugin.adapters().permission().has(sender, permission)) {
            return false;
        }
        plugin.messages().warning(sender, "general.no_permission");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return CommandSuggestions.filter(SUBCOMMANDS, args[0]);
        }
        String sub = args[0].toLowerCase(Locale.ROOT);
        if (sub.equals("create")) {
            if (args.length == 2) {
                return CommandSuggestions.filter(plugin.machineTypes().ids(), args[1]);
            }
            if (args.length == 3) {
                return CommandSuggestions.filter(companyIds(), args[2]);
            }
        }
        if (sub.equals("giveitem") && args.length == 2) {
            return CommandSuggestions.filter(plugin.products().ids(), args[1]);
        }
        return List.of();
    }

    private List<String> companyIds() {
        ConfigurationSection companies = plugin.getConfig().getConfigurationSection("businesses.companies");
        return companies == null ? List.of() : new ArrayList<>(companies.getKeys(false));
    }
}
