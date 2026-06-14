package dev.openrp.weapons.actions;

import dev.openrp.weapons.module.WeaponsModule;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class QuickActionListener implements Listener {
   private static final double MAX_RANGE = 4.0;
   private static final int SLOT_FRISK = 11;
   private static final int SLOT_TRADE = 15;
   private final WeaponsModule module;

   public QuickActionListener(WeaponsModule module) {
      this.module = module;
   }

   @EventHandler
   public void onPlayerShiftRightClick(PlayerInteractEntityEvent event) {
      if (event.getHand() == EquipmentSlot.HAND) {
         if (event.getRightClicked() instanceof Player target) {
            Player actor = event.getPlayer();
            if (actor.isSneaking() && !this.module.getHandcuffManager().isRestrained(actor)) {
               if (this.isEmpty(actor.getInventory().getItemInMainHand())) {
                  event.setCancelled(true);
                  this.open(actor, target);
               }
            }
         }
      }
   }

   private void open(Player actor, Player target) {
      QuickActionHolder holder = new QuickActionHolder(actor.getUniqueId(), target.getUniqueId());
      Inventory inventory = Bukkit.createInventory(holder, 27, Component.text("Azioni rapide: " + target.getName(), NamedTextColor.DARK_GRAY));
      holder.setInventory(inventory);
      inventory.setItem(SLOT_FRISK, this.createButton(Material.SPYGLASS, "Perquisisci", NamedTextColor.YELLOW, List.of("Esegue /perquisisci su questo giocatore.")));
      inventory.setItem(SLOT_TRADE, this.createButton(Material.EMERALD, "Scambia", NamedTextColor.GREEN, List.of("Esegue /trade su questo giocatore.")));
      actor.openInventory(inventory);
   }

   @EventHandler
   public void onInventoryClick(InventoryClickEvent event) {
      Inventory top = event.getView().getTopInventory();
      if (top.getHolder() instanceof QuickActionHolder holder) {
         event.setCancelled(true);
         if (event.getWhoClicked() instanceof Player actor) {
            if (!actor.getUniqueId().equals(holder.getActorUuid())) {
               actor.closeInventory();
            } else {
               Player target = Bukkit.getPlayer(holder.getTargetUuid());
               if (target != null && target.isOnline()) {
                  if (actor.getWorld().equals(target.getWorld()) && !(actor.getLocation().distance(target.getLocation()) > 4.0)) {
                     int slot = event.getRawSlot();
                     actor.closeInventory();
                     if (slot == SLOT_FRISK) {
                        Bukkit.dispatchCommand(actor, "perquisisci " + target.getName());
                     } else if (slot == SLOT_TRADE) {
                        Bukkit.dispatchCommand(actor, "trade " + target.getName());
                     }
                  } else {
                     actor.closeInventory();
                     actor.sendMessage(Component.text("Sei troppo lontano da quel giocatore.", NamedTextColor.RED));
                  }
               } else {
                  actor.closeInventory();
                  actor.sendMessage(Component.text("Quel giocatore non e' piu' online.", NamedTextColor.RED));
               }
            }
         }
      }
   }

   private ItemStack createButton(Material material, String name, NamedTextColor color, List<String> loreLines) {
      ItemStack item = new ItemStack(material);
      ItemMeta meta = item.getItemMeta();
      if (meta != null) {
         meta.displayName(Component.text(name, color, new TextDecoration[]{TextDecoration.BOLD}).decoration(TextDecoration.ITALIC, false));
         meta.lore(loreLines.stream().map(line -> (TextComponent)Component.text(line, NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)).toList());
         item.setItemMeta(meta);
      }

      return item;
   }

   private boolean isEmpty(ItemStack item) {
      return item == null || item.getType().isAir() || item.getAmount() <= 0;
   }
}
