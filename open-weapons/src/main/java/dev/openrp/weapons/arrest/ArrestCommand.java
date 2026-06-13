package dev.openrp.weapons.arrest;

import dev.openrp.weapons.module.WeaponsModule;
import it.meridian.police.model.PoliceAction;
import it.meridian.police.module.PoliceModule;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class ArrestCommand implements CommandExecutor, TabCompleter {
    private final WeaponsModule module;

    public ArrestCommand(WeaponsModule module) {
        this.module = module;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player officer)) return true;

        // LEO check
        PoliceModule police = module.getCore().getModuleManager().getModule(PoliceModule.class);
        if (police != null && police.getService() != null && !officer.hasPermission("openrp.weapons.arrest.bypass")) {
            if (!police.getService().canPerform(officer, PoliceAction.ARREST)) {
                officer.sendMessage(Component.text("Solo gli agenti in servizio possono arrestare giocatori.", NamedTextColor.RED));
                return true;
            }
        } else if (!module.isLEO(officer.getUniqueId()) && !officer.hasPermission("openrp.weapons.arrest.bypass")) {
            officer.sendMessage(Component.text("Solo le forze dell'ordine possono arrestare giocatori.", NamedTextColor.RED));
            return true;
        }

        if (args.length != 1) {
            officer.sendMessage(Component.text("Uso: /arrest <giocatore>", NamedTextColor.RED));
            return true;
        }

        @SuppressWarnings("deprecation")
        org.bukkit.OfflinePlayer target = Bukkit.getOfflinePlayer(args[0]);
        if (target == null || (!target.isOnline() && !target.hasPlayedBefore())) {
            officer.sendMessage(Component.text("Giocatore non trovato o mai entrato qui.", NamedTextColor.RED));
            return true;
        }

        if (target.getUniqueId().equals(officer.getUniqueId())) {
            officer.sendMessage(Component.text("Non puoi arrestare te stesso.", NamedTextColor.RED));
            return true;
        }

        Player onlineTarget = target.getPlayer();
        if (onlineTarget != null && onlineTarget.isOnline()) {
            // Must be handcuffed if online
            if (!module.getHandcuffManager().isHandcuffed(target.getUniqueId())) {
                officer.sendMessage(Component.text("Il giocatore deve essere ammanettato prima dell'arresto.", NamedTextColor.RED));
                return true;
            }

            // Must be nearby if online
            if (officer.getLocation().distance(onlineTarget.getLocation()) > 5) {
                officer.sendMessage(Component.text("Devi essere piu' vicino al giocatore per arrestarlo.", NamedTextColor.RED));
                return true;
            }
        }

        // Already arrested
        if (module.getArrestManager().isArrested(target.getUniqueId())) {
            officer.sendMessage(Component.text("Questo giocatore e' gia' arrestato.", NamedTextColor.RED));
            return true;
        }

        // Open GUI
        module.getArrestGUI().open(officer, target);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> names = new ArrayList<>();
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (module.getHandcuffManager().isHandcuffed(p.getUniqueId())) {
                    names.add(p.getName());
                }
            }
            return names;
        }
        return List.of();
    }
}
