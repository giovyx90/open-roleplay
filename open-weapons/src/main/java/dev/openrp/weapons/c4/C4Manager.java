package dev.openrp.weapons.c4;

import dev.openrp.weapons.util.OpenGuiItems;
import dev.openrp.weapons.util.ItemBuilder;
import dev.openrp.weapons.grenades.GrenadeDefinition;
import dev.openrp.weapons.module.WeaponsModule;
import dev.openrp.weapons.utility.UtilityItemType;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

public class C4Manager implements Listener {
   public static final String C4_GRENADE_ID = "c4_charge";
   private static final int[] TIMER_SLOTS = new int[]{10, 12, 14, 16};
   private static final int[] TIMER_SECONDS = new int[]{30, 60, 120, 300};
   private static final int[] WIRE_SLOTS = new int[]{10, 11, 12, 13, 14};
   private static final C4Manager.Wire[] WIRES = new C4Manager.Wire[]{
      new C4Manager.Wire("Rosso", Material.RED_WOOL, NamedTextColor.RED, "Taglia il filo collegato alle luci di emergenza."),
      new C4Manager.Wire("Blu", Material.BLUE_WOOL, NamedTextColor.BLUE, "Taglia il filo collegato al segnale stabile."),
      new C4Manager.Wire("Verde", Material.GREEN_WOOL, NamedTextColor.GREEN, "Taglia il filo collegato al percorso libero."),
      new C4Manager.Wire("Giallo", Material.YELLOW_WOOL, NamedTextColor.YELLOW, "Taglia il filo collegato al nastro di sicurezza."),
      new C4Manager.Wire("Bianco", Material.WHITE_WOOL, NamedTextColor.WHITE, "Taglia il filo collegato all'etichetta vuota.")
   };
   private final WeaponsModule module;
   private final NamespacedKey chargeKey;
   private final Map<String, C4Charge> activeCharges = new ConcurrentHashMap<>();
   private final Random random = new Random();

   public C4Manager(WeaponsModule module) {
      this.module = module;
      this.chargeKey = new NamespacedKey(module.getCore(), "c4_charge_id");
   }

   public boolean isC4(GrenadeDefinition definition) {
      return definition != null && "c4_charge".equalsIgnoreCase(definition.getId());
   }

   public void openTimerGui(Player player, GrenadeDefinition definition, EquipmentSlot hand, Block clickedBlock, BlockFace blockFace) {
      if (this.isC4(definition)) {
         if (!player.hasPermission("openrp.c4.use")) {
            player.sendMessage(Component.text("Non sei autorizzato ad armare il C4.", NamedTextColor.RED));
         } else if (clickedBlock == null) {
            player.sendMessage(Component.text("Posiziona il C4 su una superficie.", NamedTextColor.RED));
         } else {
            Location placement = this.resolvePlacement(clickedBlock, blockFace);
            C4Manager.TimerHolder holder = new C4Manager.TimerHolder(definition, hand, placement);
            Inventory inventory = Bukkit.createInventory(holder, 27, OpenGuiItems.getGlyphTitle("c4_timer_gui", "C4 Timer"));
            holder.inventory = inventory;
            this.fill(inventory);

            for (int i = 0; i < TIMER_SLOTS.length; i++) {
               inventory.setItem(TIMER_SLOTS[i], this.timerItem(TIMER_SECONDS[i]));
            }

            inventory.setItem(
               22,
               OpenGuiItems.getCancelButton(
                  Component.text("Annulla", NamedTextColor.RED, new TextDecoration[]{TextDecoration.BOLD}).decoration(TextDecoration.ITALIC, false)
               )
            );
            player.openInventory(inventory);
         }
      }
   }

   @EventHandler
   public void onTimerClick(InventoryClickEvent event) {
      if (event.getInventory().getHolder() instanceof C4Manager.TimerHolder holder) {
         event.setCancelled(true);
         if (event.getWhoClicked() instanceof Player player) {
            if (event.getClickedInventory() != null && event.getClickedInventory().equals(event.getInventory())) {
               int rawSlot = event.getRawSlot();
               if (rawSlot == 22) {
                  player.closeInventory();
               } else {
                  for (int i = 0; i < TIMER_SLOTS.length; i++) {
                     if (rawSlot == TIMER_SLOTS[i]) {
                        this.placeCharge(player, holder, TIMER_SECONDS[i]);
                        return;
                     }
                  }
               }
            }
         }
      }
   }

