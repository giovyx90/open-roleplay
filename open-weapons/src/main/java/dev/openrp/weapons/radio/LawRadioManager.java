package dev.openrp.weapons.radio;

import io.papermc.paper.event.player.AsyncChatEvent;
import it.meridian.core.gui.NexoUI;
import it.meridian.core.utils.ItemBuilder;
import dev.openrp.weapons.module.WeaponsModule;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.TextComponent.Builder;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

public class LawRadioManager implements Listener, CommandExecutor {
   private static final int RADIO_CUSTOM_MODEL_DATA = 91;
   private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss").withZone(ZoneId.of("Europe/Rome"));
   private final WeaponsModule module;
   private final NamespacedKey radioKey;
   private final Set<UUID> radioChat = ConcurrentHashMap.newKeySet();
   private final Map<String, LawRadioManager.SupportCall> activeSupportCalls = new ConcurrentHashMap<>();
   private volatile long globalSupportCooldownUntil = 0L;

   public LawRadioManager(WeaponsModule module) {
      this.module = module;
      this.radioKey = new NamespacedKey(module.getCore(), "law_radio");
   }

   public ItemStack createLawRadio() {
      ItemStack item = new ItemBuilder(Material.PAPER)
         .name(Component.text("Law Radio", NamedTextColor.GRAY)
            .decoration(TextDecoration.BOLD, false)
            .decoration(TextDecoration.ITALIC, false))
         .customModelData(91)
         .lore(
            new Component[]{
               Component.text("", NamedTextColor.GRAY),
               Component.text("Law enforcement cross-corps radio", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
               Component.text("Right-click to open radio controls", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)
            }
         )
         .build();
      ItemMeta meta = item.getItemMeta();
      if (meta != null) {
         meta.getPersistentDataContainer().set(this.radioKey, PersistentDataType.BYTE, (byte)1);
         item.setItemMeta(meta);
      }

      return item;
   }

   public boolean isLawRadio(ItemStack item) {
      return item != null && item.hasItemMeta() ? item.getItemMeta().getPersistentDataContainer().has(this.radioKey, PersistentDataType.BYTE) : false;
   }

   public void cleanup() {
      this.radioChat.clear();
      this.activeSupportCalls.clear();
      this.globalSupportCooldownUntil = 0L;
   }

   @EventHandler
   public void onInteract(PlayerInteractEvent event) {
      if (event.getHand() == EquipmentSlot.HAND) {
         if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            if (this.isLawRadio(event.getItem())) {
               event.setCancelled(true);
               Player player = event.getPlayer();
               if (!this.canUseRadio(player)) {
                  player.sendMessage(Component.text("Only law enforcement can use this radio.", NamedTextColor.RED));
               } else {
                  this.openMainGui(player);
               }
            }
         }
      }
   }

   @EventHandler
   public void onInventoryClick(InventoryClickEvent event) {
      if (event.getWhoClicked() instanceof Player player) {
         if (event.getInventory().getHolder() instanceof LawRadioManager.MainHolder) {
            event.setCancelled(true);
            if (event.getClickedInventory() != null && event.getClickedInventory().equals(event.getInventory())) {
               switch (event.getRawSlot()) {
                  case 11:
                     this.toggleRadioChat(player);
                     break;
                  case 15:
                     this.openSupportGui(player);
                     break;
                  case 22:
                     player.closeInventory();
               }
            }
         } else {
            if (event.getInventory().getHolder() instanceof LawRadioManager.SupportHolder) {
               event.setCancelled(true);
               if (event.getClickedInventory() == null || !event.getClickedInventory().equals(event.getInventory())) {
                  return;
               }

               if (event.getRawSlot() >= 10 && event.getRawSlot() <= 14) {
                  int level = event.getRawSlot() - 9;
                  this.sendSupportRequest(player, level);
                  player.closeInventory();
               }
            }
         }
      }
   }

