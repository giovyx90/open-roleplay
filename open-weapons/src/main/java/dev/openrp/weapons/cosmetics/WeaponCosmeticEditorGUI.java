package dev.openrp.weapons.cosmetics;

import it.meridian.core.utils.ItemBuilder;
import dev.openrp.weapons.module.WeaponsModule;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class WeaponCosmeticEditorGUI implements Listener {
    private static final String TITLE = "Weapon Skin Editor";
    private static final int SIZE = 54;
    private static final int WEAPON_SLOT = 22;
    private static final int RESET_SLOT = 49;
    private static final int CLOSE_SLOT = 53;
    private static final int[] SKIN_SLOTS = {10, 11, 12};
    private static final int[] LED_SLOTS = {28, 29, 30, 31, 32, 33};
    private static final int[] COLOR_SLOTS = {37, 38, 39, 40, 41, 42};
    private static final int CUSTOM_COLOR_SLOT = 43;

    private final WeaponsModule module;
    private final WeaponCosmeticManager manager;
    private final NamespacedKey actionKey;
    private final NamespacedKey valueKey;
    private final Map<UUID, PendingCustomColor> pendingCustomColors = new java.util.concurrent.ConcurrentHashMap<>();

    public WeaponCosmeticEditorGUI(WeaponsModule module, WeaponCosmeticManager manager) {
        this.module = module;
        this.manager = manager;
        this.actionKey = new NamespacedKey(module.getCore(), "weapon_cosmetic_editor_action");
        this.valueKey = new NamespacedKey(module.getCore(), "weapon_cosmetic_editor_value");
    }

    public void open(Player player) {
        EditorHolder holder = new EditorHolder(player.getUniqueId());
        Inventory inv = Bukkit.createInventory(holder, SIZE, Component.text(TITLE, NamedTextColor.DARK_GRAY, TextDecoration.BOLD));
        holder.inventory = inv;
        fill(inv);

        ItemStack held = player.getInventory().getItemInMainHand();
        if (manager.isSupportedWeapon(held)) {
            inv.setItem(WEAPON_SLOT, held.clone());
            player.getInventory().setItemInMainHand(new ItemStack(Material.AIR));
        }
        update(inv);
        player.openInventory(inv);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getView().getTopInventory().getHolder() instanceof EditorHolder holder)) {
            return;
        }
        if (!(event.getWhoClicked() instanceof Player player) || !player.getUniqueId().equals(holder.playerUuid())) {
            event.setCancelled(true);
            return;
        }
        if (event.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY
                || event.getAction() == InventoryAction.COLLECT_TO_CURSOR) {
            event.setCancelled(true);
            return;
        }
        boolean topClick = event.getRawSlot() >= 0 && event.getRawSlot() < event.getView().getTopInventory().getSize();
        if (!topClick) {
            return;
        }
        Inventory inv = event.getView().getTopInventory();
        int slot = event.getRawSlot();
        if (slot == WEAPON_SLOT) {
            ItemStack incoming = event.getHotbarButton() >= 0
                    ? player.getInventory().getItem(event.getHotbarButton())
                    : event.getCursor();
            if (!isEmpty(incoming) && !manager.isSupportedWeapon(incoming)) {
                event.setCancelled(true);
                player.sendActionBar(Component.text("Insert a supported weapon.", NamedTextColor.RED));
                return;
            }
            Bukkit.getScheduler().runTask(module.getCore(), () -> update(inv));
            return;
        }
        event.setCancelled(true);
        if (slot == CLOSE_SLOT) {
            player.closeInventory();
            return;
        }
        if (slot == RESET_SLOT) {
            applySelection(player, inv, WeaponCosmeticManager.NONE, WeaponCosmeticManager.NONE, null);
            return;
        }
        ItemStack clicked = event.getCurrentItem();
        Button button = button(clicked);
        if (button == null) {
            return;
        }
        ItemStack weapon = inv.getItem(WEAPON_SLOT);
        if (!manager.isSupportedWeapon(weapon)) {
            player.sendActionBar(Component.text("Insert a supported weapon first.", NamedTextColor.RED));
            return;
        }
        WeaponCosmeticManager.CosmeticSelection current = manager.getSelection(weapon);
        switch (button.action()) {
            case WeaponCosmeticManager.TYPE_SKIN -> applySelection(player, inv, button.value(),
                    WeaponCosmeticManager.NONE, null);
            case WeaponCosmeticManager.TYPE_LED -> {
                if (!manager.supportsCosmeticType(weapon, WeaponCosmeticManager.TYPE_LED)) {
                    player.sendActionBar(Component.text("This weapon does not support LEDs.", NamedTextColor.RED));
                    return;
                }
                if (!current.skinId().equals(WeaponCosmeticManager.NONE)) {
                    player.sendActionBar(Component.text("LEDs work only with standard or colored finishes.", NamedTextColor.YELLOW));
                    return;
                }
                applySelection(player, inv, WeaponCosmeticManager.NONE, button.value(), current.colorRgb());
            }
            case WeaponCosmeticManager.TYPE_COLOR -> {
                if (!manager.supportsCosmeticType(weapon, WeaponCosmeticManager.TYPE_COLOR)) {
                    player.sendActionBar(Component.text("This weapon does not support colors.", NamedTextColor.RED));
                    return;
                }
                applySelection(player, inv, WeaponCosmeticManager.NONE,
                        current.ledId(), button.value().equals(WeaponCosmeticManager.NONE)
                                ? null
                                : manager.getColorOption(button.value()).rgb());
            }
            case "custom-color" -> beginCustomColorPrompt(player, inv);
            default -> {
            }
        }
    }

    @EventHandler
    public void onCustomColorChat(AsyncPlayerChatEvent event) {
        PendingCustomColor pending = pendingCustomColors.remove(event.getPlayer().getUniqueId());
        if (pending == null) {
            return;
        }
        event.setCancelled(true);
        String message = event.getMessage() == null ? "" : event.getMessage().trim();
        Player player = event.getPlayer();
        if (message.equalsIgnoreCase("cancel") || message.equalsIgnoreCase("annulla")) {
            Bukkit.getScheduler().runTask(module.getCore(), () -> {
                returnPendingWeapon(player, pending);
                player.sendMessage(Component.text("Custom color cancelled.", NamedTextColor.YELLOW));
                open(player);
            });
            return;
        }
        Integer rgb = WeaponCosmeticManager.parseColorRgb(message);
        Bukkit.getScheduler().runTask(module.getCore(), () -> {
            ItemStack weapon = pending.weapon();
            if (rgb == null) {
                returnPendingWeapon(player, pending);
                player.sendMessage(Component.text("Use a valid #RRGGBB color.", NamedTextColor.RED));
                open(player);
                return;
            }
            if (!manager.applySelection(weapon, WeaponCosmeticManager.NONE, pending.ledId(), rgb)) {
                returnPendingWeapon(player, pending);
                player.sendMessage(Component.text("This cosmetic combination is not available.", NamedTextColor.RED));
                open(player);
                return;
            }
            module.refreshWeaponVisual(weapon);
            returnPendingWeapon(player, new PendingCustomColor(weapon, pending.ledId()));
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.6f, 1.4f);
            player.sendMessage(Component.text("Weapon custom color applied: " + WeaponCosmeticManager.formatHex(rgb),
                    NamedTextColor.GREEN));
            open(player);
        });
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (!(event.getView().getTopInventory().getHolder() instanceof EditorHolder)) {
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
        if (!(event.getInventory().getHolder() instanceof EditorHolder)) {
            return;
        }
        if (event.getPlayer() instanceof Player player) {
            returnWeapon(player, event.getInventory());
        }
    }

    private void applySelection(Player player, Inventory inv, String skinId, String ledId, Integer colorRgb) {
        ItemStack weapon = inv.getItem(WEAPON_SLOT);
        if (!manager.applySelection(weapon, skinId, ledId, colorRgb)) {
            player.sendActionBar(Component.text("This cosmetic combination is not available.", NamedTextColor.RED));
            return;
        }
        module.refreshWeaponVisual(weapon);
        inv.setItem(WEAPON_SLOT, weapon);
        update(inv);
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.6f, 1.4f);
        player.sendActionBar(Component.text("Weapon cosmetics updated.", NamedTextColor.GREEN));
    }

    private void fill(Inventory inv) {
        ItemStack filler = ItemBuilder.filler(Material.BLACK_STAINED_GLASS_PANE);
        for (int i = 0; i < SIZE; i++) {
            inv.setItem(i, filler);
        }
        inv.setItem(WEAPON_SLOT, null);
    }

    private void update(Inventory inv) {
        ItemStack weapon = inv.getItem(WEAPON_SLOT);
        WeaponCosmeticManager.CosmeticSelection selection = manager.getSelection(weapon);
        String weaponId = selection.weaponId();

        inv.setItem(4, label(Material.CROSSBOW, "Weapon", NamedTextColor.AQUA,
                List.of(manager.isSupportedWeapon(weapon) ? "Editing " + weaponId : "Place a supported weapon here.")));
        inv.setItem(WEAPON_SLOT, weapon);
        boolean supportsLed = manager.supportsCosmeticType(weapon, WeaponCosmeticManager.TYPE_LED);
        boolean supportsColor = manager.supportsCosmeticType(weapon, WeaponCosmeticManager.TYPE_COLOR);

        List<String> skinIds = new ArrayList<>();
        skinIds.add(WeaponCosmeticManager.NONE);
        if (!weaponId.equals(WeaponCosmeticManager.NONE)) {
            skinIds.addAll(manager.getSkinIds(weaponId));
        }
        for (int i = 0; i < SKIN_SLOTS.length; i++) {
            String skin = i < skinIds.size() ? skinIds.get(i) : null;
            inv.setItem(SKIN_SLOTS[i], skin == null ? ItemBuilder.filler(Material.GRAY_STAINED_GLASS_PANE)
                    : weaponPreviewButton(weapon, WeaponCosmeticManager.TYPE_SKIN, skin,
                    selectionFor(weapon, skin, WeaponCosmeticManager.NONE, null),
                    displaySkin(weaponId, skin), selected(selection.skinId(), skin), List.of("Fixed finish.")));
        }

        if (supportsLed) {
            List<String> ledIds = new ArrayList<>();
            ledIds.add(WeaponCosmeticManager.NONE);
            ledIds.addAll(manager.getLedIds());
            for (int i = 0; i < LED_SLOTS.length; i++) {
                String led = i < ledIds.size() ? ledIds.get(i) : null;
                boolean disabled = !selection.skinId().equals(WeaponCosmeticManager.NONE);
                inv.setItem(LED_SLOTS[i], led == null ? ItemBuilder.filler(Material.GRAY_STAINED_GLASS_PANE)
                        : weaponPreviewButton(weapon, WeaponCosmeticManager.TYPE_LED, led,
                        selectionFor(weapon, WeaponCosmeticManager.NONE, led, selection.colorRgb()),
                        led.equals(WeaponCosmeticManager.NONE) ? "No LED" : manager.getLedOption(led).displayName(),
                        disabled ? "Disabled by skin" : selected(selection.ledId(), led),
                        List.of("Disabled while a fixed skin is active.")));
            }
        } else {
            for (int slot : LED_SLOTS) {
                inv.setItem(slot, ItemBuilder.filler(Material.GRAY_STAINED_GLASS_PANE));
            }
        }

        if (supportsColor) {
            List<String> colorIds = new ArrayList<>();
            colorIds.add(WeaponCosmeticManager.NONE);
            colorIds.addAll(manager.getColorIds());
            for (int i = 0; i < COLOR_SLOTS.length; i++) {
                String color = i < colorIds.size() ? colorIds.get(i) : null;
                inv.setItem(COLOR_SLOTS[i], color == null ? ItemBuilder.filler(Material.GRAY_STAINED_GLASS_PANE)
                        : colorButton(color,
                        color.equals(WeaponCosmeticManager.NONE) ? "No Color" : manager.getColorOption(color).displayName(),
                        selected(selection.colorHex(), colorValue(color)), List.of("Works with standard/LED finishes.")));
            }
            inv.setItem(CUSTOM_COLOR_SLOT, customColorButton(selection.colorHex()));
        } else {
            for (int slot : COLOR_SLOTS) {
                inv.setItem(slot, ItemBuilder.filler(Material.GRAY_STAINED_GLASS_PANE));
            }
            inv.setItem(CUSTOM_COLOR_SLOT, ItemBuilder.filler(Material.GRAY_STAINED_GLASS_PANE));
        }

        inv.setItem(RESET_SLOT, label(Material.BARRIER, "Reset", NamedTextColor.RED, List.of("Clear skin, LED and color.")));
        inv.setItem(CLOSE_SLOT, label(Material.OAK_DOOR, "Close", NamedTextColor.GRAY, List.of("Return the weapon.")));
    }

    private String displaySkin(String weaponId, String skinId) {
        if (skinId.equals(WeaponCosmeticManager.NONE)) {
            return "Standard";
        }
        WeaponCosmeticManager.SkinOption option = manager.getSkinOption(weaponId, skinId);
        return option == null ? skinId : option.displayName();
    }

    private String selected(String current, String value) {
        return WeaponCosmeticManager.normalize(current).equals(WeaponCosmeticManager.normalize(value)) ? "Selected" : "Click to apply";
    }

    private WeaponCosmeticManager.CosmeticSelection selectionFor(ItemStack base, String skinId, String ledId, Integer colorRgb) {
        String weaponId = manager.getSelection(base).weaponId();
        return new WeaponCosmeticManager.CosmeticSelection(weaponId, skinId, ledId, colorRgb);
    }

    private String colorValue(String colorId) {
        if (WeaponCosmeticManager.normalize(colorId).equals(WeaponCosmeticManager.NONE)) {
            return WeaponCosmeticManager.NONE;
        }
        WeaponCosmeticManager.CosmeticOption option = manager.getColorOption(colorId);
        return option == null || option.rgb() == null ? colorId : WeaponCosmeticManager.formatHex(option.rgb());
    }

    private ItemStack button(Material material, String action, String value, String name, String status, List<String> lore) {
        ItemStack item = label(material, name, status.equals("Selected") ? NamedTextColor.GREEN : NamedTextColor.YELLOW,
                combine(status, lore));
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.getPersistentDataContainer().set(actionKey, PersistentDataType.STRING, action);
            meta.getPersistentDataContainer().set(valueKey, PersistentDataType.STRING, value);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack weaponPreviewButton(ItemStack weapon, String action, String value,
                                          WeaponCosmeticManager.CosmeticSelection previewSelection,
                                          String name, String status, List<String> lore) {
        ItemStack item = manager.isSupportedWeapon(weapon) ? weapon.clone() : new ItemStack(Material.CROSSBOW);
        manager.applySelection(item, previewSelection);
        module.refreshWeaponVisual(item);
        NamedTextColor fallbackColor = status.startsWith("Selected") ? NamedTextColor.GREEN : NamedTextColor.YELLOW;
        Component displayName = previewItemName(item, action, value, name, fallbackColor);
        return decorateButton(item, action, value, displayName, combine(status, lore));
    }

    private ItemStack colorButton(String color, String name, String status, List<String> lore) {
        if (WeaponCosmeticManager.normalize(color).equals(WeaponCosmeticManager.NONE)) {
            return button(Material.POTION, WeaponCosmeticManager.TYPE_COLOR, color, name, status, lore);
        }
        ItemStack item = manager.createToken(WeaponCosmeticManager.TYPE_COLOR, colorValue(color), 1);
        if (item == null) {
            item = new ItemStack(Material.PAPER);
        }
        return decorateButton(item, WeaponCosmeticManager.TYPE_COLOR, color, name,
                status.startsWith("Selected") ? NamedTextColor.GREEN : NamedTextColor.YELLOW,
                combine(status, lore));
    }

    private ItemStack customColorButton(String currentColor) {
        String preview = WeaponCosmeticManager.parseColorRgb(currentColor) == null ? "#FF66CC" : currentColor;
        ItemStack item = manager.createToken(WeaponCosmeticManager.TYPE_COLOR, preview, 1);
        if (item == null) {
            item = new ItemStack(Material.NAME_TAG);
        }
        return decorateButton(item, "custom-color", preview, "Custom Color", NamedTextColor.AQUA,
                List.of("Click to type any #RRGGBB color.", "Current: " + currentColor));
    }

    private ItemStack decorateButton(ItemStack item, String action, String value, String name,
                                     NamedTextColor color, List<String> lore) {
        return decorateButton(item, action, value,
                Component.text(name, color).decoration(TextDecoration.ITALIC, false), lore);
    }

    private ItemStack decorateButton(ItemStack item, String action, String value, Component name, List<String> lore) {
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(name.decoration(TextDecoration.ITALIC, false));
            meta.lore(lore.stream()
                    .map(line -> (Component) Component.text(line, NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false))
                    .toList());
            meta.getPersistentDataContainer().set(actionKey, PersistentDataType.STRING, action);
            meta.getPersistentDataContainer().set(valueKey, PersistentDataType.STRING, value);
            item.setItemMeta(meta);
        }
        return item;
    }

    private Component previewItemName(ItemStack item, String action, String value, String fallback, NamedTextColor fallbackColor) {
        if (action.equals(WeaponCosmeticManager.TYPE_SKIN)
                && !WeaponCosmeticManager.normalize(value).equals(WeaponCosmeticManager.NONE)
                && item != null
                && item.hasItemMeta()
                && item.getItemMeta().displayName() != null) {
            return item.getItemMeta().displayName();
        }
        return Component.text(fallback, fallbackColor).decoration(TextDecoration.ITALIC, false);
    }

    private List<String> combine(String status, List<String> lore) {
        List<String> lines = new ArrayList<>();
        lines.add(status);
        lines.addAll(lore);
        return lines;
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

    private Button button(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return null;
        }
        String action = item.getItemMeta().getPersistentDataContainer().get(actionKey, PersistentDataType.STRING);
        String value = item.getItemMeta().getPersistentDataContainer().get(valueKey, PersistentDataType.STRING);
        return action == null || value == null ? null : new Button(action, value);
    }

    private void returnWeapon(Player player, Inventory inv) {
        ItemStack weapon = inv.getItem(WEAPON_SLOT);
        if (isEmpty(weapon)) {
            return;
        }
        inv.setItem(WEAPON_SLOT, null);
        Map<Integer, ItemStack> leftovers = player.getInventory().addItem(weapon);
        leftovers.values().forEach(leftover -> player.getWorld().dropItemNaturally(player.getLocation(), leftover));
    }

    private void beginCustomColorPrompt(Player player, Inventory inv) {
        ItemStack weapon = inv.getItem(WEAPON_SLOT);
        if (!manager.isSupportedWeapon(weapon)) {
            player.sendActionBar(Component.text("Insert a supported weapon first.", NamedTextColor.RED));
            return;
        }
        if (!manager.supportsCosmeticType(weapon, WeaponCosmeticManager.TYPE_COLOR)) {
            player.sendActionBar(Component.text("This weapon does not support colors.", NamedTextColor.RED));
            return;
        }
        WeaponCosmeticManager.CosmeticSelection current = manager.getSelection(weapon);
        inv.setItem(WEAPON_SLOT, null);
        pendingCustomColors.put(player.getUniqueId(), new PendingCustomColor(weapon.clone(), current.ledId()));
        player.closeInventory();
        player.sendMessage(Component.text("Type a custom color in chat, e.g. #FF66CC. Type cancel to abort.", NamedTextColor.AQUA));
    }

    private void returnPendingWeapon(Player player, PendingCustomColor pending) {
        Map<Integer, ItemStack> leftovers = player.getInventory().addItem(pending.weapon());
        leftovers.values().forEach(leftover -> player.getWorld().dropItemNaturally(player.getLocation(), leftover));
    }

    private boolean isEmpty(ItemStack item) {
        return item == null || item.getType() == Material.AIR || item.getAmount() <= 0;
    }

    private record Button(String action, String value) {
    }

    private record PendingCustomColor(ItemStack weapon, String ledId) {
    }

    private static final class EditorHolder implements InventoryHolder {
        private final UUID playerUuid;
        private Inventory inventory;

        private EditorHolder(UUID playerUuid) {
            this.playerUuid = playerUuid;
        }

        private UUID playerUuid() {
            return playerUuid;
        }

        @Override
        public Inventory getInventory() {
            return inventory;
        }
    }
}