   @EventHandler
   public void onChargeInteract(PlayerInteractEntityEvent event) {
      if (event.getRightClicked() instanceof ItemDisplay display) {
         String chargeId = (String)display.getPersistentDataContainer().get(this.chargeKey, PersistentDataType.STRING);
         if (chargeId != null) {
            if (!this.module.getUtilityItemManager().isType(event.getPlayer().getInventory().getItemInMainHand(), UtilityItemType.FINGERPRINT_SHEET)) {
               event.setCancelled(true);
               C4Charge charge = this.activeCharges.get(chargeId);
               if (charge != null && !charge.isDetonating()) {
                  if (!event.getPlayer().hasPermission("openrp.c4.defuse")) {
                     event.getPlayer().sendMessage(Component.text("Non sei addestrato a disinnescare il C4.", NamedTextColor.RED));
                  } else {
                     this.openDefuseGui(event.getPlayer(), charge);
                  }
               } else {
                  event.getPlayer().sendMessage(Component.text("Questa carica non puo' essere disinnescata ora.", NamedTextColor.RED));
               }
            }
         }
      }
   }

   @EventHandler
   public void onDefuseClick(InventoryClickEvent event) {
      if (event.getInventory().getHolder() instanceof C4Manager.DefuseHolder holder) {
         event.setCancelled(true);
         if (event.getWhoClicked() instanceof Player player) {
            if (event.getClickedInventory() != null && event.getClickedInventory().equals(event.getInventory())) {
               for (int i = 0; i < WIRE_SLOTS.length; i++) {
                  if (event.getRawSlot() == WIRE_SLOTS[i]) {
                     C4Charge charge = this.activeCharges.get(holder.chargeId);
                     if (charge != null && !charge.isDetonating()) {
                        if (i == holder.correctWireIndex) {
                           this.disarm(player, charge);
                        } else {
                           this.forceDetonateSoon(player, charge);
                        }

                        player.closeInventory();
                        return;
                     }

                     player.closeInventory();
                     player.sendMessage(Component.text("Il C4 non e' piu' attivo.", NamedTextColor.RED));
                     return;
                  }
               }
            }
         }
      }
   }

   public void cleanup() {
      for (C4Charge charge : this.activeCharges.values()) {
         if (charge.getDetonationTask() != null) {
            charge.getDetonationTask().cancel();
         }

         if (charge.getDisplay() != null && charge.getDisplay().isValid()) {
            charge.getDisplay().remove();
         }
      }

      this.activeCharges.clear();
   }

   public List<C4Charge> getVisibleCharges(Player player) {
      boolean admin = player.hasPermission("openrp.c4.remote.admin");
      List<C4Charge> charges = new ArrayList<>();

      for (C4Charge charge : this.activeCharges.values()) {
         if (admin || charge.getOwnerUuid().equals(player.getUniqueId())) {
            charges.add(charge);
         }
      }

      charges.sort(Comparator.comparing(C4Charge::getCreatedAt));
      return charges;
   }

   public C4Charge getCharge(String chargeId) {
      return this.activeCharges.get(chargeId);
   }

   public boolean canRemoteManage(Player player, C4Charge charge) {
      return charge != null && (charge.getOwnerUuid().equals(player.getUniqueId()) || player.hasPermission("openrp.c4.remote.admin"));
   }

   public boolean remoteDetonate(Player player, String chargeId) {
      C4Charge charge = this.activeCharges.get(chargeId);
      if (this.canRemoteManage(player, charge) && !charge.isDetonating()) {
         if (charge.getDetonationTask() != null) {
            charge.getDetonationTask().cancel();
         }

         charge.setDetonating(true);
         this.detonate(charge);
         return true;
      } else {
         return false;
      }
   }

