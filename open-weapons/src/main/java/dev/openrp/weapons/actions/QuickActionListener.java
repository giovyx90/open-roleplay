package dev.openrp.weapons.actions;

import it.meridian.cityhall.module.CityHallModule;
import dev.openrp.weapons.module.WeaponsModule;
import java.util.List;
import java.util.Map;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
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
   private static final int SLOT_FRISK = 10;
   private static final int SLOT_ARREST = 12;
   private static final int SLOT_UNMASK = 14;
   private static final int SLOT_TRADE = 16;
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
      inventory.setItem(10, this.createButton(Material.SPYGLASS, "Perquisisci", NamedTextColor.YELLOW, List.of("Esegue /frisk su questo giocatore.")));
      inventory.setItem(
         12, this.createButton(Material.IRON_BARS, "Arresto", NamedTextColor.RED, List.of("Disponibile alle forze dell'ordine", "quando il bersaglio e' ammanettato."))
      );
      inventory.setItem(
         14, this.createButton(Material.SHEARS, "Rimuovi passamontagna", NamedTextColor.AQUA, List.of("Solo forze dell'ordine.", "Il bersaglio deve essere ammanettato."))
      );
      inventory.setItem(16, this.createButton(Material.EMERALD, "Scambia", NamedTextColor.GREEN, List.of("Esegue /trade su questo giocatore.")));
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
                     if (slot == 10) {
                        Bukkit.dispatchCommand(actor, "frisk " + target.getName());
                     } else if (slot == 12) {
                        Bukkit.dispatchCommand(actor, "arrest " + target.getName());
                     } else if (slot == 14) {
                        this.removeBalaclava(actor, target);
                     } else if (slot == 16) {
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

   private void removeBalaclava(Player actor, Player target) {
      if (!this.module.isLEO(actor.getUniqueId()) && !actor.hasPermission("openrp.quickactions.balaclava")) {
         actor.sendMessage(Component.text("Solo le forze dell'ordine possono rimuovere un passamontagna.", NamedTextColor.RED));
      } else if (!this.module.getHandcuffManager().isHandcuffed(target)) {
         actor.sendMessage(Component.text("Il bersaglio deve essere ammanettato.", NamedTextColor.RED));
      } else {
         ItemStack helmet = target.getInventory().getHelmet();
         if (!this.module.getBalaclavaManager().isBalaclava(helmet)) {
            actor.sendMessage(Component.text("Quel giocatore non indossa un passamontagna.", NamedTextColor.RED));
         } else {
            target.getInventory().setHelmet(null);
            this.module.getBalaclavaManager().setMasked(target.getUniqueId(), false);
            this.refreshNameTag(target);
            Map<Integer, ItemStack> leftovers = actor.getInventory().addItem(new ItemStack[]{helmet});
            leftovers.values().forEach(item -> actor.getWorld().dropItemNaturally(actor.getLocation(), item));
            actor.playSound(actor.getLocation(), Sound.ITEM_ARMOR_EQUIP_LEATHER, 0.8F, 1.2F);
            target.playSound(target.getLocation(), Sound.ITEM_ARMOR_EQUIP_LEATHER, 0.8F, 0.8F);
            actor.sendMessage(Component.text("Hai rimosso il passamontagna di " + target.getName() + ".", NamedTextColor.GREEN));
            target.sendMessage(Component.text("Un agente ti ha rimosso il passamontagna.", NamedTextColor.YELLOW));
         }
      }
   }

   private void refreshNameTag(Player target) {
      CityHallModule cityHall = (CityHallModule)this.module.getCore().getModuleManager().getModule(CityHallModule.class);
      if (cityHall != null && cityHall.getNameTagHandler() != null) {
         cityHall.getNameTagHandler().refreshPlayer(target);
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
