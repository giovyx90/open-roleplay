package dev.openrp.weapons.frisk;

import it.meridian.core.gui.NexoUI;
import it.meridian.core.staffboard.StaffBoardMetadata;
import it.meridian.core.staffboard.model.StaffBoardCategory;
import it.meridian.core.staffboard.model.StaffBoardLogEvent;
import it.meridian.core.staffboard.model.StaffBoardSensitivity;
import it.meridian.core.staffboard.model.StaffBoardSeverity;
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
         sender.sendMessage("Only players can use this command.");
         return true;
      } else {
         if (!player.hasPermission("openrp.frisk.use")) {
            player.sendMessage(Component.text("You don't have permission to frisk players.", NamedTextColor.RED));
            return true;
         }

         if (args.length == 0) {
            player.sendMessage(Component.text("Usage: /frisk <player>", NamedTextColor.RED));
            return true;
         }

         String subCmd = args[0];
         if (subCmd.equalsIgnoreCase("accept")) {
            FriskCommand.FriskRequest req = this.pendingRequests.remove(player.getUniqueId());
            if (req == null) {
               player.sendMessage(Component.text("You have no pending frisk requests.", NamedTextColor.RED));
               return true;
            } else {
               Player searcher = Bukkit.getPlayer(req.requesterUuid);
               if (searcher != null && searcher.isOnline()) {
                  this.openFriskInventory(searcher, player);
                  return true;
               } else {
                  player.sendMessage(Component.text("The player who requested the frisk is no longer online.", NamedTextColor.RED));
                  return true;
               }
            }
         } else {
            if (subCmd.equalsIgnoreCase("deny")) {
               this.handleDeny(player);
               return true;
            }

            Player target = Bukkit.getPlayer(subCmd);
            if (target != null && target.isOnline()) {
               if (target.getUniqueId().equals(player.getUniqueId())) {
                  player.sendMessage(Component.text("You cannot frisk yourself.", NamedTextColor.RED));
                  return true;
               }

               if (player.getLocation().distance(target.getLocation()) > 5.0) {
                  player.sendMessage(Component.text("You are too far from " + target.getName() + " (max 5 blocks).", NamedTextColor.RED));
                  return true;
               }

               boolean isFdo = this.module.isLEO(player.getUniqueId());
               if (!isFdo) {
                  ItemStack itemInHand = player.getInventory().getItemInMainHand();
                  WeaponDefinition weapon = this.module.getWeaponRegistry().getWeapon(itemInHand);
                  if (weapon == null) {
                     player.sendMessage(Component.text("You must be armed to frisk someone.", NamedTextColor.RED));
                     return true;
                  }
               }

               boolean forceFrisk = false;
               if (this.module.getArrestManager().isArrested(target.getUniqueId())) {
                  for (String regionId : this.module.getArrestManager().getJailRegions()) {
                     if (this.module.getArrestManager().isInJailRegion(target, regionId)) {
                        forceFrisk = true;
                        break;
                     }
                  }
               }

               if (forceFrisk) {
                  player.sendMessage(Component.text("Target is in jail. Forcing frisk...", NamedTextColor.GREEN));
                  this.openFriskInventory(player, target);
                  return true;
               } else {
                  this.pendingRequests.put(target.getUniqueId(), new FriskCommand.FriskRequest(player.getUniqueId(), isFdo));
                  player.sendMessage(Component.text("Frisk request sent to " + target.getName() + ". Waiting for response...", NamedTextColor.YELLOW));
                  Component acceptBtn = Component.text("[Accept]", NamedTextColor.GREEN).clickEvent(ClickEvent.runCommand("/frisk accept"));
                  Component denyBtn = Component.text("[Deny]", NamedTextColor.RED).clickEvent(ClickEvent.runCommand("/frisk deny"));
                  target.sendMessage(
                     ((Builder)((Builder)((Builder)((Builder)((Builder)((Builder)Component.text().append(Component.text("[Frisk] ", NamedTextColor.YELLOW)))
                                       .append(Component.text(player.getName(), NamedTextColor.WHITE)))
                                    .append(Component.text(" wants to frisk you. ", NamedTextColor.GRAY)))
                                 .append(acceptBtn))
                              .append(Component.text(" ", NamedTextColor.GRAY)))
                           .append(denyBtn))
                        .build()
                  );
                  Bukkit.getScheduler().runTaskLater(this.module.getCore(), () -> {
                     FriskCommand.FriskRequest req = this.pendingRequests.get(target.getUniqueId());
                     if (req != null && req.requesterUuid.equals(player.getUniqueId())) {
                        player.sendMessage(Component.text("Frisk request to " + target.getName() + " timed out.", NamedTextColor.RED));
                        target.sendMessage(Component.text("Frisk request from " + player.getName() + " has expired.", NamedTextColor.GRAY));
                        this.handleDeny(target);
                     }
                  }, 1200L);
                  return true;
               }
            } else {
               player.sendMessage(Component.text("Player not found or not online.", NamedTextColor.RED));
               return true;
            }
         }
      }
   }

   private void handleDeny(Player player) {
      FriskCommand.FriskRequest req = this.pendingRequests.remove(player.getUniqueId());
      if (req == null) {
         player.sendMessage(Component.text("You have no pending frisk requests.", NamedTextColor.RED));
      } else {
         Player searcher = Bukkit.getPlayer(req.requesterUuid);
         if (searcher != null && searcher.isOnline()) {
            searcher.sendMessage(Component.text(player.getName() + " denied your frisk request.", NamedTextColor.RED));
         }

         Location loc = player.getLocation().add(0.0, 2.2, 0.0);
         final TextDisplay display = (TextDisplay)player.getWorld().spawn(loc, TextDisplay.class, d -> {
            if (req.isFdo) {
               d.text(Component.text("⚠ ARRESTABLE ⚠", NamedTextColor.YELLOW, new TextDecoration[]{TextDecoration.BOLD}));
            } else {
               d.text(Component.text("☠ KILLABLE ☠", NamedTextColor.RED, new TextDecoration[]{TextDecoration.BOLD}));
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
         player.sendMessage(Component.text("Frisk request denied. You have been tagged.", NamedTextColor.YELLOW));
      }
   }

   private void openFriskInventory(Player searcher, Player target) {
      Component title = NexoUI.getGlyphTitle("frisk_gui", "Frisk");
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
      searcher.sendMessage(Component.text("You are frisking " + target.getName() + " (view-only).", NamedTextColor.GREEN));
      target.sendMessage(Component.text(searcher.getName() + " is now frisking you.", NamedTextColor.YELLOW));
      emitSearchPerformed(searcher, target);
   }

   private void emitSearchPerformed(Player searcher, Player target) {
      StaffBoardMetadata metadata = StaffBoardMetadata.create()
         .put("officer_uuid", searcher.getUniqueId())
         .put("officer_name", searcher.getName())
         .put("target_uuid", target.getUniqueId())
         .put("target_name", target.getName())
         .put("source_system", "OpenWeapons")
         .putLocation(target.getLocation());

      this.module.getCore().getStaffBoardPublisher().emit(StaffBoardLogEvent.builder("fdo.search.performed", "OpenWeapons")
         .category(StaffBoardCategory.FDO)
         .severity(StaffBoardSeverity.NOTICE)
         .sensitivity(StaffBoardSensitivity.SENSITIVE)
         .actor(searcher)
         .target(target)
         .location(target.getLocation())
         .message(searcher.getName() + " frisked " + target.getName() + ".")
         .metadataJson(metadata.toJson())
         .build());
   }

   private static class FriskRequest {
      UUID requesterUuid;
      boolean isFdo;

      FriskRequest(UUID requesterUuid, boolean isFdo) {
         this.requesterUuid = requesterUuid;
         this.isFdo = isFdo;
      }
   }
}
