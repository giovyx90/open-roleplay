package dev.openrp.weapons.phone;

import io.papermc.paper.event.player.AsyncChatEvent;
import it.meridian.core.web.WebRecordPublisher;
import dev.openrp.weapons.module.WeaponsModule;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
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
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.scheduler.BukkitTask;

public class SosManager implements Listener {
   private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss").withZone(ZoneId.of("Europe/Rome"));
   private final WeaponsModule module;
   private final Map<String, SosCall> activeCalls = new ConcurrentHashMap<>();
   private final Map<UUID, SosManager.PendingReason> pendingReasons = new ConcurrentHashMap<>();
   private final Map<UUID, SosManager.PendingResponse> pendingResponses = new ConcurrentHashMap<>();

   public SosManager(WeaponsModule module) {
      this.module = module;
   }

   public void startReasonInput(Player caller, SosCall.Service service) {
      this.clearPending(caller.getUniqueId());
      SosManager.PendingReason pending = new SosManager.PendingReason(service);
      pending.task = Bukkit.getScheduler().runTaskLater(this.module.getCore(), () -> {
         SosManager.PendingReason removed = this.pendingReasons.remove(caller.getUniqueId());
         if (removed == pending && caller.isOnline()) {
            caller.sendMessage(Component.text("SOS request timed out.", NamedTextColor.RED));
         }
      }, 600L);
      this.pendingReasons.put(caller.getUniqueId(), pending);
      caller.closeInventory();
      caller.sendMessage(
         Component.text("Type the reason for your " + service.getDisplayName() + " SOS call within 30 seconds. Type cancel to abort.", NamedTextColor.YELLOW)
      );
   }

   public void startResponseInput(Player responder, String callId) {
      SosCall call = this.activeCalls.get(callId);
      if (call == null) {
         responder.sendMessage(Component.text("That SOS call is no longer active.", NamedTextColor.RED));
      } else if (!this.canUseCall(responder, call)) {
         responder.sendMessage(Component.text("You cannot respond to this SOS call.", NamedTextColor.RED));
      } else {
         this.clearPending(responder.getUniqueId());
         SosManager.PendingResponse pending = new SosManager.PendingResponse(callId);
         pending.task = Bukkit.getScheduler().runTaskLater(this.module.getCore(), () -> {
            SosManager.PendingResponse removed = this.pendingResponses.remove(responder.getUniqueId());
            if (removed == pending && responder.isOnline()) {
               responder.sendMessage(Component.text("SOS response timed out.", NamedTextColor.RED));
            }
         }, 600L);
         this.pendingResponses.put(responder.getUniqueId(), pending);
         responder.sendMessage(Component.text("Type your response within 30 seconds. Type cancel to abort.", NamedTextColor.YELLOW));
      }
   }

   public void activateGps(Player responder, String callId) {
      SosCall call = this.activeCalls.get(callId);
      if (call == null) {
         responder.sendMessage(Component.text("That SOS call is no longer active.", NamedTextColor.RED));
      } else if (!this.canUseCall(responder, call)) {
         responder.sendMessage(Component.text("You cannot use GPS for this SOS call.", NamedTextColor.RED));
      } else {
         this.module.getDispatchGpsManager().activate(responder, "SOS", () -> {
            Player caller = Bukkit.getPlayer(call.getCallerUuid());
            return caller != null && caller.isOnline() ? caller.getLocation().clone() : call.getLocation().clone();
         });
      }
   }

   public void stopGps(Player player, boolean notify) {
      this.module.getDispatchGpsManager().stop(player, notify);
   }

   public void cleanup() {
      for (SosManager.PendingReason pending : this.pendingReasons.values()) {
         if (pending.task != null) {
            pending.task.cancel();
         }
      }

      for (SosManager.PendingResponse pending : this.pendingResponses.values()) {
         if (pending.task != null) {
            pending.task.cancel();
         }
      }

      this.pendingReasons.clear();
      this.pendingResponses.clear();
      this.activeCalls.clear();
   }

