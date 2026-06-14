package dev.openrp.core.command;

import dev.openrp.core.OpenCorePlugin;
import dev.openrp.core.api.message.OpenMessages;
import dev.openrp.core.api.module.OpenModule;
import dev.openrp.core.api.module.OpenModuleReloadResult;
import dev.openrp.core.api.module.OpenModuleState;
import dev.openrp.core.api.permission.OpenPermissions;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class OpenCoreCommand implements CommandExecutor, TabCompleter {
    private static final String TITLE = "OpenCore";

    private final OpenCorePlugin plugin;

    public OpenCoreCommand(OpenCorePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (args.length == 0 || args[0].equalsIgnoreCase("status")) {
            if (!OpenPermissions.hasAny(sender, OpenPermissions.CORE_DEBUG, OpenPermissions.CORE_ADMIN)) {
                sender.sendMessage(OpenMessages.error(TITLE, "Non hai il permesso per leggere lo stato dei moduli."));
                return true;
            }
            showStatus(sender);
            return true;
        }

        if (args[0].equalsIgnoreCase("reload")) {
            if (!OpenPermissions.hasAny(sender, OpenPermissions.CORE_RELOAD, OpenPermissions.CORE_ADMIN)) {
                sender.sendMessage(OpenMessages.error(TITLE, "Non hai il permesso per ricaricare Open Core."));
                return true;
            }
            if (args.length == 1) {
                plugin.reloadCoreConfig();
                plugin.moduleManager().reloadAll();
                sender.sendMessage(OpenMessages.success(TITLE, "Config e moduli ricaricati."));
                return true;
            }
            String moduleId = args[1].toLowerCase(Locale.ROOT);
            OpenModuleReloadResult result = plugin.moduleManager().reload(moduleId);
            switch (result) {
                case RELOADED -> sender.sendMessage(OpenMessages.success(TITLE, "Modulo '" + moduleId + "' ricaricato."));
                case DISABLED_BY_CONFIG -> sender.sendMessage(OpenMessages.warning(TITLE,
                        "Modulo '" + moduleId + "' disabilitato in config.yml."));
                case FAILED -> sender.sendMessage(OpenMessages.error(TITLE,
                        "Modulo '" + moduleId + "' non ricaricato. Controlla la console."));
                case NOT_FOUND -> sender.sendMessage(OpenMessages.error(TITLE, "Modulo '" + moduleId + "' non trovato."));
            }
            return true;
        }

        sender.sendMessage(OpenMessages.info(TITLE, "Uso: /opencore [status|reload] [modulo]"));
        return true;
    }

    private void showStatus(CommandSender sender) {
        sender.sendMessage(OpenMessages.info(TITLE, "Database: "
                + (plugin.isDatabaseAvailable() ? "disponibile" : "non disponibile")));
        Map<String, OpenModule> modules = plugin.moduleManager().registeredModules();
        if (modules.isEmpty()) {
            sender.sendMessage(OpenMessages.warning(TITLE, "Nessun modulo registrato."));
            return;
        }
        sender.sendMessage(OpenMessages.info(TITLE, "Stato moduli:"));
        for (String id : modules.keySet()) {
            OpenModuleState state = plugin.moduleManager().state(id);
            String error = plugin.moduleManager().lastError(id)
                    .map(value -> " (" + sanitize(value) + ")")
                    .orElse("");
            sender.sendMessage(OpenMessages.info(TITLE, id + ": " + state.name() + error));
        }
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                @NotNull String label, @NotNull String[] args) {
        if (!OpenPermissions.hasAny(sender, OpenPermissions.CORE_DEBUG, OpenPermissions.CORE_RELOAD,
                OpenPermissions.CORE_ADMIN)) {
            return List.of();
        }
        if (args.length == 1) {
            List<String> values = new ArrayList<>();
            if (OpenPermissions.hasAny(sender, OpenPermissions.CORE_DEBUG, OpenPermissions.CORE_ADMIN)) {
                values.add("status");
            }
            if (OpenPermissions.hasAny(sender, OpenPermissions.CORE_RELOAD, OpenPermissions.CORE_ADMIN)) {
                values.add("reload");
            }
            return matching(values, args[0]);
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("reload")
                && OpenPermissions.hasAny(sender, OpenPermissions.CORE_RELOAD, OpenPermissions.CORE_ADMIN)) {
            return matching(new ArrayList<>(plugin.moduleManager().registeredModules().keySet()), args[1]);
        }
        return List.of();
    }

    private List<String> matching(List<String> values, String prefix) {
        String normalized = prefix == null ? "" : prefix.toLowerCase(Locale.ROOT);
        return values.stream()
                .filter(value -> value.startsWith(normalized))
                .toList();
    }

    private String sanitize(String value) {
        return value == null ? "" : value.replace("<", "").replace(">", "");
    }
}
