package dev.openrp.weapons.frisk;

import dev.openrp.weapons.util.OpenGuiItems;
import dev.openrp.weapons.bridge.staff.StaffBoardMetadata;
import dev.openrp.weapons.bridge.staff.StaffBoardCategory;
import dev.openrp.weapons.bridge.staff.StaffBoardLogEvent;
import dev.openrp.weapons.bridge.staff.StaffBoardSensitivity;
import dev.openrp.weapons.bridge.staff.StaffBoardSeverity;
import dev.openrp.weapons.model.WeaponDefinition;
import dev.openrp.weapons.module.WeaponsModule;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent.Builder;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.entity.Display.Billboard;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

public class FriskCommand implements CommandExecutor {
   private final WeaponsModule module;
   private final Map<UUID, FriskCommand.FriskRequest> pendingRequests = new HashMap<>();

   public FriskCommand(WeaponsModule module) {
      this.module = module;
   }

   public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
      if (!(sender instanceof Player player)) {
         sender.sendMessage("Solo i giocatori possono usare questo comando.");
         return true;
      } else {
         if (!player.hasPermission("openrp.frisk.use")) {
            player.sendMessage(Component.text("Non hai il permesso di perquisire giocatori.", NamedTextColor.RED));
            return true;
         }

         if (args.length == 0) {
            player.sendMessage(Component.text("Uso: /perquisisci <giocatore>", NamedTextColor.RED));
            return true;
         }

         String subCmd = args[0];
         if (subCmd.equalsIgnoreCase("accetta") || subCmd.equalsIgnoreCase("accept")) {
            FriskCommand.FriskRequest req = this.pendingRequests.remove(player.getUniqueId());
            if (req == null) {
               player.sendMessage(Component.text("Non hai richieste di perquisizione in sospeso.", NamedTextColor.RED));
               return true;
            } else {
               Player searcher = Bukkit.getPlayer(req.requesterUuid);
               if (searcher != null && searcher.isOnline()) {
                  this.openFriskInventory(searcher, player);
                  return true;
               } else {
                  player.sendMessage(Component.text("Il giocatore che ha richiesto la perquisizione non e' piu' online.", NamedTextColor.RED));
                  return true;
               }
            }
         } else {
            if (subCmd.equalsIgnoreCase("rifiuta") || subCmd.equalsIgnoreCase("deny")) {
               this.handleDeny(player);
               return true;
            }

            Player target = Bukkit.getPlayer(subCmd);
            if (target != null && target.isOnline()) {
               if (target.getUniqueId().equals(player.getUniqueId())) {
                  player.sendMessage(Component.text("Non puoi perquisire te stesso.", NamedTextColor.RED));
                  return true;
               }

               if (player.getLocation().distance(target.getLocation()) > 5.0) {
                  player.sendMessage(Component.text("Sei troppo lontano da " + target.getName() + " (massimo 5 blocchi).", NamedTextColor.RED));
                  return true;
               }

               ItemStack itemInHand = player.getInventory().getItemInMainHand();
               WeaponDefinition weapon = this.module.getWeaponRegistry().getWeapon(itemInHand);
               if (weapon == null) {
                  player.sendMessage(Component.text("Devi essere armato per perquisire qualcuno.", NamedTextColor.RED));
                  return true;
               }

               boolean markArrestable = player.hasPermission("openrp.frisk.arrestable");
               this.pendingRequests.put(target.getUniqueId(), new FriskCommand.FriskRequest(player.getUniqueId(), markArrestable));
               player.sendMessage(Component.text("Richiesta di perquisizione inviata a " + target.getName() + ". In attesa di risposta...", NamedTextColor.YELLOW));
               Component acceptBtn = Component.text("[Accetta]", NamedTextColor.GREEN).clickEvent(ClickEvent.runCommand("/perquisisci accetta"));
               Component denyBtn = Component.text("[Rifiuta]", NamedTextColor.RED).clickEvent(ClickEvent.runCommand("/perquisisci rifiuta"));
               target.sendMessage(
                  ((Builder)((Builder)((Builder)((Builder)((Builder)((Builder)Component.text().append(Component.text("[Perquisizione] ", NamedTextColor.YELLOW)))
                                    .append(Component.text(player.getName(), NamedTextColor.WHITE)))
                                 .append(Component.text(" vuole perquisirti. ", NamedTextColor.GRAY)))
                              .append(acceptBtn))
                           .append(Component.text(" ", NamedTextColor.GRAY)))
                        .append(denyBtn))
                     .build()
               );
               Bukkit.getScheduler().runTaskLater(this.module.getCore(), () -> {
                  FriskCommand.FriskRequest req = this.pendingRequests.get(target.getUniqueId());
                  if (req != null && req.requesterUuid.equals(player.getUniqueId())) {
                     player.sendMessage(Component.text("Richiesta di perquisizione a " + target.getName() + " scaduta.", NamedTextColor.RED));
                     target.sendMessage(Component.text("Richiesta di perquisizione da " + player.getName() + " scaduta.", NamedTextColor.GRAY));
                     this.handleDeny(target);
                  }
               }, 1200L);
               return true;
            } else {
               player.sendMessage(Component.text("Giocatore non trovato o non online.", NamedTextColor.RED));
               return true;
            }
         }
      }
   }

   private void handleDeny(Player player) {
      FriskCommand.FriskRequest req = this.pendingRequests.remove(player.getUniqueId());
      if (req == null) {
         player.sendMessage(Component.text("Non hai richieste di perquisizione in sospeso.", NamedTextColor.RED));
      } else {
         Player searcher = Bukkit.getPlayer(req.requesterUuid);
         if (searcher != null && searcher.isOnline()) {
            searcher.sendMessage(Component.text(player.getName() + " ha rifiutato la tua richiesta di perquisizione.", NamedTextColor.RED));
         }

         Location loc = player.getLocation().add(0.0, 2.2, 0.0);
         final TextDisplay display = (TextDisplay)player.getWorld().spawn(loc, TextDisplay.class, d -> {
            if (req.markArrestable) {
               d.text(Component.text("⚠ Arrestabile ⚠", NamedTextColor.YELLOW, new TextDecoration[]{TextDecoration.BOLD}));
            } else {
               d.text(Component.text("☠ Abbattibile ☠", NamedTextColor.RED, new TextDecoration[]{TextDecoration.BOLD}));
            }

            d.setBillboard(Billboard.CENTER);
            d.setSeeThrough(false);
            d.setShadowed(true);
         });
         player.addPassenger(display);
         (new BukkitRunnable() {
            public void run() {
               if (display.isValid()) {
                  display.remove();
               }
            }
         }).runTaskLater(this.module.getCore(), 1200L);
         player.sendMessage(Component.text("Perquisizione rifiutata. Sei stato segnalato.", NamedTextColor.YELLOW));
      }
   }

   private void openFriskInventory(Player searcher, Player target) {
      Component title = OpenGuiItems.getGlyphTitle("frisk_gui", "Perquisisci");
      FriskGUIHolder holder = new FriskGUIHolder(searcher.getUniqueId(), target.getUniqueId());
      Inventory viewInv = Bukkit.createInventory(holder, 54, title);
      holder.setInventory(viewInv);
      ItemStack[] armor = target.getInventory().getArmorContents();

      for (int i = 0; i < armor.length; i++) {
         if (armor[i] != null) {
            viewInv.setItem(i, armor[i].clone());
         }
      }

      ItemStack offhand = target.getInventory().getItemInOffHand();
      if (offhand.getType() != Material.AIR) {
         viewInv.setItem(4, offhand.clone());
      }

      ItemStack[] contents = target.getInventory().getStorageContents();

      for (int i = 0; i < contents.length && 18 + i < 54; i++) {
         if (contents[i] != null) {
            viewInv.setItem(18 + i, contents[i].clone());
         }
      }

      searcher.openInventory(viewInv);
      searcher.sendMessage(Component.text("Stai perquisendo " + target.getName() + " (sola visualizzazione).", NamedTextColor.GREEN));
      target.sendMessage(Component.text(searcher.getName() + " ti sta perquisendo.", NamedTextColor.YELLOW));
      emitSearchPerformed(searcher, target);
   }

   private void emitSearchPerformed(Player searcher, Player target) {
      StaffBoardMetadata metadata = StaffBoardMetadata.create()
         .put("searcher_uuid", searcher.getUniqueId())
         .put("searcher_name", searcher.getName())
         .put("target_uuid", target.getUniqueId())
         .put("target_name", target.getName())
         .put("source_system", "OpenWeapons")
         .putLocation(target.getLocation());

      this.module.getStaffLogBridge().emit(StaffBoardLogEvent.builder("crime.frisk.performed", "OpenWeapons")
         .category(StaffBoardCategory.CRIME)
         .severity(StaffBoardSeverity.NOTICE)
         .sensitivity(StaffBoardSensitivity.SENSITIVE)
         .actor(searcher)
         .target(target)
         .location(target.getLocation())
         .message(searcher.getName() + " ha perquisito " + target.getName() + ".")
         .metadataJson(metadata.toJson())
         .build());
   }

   private static class FriskRequest {
      UUID requesterUuid;
      boolean markArrestable;

      FriskRequest(UUID requesterUuid, boolean markArrestable) {
         this.requesterUuid = requesterUuid;
         this.markArrestable = markArrestable;
      }
   }
}