   @EventHandler(priority = EventPriority.LOWEST)
   public void onAsyncChat(AsyncChatEvent event) {
      Player player = event.getPlayer();
      UUID uuid = player.getUniqueId();
      SosManager.PendingReason reason = this.pendingReasons.remove(uuid);
      SosManager.PendingResponse response = reason == null ? this.pendingResponses.remove(uuid) : null;
      if (reason != null || response != null) {
         event.setCancelled(true);
         String message = PlainTextComponentSerializer.plainText().serialize(event.message()).trim();
         if (message.equalsIgnoreCase("cancel")) {
            this.cancelPending(reason, response);
            Bukkit.getScheduler().runTask(this.module.getCore(), () -> player.sendMessage(Component.text("SOS input cancelled.", NamedTextColor.RED)));
         } else {
            Bukkit.getScheduler().runTask(this.module.getCore(), () -> {
               if (reason != null) {
                  this.cancelTask(reason.task);
                  this.submitCall(player, reason.service, message);
               } else {
                  this.cancelTask(response.task);
                  this.submitResponse(player, response.callId, message);
               }
            });
         }
      }
   }

   private void submitCall(Player caller, SosCall.Service service, String reason) {
      if (caller.isOnline()) {
         List<Player> recipients = this.module.getOnlineCompanyEmployees(service.getCompanyType());
         if (recipients.isEmpty()) {
            caller.sendMessage(Component.text("No " + service.getDisplayName() + " units are currently available.", NamedTextColor.RED));
         } else {
            String id = this.createCallId();
            SosCall call = new SosCall(id, service, caller.getUniqueId(), caller.getName(), caller.getLocation().clone(), reason, Instant.now());
            this.activeCalls.put(id, call);
            this.publish(call);
            Component alert = this.buildDispatchAlert(call);

            for (Player recipient : recipients) {
               recipient.sendMessage(alert);
               recipient.playSound(recipient.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 0.9F, 1.3F);
            }

            caller.sendMessage(
               Component.text("Your SOS call has been sent to " + recipients.size() + " " + service.getDisplayName() + " unit(s).", NamedTextColor.GREEN)
            );
         }
      }
   }

   private void submitResponse(Player responder, String callId, String message) {
      SosCall call = this.activeCalls.get(callId);
      if (call == null) {
         responder.sendMessage(Component.text("That SOS call is no longer active.", NamedTextColor.RED));
      } else if (!this.canUseCall(responder, call)) {
         responder.sendMessage(Component.text("You cannot respond to this SOS call.", NamedTextColor.RED));
      } else {
         Player caller = Bukkit.getPlayer(call.getCallerUuid());
         if (caller != null && caller.isOnline()) {
            caller.sendMessage(
               ((Builder)((Builder)((Builder)((Builder)Component.text().append(Component.text("SOS response from ", NamedTextColor.GREEN)))
                           .append(Component.text(responder.getName(), NamedTextColor.WHITE)))
                        .append(Component.text(" (" + call.getService().getDisplayName() + "): ", NamedTextColor.GRAY)))
                     .append(Component.text(message, NamedTextColor.WHITE)))
                  .build()
            );
            responder.sendMessage(Component.text("Response sent to " + call.getCallerName() + ".", NamedTextColor.GREEN));
         } else {
            responder.sendMessage(Component.text("The caller is no longer online.", NamedTextColor.RED));
         }
      }
   }

