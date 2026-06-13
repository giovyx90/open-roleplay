package dev.openrp.weapons.wanted;

import it.meridian.core.permissions.NextPermissions;
import dev.openrp.weapons.module.WeaponsModule;
import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent.Builder;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

public class WantedCommand implements CommandExecutor, TabCompleter {
   private static final int RECORDS_PER_PAGE = 6;
   private final WeaponsModule module;

   public WantedCommand(WeaponsModule module) {
      this.module = module;
   }

   public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
      if (!this.isAuthorized(sender)) {
         sender.sendMessage(Component.text("Only law enforcement can use this command.", NamedTextColor.RED));
         return true;
      }

      if (args.length == 0) {
         this.sendHelp(sender);
         return true;
      }

      switch (args[0].toLowerCase()) {
         case "add":
            if (!(sender instanceof Player player)) {
               sender.sendMessage(Component.text("Only players can open the wanted GUI.", NamedTextColor.RED));
               return true;
            }

            this.module.getWantedGUI().open(player);
            break;
         case "listonline":
            this.sendList(sender, this.module.getWantedManager().getOnlineRecords(), "Online wanted records", this.parsePage(args, 1));
            break;
         case "list":
            this.sendList(sender, this.module.getWantedManager().getAllRecords(), "All wanted records", this.parsePage(args, 1));
            break;
         case "remove":
            if (args.length < 2) {
               sender.sendMessage(Component.text("Usage: /wanted remove <player>", NamedTextColor.RED));
               return true;
            }

            WantedRecord removed = this.module.getWantedManager().removeRecord(args[1]);
            if (removed == null) {
               sender.sendMessage(Component.text("No active wanted record found for " + args[1] + ".", NamedTextColor.RED));
               return true;
            }

            sender.sendMessage(Component.text("Removed wanted record for " + removed.getPlayerName() + ".", NamedTextColor.GREEN));
            break;
         default:
            this.sendHelp(sender);
      }

      return true;
   }

   private boolean isAuthorized(CommandSender sender) {
      return sender.hasPermission("openrp.wanted.manage")
         || NextPermissions.hasAny(sender,
            NextPermissions.Police.ARRESTS_MANAGE,
            NextPermissions.Police.ADMIN)
         || sender instanceof Player player && this.module.isLEO(player.getUniqueId());
   }

   private void sendHelp(CommandSender sender) {
      sender.sendMessage(Component.text("Wanted Commands", NamedTextColor.GOLD, new TextDecoration[]{TextDecoration.BOLD}));
      sender.sendMessage(Component.text("/wanted add", NamedTextColor.YELLOW).append(Component.text(" - Open the wanted record GUI", NamedTextColor.GRAY)));
      sender.sendMessage(
         Component.text("/wanted listonline [page]", NamedTextColor.YELLOW).append(Component.text(" - List online wanted players", NamedTextColor.GRAY))
      );
      sender.sendMessage(Component.text("/wanted list [page]", NamedTextColor.YELLOW).append(Component.text(" - List all wanted players", NamedTextColor.GRAY)));
      sender.sendMessage(
         Component.text("/wanted remove <player>", NamedTextColor.YELLOW).append(Component.text(" - Remove a wanted record", NamedTextColor.GRAY))
      );
   }

   private int parsePage(String[] args, int index) {
      if (args.length <= index) {
         return 1;
      }

      try {
         return Math.max(1, Integer.parseInt(args[index]));
      } catch (NumberFormatException ignored) {
         return 1;
      }
   }

   private void sendList(CommandSender sender, Iterable<WantedRecord> records, String title, int requestedPage) {
      List<WantedRecord> list = new ArrayList<>();
      records.forEach(list::add);
      if (list.isEmpty()) {
         sender.sendMessage(Component.text("No wanted records found.", NamedTextColor.GRAY));
      } else {
         list.sort((left, right) -> right.getCreatedAt().compareTo(left.getCreatedAt()));
         int totalPages = Math.max(1, (int)Math.ceil(list.size() / 6.0));
         int page = Math.min(requestedPage, totalPages);
         int start = (page - 1) * 6;
         int end = Math.min(start + 6, list.size());
         sender.sendMessage(
            Component.text(title + " (" + list.size() + ") - Page " + page + "/" + totalPages, NamedTextColor.GOLD, new TextDecoration[]{TextDecoration.BOLD})
         );

         for (int i = start; i < end; i++) {
            WantedRecord record = list.get(i);
            boolean online = Bukkit.getPlayer(record.getPlayerUuid()) != null;
            sender.sendMessage(
               ((Builder)((Builder)((Builder)((Builder)((Builder)((Builder)((Builder)((Builder)((Builder)Component.text()
                                             .append(Component.text("#" + (i + 1) + " ", NamedTextColor.DARK_GRAY)))
                                          .append(Component.text(online ? "[ONLINE] " : "[OFFLINE] ", online ? NamedTextColor.GREEN : NamedTextColor.RED)))
                                       .append(Component.text(record.getPlayerName(), NamedTextColor.WHITE)))
                                    .append(Component.text("  Arrest: ", NamedTextColor.DARK_GRAY)))
                                 .append(
                                    Component.text(
                                       record.isArrestRequired() ? "Yes" : "No", record.isArrestRequired() ? NamedTextColor.RED : NamedTextColor.GREEN
                                    )
                                 ))
                              .append(Component.text("  Officer: ", NamedTextColor.DARK_GRAY)))
                           .append(Component.text(record.getOfficerName(), NamedTextColor.WHITE)))
                        .append(Component.text("  Added: ", NamedTextColor.DARK_GRAY)))
                     .append(Component.text(this.module.getWantedManager().formatDate(record.getCreatedAt()), NamedTextColor.YELLOW)))
                  .build()
            );
            sender.sendMessage(
               ((Builder)((Builder)Component.text().append(Component.text("   Reason: ", NamedTextColor.DARK_GRAY)))
                     .append(Component.text(record.getReason(), NamedTextColor.GRAY)))
                  .build()
            );
         }
      }
   }

   public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
      if (!this.isAuthorized(sender)) {
         return List.of();
      } else if (args.length == 1) {
         return List.of("add", "listonline", "list", "remove").stream().filter(s -> s.startsWith(args[0].toLowerCase())).toList();
      } else {
         return args.length == 2 && args[0].equalsIgnoreCase("remove")
            ? this.module
               .getWantedManager()
               .getAllRecords()
               .stream()
               .map(WantedRecord::getPlayerName)
               .filter(name -> name.toLowerCase().startsWith(args[1].toLowerCase()))
               .toList()
            : List.of();
      }
   }
}
