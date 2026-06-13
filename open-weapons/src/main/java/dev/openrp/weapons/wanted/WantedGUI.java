package dev.openrp.weapons.wanted;

import it.meridian.core.gui.NexoUI;
import dev.openrp.weapons.module.WeaponsModule;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.wesjd.anvilgui.AnvilGUI.Builder;
import net.wesjd.anvilgui.AnvilGUI.ResponseAction;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

public class WantedGUI implements Listener {
   private static final int SLOT_TARGET = 10;
   private static final int SLOT_REASON = 12;
   private static final int SLOT_ARREST_REQUIRED = 14;
   private static final int SLOT_CONFIRM = 21;
   private static final int SLOT_CANCEL = 23;
   private final WeaponsModule module;

   public WantedGUI(WeaponsModule module) {
      this.module = module;
   }

   public void open(Player officer) {
      this.openMainGUI(officer, new WantedGUI.WantedSession());
   }

   private void openMainGUI(Player officer, WantedGUI.WantedSession session) {
      WantedGUI.WantedGUIHolder holder = new WantedGUI.WantedGUIHolder(session);
      Inventory gui = Bukkit.createInventory(holder, 27, NexoUI.getGlyphTitle("wanted_gui", "Registro ricercati"));
      holder.setInventory(gui);
      ItemStack filler = NexoUI.getFiller();

      for (int i = 0; i < gui.getSize(); i++) {
         gui.setItem(i, filler);
      }

      gui.setItem(10, this.createTargetItem(session));
      gui.setItem(
         12,
         NexoUI.getArrestReasonButton(
            Component.text("Motivo", NamedTextColor.AQUA).decoration(TextDecoration.ITALIC, false),
            List.of(
               Component.text("Clicca per impostare il motivo della ricerca", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
               Component.empty(),
               Component.text("Attuale: " + (session.reason == null ? "Non impostato" : session.reason), NamedTextColor.WHITE)
                  .decoration(TextDecoration.ITALIC, false)
            )
         )
      );
      gui.setItem(14, this.createArrestRequiredItem(session.arrestRequired));
      gui.setItem(
         21,
         NexoUI.getConfirmButton(
            Component.text("Conferma scheda ricercato", NamedTextColor.GREEN, new TextDecoration[]{TextDecoration.BOLD}).decoration(TextDecoration.ITALIC, false)
         )
      );
      gui.setItem(
         23,
         NexoUI.getCancelButton(
            Component.text("Annulla", NamedTextColor.RED, new TextDecoration[]{TextDecoration.BOLD}).decoration(TextDecoration.ITALIC, false)
         )
      );
      officer.openInventory(gui);
   }

   @EventHandler
   public void onClick(InventoryClickEvent event) {
      if (event.getWhoClicked() instanceof Player officer) {
         Inventory topInv = event.getView().getTopInventory();
         if (topInv.getHolder() instanceof WantedGUI.WantedGUIHolder holder) {
            event.setCancelled(true);
            if (event.getClickedInventory() == topInv) {
               WantedGUI.WantedSession session = holder.getSession();
               switch (event.getRawSlot()) {
                  case 10:
                     officer.closeInventory();
                     Bukkit.getScheduler().runTaskLater(this.module.getCore(), () -> this.openTargetAnvil(officer, session), 2L);
                  case 11:
                  case 13:
                  case 15:
                  case 16:
                  case 17:
                  case 18:
                  case 19:
                  case 20:
                  case 22:
                  default:
                     break;
                  case 12:
                     officer.closeInventory();
                     Bukkit.getScheduler().runTaskLater(this.module.getCore(), () -> this.openReasonAnvil(officer, session), 2L);
                     break;
                  case 14:
                     session.arrestRequired = !session.arrestRequired;
                     this.openMainGUI(officer, session);
                     break;
                  case 21:
                     this.confirm(officer, session);
                     break;
                  case 23:
                     officer.closeInventory();
                     officer.sendMessage(Component.text("Scheda ricercato annullata.", NamedTextColor.RED));
               }
            }
         }
      }
   }

   private void openTargetAnvil(Player officer, WantedGUI.WantedSession session) {
      new Builder()
         .onClick(
            (slot, stateSnapshot) -> {
               if (slot != 2) {
                  return Collections.emptyList();
               }

               String text = stateSnapshot.getText() == null ? "" : stateSnapshot.getText().trim();
               if (!text.isBlank() && !text.equalsIgnoreCase("Nome giocatore")) {
                  OfflinePlayer target = Bukkit.getOfflinePlayer(text);
                  if (target != null && (target.isOnline() || target.hasPlayedBefore())) {
                     if (target.getUniqueId().equals(officer.getUniqueId())) {
                        return Collections.singletonList(ResponseAction.replaceInputText("Non te stesso"));
                     }

                     session.targetUuid = target.getUniqueId();
                     session.targetName = target.getName() != null ? target.getName() : text;
                     return Collections.singletonList(
                        ResponseAction.run(() -> Bukkit.getScheduler().runTask(this.module.getCore(), () -> this.openMainGUI(officer, session)))
                     );
                  } else {
                     return Collections.singletonList(ResponseAction.replaceInputText("Giocatore non trovato"));
                  }
               } else {
                  return Collections.singletonList(ResponseAction.replaceInputText("Giocatore richiesto"));
               }
            }
         )
         .text(session.targetName == null ? "Nome giocatore" : session.targetName)
         .title("Giocatore ricercato")
         .plugin(this.module.getCore())
         .open(officer);
   }

   private void openReasonAnvil(Player officer, WantedGUI.WantedSession session) {
      new Builder()
         .onClick(
            (slot, stateSnapshot) -> {
               if (slot != 2) {
                  return Collections.emptyList();
               } else {
                  String text = stateSnapshot.getText() == null ? "" : stateSnapshot.getText().trim();
                  if (!text.isBlank() && !text.equalsIgnoreCase("Motivo...")) {
                     session.reason = text;
                     return Collections.singletonList(
                        ResponseAction.run(() -> Bukkit.getScheduler().runTask(this.module.getCore(), () -> this.openMainGUI(officer, session)))
                     );
                  } else {
                     return Collections.singletonList(ResponseAction.replaceInputText("Motivo richiesto"));
                  }
               }
            }
         )
         .text(session.reason == null ? "Motivo..." : session.reason)
         .title("Motivo ricerca")
         .plugin(this.module.getCore())
         .open(officer);
   }

   private void confirm(Player officer, WantedGUI.WantedSession session) {
      if (session.targetUuid == null || session.targetName == null) {
         officer.sendMessage(Component.text("Devi impostare un giocatore ricercato.", NamedTextColor.RED));
      } else if (session.reason != null && !session.reason.isBlank()) {
         if (this.module.getWantedManager().isWanted(session.targetUuid)) {
            officer.sendMessage(Component.text("Quel giocatore e' gia' ricercato.", NamedTextColor.RED));
         } else {
            WantedRecord record = new WantedRecord(
               session.targetUuid, session.targetName, session.reason, session.arrestRequired, officer.getUniqueId(), officer.getName(), Instant.now()
            );
            if (!this.module.getWantedManager().addRecord(record)) {
               officer.sendMessage(Component.text("Quel giocatore e' gia' ricercato.", NamedTextColor.RED));
            } else {
               officer.closeInventory();
               officer.sendMessage(Component.text("Scheda ricercato aggiunta per " + session.targetName + ".", NamedTextColor.GREEN));
            }
         }
      } else {
         officer.sendMessage(Component.text("Devi impostare un motivo.", NamedTextColor.RED));
      }
   }

   private ItemStack createTargetItem(WantedGUI.WantedSession session) {
      ItemStack item = new ItemStack(Material.PLAYER_HEAD);
      SkullMeta meta = (SkullMeta)item.getItemMeta();
      if (meta != null) {
         if (session.targetUuid != null) {
            meta.setOwningPlayer(Bukkit.getOfflinePlayer(session.targetUuid));
         }

         meta.displayName(Component.text("Giocatore ricercato", NamedTextColor.GOLD).decoration(TextDecoration.ITALIC, false));
         List<Component> lore = new ArrayList<>();
         lore.add(Component.text("Clicca per impostare il giocatore ricercato", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
         lore.add(Component.empty());
         lore.add(
            Component.text("Attuale: " + (session.targetName == null ? "Non impostato" : session.targetName), NamedTextColor.WHITE)
               .decoration(TextDecoration.ITALIC, false)
         );
         meta.lore(lore);
         item.setItemMeta(meta);
      }

      return item;
   }

   private ItemStack createArrestRequiredItem(boolean arrestRequired) {
      ItemStack item = new ItemStack(arrestRequired ? Material.REDSTONE_TORCH : Material.LEVER);
      ItemMeta meta = item.getItemMeta();
      if (meta != null) {
         meta.displayName(
            Component.text("Arresto richiesto", arrestRequired ? NamedTextColor.RED : NamedTextColor.GREEN).decoration(TextDecoration.ITALIC, false)
         );
         meta.lore(
            List.of(
               (TextComponent)Component.text("Clicca per cambiare", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
               Component.empty(),
               (TextComponent)Component.text("Attuale: " + (arrestRequired ? "Si" : "No"), NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false)
            )
         );
         if (arrestRequired) {
            meta.setEnchantmentGlintOverride(true);
         }

         item.setItemMeta(meta);
      }

      return item;
   }

   public static class WantedGUIHolder implements InventoryHolder {
      private final WantedGUI.WantedSession session;
      private Inventory inventory;

      public WantedGUIHolder(WantedGUI.WantedSession session) {
         this.session = session;
      }

      public Inventory getInventory() {
         return this.inventory;
      }

      public void setInventory(Inventory inventory) {
         this.inventory = inventory;
      }

      public WantedGUI.WantedSession getSession() {
         return this.session;
      }
   }

   public static class WantedSession {
      UUID targetUuid;
      String targetName;
      String reason;
      boolean arrestRequired;
   }
}