   @EventHandler(priority = EventPriority.LOWEST)
   public void onRadioChat(AsyncChatEvent event) {
      Player player = event.getPlayer();
      if (this.radioChat.contains(player.getUniqueId())) {
         event.setCancelled(true);
         String message = PlainTextComponentSerializer.plainText().serialize(event.message()).trim();
         Bukkit.getScheduler().runTask(this.module.getCore(), () -> {
            if (message.equalsIgnoreCase("cancel")) {
               this.radioChat.remove(player.getUniqueId());
               player.sendMessage(Component.text("Law radio chat disabled.", NamedTextColor.YELLOW));
            } else {
               this.sendRadioMessage(player, message);
            }
         });
      }
   }

   public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
      if (sender instanceof Player player) {
         if (args.length == 0) {
            if (!this.canUseRadio(player)) {
               player.sendMessage(Component.text("Only law enforcement can use this radio.", NamedTextColor.RED));
               return true;
            } else {
               this.openMainGui(player);
               return true;
            }
         } else {
            switch (args[0].toLowerCase()) {
               case "gps":
                  if (args.length < 2) {
                     player.sendMessage(Component.text("Usage: /lawradio gps <supportId>", NamedTextColor.RED));
                     return true;
                  }

                  this.activateSupportGps(player, args[1]);
                  break;
               case "toggle":
                  this.toggleRadioChat(player);
                  break;
               case "support":
                  if (args.length < 2) {
                     player.sendMessage(Component.text("Usage: /lawradio support <1-5>", NamedTextColor.RED));
                     return true;
                  }

                  try {
                     this.sendSupportRequest(player, Integer.parseInt(args[1]));
                  } catch (NumberFormatException e) {
                     player.sendMessage(Component.text("Severity must be between 1 and 5.", NamedTextColor.RED));
                  }
                  break;
               default:
                  player.sendMessage(Component.text("Usage: /lawradio <gps|toggle|support>", NamedTextColor.RED));
            }

            return true;
         }
      } else {
         sender.sendMessage(Component.text("Only players can use law radio actions.", NamedTextColor.RED));
         return true;
      }
   }

   private void openMainGui(Player player) {
      LawRadioManager.MainHolder holder = new LawRadioManager.MainHolder();
      Inventory inventory = Bukkit.createInventory(holder, 27, NexoUI.getGlyphTitle("law_radio_gui", "Law Radio"));
      holder.inventory = inventory;
      this.fill(inventory);
      inventory.setItem(
         11,
         new ItemBuilder(Material.NOTE_BLOCK)
            .name(
               Component.text(
                     this.radioChat.contains(player.getUniqueId()) ? "Radio Chat: ON" : "Radio Chat: OFF",
                     this.radioChat.contains(player.getUniqueId()) ? NamedTextColor.GREEN : NamedTextColor.RED,
                     new TextDecoration[]{TextDecoration.BOLD}
                  )
                  .decoration(TextDecoration.ITALIC, false)
            )
            .lore(new Component[]{Component.text("Toggle law enforcement radio chat", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)})
            .build()
      );
      inventory.setItem(
         15,
         new ItemBuilder(Material.REDSTONE_TORCH)
            .name(Component.text("Request Support", NamedTextColor.GOLD, new TextDecoration[]{TextDecoration.BOLD}).decoration(TextDecoration.ITALIC, false))
            .lore(new Component[]{Component.text("Select severity level 1-5", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)})
            .build()
      );
      inventory.setItem(
         22,
         NexoUI.getCancelButton(Component.text("Close", NamedTextColor.RED, new TextDecoration[]{TextDecoration.BOLD}).decoration(TextDecoration.ITALIC, false))
      );
      player.openInventory(inventory);
   }

   private void openSupportGui(Player player) {
      LawRadioManager.SupportHolder holder = new LawRadioManager.SupportHolder();
      Inventory inventory = Bukkit.createInventory(holder, 27, NexoUI.getGlyphTitle("law_support_gui", "Support"));
      holder.inventory = inventory;
      this.fill(inventory);

      for (int level = 1; level <= 5; level++) {
         inventory.setItem(
            9 + level,
            new ItemBuilder(Material.RED_CONCRETE)
               .name(
                  Component.text("Support LV" + level, this.severityColor(level), new TextDecoration[]{TextDecoration.BOLD})
                     .decoration(TextDecoration.ITALIC, false)
               )
               .lore(
                  new Component[]{
                     Component.text(level == 5 ? "Maximum severity" : "Severity level " + level, NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)
                  }
               )
               .build()
         );
      }

      player.openInventory(inventory);
   }

   private void toggleRadioChat(Player player) {
      if (!this.canUseRadio(player)) {
         player.sendMessage(Component.text("Only law enforcement can use this radio.", NamedTextColor.RED));
      } else if (!player.hasPermission("openrp.radio.use") && !this.hasRadio(player)) {
         player.sendMessage(Component.text("You need a Law Radio item.", NamedTextColor.RED));
      } else {
         if (this.radioChat.remove(player.getUniqueId())) {
            player.sendMessage(Component.text("Law radio chat disabled.", NamedTextColor.YELLOW));
         } else {
            this.radioChat.add(player.getUniqueId());
            player.sendMessage(Component.text("Law radio chat enabled. Type in chat to transmit, or type cancel.", NamedTextColor.GREEN));
         }

         player.closeInventory();
      }
   }

   private void sendRadioMessage(Player sender, String message) {
      if (!this.canUseRadio(sender)) {
         this.radioChat.remove(sender.getUniqueId());
         sender.sendMessage(Component.text("You can no longer use law radio.", NamedTextColor.RED));
      } else if (!sender.hasPermission("openrp.radio.use") && !this.hasRadio(sender)) {
         this.radioChat.remove(sender.getUniqueId());
         sender.sendMessage(Component.text("Law radio chat disabled because you do not have a radio.", NamedTextColor.RED));
      } else {
         Component component = ((Builder)((Builder)((Builder)((Builder)Component.text()
                        .append(Component.text("[LAW RADIO] ", NamedTextColor.DARK_AQUA, new TextDecoration[]{TextDecoration.BOLD})))
                     .append(Component.text(sender.getName(), NamedTextColor.WHITE)))
                  .append(Component.text(" » ", NamedTextColor.DARK_GRAY)))
               .append(Component.text(message, NamedTextColor.AQUA)))
            .build();

         for (Player recipient : this.getLawRecipients(true)) {
            recipient.sendMessage(component);
         }

         this.module.getCore().getLogger().info("[FDORadio] " + sender.getName() + ": " + message);
      }
   }

   private void sendSupportRequest(Player caller, int level) {
      if (!this.canUseRadio(caller)) {
         caller.sendMessage(Component.text("Only law enforcement can request radio support.", NamedTextColor.RED));
      } else if (!caller.hasPermission("openrp.radio.use") && !this.hasRadio(caller)) {
         caller.sendMessage(Component.text("You need a Law Radio item.", NamedTextColor.RED));
      } else if (level >= 1 && level <= 5) {
         long now = System.currentTimeMillis();
         if (this.globalSupportCooldownUntil > now) {
            long remaining = Math.max(1L, (this.globalSupportCooldownUntil - now + 999L) / 1000L);
            caller.sendMessage(Component.text("Support request cooldown: " + remaining + "s.", NamedTextColor.RED));
         } else {
            String id = UUID.randomUUID().toString().substring(0, 8);
            LawRadioManager.SupportCall call = new LawRadioManager.SupportCall(
               id, caller.getUniqueId(), caller.getName(), caller.getLocation().clone(), level, Instant.now()
            );
            this.activeSupportCalls.put(id, call);
            this.globalSupportCooldownUntil = now + 60000L;
            Component alert = this.buildSupportAlert(call);
            int recipients = 0;

            for (Player recipient : this.getLawRecipients(true)) {
               recipient.sendMessage(alert);
               recipient.playSound(recipient.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 0.9F, 1.0F + level * 0.08F);
               recipients++;
            }

            caller.sendMessage(Component.text("Support LV" + level + " request sent to " + recipients + " unit(s).", NamedTextColor.GREEN));
         }
      } else {
         caller.sendMessage(Component.text("Severity must be between 1 and 5.", NamedTextColor.RED));
      }
   }

   private void activateSupportGps(Player player, String supportId) {
      LawRadioManager.SupportCall call = this.activeSupportCalls.get(supportId);
      if (call == null) {
         player.sendMessage(Component.text("That support request is no longer active.", NamedTextColor.RED));
      } else if (!this.canUseRadio(player) && !player.hasPermission("openrp.radio.monitor")) {
         player.sendMessage(Component.text("You cannot use this support GPS.", NamedTextColor.RED));
      } else {
         this.module.getDispatchGpsManager().activate(player, "SUPPORT LV" + call.level(), () -> {
            Player caller = Bukkit.getPlayer(call.callerUuid());
            return caller != null && caller.isOnline() ? caller.getLocation().clone() : call.location().clone();
         });
      }
   }

   private Component buildSupportAlert(LawRadioManager.SupportCall call) {
      String coords = String.format("%.0f %.0f %.0f", call.location().getX(), call.location().getY(), call.location().getZ());
      Component gps = ((TextComponent)Component.text("[Activate GPS]", NamedTextColor.GREEN, new TextDecoration[]{TextDecoration.BOLD})
            .clickEvent(ClickEvent.runCommand("/lawradio gps " + call.id())))
         .hoverEvent(HoverEvent.showText(Component.text("Activate support GPS", NamedTextColor.GREEN)));
      return ((Builder)((Builder)((Builder)((Builder)((Builder)((Builder)((Builder)((Builder)((Builder)((Builder)((Builder)((Builder)Component.text()
                                             .append(
                                                Component.text(
                                                   "SUPPORT LV" + call.level(), this.severityColor(call.level()), new TextDecoration[]{TextDecoration.BOLD}
                                                )
                                             ))
                                          .append(Component.newline()))
                                       .append(Component.text("Coordinates: ", NamedTextColor.GRAY)))
                                    .append(Component.text(coords, NamedTextColor.WHITE)))
                                 .append(Component.newline()))
                              .append(Component.text("Caller: ", NamedTextColor.GRAY)))
                           .append(Component.text(call.callerName(), NamedTextColor.WHITE)))
                        .append(Component.newline()))
                     .append(Component.text("Time: ", NamedTextColor.GRAY)))
                  .append(Component.text(DATE_FORMAT.format(call.createdAt()), NamedTextColor.YELLOW)))
               .append(Component.newline()))
            .append(gps))
         .build();
   }

   private boolean canUseRadio(Player player) {
      return player.hasPermission("openrp.radio.use") || this.module.isLEO(player.getUniqueId());
   }

   private boolean hasRadio(Player player) {
      for (ItemStack item : player.getInventory().getContents()) {
         if (this.isLawRadio(item)) {
            return true;
         }
      }

      return false;
   }

   private Set<Player> getLawRecipients(boolean includeMonitors) {
      Set<Player> recipients = new LinkedHashSet<>(this.module.getOnlineCompanyEmployees("LAW_ENFORCEMENT"));
      if (includeMonitors) {
         for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.hasPermission("openrp.radio.monitor")) {
               recipients.add(player);
            }
         }
      }

      return recipients;
   }

   private NamedTextColor severityColor(int level) {
      return switch (level) {
         case 1 -> NamedTextColor.GREEN;
         case 2 -> NamedTextColor.YELLOW;
         case 3 -> NamedTextColor.GOLD;
         case 4 -> NamedTextColor.RED;
         default -> NamedTextColor.DARK_RED;
      };
   }

   private void fill(Inventory inventory) {
      ItemStack filler = NexoUI.getFiller();

      for (int i = 0; i < inventory.getSize(); i++) {
         inventory.setItem(i, filler);
      }
   }

   private static class MainHolder implements InventoryHolder {
      private Inventory inventory;

      public Inventory getInventory() {
         return this.inventory;
      }
   }

   private record SupportCall(String id, UUID callerUuid, String callerName, Location location, int level, Instant createdAt) {
   }

   private static class SupportHolder implements InventoryHolder {
      private Inventory inventory;

      public Inventory getInventory() {
         return this.inventory;
      }
   }
}