   private Component buildDispatchAlert(SosCall call) {
      Location loc = call.getLocation();
      String coords = String.format("%.0f %.0f %.0f", loc.getX(), loc.getY(), loc.getZ());
      Component gps = ((TextComponent)Component.text("[GPS]", NamedTextColor.GREEN, new TextDecoration[]{TextDecoration.BOLD})
            .clickEvent(ClickEvent.runCommand("/sos gps " + call.getId())))
         .hoverEvent(HoverEvent.showText(Component.text("Activate GPS", NamedTextColor.GREEN)));
      Component respond = ((TextComponent)Component.text("[Respond]", NamedTextColor.AQUA, new TextDecoration[]{TextDecoration.BOLD})
            .clickEvent(ClickEvent.runCommand("/sos respond " + call.getId())))
         .hoverEvent(HoverEvent.showText(Component.text("Reply to caller", NamedTextColor.AQUA)));
      return ((Builder)((Builder)((Builder)((Builder)((Builder)((Builder)((Builder)((Builder)((Builder)((Builder)((Builder)((Builder)((Builder)((Builder)((Builder)((Builder)((Builder)Component.text()
                                                            .append(
                                                               Component.text(
                                                                  "Emergency Dispatch - " + call.getService().getDisplayName(),
                                                                  NamedTextColor.RED,
                                                                  new TextDecoration[]{TextDecoration.BOLD}
                                                               )
                                                            ))
                                                         .append(Component.newline()))
                                                      .append(Component.text("Coordinates: ", NamedTextColor.GRAY)))
                                                   .append(Component.text(coords, NamedTextColor.WHITE)))
                                                .append(Component.newline()))
                                             .append(Component.text("Caller: ", NamedTextColor.GRAY)))
                                          .append(Component.text(call.getCallerName(), NamedTextColor.WHITE)))
                                       .append(Component.newline()))
                                    .append(Component.text("Reason: ", NamedTextColor.GRAY)))
                                 .append(Component.text(call.getReason(), NamedTextColor.WHITE)))
                              .append(Component.newline()))
                           .append(Component.text("Time: ", NamedTextColor.GRAY)))
                        .append(Component.text(DATE_FORMAT.format(call.getCreatedAt()), NamedTextColor.YELLOW)))
                     .append(Component.newline()))
                  .append(gps))
               .append(Component.text(" ")))
            .append(respond))
         .build();
   }

   private boolean canUseCall(Player player, SosCall call) {
      return player.hasPermission("openrp.sos.dispatch") || this.module.isCompanyEmployeeOfType(player.getUniqueId(), call.getService().getCompanyType());
   }

   private String createCallId() {
      String id;
      do {
         id = UUID.randomUUID().toString().substring(0, 8);
      } while (this.activeCalls.containsKey(id));

      return id;
   }

   private void publish(SosCall call) {
      Location loc = call.getLocation();
      String json = "{"
         + WebRecordPublisher.jsonPair("service", call.getService().getDisplayName())
         + ","
         + WebRecordPublisher.jsonPair("callerName", call.getCallerName())
         + ","
         + WebRecordPublisher.jsonPair("reason", call.getReason())
         + ","
         + WebRecordPublisher.jsonPair("world", loc.getWorld() != null ? loc.getWorld().getName() : "")
         + ","
         + WebRecordPublisher.jsonPair("x", loc.getX())
         + ","
         + WebRecordPublisher.jsonPair("y", loc.getY())
         + ","
         + WebRecordPublisher.jsonPair("z", loc.getZ())
         + ","
         + WebRecordPublisher.jsonPair("createdAt", call.getCreatedAt().toEpochMilli())
         + "}";
      WebRecordPublisher.upsert(this.module.getCore(), "sos", call.getId(), call.getCallerUuid().toString(), json);
   }

   private void clearPending(UUID uuid) {
      SosManager.PendingReason reason = this.pendingReasons.remove(uuid);
      SosManager.PendingResponse response = this.pendingResponses.remove(uuid);
      this.cancelPending(reason, response);
   }

   private void cancelPending(SosManager.PendingReason reason, SosManager.PendingResponse response) {
      if (reason != null) {
         this.cancelTask(reason.task);
      }

      if (response != null) {
         this.cancelTask(response.task);
      }
   }

   private void cancelTask(BukkitTask task) {
      if (task != null) {
         task.cancel();
      }
   }

   private static class PendingReason {
      private final SosCall.Service service;
      private BukkitTask task;

      private PendingReason(SosCall.Service service) {
         this.service = service;
      }
   }

   private static class PendingResponse {
      private final String callId;
      private BukkitTask task;

      private PendingResponse(String callId) {
         this.callId = callId;
      }
   }
}
