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
      Inventory inventory = Bukkit.createInventory(holder, 27, Component.text("Quick Actions: " + target.getName(), NamedTextColor.DARK_GRAY));
      holder.setInventory(inventory);
      inventory.setItem(10, this.createButton(Material.SPYGLASS, "Frisk", NamedTextColor.YELLOW, List.of("Run /frisk on this player.")));
      inventory.setItem(
         12, this.createButton(Material.IRON_BARS, "Arrest", NamedTextColor.RED, List.of("Available to law enforcement", "when the target is handcuffed."))
      );
      inventory.setItem(
         14, this.createButton(Material.SHEARS, "Remove Balaclava", NamedTextColor.AQUA, List.of("Law enforcement only.", "Target must be handcuffed."))
      );
      inventory.setItem(16, this.createButton(Material.EMERALD, "Trade", NamedTextColor.GREEN, List.of("Run /trade on this player.")));
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
                     actor.sendMessage(Component.text("You are too far away from that player.", NamedTextColor.RED));
                  }
               } else {
                  actor.closeInventory();
                  actor.sendMessage(Component.text("That player is no longer online.", NamedTextColor.RED));
               }
            }
         }
      }
   }

   private void removeBalaclava(Player actor, Player target) {
      if (!this.module.isLEO(actor.getUniqueId()) && !actor.hasPermission("openrp.quickactions.balaclava")) {
         actor.sendMessage(Component.text("Only law enforcement can remove a balaclava.", NamedTextColor.RED));
      } else if (!this.module.getHandcuffManager().isHandcuffed(target)) {
         actor.sendMessage(Component.text("The target must be handcuffed.", NamedTextColor.RED));
      } else {
         ItemStack helmet = target.getInventory().getHelmet();
         if (!this.module.getBalaclavaManager().isBalaclava(helmet)) {
            actor.sendMessage(Component.text("That player is not wearing a balaclava.", NamedTextColor.RED));
         } else {
            target.getInventory().setHelmet(null);
            this.module.getBalaclavaManager().setMasked(target.getUniqueId(), false);
            this.refreshNameTag(target);
            Map<Integer, ItemStack> leftovers = actor.getInventory().addItem(new ItemStack[]{helmet});
            leftovers.values().forEach(item -> actor.getWorld().dropItemNaturally(actor.getLocation(), item));
            actor.playSound(actor.getLocation(), Sound.ITEM_ARMOR_EQUIP_LEATHER, 0.8F, 1.2F);
            target.playSound(target.getLocation(), Sound.ITEM_ARMOR_EQUIP_LEATHER, 0.8F, 0.8F);
            actor.sendMessage(Component.text("You removed " + target.getName() + "'s balaclava.", NamedTextColor.GREEN));
            target.sendMessage(Component.text("An officer removed your balaclava.", NamedTextColor.YELLOW));
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
