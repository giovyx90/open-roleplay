package dev.openrp.weapons.utility;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import io.papermc.paper.event.player.AsyncChatEvent;
import it.meridian.core.gui.NexoUI;
import it.meridian.core.permissions.NextPermissions;
import it.meridian.core.utils.ItemBuilder;
import dev.openrp.weapons.c4.C4Charge;
import dev.openrp.weapons.handcuffs.RestraintType;
import dev.openrp.weapons.module.WeaponsModule;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.Particle.DustOptions;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Bisected;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Lightable;
import org.bukkit.block.data.Openable;
import org.bukkit.block.data.type.Door;
import org.bukkit.entity.Entity;
import org.bukkit.entity.FishHook;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDismountEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.vehicle.VehicleMoveEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;

public class UtilityItemListener implements Listener {
   private static final int[] C4_TIMER_SLOTS = new int[]{10, 12, 14, 16};
   private static final int[] C4_TIMER_SECONDS = new int[]{30, 60, 120, 300};
   private static final double TRACE_SCAN_RADIUS = 6.0;
   private static final long TRACKER_DURATION_TICKS = 6000L;
   private static final long FIRE_AXE_RESTORE_TICKS = 1200L;
   private static final int FIRE_AXE_RESTORE_RETRIES = 10;
   private static final long PUNCTURE_DURATION_MILLIS = 12000L;
   private static final DateTimeFormatter TRACE_TIME_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss").withZone(ZoneId.of("Europe/Rome"));
   private final WeaponsModule module;
   private final UtilityItemManager manager;
   private final UtilitySettings settings;
   private final NamespacedKey gaggedKey;
   private final NamespacedKey blindfoldedKey;
   private final NamespacedKey barrierKey;
   private final NamespacedKey barrierOwnerKey;
   private final NamespacedKey barrierBlocksKey;
   private final NamespacedKey spikeStripKey;
   private final NamespacedKey spikeStripOwnerKey;
   private final Map<String, List<UtilityItemListener.ForensicTrace>> forensicTraces = new ConcurrentHashMap<>();
   private final Map<String, UtilityItemListener.TrackerRecord> trackerRecords = new ConcurrentHashMap<>();
   private final Map<UUID, Long> activeActions = new ConcurrentHashMap<>();
   private final Map<UUID, UUID> stretcherTargets = new ConcurrentHashMap<>();
   private final Map<UUID, Long> grappleCooldowns = new ConcurrentHashMap<>();
   private final Map<UUID, Long> fireAxeCooldowns = new ConcurrentHashMap<>();
   private final Map<UUID, Boolean> activeParachutes = new ConcurrentHashMap<>();
   private final Map<UUID, Long> puncturedVehicles = new ConcurrentHashMap<>();
   private final BukkitTask maintenanceTask;

   public UtilityItemListener(WeaponsModule module) {
      this.module = module;
      this.manager = module.getUtilityItemManager();
      this.settings = module.getUtilitySettings();
      this.gaggedKey = new NamespacedKey(module.getCore(), "gagged_by");
      this.blindfoldedKey = new NamespacedKey(module.getCore(), "blindfolded_by");
      this.barrierKey = new NamespacedKey(module.getCore(), "utility_barrier_id");
      this.barrierOwnerKey = new NamespacedKey(module.getCore(), "utility_barrier_owner");
      this.barrierBlocksKey = new NamespacedKey(module.getCore(), "utility_barrier_blocks");
      this.spikeStripKey = new NamespacedKey(module.getCore(), "utility_spike_strip_id");
      this.spikeStripOwnerKey = new NamespacedKey(module.getCore(), "utility_spike_strip_owner");
      this.maintenanceTask = Bukkit.getScheduler().runTaskTimer(module.getCore(), this::tickUtilityEffects, 20L, 40L);
   }

   public void cleanup() {
      this.maintenanceTask.cancel();
      this.forensicTraces.clear();
      this.trackerRecords.clear();
      this.activeActions.clear();
      this.stretcherTargets.clear();
      this.grappleCooldowns.clear();
      this.fireAxeCooldowns.clear();
      this.activeParachutes.clear();
      this.puncturedVehicles.clear();
   }

   @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
   public void onInteract(PlayerInteractEvent event) {
      if (event.getHand() != null) {
         Player player = event.getPlayer();
         Action action = event.getAction();
         ItemStack item = event.getItem();
         UtilityItemType type = this.manager.getType(item);
         if (event.getClickedBlock() != null
            && type != UtilityItemType.FINGERPRINT_SHEET
            && type != UtilityItemType.UV_FLASHLIGHT
            && (action == Action.RIGHT_CLICK_BLOCK || action == Action.LEFT_CLICK_BLOCK)) {
            this.recordForensicTrace(player, event.getClickedBlock());
         }

         if (!this.module.getHandcuffManager().isRestrained(player)) {
            if (type != UtilityItemType.FINGERPRINT_SHEET
               && (action == Action.LEFT_CLICK_BLOCK || action == Action.LEFT_CLICK_AIR || player.isSneaking())
               && (action == Action.RIGHT_CLICK_BLOCK || action == Action.LEFT_CLICK_BLOCK || action == Action.LEFT_CLICK_AIR)) {
               ItemDisplay equipment = this.findRoadEquipmentTarget(player, event.getClickedBlock());
               if (equipment != null) {
                  event.setCancelled(true);
                  if (this.isRoadBarrier(equipment)) {
                     this.removeRoadBarrier(player, equipment);
                  } else {
                     this.removeSpikeStrip(player, equipment);
                  }
                  return;
               }
            }

            if (type != null) {
               if (type == UtilityItemType.FIRE_AXE && action == Action.LEFT_CLICK_BLOCK && event.getClickedBlock() != null) {
                  if (isFireAxeDoorTarget(event.getClickedBlock())) {
                     event.setCancelled(true);
                     this.useFireAxe(player, item, event.getClickedBlock());
                  }
               } else if (action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK) {
                  switch (type) {
                     case C4_REMOTE:
                        event.setCancelled(true);
                        this.openC4Remote(player);
                        break;
                     case GPS_TRACKER:
                        event.setCancelled(true);
                        if (player.isSneaking() && event.getClickedBlock() != null) {
                           this.placeBlockTracker(player, item, event.getClickedBlock());
                        } else {
                           this.openTrackerGui(player);
                        }
                        break;
                     case PARACHUTE:
                        event.setCancelled(true);
                        if (!player.hasPermission("openrp.utility.wearables")) {
                           player.sendMessage(Component.text("Non puoi usare questo equipaggiamento.", NamedTextColor.RED));
                           return;
                        }

                        player.sendActionBar(Component.text("Tieni il paracadute mentre cadi per aprirlo.", NamedTextColor.YELLOW));
                        break;
                     case GRAPPLING_HOOK:
                        event.setCancelled(true);
                        this.useGrapplingHook(player);
                        break;
                     case STRETCHER:
                        event.setCancelled(true);
                        if (!this.releaseStretcher(player)) {
                           player.sendActionBar(Component.text("Clic destro su un giocatore vicino per caricarlo sulla barella.", NamedTextColor.YELLOW));
                        }
                        break;
                     case FIRE_EXTINGUISHER:
                        event.setCancelled(true);
                        this.useFireExtinguisher(player, item);
                        break;
                     case ROAD_BARRIER:
                        if (event.getClickedBlock() != null) {
                           event.setCancelled(true);
                           if (!player.hasPermission("openrp.utility.roadbarrier")) {
                              player.sendMessage(Component.text("Non puoi piazzare barriere stradali.", NamedTextColor.RED));
                              return;
                           }

                           this.placeRoadBarrier(player, event.getHand(), item, event.getClickedBlock(), event.getBlockFace());
                        }
                        break;
                     case SPIKE_STRIP:
                        if (event.getClickedBlock() != null) {
                           event.setCancelled(true);
                           if (!player.hasPermission("openrp.utility.roadbarrier")) {
                              player.sendMessage(Component.text("Non puoi piazzare equipaggiamento stradale.", NamedTextColor.RED));
                              return;
                           }

                           this.placeSpikeStrip(player, event.getHand(), item, event.getClickedBlock(), event.getBlockFace());
                        }
                        break;
                     case DUFFEL_BAG:
                        event.setCancelled(true);
                        this.openDuffel(player, event.getHand(), item);
                        break;
                     case GAS_MASK:
                     case NIGHT_VISION_GOGGLES:
                        event.setCancelled(true);
                        if (!player.hasPermission("openrp.utility.wearables")) {
                           player.sendMessage(Component.text("Non puoi usare questo equipaggiamento.", NamedTextColor.RED));
                           return;
                        }

                        this.equipWearable(player, event.getHand(), item, type);
                        break;
                     case UV_FLASHLIGHT:
                        event.setCancelled(true);
                        this.revealForensicTraces(player);
                        break;
                     case SCANNER:
                        event.setCancelled(true);
                        player.sendActionBar(Component.text("Clic destro su un giocatore per scansionare oggetti metallici.", NamedTextColor.YELLOW));
                        break;
                     case PAINT_SPRAY:
                        event.setCancelled(true);
                        player.sendActionBar(Component.text("Clic destro su un giocatore per usare lo spray al peperoncino.", NamedTextColor.YELLOW));
                        break;
                     case FINGERPRINT_SHEET:
                        event.setCancelled(true);
                        this.showFingerprintSheet(player, event.getClickedBlock(), item);
                  }
               }
            }
         }
      }
   }