   public boolean remoteSetTimer(Player player, String chargeId, int seconds) {
      C4Charge charge = this.activeCharges.get(chargeId);
      if (this.canRemoteManage(player, charge) && !charge.isDetonating()) {
         if (charge.getDetonationTask() != null) {
            charge.getDetonationTask().cancel();
         }

         charge.setFuseSeconds(seconds);
         charge.setDetonationTask(Bukkit.getScheduler().runTaskLater(this.module.getCore(), () -> this.detonate(charge), seconds * 20L));
         charge.getLocation().getWorld().playSound(charge.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 0.8F, 1.2F);
         return true;
      } else {
         return false;
      }
   }

   private Location resolvePlacement(Block clickedBlock, BlockFace blockFace) {
      Block target = clickedBlock.getRelative(blockFace == null ? BlockFace.UP : blockFace);
      if (!target.isPassable()) {
         target = clickedBlock.getRelative(BlockFace.UP);
      }

      return target.getLocation().add(0.5, 0.15, 0.5);
   }

   private void placeCharge(Player player, C4Manager.TimerHolder holder, int fuseSeconds) {
      ItemStack inHand = player.getInventory().getItem(holder.hand);
      GrenadeDefinition currentDefinition = this.module.getGrenadeManager().getGrenade(inHand);
      if (!this.isC4(currentDefinition)) {
         player.closeInventory();
         player.sendMessage(Component.text("Non hai piu' il C4 in mano.", NamedTextColor.RED));
      } else {
         this.consumeOne(player, holder.hand);
         player.closeInventory();
         String id = UUID.randomUUID().toString().substring(0, 8);
         C4Charge charge = new C4Charge(id, holder.definition, player.getUniqueId(), player.getName(), holder.location.clone(), Instant.now(), fuseSeconds);
         ItemStack displayStack = this.module.getGrenadeManager().createItemStack(holder.definition.getId());
         if (displayStack == null) {
            displayStack = new ItemStack(Material.FIREWORK_STAR);
         }

         displayStack.setAmount(1);
         ItemStack displayItem = displayStack;
         ItemDisplay display = (ItemDisplay)holder.location.getWorld().spawn(holder.location, ItemDisplay.class, entity -> {
            entity.setItemStack(displayItem);
            entity.setPersistent(true);
            entity.getPersistentDataContainer().set(this.chargeKey, PersistentDataType.STRING, id);
            entity.addScoreboardTag("next_c4_charge");
         });
         charge.setDisplay(display);
         charge.setDetonationTask(Bukkit.getScheduler().runTaskLater(this.module.getCore(), () -> this.detonate(charge), fuseSeconds * 20L));
         this.activeCharges.put(id, charge);
         holder.location.getWorld().playSound(holder.location, Sound.BLOCK_CHAIN_PLACE, 0.8F, 0.8F);
         player.sendMessage(Component.text("C4 armato per " + this.formatSeconds(fuseSeconds) + ".", NamedTextColor.GREEN));
      }
   }

   private void openDefuseGui(Player player, C4Charge charge) {
      int correctWire = this.random.nextInt(WIRES.length);
      C4Manager.DefuseHolder holder = new C4Manager.DefuseHolder(charge.getId(), correctWire);
      Inventory inventory = Bukkit.createInventory(holder, 27, OpenGuiItems.getGlyphTitle("c4_defuse_gui", "C4 Defuse"));
      holder.inventory = inventory;
      this.fill(inventory);
      inventory.setItem(
         4,
         new ItemBuilder(Material.PAPER)
            .name(Component.text("Indizio filo", NamedTextColor.GOLD, new TextDecoration[]{TextDecoration.BOLD}).decoration(TextDecoration.ITALIC, false))
            .lore(new Component[]{Component.text(WIRES[correctWire].hint, NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)})
            .build()
      );

      for (int i = 0; i < WIRE_SLOTS.length; i++) {
         inventory.setItem(WIRE_SLOTS[i], this.wireItem(WIRES[i]));
      }

      player.openInventory(inventory);
   }

   private void disarm(Player player, C4Charge charge) {
      this.activeCharges.remove(charge.getId());
      if (charge.getDetonationTask() != null) {
         charge.getDetonationTask().cancel();
      }

      if (charge.getDisplay() != null && charge.getDisplay().isValid()) {
         charge.getDisplay().remove();
      }

      charge.getLocation().getWorld().playSound(charge.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 1.0F, 1.4F);
      player.sendMessage(Component.text("C4 disinnescato.", NamedTextColor.GREEN));
   }

