package dev.openrp.weapons.cosmetics;

import it.meridian.core.utils.ItemBuilder;
import dev.openrp.weapons.module.WeaponsModule;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public class WeaponCosmeticWorkbenchGUI implements Listener {
    private static final String TITLE = "Weapon Cosmetic Bench";
    private static final int SIZE = 54;
    private static final int WEAPON_SLOT = 20;
    private static final int LED_SLOT = 22;
    private static final int COLOR_SLOT = 24;
    private static final int APPLY_SLOT = 40;
    private static final int CLEAR_LED_SLOT = 45;
    private static final int CLEAR_COLOR_SLOT = 46;
    private static final int CLEAR_ALL_SLOT = 47;
    private static final int CLOSE_SLOT = 53;

    private final WeaponsModule module;
    private final WeaponCosmeticManager manager;

    public WeaponCosmeticWorkbenchGUI(WeaponsModule module, WeaponCosmeticManager manager) {
        this.module = module;
        this.manager = manager;
    }

    public void open(Player player) {
        CosmeticHolder holder = new CosmeticHolder(player.getUniqueId());
        Inventory inv = Bukkit.createInventory(holder, SIZE, Component.text(TITLE, NamedTextColor.DARK_GRAY, TextDecoration.BOLD));
        holder.setInventory(inv);

        ItemStack filler = ItemBuilder.filler(Material.BLACK_STAINED_GLASS_PANE);
        for (int i = 0; i < SIZE; i++) {
            inv.setItem(i, filler);
        }
        inv.setItem(WEAPON_SLOT, null);
        inv.setItem(LED_SLOT, null);
        inv.setItem(COLOR_SLOT, null);
        updateControlItems(inv);
        player.openInventory(inv);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getView().getTopInventory().getHolder() instanceof CosmeticHolder)) {
            return;
        }
        if (!(event.getWhoClicked() instanceof Player player)) {
            event.setCancelled(true);
            return;
        }

        if (event.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY
                || event.getAction() == InventoryAction.COLLECT_TO_CURSOR) {
            event.setCancelled(true);
            return;
        }

        boolean topInventoryClick = event.getRawSlot() >= 0 && event.getRawSlot() < event.getView().getTopInventory().getSize();
        if (!topInventoryClick) {
            return;
        }

        Inventory inv = event.getView().getTopInventory();
        int slot = event.getRawSlot();
        if (slot == APPLY_SLOT) {
            event.setCancelled(true);
            handleApply(player, inv);
            return;
        }
        if (slot == CLEAR_LED_SLOT || slot == CLEAR_COLOR_SLOT || slot == CLEAR_ALL_SLOT) {
            event.setCancelled(true);
            handleClear(player, inv, slot);
            return;
        }
        if (slot == CLOSE_SLOT) {
            event.setCancelled(true);
            player.closeInventory();
            return;
        }
        if (slot == WEAPON_SLOT || slot == LED_SLOT || slot == COLOR_SLOT) {
            if (!isValidInputClick(event, slot)) {
                event.setCancelled(true);
            }
            scheduleControlUpdate(inv);
            return;
        }

        event.setCancelled(true);
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (!(event.getView().getTopInventory().getHolder() instanceof CosmeticHolder)) {
            return;
        }
        for (int rawSlot : event.getRawSlots()) {
            if (rawSlot >= 0 && rawSlot < event.getView().getTopInventory().getSize()) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (!(event.getInventory().getHolder() instanceof CosmeticHolder)) {
            return;
        }
        if (!(event.getPlayer() instanceof Player player)) {
            return;
        }
        returnSlot(player, event.getInventory(), WEAPON_SLOT);
        returnSlot(player, event.getInventory(), LED_SLOT);
        returnSlot(player, event.getInventory(), COLOR_SLOT);
    }

    private void handleApply(Player player, Inventory inv) {
        ItemStack weapon = inv.getItem(WEAPON_SLOT);
        if (!manager.isSupportedWeapon(weapon)) {
            player.sendActionBar(Component.text("Insert a supported weapon in the weapon slot.", NamedTextColor.RED));
            return;
        }

        boolean changed = false;
        ItemStack colorToken = inv.getItem(COLOR_SLOT);
        WeaponCosmeticManager.TokenData color = manager.getToken(colorToken);
        if (color != null && color.type().equals(WeaponCosmeticManager.TYPE_COLOR)) {
            boolean applied = manager.applyCosmetic(weapon, color.type(), color.id());
            changed |= applied;
            if (applied) {
                manager.consumeOne(colorToken);
                inv.setItem(COLOR_SLOT, colorToken.getAmount() <= 0 ? null : colorToken);
            }
        }

        ItemStack ledToken = inv.getItem(LED_SLOT);
        WeaponCosmeticManager.TokenData led = manager.getToken(ledToken);
        if (led != null && led.type().equals(WeaponCosmeticManager.TYPE_LED)) {
            boolean applied = manager.applyCosmetic(weapon, led.type(), led.id());
            changed |= applied;
            if (applied) {
                manager.consumeOne(ledToken);
                inv.setItem(LED_SLOT, ledToken.getAmount() <= 0 ? null : ledToken);
            }
        }

        if (!changed) {
            player.sendActionBar(Component.text("Insert a LED token, a color token, or both.", NamedTextColor.YELLOW));
            return;
        }

        module.refreshWeaponVisual(weapon);
        inv.setItem(WEAPON_SLOT, weapon);
        updateControlItems(inv);
        player.updateInventory();
        player.playSound(player.getLocation(), Sound.BLOCK_SMITHING_TABLE_USE, 0.8f, 1.25f);
        player.sendActionBar(Component.text("Weapon cosmetic applied.", NamedTextColor.GREEN));
    }

    private void handleClear(Player player, Inventory inv, int slot) {
        ItemStack weapon = inv.getItem(WEAPON_SLOT);
        String clearType = slot == CLEAR_LED_SLOT
                ? WeaponCosmeticManager.TYPE_LED
                : slot == CLEAR_COLOR_SLOT ? WeaponCosmeticManager.TYPE_COLOR : "all";
        if (!manager.clearCosmetic(weapon, clearType)) {
            player.sendActionBar(Component.text("Insert a supported weapon before clearing cosmetics.", NamedTextColor.RED));
            return;
        }
        module.refreshWeaponVisual(weapon);
        inv.setItem(WEAPON_SLOT, weapon);
        updateControlItems(inv);
        player.updateInventory();
        player.playSound(player.getLocation(), Sound.ITEM_ARMOR_EQUIP_IRON, 0.6f, 1.35f);
        player.sendActionBar(Component.text("Weapon cosmetic cleared.", NamedTextColor.GREEN));
    }

    private boolean isValidInputClick(InventoryClickEvent event, int slot) {
        if (event.getHotbarButton() >= 0) {
            ItemStack hotbarItem = event.getWhoClicked().getInventory().getItem(event.getHotbarButton());
            return isEmpty(hotbarItem) || isAllowedForSlot(slot, hotbarItem);
        }
        ItemStack cursor = event.getCursor();
        return isEmpty(cursor) || isAllowedForSlot(slot, cursor);
    }

    private boolean isAllowedForSlot(int slot, ItemStack item) {
        if (slot == WEAPON_SLOT) {
            return manager.isSupportedWeapon(item);
        }
        WeaponCosmeticManager.TokenData token = manager.getToken(item);
        if (slot == LED_SLOT) {
            return token != null && token.type().equals(WeaponCosmeticManager.TYPE_LED);
        }
        if (slot == COLOR_SLOT) {
            return token != null && token.type().equals(WeaponCosmeticManager.TYPE_COLOR);
        }
        return false;
    }

    private void updateControlItems(Inventory inv) {
        inv.setItem(10, label(Material.CROSSBOW, "Weapon", NamedTextColor.AQUA,
                List.of("Supported weapon to customize.")));
        inv.setItem(12, label(Material.REDSTONE_TORCH, "LED Token", NamedTextColor.YELLOW,
                List.of("USA, Italy, France, Anime or Pacman LED.")));
        inv.setItem(14, label(Material.PAPER, "Color Token", NamedTextColor.LIGHT_PURPLE,
                List.of("Any #RRGGBB color token.")));
        inv.setItem(APPLY_SLOT, label(Material.LIME_CONCRETE, "Apply", NamedTextColor.GREEN,
                List.of("Consumes one inserted token of each type.")));
        inv.setItem(CLEAR_LED_SLOT, label(Material.REDSTONE_TORCH, "Clear LED", NamedTextColor.RED,
                List.of("Remove only the LED.")));
        inv.setItem(CLEAR_COLOR_SLOT, label(Material.CAULDRON, "Clear Color", NamedTextColor.RED,
                List.of("Remove only the color tint.")));
        inv.setItem(CLEAR_ALL_SLOT, label(Material.BARRIER, "Clear All", NamedTextColor.RED,
                List.of("Remove LED, color tint and skin.")));
        inv.setItem(CLOSE_SLOT, label(Material.OAK_DOOR, "Close", NamedTextColor.GRAY, List.of("Return inserted items.")));
    }

    private ItemStack label(Material material, String name, NamedTextColor color, List<String> lore) {
        List<Component> lines = lore.stream()
                .map(line -> (Component) Component.text(line, NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false))
                .toList();
        return new ItemBuilder(material)
                .name(Component.text(name, color).decoration(TextDecoration.ITALIC, false))
                .lore(lines)
                .build();
    }

    private void scheduleControlUpdate(Inventory inv) {
        Bukkit.getScheduler().runTask(module.getCore(), () -> updateControlItems(inv));
    }

    private void returnSlot(Player player, Inventory inv, int slot) {
        ItemStack item = inv.getItem(slot);
        if (isEmpty(item)) {
            return;
        }
        inv.setItem(slot, null);
        Map<Integer, ItemStack> leftovers = player.getInventory().addItem(item);
        leftovers.values().forEach(leftover -> player.getWorld().dropItemNaturally(player.getLocation(), leftover));
    }

    private boolean isEmpty(ItemStack item) {
        return item == null || item.getType().isAir();
    }

    private static final class CosmeticHolder implements InventoryHolder {
        private final UUID ownerId;
        private Inventory inventory;

        private CosmeticHolder(UUID ownerId) {
            this.ownerId = ownerId;
        }

        @Override
        public Inventory getInventory() {
            return inventory;
        }

        private void setInventory(Inventory inventory) {
            this.inventory = inventory;
        }

        @SuppressWarnings("unused")
        private UUID getOwnerId() {
            return ownerId;
        }
    }
}