   @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
   public void onEntityInteract(PlayerInteractEntityEvent event) {
      if (event.getHand() == EquipmentSlot.HAND) {
         Player player = event.getPlayer();
         Entity clicked = event.getRightClicked();
         ItemStack item = player.getInventory().getItemInMainHand();
         UtilityItemType type = this.manager.getType(item);
         if (type == UtilityItemType.FINGERPRINT_SHEET) {
            event.setCancelled(true);
            this.showFingerprintSheet(player, clicked, item);
         } else {
            if (!(clicked instanceof Player)) {
               this.recordForensicTrace(player, clicked);
            }

            if (clicked instanceof ItemDisplay display && this.isRoadBarrier(display)) {
               event.setCancelled(true);
               this.removeRoadBarrier(player, display);
            } else if (clicked instanceof ItemDisplay display && this.isSpikeStrip(display)) {
               event.setCancelled(true);
               this.removeSpikeStrip(player, display);
            } else if (!this.module.getHandcuffManager().isRestrained(player)) {
               if (type != null) {
                  if (type == UtilityItemType.GPS_TRACKER && player.isSneaking()) {
                     event.setCancelled(true);
                     this.placeEntityTracker(player, item, clicked);
                  } else if (clicked instanceof Player target) {
                     switch (type) {
                        case STRETCHER:
                           event.setCancelled(true);
                           this.loadStretcher(player, target);
                        case FIRE_EXTINGUISHER:
                        case ROAD_BARRIER:
                        case SPIKE_STRIP:
                        case DUFFEL_BAG:
                        case GAS_MASK:
                        case NIGHT_VISION_GOGGLES:
                        case UV_FLASHLIGHT:
                        case FINGERPRINT_SHEET:
                        default:
                           break;
                        case SCANNER:
                           event.setCancelled(true);
                           this.startMetalScan(player, target);
                           break;
                        case PAINT_SPRAY:
                           event.setCancelled(true);
                           this.usePepperSpray(player, target, item);
                           break;
                        case GAG:
                           event.setCancelled(true);
                           this.startGag(player, target);
                           break;
                        case BLINDFOLD:
                           event.setCancelled(true);
                           this.startBlindfold(player, target);
                           break;
                        case ROPE:
                           event.setCancelled(true);
                           this.startRopeTie(player, target, item);
                           break;
                        case SCISSORS:
                           event.setCancelled(true);
                           this.useScissorsOnPlayer(player, target);
                     }
                  }
               }
            }
         }
      }
   }

   @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
   public void onAsyncChat(AsyncChatEvent event) {
      if (this.isGagged(event.getPlayer())) {
         event.setCancelled(true);
         Bukkit.getScheduler()
            .runTask(this.module.getCore(), () -> event.getPlayer().sendMessage(Component.text("Non puoi parlare mentre sei imbavagliato.", NamedTextColor.RED)));
      }
   }