   private void forceDetonateSoon(Player player, C4Charge charge) {
      if (!charge.isDetonating()) {
         charge.setDetonating(true);
         if (charge.getDetonationTask() != null) {
            charge.getDetonationTask().cancel();
         }

         charge.setDetonationTask(Bukkit.getScheduler().runTaskLater(this.module.getCore(), () -> this.detonate(charge), 60L));
         charge.getLocation().getWorld().playSound(charge.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0F, 0.6F);
         player.sendMessage(Component.text("Filo sbagliato. Detonazione imminente.", NamedTextColor.RED));
      }
   }

   private void detonate(C4Charge charge) {
      if (this.activeCharges.remove(charge.getId()) != null) {
         Location loc = charge.getLocation();
         if (charge.getDisplay() != null && charge.getDisplay().isValid()) {
            charge.getDisplay().remove();
         }

         loc.getWorld().playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 2.4F, 0.85F);
         loc.getWorld().spawnParticle(Particle.EXPLOSION_EMITTER, loc, 1);
         double radius = charge.getDefinition().getRadius();
         double maxDamage = charge.getDefinition().getDamage();

         for (LivingEntity entity : loc.getWorld().getLivingEntities()) {
            double distance = entity.getLocation().distance(loc);
            if (distance <= radius) {
               double damageMult = Math.max(0.1, 1.0 - distance / radius);
               entity.damage(maxDamage * damageMult);
            }
         }
      }
   }

   private void consumeOne(Player player, EquipmentSlot hand) {
      ItemStack item = player.getInventory().getItem(hand);
      if (item != null) {
         if (item.getAmount() <= 1) {
            player.getInventory().setItem(hand, null);
         } else {
            item.setAmount(item.getAmount() - 1);
         }
      }
   }

   private ItemStack timerItem(int seconds) {
      return new ItemBuilder(Material.CLOCK)
         .name(
            Component.text(this.formatSeconds(seconds), NamedTextColor.YELLOW, new TextDecoration[]{TextDecoration.BOLD})
               .decoration(TextDecoration.ITALIC, false)
         )
         .lore(new Component[]{Component.text("Arma il C4 con questo timer", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)})
         .build();
   }

   private ItemStack wireItem(C4Manager.Wire wire) {
      return new ItemBuilder(wire.material)
         .name(Component.text(wire.name + " wire", wire.color, new TextDecoration[]{TextDecoration.BOLD}).decoration(TextDecoration.ITALIC, false))
         .lore(new Component[]{Component.text("Taglia questo filo", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)})
         .build();
   }

   private void fill(Inventory inventory) {
      ItemStack filler = OpenGuiItems.getFiller();

      for (int i = 0; i < inventory.getSize(); i++) {
         inventory.setItem(i, filler);
      }
   }

   private String formatSeconds(int seconds) {
      if (seconds < 60) {
         return seconds + "s";
      }

      int minutes = seconds / 60;
      int remaining = seconds % 60;
      return remaining == 0 ? minutes + "m" : minutes + "m " + remaining + "s";
   }

   private static class DefuseHolder implements InventoryHolder {
      private final String chargeId;
      private final int correctWireIndex;
      private Inventory inventory;

      private DefuseHolder(String chargeId, int correctWireIndex) {
         this.chargeId = chargeId;
         this.correctWireIndex = correctWireIndex;
      }

      public Inventory getInventory() {
         return this.inventory;
      }
   }

   private static class TimerHolder implements InventoryHolder {
      private final GrenadeDefinition definition;
      private final EquipmentSlot hand;
      private final Location location;
      private Inventory inventory;

      private TimerHolder(GrenadeDefinition definition, EquipmentSlot hand, Location location) {
         this.definition = definition;
         this.hand = hand;
         this.location = location;
      }

      public Inventory getInventory() {
         return this.inventory;
      }
   }

   private record Wire(String name, Material material, NamedTextColor color, String hint) {
   }
}