   @EventHandler
   public void onCommand(PlayerCommandPreprocessEvent event) {
      if (this.isGagged(event.getPlayer())) {
         String command = event.getMessage().toLowerCase().split(" ")[0];
         if (command.equals("/me")
            || command.equals("/do")
            || command.equals("/shout")
            || command.equals("/whisper")
            || command.equals("/w")
            || command.equals("/action")
            || command.equals("/c")
            || command.equals("/chat")) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(Component.text("Non puoi parlare mentre sei imbavagliato.", NamedTextColor.RED));
         }
      }
   }

   @EventHandler
   public void onJoin(PlayerJoinEvent event) {
      if (this.isBlindfolded(event.getPlayer())) {
         this.applyBlindfoldEffects(event.getPlayer());
      }
   }

   @EventHandler
   public void onQuit(PlayerQuitEvent event) {
      this.releaseStretcher(event.getPlayer());
      UUID target = this.stretcherTargets.remove(event.getPlayer().getUniqueId());
      if (target != null) {
         Player targetPlayer = Bukkit.getPlayer(target);
         if (targetPlayer != null) {
            targetPlayer.leaveVehicle();
         }
      }
   }

   @EventHandler
   public void onDeath(PlayerDeathEvent event) {
      this.removeGag(event.getEntity());
      this.removeBlindfold(event.getEntity());
      this.releaseStretcher(event.getEntity());
   }

   @EventHandler
   public void onMove(PlayerMoveEvent event) {
      Player player = event.getPlayer();
      if (player.isOnGround()) {
         this.resetParachute(player);
      } else {
         boolean holdingParachute = this.isHoldingParachute(player);
         if (!this.activeParachutes.containsKey(player.getUniqueId())
            && holdingParachute
            && player.getVelocity().getY() < -0.15) {
            this.activateParachute(player);
         } else if (this.activeParachutes.containsKey(player.getUniqueId()) && !holdingParachute) {
            this.resetParachute(player);
         }

         if (player.isInsideVehicle()) {
            this.handleVehicleOnSpikeStrip(player.getVehicle());
         }
      }
   }

   @EventHandler(ignoreCancelled = true)
   public void onVehicleMove(VehicleMoveEvent event) {
      this.handleVehicleOnSpikeStrip(event.getVehicle());
   }

   @EventHandler
   public void onDismount(EntityDismountEvent event) {
      if (event.getEntity() instanceof Player target && event.getDismounted() instanceof Player carrier) {
         UUID carried = this.stretcherTargets.get(carrier.getUniqueId());
         if (carried != null && carried.equals(target.getUniqueId())) {
            event.setCancelled(true);
         }
      }
   }

   @EventHandler
   public void onInventoryClick(InventoryClickEvent event) {
      Inventory top = event.getView().getTopInventory();
      InventoryHolder holder = top.getHolder();
      if (holder instanceof UtilityItemListener.C4ListHolder c4ListHolder) {
         event.setCancelled(true);
         if (event.getWhoClicked() instanceof Player player) {
            String chargeId = c4ListHolder.slotToCharge.get(event.getRawSlot());
            if (chargeId != null) {
               this.openC4Detail(player, chargeId);
            }
         }
      } else if (holder instanceof UtilityItemListener.C4DetailHolder detailHolder) {
         event.setCancelled(true);
         if (event.getWhoClicked() instanceof Player player) {
            this.handleC4DetailClick(player, detailHolder.chargeId, event.getRawSlot());
         }
      } else if (holder instanceof UtilityItemListener.TrackerHolder trackerHolder) {
         event.setCancelled(true);
         if (event.getWhoClicked() instanceof Player player) {
            String trackerId = trackerHolder.slotToTracker.get(event.getRawSlot());
            if (trackerId != null) {
               if (event.isShiftClick() || event.isRightClick()) {
                  this.removeTracker(player, trackerId);
               } else {
                  this.activateTracker(player, trackerId);
               }
            }
         }
      } else {
         if (holder instanceof UtilityItemListener.DuffelHolder && (this.isDuffelBag(event.getCurrentItem()) || this.isDuffelBag(event.getCursor()))) {
            event.setCancelled(true);
         }
      }
   }

   @EventHandler
   public void onInventoryDrag(InventoryDragEvent event) {
      if (event.getView().getTopInventory().getHolder() instanceof UtilityItemListener.DuffelHolder) {
         if (this.isDuffelBag(event.getOldCursor())) {
            event.setCancelled(true);
         }
      }
   }

   @EventHandler
   public void onInventoryClose(InventoryCloseEvent event) {
      if (event.getInventory().getHolder() instanceof UtilityItemListener.DuffelHolder holder && event.getPlayer() instanceof Player player) {
         ItemStack var6 = player.getInventory().getItem(holder.hand);
         if (this.manager.isType(var6, UtilityItemType.DUFFEL_BAG)) {
            this.saveDuffel(var6, event.getInventory().getContents());
            player.sendActionBar(Component.text("Borsone salvato.", NamedTextColor.GREEN));
         }
      }
   }

   private void openC4Remote(Player player) {
      if (!player.hasPermission("openrp.c4.remote")) {
         player.sendMessage(Component.text("Segnale remoto C4 bloccato.", NamedTextColor.RED));
      } else {
         List<C4Charge> charges = this.module.getC4Manager().getVisibleCharges(player);
         int size = Math.max(27, Math.min(54, (charges.size() + 8) / 9 * 9));
         UtilityItemListener.C4ListHolder holder = new UtilityItemListener.C4ListHolder();
         Inventory inventory = Bukkit.createInventory(holder, size, NexoUI.getGlyphTitle("c4_remote_gui", "Telecomando C4"));
         holder.inventory = inventory;
         this.fill(inventory);
         int slot = 0;

         for (C4Charge charge : charges) {
            if (slot >= size) {
               break;
            }

            holder.slotToCharge.put(slot, charge.getId());
            inventory.setItem(slot++, this.c4ChargeItem(player, charge));
         }

         if (charges.isEmpty()) {
            inventory.setItem(
               13,
               new ItemBuilder(Material.BARRIER)
                  .name(Component.text("Nessuna carica C4", NamedTextColor.RED, new TextDecoration[]{TextDecoration.BOLD}).decoration(TextDecoration.ITALIC, false))
                  .lore(new Component[]{Component.text("Non hai cariche attive.", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)})
                  .build()
            );
         }

         player.openInventory(inventory);
      }
   }

   private void openC4Detail(Player player, String chargeId) {
      C4Charge charge = this.module.getC4Manager().getCharge(chargeId);
      if (!this.module.getC4Manager().canRemoteManage(player, charge)) {
         player.sendMessage(Component.text("Segnale C4 non disponibile.", NamedTextColor.RED));
         this.openC4Remote(player);
      } else {
         UtilityItemListener.C4DetailHolder holder = new UtilityItemListener.C4DetailHolder(chargeId);
         Inventory inventory = Bukkit.createInventory(holder, 27, NexoUI.getGlyphTitle("c4_remote_detail_gui", "Controllo C4"));
         holder.inventory = inventory;
         this.fill(inventory);

         for (int i = 0; i < C4_TIMER_SLOTS.length; i++) {
            inventory.setItem(
               C4_TIMER_SLOTS[i],
               new ItemBuilder(Material.CLOCK)
                  .name(
                     Component.text("Imposta " + this.formatSeconds(C4_TIMER_SECONDS[i]), NamedTextColor.YELLOW, new TextDecoration[]{TextDecoration.BOLD})
                        .decoration(TextDecoration.ITALIC, false)
                  )
                  .lore(new Component[]{Component.text("Reimposta il timer da ora.", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)})
                  .build()
            );
         }

         inventory.setItem(4, this.c4ChargeItem(player, charge));
         inventory.setItem(
            22,
            new ItemBuilder(Material.FIRE_CHARGE)
               .name(Component.text("Detona ora", NamedTextColor.RED, new TextDecoration[]{TextDecoration.BOLD}).decoration(TextDecoration.ITALIC, false))
               .lore(new Component[]{Component.text("Detonazione remota.", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)})
               .build()
         );
         inventory.setItem(
            26,
            NexoUI.getCancelButton(
               Component.text("Indietro", NamedTextColor.RED, new TextDecoration[]{TextDecoration.BOLD}).decoration(TextDecoration.ITALIC, false)
            )
         );
         player.openInventory(inventory);
      }
   }

   private void handleC4DetailClick(Player player, String chargeId, int rawSlot) {
      if (rawSlot == 26) {
         this.openC4Remote(player);
      } else if (rawSlot == 22) {
         boolean success = this.module.getC4Manager().remoteDetonate(player, chargeId);
         player.closeInventory();
         player.sendMessage(Component.text(success ? "C4 detonato." : "Segnale C4 non disponibile.", success ? NamedTextColor.GREEN : NamedTextColor.RED));
      } else {
         for (int i = 0; i < C4_TIMER_SLOTS.length; i++) {
            if (rawSlot == C4_TIMER_SLOTS[i]) {
               boolean success = this.module.getC4Manager().remoteSetTimer(player, chargeId, C4_TIMER_SECONDS[i]);
               player.sendMessage(
                  Component.text(
                     success ? "Timer C4 impostato a " + this.formatSeconds(C4_TIMER_SECONDS[i]) + "." : "Segnale C4 non disponibile.",
                     success ? NamedTextColor.GREEN : NamedTextColor.RED
                  )
               );
               this.openC4Detail(player, chargeId);
               return;
            }
         }
      }
   }

   private ItemStack c4ChargeItem(Player player, C4Charge charge) {
      double distance = player.getWorld().equals(charge.getLocation().getWorld()) ? player.getLocation().distance(charge.getLocation()) : -1.0;
      List<Component> lore = new ArrayList<>();
      lore.add(Component.text("Proprietario: " + charge.getOwnerName(), NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
      lore.add(
         Component.text("Rimanente: " + this.formatSeconds((int)charge.getRemainingSeconds()), NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false)
      );
      lore.add(
         Component.text(distance >= 0.0 ? "Distanza: " + Math.round(distance) + "m" : "Mondo diverso", NamedTextColor.GRAY)
            .decoration(TextDecoration.ITALIC, false)
      );
      return new ItemBuilder(Material.FIREWORK_STAR)
         .name(Component.text("C4 " + charge.getId(), NamedTextColor.RED, new TextDecoration[]{TextDecoration.BOLD}).decoration(TextDecoration.ITALIC, false))
         .customModelData(304)
         .lore(lore)
         .build();
   }

   private void placeBlockTracker(Player player, ItemStack item, Block block) {
      if (player.getLocation().distance(block.getLocation().add(0.5, 0.5, 0.5)) > 4.5) {
         player.sendMessage(Component.text("Il bersaglio del tracker e' troppo lontano.", NamedTextColor.RED));
      } else {
         String id = UUID.randomUUID().toString().substring(0, 8);
         Location location = block.getLocation().add(0.5, 0.5, 0.5);
         this.trackerRecords.put(id, UtilityItemListener.TrackerRecord.forLocation(id, player.getUniqueId(), "Blocco " + block.getType().name(), location));
         this.consumeOne(player, EquipmentSlot.HAND);
         player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 0.8F, 1.4F);
         player.sendMessage(Component.text("Tracker GPS piazzato sul blocco.", NamedTextColor.GREEN));
      }
   }

   private void placeEntityTracker(Player player, ItemStack item, Entity entity) {
      if (entity.equals(player)) {
         player.sendMessage(Component.text("Non puoi tracciare te stesso.", NamedTextColor.RED));
      } else if (player.getLocation().distance(entity.getLocation()) > 4.5) {
         player.sendMessage(Component.text("Il bersaglio del tracker e' troppo lontano.", NamedTextColor.RED));
      } else {
         String id = UUID.randomUUID().toString().substring(0, 8);
         this.trackerRecords.put(id, UtilityItemListener.TrackerRecord.forEntity(id, player.getUniqueId(), entity.getName(), entity.getUniqueId()));
         this.consumeOne(player, EquipmentSlot.HAND);
         player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 0.8F, 1.4F);
         player.sendMessage(Component.text("Tracker GPS agganciato a " + entity.getName() + ".", NamedTextColor.GREEN));
      }
   }

   private void openTrackerGui(Player player) {
      this.removeInvalidTrackers(player.getUniqueId());
      UtilityItemListener.TrackerHolder holder = new UtilityItemListener.TrackerHolder();
      Inventory inventory = Bukkit.createInventory(holder, 27, NexoUI.getGlyphTitle("gps_tracker_gui", "Tracker GPS"));
      holder.inventory = inventory;
      this.fill(inventory);
      int slot = 0;

      for (UtilityItemListener.TrackerRecord record : this.trackerRecords.values()) {
         if (record.ownerUuid.equals(player.getUniqueId())) {
            if (slot >= 27) {
               break;
            }

            holder.slotToTracker.put(slot, record.id);
            Location loc = record.currentLocation();
            inventory.setItem(
               slot++,
               new ItemBuilder(Material.COMPASS)
                  .name(Component.text(record.label, NamedTextColor.GREEN, new TextDecoration[]{TextDecoration.BOLD}).decoration(TextDecoration.ITALIC, false))
                  .lore(trackerLore(loc))
                  .build()
            );
         }
      }

      if (holder.slotToTracker.isEmpty()) {
         inventory.setItem(
            13,
            new ItemBuilder(Material.BARRIER)
               .name(Component.text("Nessun tracker", NamedTextColor.RED, new TextDecoration[]{TextDecoration.BOLD}).decoration(TextDecoration.ITALIC, false))
               .build()
         );
      }

      player.openInventory(inventory);
   }

   private void activateTracker(Player player, String trackerId) {
      UtilityItemListener.TrackerRecord record = this.trackerRecords.get(trackerId);
      if (record != null && record.ownerUuid.equals(player.getUniqueId())) {
         if (record.currentLocation() == null) {
            this.trackerRecords.remove(trackerId);
            player.sendMessage(Component.text("Segnale tracker scaduto e rimosso.", NamedTextColor.YELLOW));
            Bukkit.getScheduler().runTask(this.module.getCore(), () -> this.openTrackerGui(player));
            return;
         }
         this.module.getDispatchGpsManager().activate(player, "TRACKER", record::currentLocation, 6000L, 4.0, settings.trackerShowCoordinates());
         player.closeInventory();
      } else {
         player.sendMessage(Component.text("Segnale tracker non disponibile.", NamedTextColor.RED));
      }
   }

   private void removeTracker(Player player, String trackerId) {
      UtilityItemListener.TrackerRecord record = this.trackerRecords.get(trackerId);
      if (record != null && record.ownerUuid.equals(player.getUniqueId())) {
         this.trackerRecords.remove(trackerId);
         player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 0.7F, 1.1F);
         player.sendMessage(Component.text("Tracker GPS rimosso.", NamedTextColor.GREEN));
         Bukkit.getScheduler().runTask(this.module.getCore(), () -> this.openTrackerGui(player));
      } else {
         player.sendMessage(Component.text("Segnale tracker non disponibile.", NamedTextColor.RED));
      }
   }

   private void removeInvalidTrackers(UUID ownerUuid) {
      this.trackerRecords.entrySet().removeIf(entry -> entry.getValue().ownerUuid.equals(ownerUuid)
         && entry.getValue().currentLocation() == null);
   }

   private Component[] trackerLore(Location location) {
      List<Component> lore = new ArrayList<>();
      if (location == null) {
         lore.add(Component.text("Segnale non disponibile", NamedTextColor.RED).decoration(TextDecoration.ITALIC, false));
      } else {
         lore.add(Component.text("Mondo: " + location.getWorld().getName(), NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
         if (settings.trackerShowRegion()) {
            lore.add(Component.text("Regione: " + worldGuardRegion(location), NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
         }
         if (settings.trackerShowCoordinates()) {
            lore.add(Component.text(this.formatLocation(location), NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false));
         }
      }
      lore.add(Component.text("Clic sinistro per attivare. Clic destro o shift-clic per rimuovere.", NamedTextColor.YELLOW)
            .decoration(TextDecoration.ITALIC, false));
      return lore.toArray(Component[]::new);
   }

   private void startGag(Player actor, Player target) {
      if (this.isGagged(target)) {
         actor.sendActionBar(Component.text(target.getName() + " e' gia' imbavagliato.", NamedTextColor.YELLOW));
         return;
      }
      ItemStack item = actor.getInventory().getItemInMainHand();
      if (this.manager.getRestraintUses(item, UtilityItemType.GAG) <= 0) {
         actor.sendActionBar(Component.text("Il bavaglio non ha piu' usi.", NamedTextColor.RED));
         return;
      }
      this.startTimedAction(actor, target, "Applicazione bavaglio...", "Applicazione bavaglio annullata.", settings.restraintDurationMillis(), () -> {
         ItemStack current = actor.getInventory().getItemInMainHand();
         if (!this.manager.isType(current, UtilityItemType.GAG)) {
            actor.sendMessage(Component.text("Non hai piu' il bavaglio in mano.", NamedTextColor.RED));
            return;
         }
         if (!this.manager.consumeRestraintUse(current, UtilityItemType.GAG)) {
            actor.sendActionBar(Component.text("Il bavaglio non ha piu' usi.", NamedTextColor.RED));
            return;
         }
         this.applyGag(actor, target);
      });
   }

   private void startBlindfold(Player actor, Player target) {
      if (this.isBlindfolded(target)) {
         actor.sendActionBar(Component.text(target.getName() + " ha gia' una benda.", NamedTextColor.YELLOW));
         return;
      }
      ItemStack item = actor.getInventory().getItemInMainHand();
      if (this.manager.getRestraintUses(item, UtilityItemType.BLINDFOLD) <= 0) {
         actor.sendActionBar(Component.text("La benda non ha piu' usi.", NamedTextColor.RED));
         return;
      }
      this.startTimedAction(actor, target, "Applicazione benda...", "Applicazione benda annullata.", settings.restraintDurationMillis(), () -> {
         ItemStack current = actor.getInventory().getItemInMainHand();
         if (!this.manager.isType(current, UtilityItemType.BLINDFOLD)) {
            actor.sendMessage(Component.text("Non hai piu' la benda in mano.", NamedTextColor.RED));
            return;
         }
         if (!this.manager.consumeRestraintUse(current, UtilityItemType.BLINDFOLD)) {
            actor.sendActionBar(Component.text("La benda non ha piu' usi.", NamedTextColor.RED));
            return;
         }
         this.applyBlindfold(actor, target);
      });
   }

   private void applyGag(Player actor, Player target) {
      if (this.isGagged(target)) {
         actor.sendActionBar(Component.text(target.getName() + " e' gia' imbavagliato.", NamedTextColor.YELLOW));
      } else {
         target.getPersistentDataContainer().set(this.gaggedKey, PersistentDataType.STRING, actor.getUniqueId().toString());
         actor.sendMessage(Component.text("Hai imbavagliato " + target.getName() + ".", NamedTextColor.GREEN));
         target.sendMessage(Component.text("Sei stato imbavagliato.", NamedTextColor.RED));
      }
   }

   private void applyBlindfold(Player actor, Player target) {
      if (this.isBlindfolded(target)) {
         actor.sendActionBar(Component.text(target.getName() + " ha gia' una benda.", NamedTextColor.YELLOW));
      } else {
         target.getPersistentDataContainer().set(this.blindfoldedKey, PersistentDataType.STRING, actor.getUniqueId().toString());
         this.applyBlindfoldEffects(target);
         actor.sendMessage(Component.text("Hai bendato " + target.getName() + ".", NamedTextColor.GREEN));
         target.sendMessage(Component.text("Sei stato bendato.", NamedTextColor.RED));
      }
   }

   private boolean isGagged(Player player) {
      return player.getPersistentDataContainer().has(this.gaggedKey, PersistentDataType.STRING);
   }

   private boolean isBlindfolded(Player player) {
      return player.getPersistentDataContainer().has(this.blindfoldedKey, PersistentDataType.STRING);
   }

   private void applyBlindfoldEffects(Player player) {
      player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 999999, 0, false, false, false));
   }

   private void removeGag(Player player) {
      player.getPersistentDataContainer().remove(this.gaggedKey);
   }

   private void removeBlindfold(Player player) {
      player.getPersistentDataContainer().remove(this.blindfoldedKey);
      player.removePotionEffect(PotionEffectType.BLINDNESS);
   }

   private void startRopeTie(Player actor, Player target, ItemStack rope) {
      if (this.module.getHandcuffManager().isRestrained(target)) {
         actor.sendActionBar(Component.text("Il bersaglio e' gia' immobilizzato.", NamedTextColor.RED));
      } else {
         this.startTimedAction(actor, target, "Legatura con corda...", "Legatura con corda annullata.", settings.restraintDurationMillis(), () -> {
            ItemStack current = actor.getInventory().getItemInMainHand();
            if (!this.manager.isType(current, UtilityItemType.ROPE)) {
               actor.sendMessage(Component.text("Non hai piu' la corda in mano.", NamedTextColor.RED));
            } else {
               this.module.getHandcuffManager().tieWithRope(target, actor);
               this.consumeOne(actor, EquipmentSlot.HAND);
               actor.playSound(actor.getLocation(), Sound.BLOCK_CHAIN_PLACE, 0.8F, 1.2F);
               target.playSound(target.getLocation(), Sound.BLOCK_CHAIN_PLACE, 0.8F, 1.0F);
               actor.sendMessage(Component.text("Hai legato " + target.getName() + ".", NamedTextColor.GREEN));
               target.sendMessage(Component.text("Sei stato legato con la corda.", NamedTextColor.RED));
            }
         });
      }
   }

   private void useScissorsOnPlayer(Player actor, Player target) {
      boolean removed = false;
      if (this.isGagged(target)) {
         this.removeGag(target);
         actor.sendMessage(Component.text("Hai rimosso il bavaglio da " + target.getName() + ".", NamedTextColor.GREEN));
         target.sendMessage(Component.text("Il tuo bavaglio e' stato rimosso.", NamedTextColor.GREEN));
         removed = true;
      }

      if (this.isBlindfolded(target)) {
         this.removeBlindfold(target);
         actor.sendMessage(Component.text("Hai rimosso la benda da " + target.getName() + ".", NamedTextColor.GREEN));
         target.sendMessage(Component.text("La tua benda e' stata rimossa.", NamedTextColor.GREEN));
         removed = true;
      }

      if (removed) {
         actor.playSound(actor.getLocation(), Sound.ENTITY_SHEEP_SHEAR, 1.0F, 1.2F);
         target.playSound(target.getLocation(), Sound.ENTITY_SHEEP_SHEAR, 1.0F, 0.8F);
      } else {
         this.startRopeCut(actor, target);
      }
   }

   private void startRopeCut(Player actor, Player target) {
      if (this.module.getHandcuffManager().getRestraintType(target) != RestraintType.ROPE) {
         actor.sendActionBar(Component.text("Questo giocatore non e' legato con la corda.", NamedTextColor.RED));
      } else {
         this.startTimedAction(actor, target, "Taglio corda...", "Taglio corda annullato.", settings.restraintDurationMillis(), () -> {
            this.module.getHandcuffManager().uncuff(target);
            actor.playSound(actor.getLocation(), Sound.ENTITY_SHEEP_SHEAR, 1.0F, 1.2F);
            target.playSound(target.getLocation(), Sound.ENTITY_SHEEP_SHEAR, 1.0F, 0.8F);
            actor.sendMessage(Component.text("Hai tagliato la corda su " + target.getName() + ".", NamedTextColor.GREEN));
            target.sendMessage(Component.text("Sei stato liberato dalla corda.", NamedTextColor.GREEN));
         });
      }
   }

   private void startTimedAction(Player actor, Player target, String startMessage, String cancelMessage, long durationMillis, Runnable completion) {
      if (!this.activeActions.containsKey(actor.getUniqueId())) {
         actor.sendMessage(Component.text(startMessage, NamedTextColor.YELLOW));
         long startedAt = System.currentTimeMillis();
         long endTime = startedAt + Math.max(250L, durationMillis);
         this.activeActions.put(actor.getUniqueId(), endTime);
         Bukkit.getScheduler().runTaskTimer(this.module.getCore(), task -> {
            if (actor.isOnline() && target.isOnline() && !(actor.getLocation().distance(target.getLocation()) > 4.0)) {
               long remaining = Math.max(0L, endTime - System.currentTimeMillis());
               int progress = (int)Math.round(10.0D * (System.currentTimeMillis() - startedAt) / Math.max(1L, endTime - startedAt));
               actor.sendActionBar(Component.text(progressBar(progress) + " " + (remaining + 999L) / 1000L + "s", NamedTextColor.YELLOW));
               if (System.currentTimeMillis() >= endTime) {
                  this.activeActions.remove(actor.getUniqueId());
                  completion.run();
                  task.cancel();
               }
            } else {
               this.activeActions.remove(actor.getUniqueId());
               actor.sendActionBar(Component.text(cancelMessage, NamedTextColor.RED));
               task.cancel();
            }
         }, 0L, 1L);
      }
   }

   private void startMetalScan(Player scanner, Player target) {
      if (scanner.equals(target)) {
         scanner.sendActionBar(Component.text("Non puoi scansionare te stesso.", NamedTextColor.RED));
         return;
      }
      this.startTimedAction(scanner, target, "Scansione oggetti metallici...", "Scansione metalli annullata.", settings.scannerDurationMillis(),
            () -> {
               ItemStack current = scanner.getInventory().getItemInMainHand();
               if (!this.manager.isType(current, UtilityItemType.SCANNER)) {
                  scanner.sendMessage(Component.text("Non hai piu' lo scanner in mano.", NamedTextColor.RED));
                  return;
               }
               this.scanMetalItems(scanner, target);
            });
   }

   private String progressBar(int progress) {
      int clamped = Math.max(0, Math.min(10, progress));
      return "[" + "|".repeat(clamped) + ".".repeat(10 - clamped) + "]";
   }

   private void equipWearable(Player player, EquipmentSlot hand, ItemStack item, UtilityItemType type) {
      this.equipHelmetUtility(player, hand, item);
   }

   private void activateParachute(Player player) {
      if (this.isHoldingParachute(player) && player.hasPermission("openrp.utility.wearables")) {
         this.setHeldParachutesOpen(player, true);
         player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING, 240, 0, false, false, true));
         player.setFallDistance(0.0F);
         this.activeParachutes.put(player.getUniqueId(), true);
         player.playSound(player.getLocation(), Sound.ITEM_ARMOR_EQUIP_ELYTRA, 0.9F, 1.1F);
      }
   }

   private void resetParachute(Player player) {
      boolean wasActive = this.activeParachutes.remove(player.getUniqueId()) != null;
      if (wasActive) {
         player.removePotionEffect(PotionEffectType.SLOW_FALLING);
      }
      this.setHeldParachutesOpen(player, false);

      for (ItemStack item : player.getInventory().getContents()) {
         if (this.manager.isType(item, UtilityItemType.PARACHUTE)) {
            this.setParachuteOpen(item, false);
         }
      }
   }

   private boolean isHoldingParachute(Player player) {
      return player != null
            && player.hasPermission("openrp.utility.wearables")
            && (this.manager.isType(player.getInventory().getItemInMainHand(), UtilityItemType.PARACHUTE)
            || this.manager.isType(player.getInventory().getItemInOffHand(), UtilityItemType.PARACHUTE));
   }

   private void setHeldParachutesOpen(Player player, boolean open) {
      ItemStack mainHand = player.getInventory().getItemInMainHand();
      if (this.manager.isType(mainHand, UtilityItemType.PARACHUTE)) {
         this.setParachuteOpen(mainHand, open);
      }
      ItemStack offHand = player.getInventory().getItemInOffHand();
      if (this.manager.isType(offHand, UtilityItemType.PARACHUTE)) {
         this.setParachuteOpen(offHand, open);
      }
   }

   private void setParachuteOpen(ItemStack parachute, boolean open) {
      if (parachute == null || !parachute.hasItemMeta()) {
         return;
      }
      ItemMeta meta = parachute.getItemMeta();
      meta.setCustomModelData(open ? UtilityItemManager.PARACHUTE_OPEN_MODEL_DATA : UtilityItemType.PARACHUTE.getCustomModelData());
      parachute.setItemMeta(meta);
   }

   private void useGrapplingHook(Player player) {
      long now = System.currentTimeMillis();
      Long cooldownUntil = this.grappleCooldowns.get(player.getUniqueId());
      if (cooldownUntil != null && cooldownUntil > now) {
         player.sendActionBar(Component.text("Rampino in cooldown.", NamedTextColor.RED));
      } else {
         RayTraceResult result = player.getWorld().rayTraceBlocks(
               player.getEyeLocation(),
               player.getEyeLocation().getDirection(),
               settings.grapplingHookMaxDistance(),
               FluidCollisionMode.NEVER,
               true);
         if (result == null || result.getHitPosition() == null) {
            player.sendActionBar(Component.text("Nessun punto di aggancio trovato.", NamedTextColor.RED));
            return;
         }
         Location anchor = result.getHitPosition().toLocation(player.getWorld());
         if (anchor.distance(player.getLocation()) < 2.0D) {
            player.sendActionBar(Component.text("Il punto di aggancio e' troppo vicino.", NamedTextColor.RED));
            return;
         }
         player.playSound(player.getLocation(), Sound.ENTITY_FISHING_BOBBER_THROW, 1.0F, 0.8F);
         player.getWorld().spawnParticle(Particle.CRIT, anchor, 12, 0.1D, 0.1D, 0.1D, 0.02D);
         this.grappleCooldowns.put(player.getUniqueId(), now + settings.grapplingHookCooldownSeconds() * 1000L);
         player.playSound(anchor, Sound.BLOCK_CHAIN_PLACE, 0.9F, 1.3F);
         this.startGrapplePull(player, null, anchor, 0);
      }
   }

   private void startGrapplePull(Player player, FishHook hook, Location anchor, int tick) {
      if (player.isOnline() && !player.isDead() && tick <= 80) {
         double distance = player.getLocation().distance(anchor);
         if (distance < 1.25) {
            player.setFallDistance(0.0F);
            if (hook != null) {
               hook.remove();
            }
         } else {
            Vector velocity = anchor.toVector().subtract(player.getLocation().toVector()).normalize().multiply(Math.min(1.85, 0.6 + distance * 0.09));
            velocity.setY(Math.min(1.25, velocity.getY() + 0.2));
            player.setFallDistance(0.0F);
            player.setVelocity(velocity);
            Bukkit.getScheduler().runTaskLater(this.module.getCore(), () -> this.startGrapplePull(player, hook, anchor, tick + 1), 1L);
         }
      } else {
         if (hook != null) {
            hook.remove();
         }
      }
   }

   private void loadStretcher(Player carrier, Player target) {
      if (this.stretcherTargets.containsKey(carrier.getUniqueId())) {
         this.releaseStretcher(carrier);
      } else if (target.equals(carrier)) {
         carrier.sendMessage(Component.text("Non puoi caricare te stesso sulla barella.", NamedTextColor.RED));
      } else if (carrier.getLocation().distance(target.getLocation()) > 4.0) {
         carrier.sendMessage(Component.text("Il bersaglio e' troppo lontano.", NamedTextColor.RED));
      } else if (target.getPassengers().isEmpty() && !target.isInsideVehicle()) {
         if (carrier.addPassenger(target)) {
            this.stretcherTargets.put(carrier.getUniqueId(), target.getUniqueId());
            carrier.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 999999, 1, false, false, false));
            carrier.sendMessage(Component.text("Barella caricata.", NamedTextColor.GREEN));
            target.sendMessage(Component.text("Sei stato caricato su una barella.", NamedTextColor.YELLOW));
         }
      } else {
         carrier.sendMessage(Component.text("Il bersaglio non puo' essere caricato ora.", NamedTextColor.RED));
      }
   }

   private boolean releaseStretcher(Player carrier) {
      UUID targetUuid = this.stretcherTargets.remove(carrier.getUniqueId());
      if (targetUuid == null) {
         return false;
      }

      Player target = Bukkit.getPlayer(targetUuid);
      if (target != null) {
         carrier.removePassenger(target);
         target.leaveVehicle();
         target.sendMessage(Component.text("Sei stato rilasciato dalla barella.", NamedTextColor.YELLOW));
      }

      carrier.removePotionEffect(PotionEffectType.SLOWNESS);
      carrier.sendMessage(Component.text("Barella rilasciata.", NamedTextColor.GREEN));
      return true;
   }

   private void useFireExtinguisher(Player player, ItemStack item) {
      int charges = this.manager.getExtinguisherCharges(item);
      if (charges <= 0) {
         player.sendActionBar(Component.text("L'estintore e' vuoto.", NamedTextColor.RED));
      } else {
         Location eye = player.getEyeLocation();
         Vector direction = eye.getDirection().normalize();
         int extinguished = 0;

         for (double step = 1.0; step <= 5.0; step += 0.75) {
            Location center = eye.clone().add(direction.clone().multiply(step));
            player.getWorld().spawnParticle(Particle.CLOUD, center, 6, 0.25, 0.25, 0.25, 0.02);

            for (int x = -1; x <= 1; x++) {
               for (int y = -1; y <= 1; y++) {
                  for (int z = -1; z <= 1; z++) {
                     if (this.extinguishBlock(center.clone().add(x, y, z).getBlock())) {
                        extinguished++;
                     }
                  }
               }
            }
         }

         for (Entity entity : player.getNearbyEntities(5.0, 3.0, 5.0)) {
            Vector toEntity = entity.getLocation().toVector().subtract(eye.toVector());
            if (toEntity.lengthSquared() > 0.0 && toEntity.normalize().dot(direction) > 0.45) {
               entity.setFireTicks(0);
            }
         }

         player.setFireTicks(0);
         this.manager.setExtinguisherCharges(item, charges - 1);
         player.playSound(player.getLocation(), Sound.BLOCK_FIRE_EXTINGUISH, 0.9F, 1.2F);
         player.sendActionBar(Component.text("Durabilita' estintore: " + (charges - 1) + "/100. Fuochi spenti: " + extinguished, NamedTextColor.AQUA));
      }
   }

   private boolean extinguishBlock(Block block) {
      if (block.getType() != Material.FIRE && block.getType() != Material.SOUL_FIRE) {
         if (block.getBlockData() instanceof Lightable lightable && lightable.isLit()) {
            lightable.setLit(false);
            block.setBlockData(lightable);
            return true;
         } else {
            return false;
         }
      } else {
         block.setType(Material.AIR);
         return true;
      }
   }

   private void placeRoadBarrier(Player player, EquipmentSlot hand, ItemStack item, Block clickedBlock, BlockFace face) {
      Block target = clickedBlock.getRelative(face == null ? BlockFace.UP : face);
      if (!target.isPassable()) {
         target = clickedBlock.getRelative(BlockFace.UP);
      }

      List<Block> hitbox = this.resolveBarrierHitbox(target, player);
      if (hitbox.stream().anyMatch(block -> !block.isPassable())) {
         player.sendMessage(Component.text("Non c'e' abbastanza spazio per piazzare la barriera.", NamedTextColor.RED));
      } else {
         Location location = target.getLocation().add(0.5, 0.0, 0.5);
         String id = UUID.randomUUID().toString().substring(0, 8);
         ItemStack displayStack = this.manager.createItem(UtilityItemType.ROAD_BARRIER);
         hitbox.forEach(block -> block.setType(Material.BARRIER, false));
         player.getWorld().spawn(location, ItemDisplay.class, display -> {
            display.setItemStack(displayStack);
            display.setPersistent(true);
            display.setRotation(this.snapBarrierYaw(player), 0.0F);
            display.addScoreboardTag("next_road_barrier");
            display.getPersistentDataContainer().set(this.barrierKey, PersistentDataType.STRING, id);
            display.getPersistentDataContainer().set(this.barrierOwnerKey, PersistentDataType.STRING, player.getUniqueId().toString());
            display.getPersistentDataContainer().set(this.barrierBlocksKey, PersistentDataType.STRING, this.encodeBlocks(hitbox));
         });
         this.consumeOne(player, hand);
         player.playSound(location, Sound.BLOCK_CHAIN_PLACE, 0.8F, 0.9F);
         player.sendMessage(Component.text("Barriera stradale piazzata.", NamedTextColor.GREEN));
      }
   }

   private boolean isRoadBarrier(ItemDisplay display) {
      return display.getPersistentDataContainer().has(this.barrierKey, PersistentDataType.STRING);
   }

   private void placeSpikeStrip(Player player, EquipmentSlot hand, ItemStack item, Block clickedBlock, BlockFace face) {
      Block target = clickedBlock.getRelative(face == null ? BlockFace.UP : face);
      if (!target.isPassable()) {
         target = clickedBlock.getRelative(BlockFace.UP);
      }

      List<Block> stripBlocks = this.resolveLine(target, player, 5);
      if (stripBlocks.stream().anyMatch(blockx -> !blockx.isPassable())) {
         player.sendMessage(Component.text("Non c'e' abbastanza spazio per piazzare la striscia chiodata.", NamedTextColor.RED));
      } else {
         ItemStack displayStack = this.manager.createItem(UtilityItemType.SPIKE_STRIP);
         String id = UUID.randomUUID().toString().substring(0, 8);

         for (Block block : stripBlocks) {
            Location location = block.getLocation().add(0.5, 0.03, 0.5);
            player.getWorld().spawn(location, ItemDisplay.class, display -> {
               display.setItemStack(displayStack);
               display.setPersistent(true);
               display.setRotation(this.snapBarrierYaw(player), 0.0F);
               display.addScoreboardTag("next_spike_strip");
               display.getPersistentDataContainer().set(this.spikeStripKey, PersistentDataType.STRING, id);
               display.getPersistentDataContainer().set(this.spikeStripOwnerKey, PersistentDataType.STRING, player.getUniqueId().toString());
            });
         }

         this.consumeOne(player, hand);
         player.playSound(target.getLocation(), Sound.BLOCK_CHAIN_PLACE, 0.7F, 1.3F);
         player.sendMessage(Component.text("Striscia chiodata piazzata.", NamedTextColor.GREEN));
      }
   }

   private boolean isSpikeStrip(ItemDisplay display) {
      return display.getPersistentDataContainer().has(this.spikeStripKey, PersistentDataType.STRING);
   }

   private void removeSpikeStrip(Player player, ItemDisplay display) {
      String owner = (String)display.getPersistentDataContainer().get(this.spikeStripOwnerKey, PersistentDataType.STRING);
      boolean canRemove = owner != null && owner.equals(player.getUniqueId().toString())
            || canManageUtilityObjects(player);
      if (!canRemove) {
         player.sendMessage(Component.text("Non puoi rimuovere questa striscia chiodata.", NamedTextColor.RED));
      } else {
         this.returnUtilityItem(player, UtilityItemType.SPIKE_STRIP);
         this.removeSpikeStripGroup(display);
         player.playSound(player.getLocation(), Sound.BLOCK_CHAIN_BREAK, 0.8F, 1.2F);
         player.sendMessage(Component.text("Striscia chiodata rimossa.", NamedTextColor.GREEN));
      }
   }

   private void handleVehicleOnSpikeStrip(Entity vehicle) {
      if (vehicle != null && vehicle.isValid()) {
         Long puncturedUntil = this.puncturedVehicles.get(vehicle.getUniqueId());
         long now = System.currentTimeMillis();
         if (puncturedUntil != null && puncturedUntil > now) {
            this.slowPuncturedVehicle(vehicle);
         } else {
            if (puncturedUntil != null) {
               this.puncturedVehicles.remove(vehicle.getUniqueId());
            }

            ItemDisplay strip = this.findSpikeStripNear(vehicle.getLocation());
            if (strip != null) {
               this.puncturedVehicles.put(vehicle.getUniqueId(), now + 12000L);
               this.slowPuncturedVehicle(vehicle);
               Location location = vehicle.getLocation();
               location.getWorld().spawnParticle(Particle.CRIT, location.clone().add(0.0, 0.3, 0.0), 18, 0.6, 0.15, 0.6, 0.05);
               location.getWorld().playSound(location, Sound.ENTITY_ITEM_BREAK, 1.0F, 0.6F);

               for (Entity passenger : vehicle.getPassengers()) {
                  if (passenger instanceof Player player) {
                     player.sendActionBar(Component.text("Pneumatici forati.", NamedTextColor.RED));
                  }
               }
            }
         }
      }
   }

   private ItemDisplay findSpikeStripNear(Location location) {
      if (location.getWorld() == null) {
         return null;
      }

      for (Entity entity : location.getWorld().getNearbyEntities(location, 3.0, 0.9, 3.0)) {
         if (entity instanceof ItemDisplay display && this.isSpikeStrip(display) && Math.abs(display.getLocation().getY() - location.getY()) <= 1.0) {
            return display;
         }
      }

      return null;
   }

   private void slowPuncturedVehicle(Entity vehicle) {
      Vector velocity = vehicle.getVelocity();
      if (velocity.lengthSquared() > 0.01) {
         vehicle.setVelocity(velocity.multiply(0.18));
      }

      for (Entity passenger : vehicle.getPassengers()) {
         if (passenger instanceof Player player) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 40, 1, false, false, false));
         }
      }
   }

   private void removeRoadBarrier(Player player, ItemDisplay display) {
      String owner = (String)display.getPersistentDataContainer().get(this.barrierOwnerKey, PersistentDataType.STRING);
      boolean canRemove = owner != null && owner.equals(player.getUniqueId().toString())
            || canManageUtilityObjects(player);
      if (!canRemove) {
         player.sendMessage(Component.text("Non puoi rimuovere questa barriera.", NamedTextColor.RED));
      } else {
         this.returnUtilityItem(player, UtilityItemType.ROAD_BARRIER);
         this.clearBarrierBlocks(display);
         display.remove();
         player.playSound(player.getLocation(), Sound.BLOCK_CHAIN_BREAK, 0.8F, 1.0F);
         player.sendMessage(Component.text("Barriera stradale rimossa.", NamedTextColor.GREEN));
      }
   }

   private List<Block> resolveBarrierHitbox(Block target, Player player) {
      BlockFace side = switch (player.getFacing()) {
         case NORTH, SOUTH -> BlockFace.EAST;
         case EAST, WEST -> BlockFace.SOUTH;
         default -> BlockFace.EAST;
      };
      List<Block> blocks = new ArrayList<>();

      for (int offset = -1; offset <= 1; offset++) {
         Block base = target.getRelative(side, offset);
         blocks.add(base);
         blocks.add(base.getRelative(BlockFace.UP));
      }

      return blocks;
   }

   private List<Block> resolveLine(Block target, Player player, int length) {
      BlockFace side = switch (player.getFacing()) {
         case NORTH, SOUTH -> BlockFace.EAST;
         case EAST, WEST -> BlockFace.SOUTH;
         default -> BlockFace.EAST;
      };
      int radius = length / 2;
      List<Block> blocks = new ArrayList<>();

      for (int offset = -radius; offset <= radius; offset++) {
         blocks.add(target.getRelative(side, offset));
      }

      return blocks;
   }

   private float snapBarrierYaw(Player player) {
      return switch (player.getFacing()) {
         case EAST, WEST -> 90.0F;
         default -> 0.0F;
      };
   }

   private String encodeBlocks(List<Block> blocks) {
      List<String> encoded = new ArrayList<>();

      for (Block block : blocks) {
         encoded.add(block.getWorld().getName() + "," + block.getX() + "," + block.getY() + "," + block.getZ());
      }

      return String.join(";", encoded);
   }

   private ItemDisplay findRoadBarrierForBlock(Block clicked) {
      for (Entity entity : clicked.getWorld().getNearbyEntities(clicked.getLocation().add(0.5, 0.5, 0.5), 4.0, 2.5, 4.0)) {
         if (entity instanceof ItemDisplay display && this.isRoadBarrier(display) && this.displayContainsBlock(display, clicked)) {
            return display;
         }
      }

      return null;
   }

   private boolean displayContainsBlock(ItemDisplay display, Block clicked) {
      String encoded = (String)display.getPersistentDataContainer().get(this.barrierBlocksKey, PersistentDataType.STRING);
      if (encoded != null && !encoded.isBlank()) {
         String target = clicked.getWorld().getName() + "," + clicked.getX() + "," + clicked.getY() + "," + clicked.getZ();

         for (String entry : encoded.split(";")) {
            if (entry.equals(target)) {
               return true;
            }
         }

         return false;
      } else {
         return false;
      }
   }

   private ItemDisplay findRoadEquipmentTarget(Player player, Block clickedBlock) {
      if (clickedBlock != null && clickedBlock.getType() == Material.BARRIER) {
         ItemDisplay barrier = this.findRoadBarrierForBlock(clickedBlock);
         if (barrier != null) {
            return barrier;
         }
      }

      RayTraceResult result = player.getWorld()
         .rayTraceEntities(
            player.getEyeLocation(),
            player.getEyeLocation().getDirection(),
            5.0,
            0.7,
            entity -> entity instanceof ItemDisplay display && (this.isRoadBarrier(display) || this.isSpikeStrip(display))
         );
      if (result != null && result.getHitEntity() instanceof ItemDisplay display) {
         return display;
      }

      ItemDisplay spikeStrip = this.findSpikeStripNear(player.getLocation());
      if (spikeStrip != null && player.getLocation().distanceSquared(spikeStrip.getLocation()) <= 9.0) {
         return spikeStrip;
      }
      return this.findRoadBarrierNear(player.getLocation());
   }

   private ItemDisplay findRoadBarrierNear(Location location) {
      if (location.getWorld() == null) {
         return null;
      }
      for (Entity entity : location.getWorld().getNearbyEntities(location, 3.0, 2.5, 3.0)) {
         if (entity instanceof ItemDisplay display && this.isRoadBarrier(display)) {
            return display;
         }
      }
      return null;
   }

   private void removeSpikeStripGroup(ItemDisplay display) {
      String id = (String)display.getPersistentDataContainer().get(this.spikeStripKey, PersistentDataType.STRING);
      if (id == null) {
         display.remove();
      } else {
         for (Entity entity : display.getWorld().getNearbyEntities(display.getLocation(), 4.0, 1.5, 4.0)) {
            if (entity instanceof ItemDisplay other && id.equals(other.getPersistentDataContainer().get(this.spikeStripKey, PersistentDataType.STRING))) {
               other.remove();
            }
         }
      }
   }

   private void returnUtilityItem(Player player, UtilityItemType type) {
      Map<Integer, ItemStack> leftovers = player.getInventory().addItem(new ItemStack[]{this.manager.createItem(type)});
      if (!leftovers.isEmpty()) {
         leftovers.values().forEach(item -> player.getWorld().dropItemNaturally(player.getLocation(), item));
         player.sendMessage(Component.text("Inventario pieno. L'oggetto e' caduto a terra vicino a te.", NamedTextColor.YELLOW));
      }
   }

   private void clearBarrierBlocks(ItemDisplay display) {
      String encoded = (String)display.getPersistentDataContainer().get(this.barrierBlocksKey, PersistentDataType.STRING);
      if (encoded != null && !encoded.isBlank()) {
         for (String entry : encoded.split(";")) {
            String[] parts = entry.split(",");
            if (parts.length == 4) {
               World world = Bukkit.getWorld(parts[0]);
               if (world != null) {
                  try {
                     Block block = world.getBlockAt(Integer.parseInt(parts[1]), Integer.parseInt(parts[2]), Integer.parseInt(parts[3]));
                     if (block.getType() == Material.BARRIER) {
                        block.setType(Material.AIR, false);
                     }
                  } catch (NumberFormatException var10) {
                  }
               }
            }
         }
      }
   }

   private void equipHelmetUtility(Player player, EquipmentSlot hand, ItemStack item) {
      ItemStack helmet = player.getInventory().getHelmet();
      if (helmet != null && !helmet.getType().isAir()) {
         player.sendActionBar(Component.text("Lo slot casco e' occupato.", NamedTextColor.RED));
      } else {
         ItemStack equipped = item.clone();
         equipped.setAmount(1);
         player.getInventory().setHelmet(equipped);
         this.consumeOne(player, hand);
         player.playSound(player.getLocation(), Sound.ITEM_ARMOR_EQUIP_LEATHER, 0.8F, 1.2F);
      }
   }

   private boolean equipChestUtility(Player player, EquipmentSlot hand, ItemStack item) {
      ItemStack chest = player.getInventory().getChestplate();
      if (chest != null && !chest.getType().isAir()) {
         player.sendActionBar(Component.text("Lo slot petto e' occupato.", NamedTextColor.RED));
         return false;
      } else {
         ItemStack equipped = item.clone();
         equipped.setAmount(1);
         player.getInventory().setChestplate(equipped);
         this.consumeOne(player, hand);
         player.playSound(player.getLocation(), Sound.ITEM_ARMOR_EQUIP_LEATHER, 0.8F, 0.9F);
         return true;
      }
   }

   private boolean hasChestEquipment(Player player) {
      ItemStack chest = player.getInventory().getChestplate();
      return chest != null && !chest.getType().isAir();
   }

   private void useFireAxe(Player player, ItemStack item, Block block) {
      Block targetBlock = this.normalizeFireAxeDoorBlock(block);
      if (!this.isFireAxeDoorTarget(targetBlock)) {
         player.sendActionBar(Component.text("L'ascia antincendio puo' forzare solo porte e botole di legno.", NamedTextColor.RED));
         return;
      }
      long now = System.currentTimeMillis();
      Long cooldownUntil = this.fireAxeCooldowns.get(player.getUniqueId());
      if (cooldownUntil != null && cooldownUntil > now) {
         player.sendActionBar(Component.text("Ascia antincendio in cooldown.", NamedTextColor.RED));
         return;
      }
      int uses = this.manager.getFireAxeUses(item);
      if (uses <= 0) {
         player.sendActionBar(Component.text("L'ascia antincendio e' rotta.", NamedTextColor.RED));
         return;
      }
      BlockData data = targetBlock.getBlockData();
      if (!(data instanceof Openable openable)) {
         return;
      }
      if (openable.isOpen()) {
         player.sendActionBar(Component.text("La porta e' gia' aperta.", NamedTextColor.YELLOW));
         return;
      }
      this.setOpenState(targetBlock, true);
      this.manager.setFireAxeUses(item, uses - 1);
      this.fireAxeCooldowns.put(player.getUniqueId(), now + settings.fireAxeCooldownMillis());
      player.playSound(player.getLocation(), Sound.ENTITY_ZOMBIE_ATTACK_WOODEN_DOOR, 0.9F, 0.8F);
      player.sendActionBar(Component.text("Porta forzata aperta.", NamedTextColor.YELLOW));
      Material openedType = targetBlock.getType();
      Location openedLocation = targetBlock.getLocation().clone();
      Bukkit.getScheduler().runTaskLater(this.module.getCore(), () -> {
         Block current = openedLocation.getBlock();
         if (current.getType() == openedType && current.getBlockData() instanceof Openable currentOpenable && currentOpenable.isOpen()) {
            this.setOpenState(current, false);
            current.getWorld().playSound(current.getLocation(), Sound.BLOCK_WOODEN_DOOR_CLOSE, 0.8F, 1.0F);
         }
      }, settings.fireAxeDoorOpenSeconds() * 20L);
   }

   private Block normalizeFireAxeDoorBlock(Block block) {
      if (block == null) {
         return null;
      }
      BlockData data = block.getBlockData();
      if (data instanceof Door door && door.getHalf() == Bisected.Half.TOP) {
         return block.getRelative(BlockFace.DOWN);
      }
      return block;
   }

   private void setOpenState(Block block, boolean open) {
      BlockData data = block.getBlockData();
      if (!(data instanceof Openable openable)) {
         return;
      }
      openable.setOpen(open);
      block.setBlockData(openable, false);

      if (data instanceof Door door) {
         Block otherHalf = door.getHalf() == Bisected.Half.TOP ? block.getRelative(BlockFace.DOWN) : block.getRelative(BlockFace.UP);
         if (otherHalf.getType() == block.getType() && otherHalf.getBlockData() instanceof Openable otherOpenable) {
            otherOpenable.setOpen(open);
            otherHalf.setBlockData(otherOpenable, false);
         }
      }
   }

   private boolean isFireAxeDoorTarget(Block block) {
      if (block == null || !(block.getBlockData() instanceof Openable)) {
         return false;
      }
      String name = block.getType().name();
      return (name.endsWith("_DOOR") || name.endsWith("_TRAPDOOR"))
            && !name.contains("IRON")
            && !name.contains("COPPER");
   }

   private UtilityItemListener.RestorableBlock capture(Block block) {
      return new UtilityItemListener.RestorableBlock(block.getLocation().clone(), block.getBlockData().clone());
   }

   private boolean sameBlock(Block a, Block b) {
      return a.getWorld().equals(b.getWorld()) && a.getX() == b.getX() && a.getY() == b.getY() && a.getZ() == b.getZ();
   }

   private void scheduleRestore(List<UtilityItemListener.RestorableBlock> blocks, int retry) {
      long delay = retry == 0 ? 1200L : 20L;
      Bukkit.getScheduler().runTaskLater(this.module.getCore(), () -> {
         if (retry < 10 && blocks.stream().anyMatch(this::hasPlayerInside)) {
            this.scheduleRestore(blocks, retry + 1);
         } else {
            for (UtilityItemListener.RestorableBlock block : blocks) {
               Block current = block.location.getBlock();
               if (current.getType().isAir()) {
                  current.setBlockData(block.blockData, false);
               }
            }
         }
      }, delay);
   }

   private boolean hasPlayerInside(UtilityItemListener.RestorableBlock block) {
      Location center = block.location.clone().add(0.5, 0.5, 0.5);
      return center.getWorld().getNearbyEntities(center, 0.6, 1.2, 0.6).stream().anyMatch(entity -> entity instanceof Player);
   }

   private boolean isFireAxeBreakable(Material material) {
      String name = material.name();
      return !name.contains("IRON") && !name.contains("COPPER")
         ? name.endsWith("_DOOR")
            || name.endsWith("_TRAPDOOR")
            || name.contains("_LOG")
            || name.contains("_WOOD")
            || name.contains("_PLANKS")
            || name.contains("_STEM")
            || name.contains("_HYPHAE")
            || name.endsWith("_SIGN")
            || name.endsWith("_HANGING_SIGN")
            || name.endsWith("_FENCE")
            || name.endsWith("_FENCE_GATE")
         : false;
   }

   private void recordForensicTrace(Player player, Block block) {
      if (this.shouldRecordForensicTrace(player) && this.isTraceable(block)) {
         String key = this.locationKey(block.getLocation());
         this.addForensicTrace(key, player, block.getLocation().clone(), block.getType().name());
      }
   }

   private void recordForensicTrace(Player player, Entity entity) {
      if (this.shouldRecordForensicTrace(player) && entity != null && entity.isValid()) {
         this.addForensicTrace(this.entityKey(entity), player, entity.getLocation().clone(), this.describeTraceEntity(entity));
      }
   }

   private void addForensicTrace(String key, Player player, Location location, String objectName) {
      List<UtilityItemListener.ForensicTrace> traces = new ArrayList<>(this.forensicTraces.getOrDefault(key, List.of()));
      traces.removeIf(trace -> trace.playerUuid.equals(player.getUniqueId()));
      traces.add(0, new UtilityItemListener.ForensicTrace(player.getUniqueId(), player.getName(), location, objectName, Instant.now()));
      if (traces.size() > 10) {
         traces = new ArrayList<>(traces.subList(0, 10));
      }

      this.forensicTraces.put(key, traces);
   }

   private boolean isTraceable(Block block) {
      return block != null && !block.getType().isAir();
   }

   private void revealForensicTraces(Player player) {
      int count = 0;
      DustOptions dust = new DustOptions(Color.fromRGB(150, 60, 255), 1.2F);
      double radiusSquared = TRACE_SCAN_RADIUS * TRACE_SCAN_RADIUS;
      Map<String, UtilityItemListener.ForensicTrace> nearestTraces = new HashMap<>();

      for (Map.Entry<String, List<UtilityItemListener.ForensicTrace>> entry : this.forensicTraces.entrySet()) {
         List<UtilityItemListener.ForensicTrace> traces = entry.getValue();
         if (!traces.isEmpty()) {
            UtilityItemListener.ForensicTrace trace = traces.get(0);
            if (this.sameWorld(player.getWorld(), trace.location.getWorld())
               && player.getLocation().distanceSquared(trace.location) <= radiusSquared) {
               nearestTraces.put(entry.getKey(), trace);
            }
         }
      }

      for (UtilityItemListener.ForensicTrace trace : nearestTraces.values()) {
         Location marker = trace.location.clone().add(0.5, 0.8, 0.5);
         player.getWorld().spawnParticle(Particle.DUST, marker, 16, 0.35, 0.35, 0.35, 0.0, dust);
         player.getWorld().spawnParticle(Particle.END_ROD, marker, 3, 0.15, 0.15, 0.15, 0.01);
         count++;
      }

      player.playSound(player.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.7F, 1.6F);
      if (count == 0) {
         player.sendActionBar(Component.text("La scansione UV non ha trovato tracce forensi vicine.", NamedTextColor.GRAY));
      } else {
         player.sendActionBar(Component.text("La scansione UV ha trovato " + count + " traccia/e.", NamedTextColor.LIGHT_PURPLE));
      }
   }

   private void showFingerprintSheet(Player player, Block block, ItemStack sheet) {
      if (block == null) {
         player.sendMessage(Component.text("Clic destro su un blocco per leggere le ultime impronte.", NamedTextColor.YELLOW));
      } else {
         this.showFingerprintSheet(player, this.locationKey(block.getLocation()), block.getType().name(), block.getLocation(), sheet);
      }
   }

   private void showFingerprintSheet(Player player, Entity entity, ItemStack sheet) {
      if (entity == null) {
         player.sendMessage(Component.text("Clic destro su un oggetto per leggere le ultime impronte.", NamedTextColor.YELLOW));
      } else {
         this.showFingerprintSheet(player, this.entityKey(entity), this.describeTraceEntity(entity), entity.getLocation(), sheet);
      }
   }

   private void showFingerprintSheet(Player player, String traceKey, String objectName, Location fallbackLocation, ItemStack sheet) {
      List<UtilityItemListener.ForensicTrace> traces = new ArrayList<>(this.forensicTraces.getOrDefault(traceKey, List.of()));
      if (traces.isEmpty() && fallbackLocation != null) {
         traces = this.findNearbyForensicTraces(fallbackLocation);
      }
      if (traces.isEmpty()) {
         player.sendMessage(Component.text("Nessuna impronta trovata su questo oggetto.", NamedTextColor.RED));
      } else {
         player.sendMessage(Component.text("Impronte su " + objectName, NamedTextColor.AQUA));
         int index = 1;

         for (UtilityItemListener.ForensicTrace trace : traces.stream().limit(10L).toList()) {
            player.sendMessage(Component.text(index++ + ". " + trace.playerName + " - " + TRACE_TIME_FORMAT.format(trace.createdAt), NamedTextColor.GRAY));
         }

         this.storeFingerprintScan(sheet, traces.get(0), objectName);
         player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 0.8F, 1.4F);
      }
   }

   private List<UtilityItemListener.ForensicTrace> findNearbyForensicTraces(Location location) {
      return this.forensicTraces.values()
         .stream()
         .flatMap(List::stream)
         .filter(trace -> this.sameWorld(location.getWorld(), trace.location.getWorld()))
         .filter(trace -> location.distanceSquared(trace.location) <= 4.0)
         .sorted(Comparator.comparing((UtilityItemListener.ForensicTrace trace) -> trace.createdAt).reversed())
         .limit(10L)
         .toList();
   }

   private void storeFingerprintScan(ItemStack sheet, UtilityItemListener.ForensicTrace trace, String objectName) {
      if (sheet == null || !sheet.hasItemMeta()) {
         return;
      }
      ItemMeta meta = sheet.getItemMeta();
      String stored = trace.playerUuid + "|" + trace.playerName + "|" + trace.createdAt + "|" + objectName;
      meta.getPersistentDataContainer().set(this.manager.getFingerprintDataKey(), PersistentDataType.STRING, stored);
      List<Component> lore = new ArrayList<>();
      if (meta.lore() != null) {
         lore.addAll(meta.lore().stream().filter(line -> {
            String plain = PlainTextComponentSerializer.plainText().serialize(line);
            return !plain.startsWith("Traccia salvata:");
         }).toList());
      }
      lore.add(Component.text("Traccia salvata: " + trace.playerName + " su " + objectName, NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false));
      meta.lore(lore);
      sheet.setItemMeta(meta);
   }

   private void openDuffel(Player player, EquipmentSlot hand, ItemStack item) {
      UtilityItemListener.DuffelHolder holder = new UtilityItemListener.DuffelHolder(hand);
      Inventory inventory = Bukkit.createInventory(holder, 9, Component.text("Borsone", NamedTextColor.DARK_GRAY));
      holder.inventory = inventory;
      ItemStack[] contents = this.loadDuffel(item);
      inventory.setContents(contents);
      player.openInventory(inventory);
   }

   private ItemStack[] loadDuffel(ItemStack item) {
      ItemStack[] empty = new ItemStack[9];
      if (item != null && item.hasItemMeta()) {
         String encoded = (String)item.getItemMeta().getPersistentDataContainer().get(this.manager.getDuffelContentsKey(), PersistentDataType.STRING);
         if (encoded != null && !encoded.isBlank()) {
            try {
               BukkitObjectInputStream input = new BukkitObjectInputStream(new ByteArrayInputStream(Base64.getDecoder().decode(encoded)));

               ItemStack[] var10;
               try {
                  ItemStack[] contents = new ItemStack[9];

                  for (int i = 0; i < contents.length; i++) {
                     contents[i] = (ItemStack)input.readObject();
                  }

                  var10 = contents;
               } catch (Throwable var8) {
                  try {
                     input.close();
                  } catch (Throwable var7) {
                     var8.addSuppressed(var7);
                  }

                  throw var8;
               }

               input.close();
               return var10;
            } catch (Exception e) {
               return empty;
            }
         } else {
            return empty;
         }
      } else {
         return empty;
      }
   }

   private void saveDuffel(ItemStack item, ItemStack[] contents) {
      try (ByteArrayOutputStream bytes = new ByteArrayOutputStream()) {
         BukkitObjectOutputStream output = new BukkitObjectOutputStream(bytes);

         try {
            for (int i = 0; i < 9; i++) {
               output.writeObject(i < contents.length ? contents[i] : null);
            }

            output.flush();
            ItemMeta meta = item.getItemMeta();
            meta.getPersistentDataContainer()
               .set(this.manager.getDuffelContentsKey(), PersistentDataType.STRING, Base64.getEncoder().encodeToString(bytes.toByteArray()));
            item.setItemMeta(meta);
         } catch (Throwable var9) {
            try {
               output.close();
            } catch (Throwable var8) {
               var9.addSuppressed(var8);
            }

            throw var9;
         }

         output.close();
      } catch (Exception e) {
         this.module.getCore().getLogger().warning("[OpenWeapons] Impossibile salvare il borsone: " + e.getMessage());
      }
   }

   private boolean isDuffelBag(ItemStack item) {
      return this.manager.isType(item, UtilityItemType.DUFFEL_BAG);
   }

   private void tickUtilityEffects() {
      long now = System.currentTimeMillis();
      this.puncturedVehicles.entrySet().removeIf(entry -> entry.getValue() <= now);

      for (Player player : Bukkit.getOnlinePlayers()) {
         if (this.manager.isWearing(player, UtilityItemType.NIGHT_VISION_GOGGLES)) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, 80, 0, false, false, true));
         }

         if (this.isBlindfolded(player)) {
            this.applyBlindfoldEffects(player);
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

   private void fill(Inventory inventory) {
      ItemStack filler = NexoUI.getFiller();

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

   private String formatLocation(Location location) {
      return String.format("%s %.0f %.0f %.0f", location.getWorld().getName(), location.getX(), location.getY(), location.getZ());
   }

   private String worldGuardRegion(Location location) {
      if (location == null || location.getWorld() == null || Bukkit.getPluginManager().getPlugin("WorldGuard") == null) {
         return "none";
      }
      try {
         ApplicableRegionSet regions = WorldGuard.getInstance()
               .getPlatform()
               .getRegionContainer()
               .createQuery()
               .getApplicableRegions(BukkitAdapter.adapt(location));
         return regions.getRegions().stream()
               .map(ProtectedRegion::getId)
               .sorted()
               .findFirst()
               .orElse("none");
      } catch (RuntimeException error) {
         return "unknown";
      }
   }

   private String locationKey(Location location) {
      return location.getWorld().getUID() + ":" + location.getBlockX() + ":" + location.getBlockY() + ":" + location.getBlockZ();
   }

   private String entityKey(Entity entity) {
      return "entity:" + entity.getUniqueId();
   }

   private String describeTraceEntity(Entity entity) {
      if (entity instanceof ItemDisplay display) {
         if (this.isRoadBarrier(display)) {
            return "ROAD_BARRIER";
         } else {
            return this.isSpikeStrip(display) ? "SPIKE_STRIP" : "ITEM_DISPLAY";
         }
      } else {
         return entity.getType().name();
      }
   }

   private boolean sameWorld(World a, World b) {
      return a != null && b != null && a.getUID().equals(b.getUID());
   }

   private boolean shouldRecordForensicTrace(Player player) {
      return player != null && !player.hasPermission("openrp.utility.forensics.no_trace");
   }

   private boolean canManageUtilityObjects(Player player) {
      return NextPermissions.hasAny(player,
            NextPermissions.Utility.ADMIN,
            NextPermissions.Build.TOOLS,
            NextPermissions.Police.ADMIN);
   }

   private void scanMetalItems(Player scanner, Player target) {
      List<String> found = new ArrayList<>();

      for (ItemStack item : target.getInventory().getContents()) {
         if (this.isMetalItem(item)) {
            found.add(this.itemName(item) + " x" + item.getAmount());
         }
      }

      for (ItemStack item : target.getInventory().getArmorContents()) {
         if (this.isMetalItem(item)) {
            found.add(this.itemName(item) + " x" + item.getAmount());
         }
      }

      ItemStack offhand = target.getInventory().getItemInOffHand();
      if (this.isMetalItem(offhand)) {
         found.add(this.itemName(offhand) + " x" + offhand.getAmount());
      }

      scanner.sendMessage(Component.text("Scansione metalli per " + target.getName(), NamedTextColor.AQUA));
      if (found.isEmpty()) {
         scanner.sendMessage(Component.text("Nessun oggetto metallico rilevato.", NamedTextColor.GRAY));
      } else {
         found.stream().limit(30L).forEach(line -> scanner.sendMessage(Component.text("- " + line, NamedTextColor.GRAY)));
      }

      scanner.playSound(scanner.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 0.8F, 1.4F);
   }

   private boolean isMetalItem(ItemStack item) {
      if (item != null && !item.getType().isAir()) {
         if (this.module.getWeaponRegistry().getWeapon(item) != null) {
            return true;
         }

         String name = item.getType().name();
         return name.contains("IRON")
            || name.contains("GOLD")
            || name.contains("COPPER")
            || name.contains("NETHERITE")
            || name.contains("CHAIN")
            || name.contains("ANVIL")
            || name.contains("SHEARS")
            || name.contains("BUCKET")
            || name.contains("MINECART")
            || name.contains("RAIL")
            || name.contains("HOPPER")
            || name.contains("CAULDRON")
            || name.contains("COMPASS")
            || name.contains("CLOCK");
      } else {
         return false;
      }
   }

   private String itemName(ItemStack item) {
      if (item == null || item.getType().isAir()) {
         return "Aria";
      } else {
         return item.hasItemMeta() && item.getItemMeta().hasDisplayName()
            ? PlainTextComponentSerializer.plainText().serialize(item.getItemMeta().displayName())
            : item.getType().name().toLowerCase(Locale.ROOT).replace('_', ' ');
      }
   }

   private void usePepperSpray(Player actor, Player target, ItemStack item) {
      int uses = this.manager.getPepperSprayUses(item);
      if (uses <= 0) {
         actor.sendActionBar(Component.text("Lo spray al peperoncino e' vuoto.", NamedTextColor.RED));
      } else {
         target.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 600, 0, false, false, false));
         target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 600, 1, false, false, false));
         this.manager.setPepperSprayUses(item, uses - 1);
         actor.playSound(actor.getLocation(), Sound.ENTITY_SPLASH_POTION_THROW, 0.8F, 1.5F);
         target.playSound(target.getLocation(), Sound.ENTITY_SPLASH_POTION_BREAK, 0.8F, 1.2F);
         actor.sendActionBar(Component.text("Usi spray al peperoncino: " + (uses - 1) + "/30", NamedTextColor.YELLOW));
         target.sendMessage(Component.text("Sei stato colpito dallo spray al peperoncino.", NamedTextColor.RED));
      }
   }

   private static class C4DetailHolder implements InventoryHolder {
      private final String chargeId;
      private Inventory inventory;

      private C4DetailHolder(String chargeId) {
         this.chargeId = chargeId;
      }

      public Inventory getInventory() {
         return this.inventory;
      }
   }

   private static class C4ListHolder implements InventoryHolder {
      private final Map<Integer, String> slotToCharge = new HashMap<>();
      private Inventory inventory;

      public Inventory getInventory() {
         return this.inventory;
      }
   }

   private static class DuffelHolder implements InventoryHolder {
      private final EquipmentSlot hand;
      private Inventory inventory;

      private DuffelHolder(EquipmentSlot hand) {
         this.hand = hand;
      }

      public Inventory getInventory() {
         return this.inventory;
      }
   }

   private static class ForensicTrace {
      private final UUID playerUuid;
      private final String playerName;
      private final Location location;
      private final String objectName;
      private final Instant createdAt;

      private ForensicTrace(UUID playerUuid, String playerName, Location location, String objectName, Instant createdAt) {
         this.playerUuid = playerUuid;
         this.playerName = playerName;
         this.location = location;
         this.objectName = objectName;
         this.createdAt = createdAt;
      }

      private String toSheetData() {
         return this.playerName + " (" + this.playerUuid.toString().substring(0, 8) + ") on " + this.objectName;
      }
   }

   private static class RestorableBlock {
      private final Location location;
      private final BlockData blockData;

      private RestorableBlock(Location location, BlockData blockData) {
         this.location = location;
         this.blockData = blockData;
      }
   }

   private static class TrackerHolder implements InventoryHolder {
      private final Map<Integer, String> slotToTracker = new HashMap<>();
      private Inventory inventory;

      public Inventory getInventory() {
         return this.inventory;
      }
   }

   private static class TrackerRecord {
      private final String id;
      private final UUID ownerUuid;
      private final String label;
      private final UUID entityUuid;
      private final Location staticLocation;

      private TrackerRecord(String id, UUID ownerUuid, String label, UUID entityUuid, Location staticLocation) {
         this.id = id;
         this.ownerUuid = ownerUuid;
         this.label = label;
         this.entityUuid = entityUuid;
         this.staticLocation = staticLocation;
      }

      private static UtilityItemListener.TrackerRecord forEntity(String id, UUID ownerUuid, String label, UUID entityUuid) {
         return new UtilityItemListener.TrackerRecord(id, ownerUuid, label, entityUuid, null);
      }

      private static UtilityItemListener.TrackerRecord forLocation(String id, UUID ownerUuid, String label, Location location) {
         return new UtilityItemListener.TrackerRecord(id, ownerUuid, label, null, location.clone());
      }

      private Location currentLocation() {
         if (this.entityUuid == null) {
            return this.staticLocation == null ? null : this.staticLocation.clone();
         }

         Entity entity = Bukkit.getEntity(this.entityUuid);
         return entity != null && entity.isValid() ? entity.getLocation().clone() : null;
      }
   }
}
