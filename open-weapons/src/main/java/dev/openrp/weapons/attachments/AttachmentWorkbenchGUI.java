package dev.openrp.weapons.attachments;

import it.meridian.core.utils.ItemBuilder;
import dev.openrp.weapons.model.WeaponDefinition;
import dev.openrp.weapons.module.WeaponsModule;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Location;
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
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public class AttachmentWorkbenchGUI implements Listener {
    private static final String TITLE = "Weapon Workbench";
    private static final int SIZE = 27;
    private static final int WEAPON_SLOT = 11;
    private static final int CONFIRM_SLOT = 13;
    private static final int ATTACHMENT_SLOT = 15;
    private static final int REMOVE_SLOT = 22;

    private final WeaponsModule module;

    public AttachmentWorkbenchGUI(WeaponsModule module) {
        this.module = module;
    }

    public void open(Player player) {
        WorkbenchHolder holder = new WorkbenchHolder(player.getUniqueId());
        Inventory inv = Bukkit.createInventory(holder, SIZE, Component.text(TITLE, NamedTextColor.DARK_GRAY, TextDecoration.BOLD));
        holder.setInventory(inv);

        ItemStack filler = ItemBuilder.filler();
        for (int i = 0; i < SIZE; i++) {
            inv.setItem(i, filler);
        }
        inv.setItem(WEAPON_SLOT, null);
        inv.setItem(ATTACHMENT_SLOT, null);
        updateControlItems(inv);
        player.openInventory(inv);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getView().getTopInventory().getHolder() instanceof WorkbenchHolder holder)) {
            return;
        }
        if (!(event.getWhoClicked() instanceof Player player)) {
            event.setCancelled(true);
            return;
        }

        if (holder.isInstalling()) {
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

        int slot = event.getRawSlot();
        if (slot == CONFIRM_SLOT) {
            event.setCancelled(true);
            handleConfirm(player, holder, event.getView().getTopInventory());
            return;
        }
        if (slot == REMOVE_SLOT) {
            event.setCancelled(true);
            handleRemove(player, event.getView().getTopInventory());
            return;
        }
        if (slot == WEAPON_SLOT || slot == ATTACHMENT_SLOT) {
            if (!isValidInputClick(event, slot)) {
                event.setCancelled(true);
            }
            scheduleControlUpdate(event.getView().getTopInventory());
            return;
        }

        event.setCancelled(true);
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (!(event.getView().getTopInventory().getHolder() instanceof WorkbenchHolder)) {
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
        if (!(event.getInventory().getHolder() instanceof WorkbenchHolder holder)) {
            return;
        }
        if (holder.isInstalling() || !(event.getPlayer() instanceof Player player)) {
            return;
        }

        boolean returned = false;
        ItemStack weapon = event.getInventory().getItem(WEAPON_SLOT);
        ItemStack attachment = event.getInventory().getItem(ATTACHMENT_SLOT);
        if (!isEmpty(weapon)) {
            giveOrDrop(player, weapon);
            event.getInventory().setItem(WEAPON_SLOT, null);
            returned = true;
        }
        if (!isEmpty(attachment)) {
            giveOrDrop(player, attachment);
            event.getInventory().setItem(ATTACHMENT_SLOT, null);
            returned = true;
        }
        if (returned) {
            player.sendActionBar(Component.text("Workbench items returned.", NamedTextColor.YELLOW));
        }
    }

    private void handleConfirm(Player player, WorkbenchHolder holder, Inventory inv) {
        ItemStack weaponStack = inv.getItem(WEAPON_SLOT);
        ItemStack attachmentStack = inv.getItem(ATTACHMENT_SLOT);
        WeaponDefinition weapon = module.getWeaponRegistry().getWeapon(weaponStack);
        AttachmentDefinition attachment = module.getAttachmentRegistry().getAttachment(attachmentStack);

        if (weapon == null) {
            player.sendActionBar(Component.text("Invalid weapon.", NamedTextColor.RED));
            return;
        }
        if (attachment == null) {
            player.sendActionBar(Component.text("Invalid attachment.", NamedTextColor.RED));
            return;
        }
        if (!attachment.getCompatibleCategories().contains(weapon.getCategory())) {
            player.sendActionBar(Component.text("Incompatible attachment.", NamedTextColor.RED));
            return;
        }
        if (module.getAttachmentManager().getAttachmentId(weaponStack, attachment.getSlot()) != null) {
            player.sendActionBar(Component.text("Slot already occupied.", NamedTextColor.RED));
            return;
        }
        if (!module.getAttachmentManager().canInstall(weaponStack, weapon, attachment)) {
            player.sendActionBar(Component.text("Incompatible attachment.", NamedTextColor.RED));
            return;
        }

        ItemStack weaponToInstall = weaponStack.clone();
        ItemStack attachmentPayment = attachmentStack.clone();
        inv.setItem(WEAPON_SLOT, null);
        inv.setItem(ATTACHMENT_SLOT, null);
        holder.setInstalling(true);
        player.closeInventory();
        player.sendActionBar(Component.text("Installing attachment...", NamedTextColor.YELLOW));

        Location fallbackDropLocation = player.getLocation().clone();
        new BukkitRunnable() {
            @Override
            public void run() {
                Player onlinePlayer = Bukkit.getPlayer(player.getUniqueId());
                if (!module.getAttachmentManager().installAttachment(weaponToInstall, weapon, attachment)) {
                    returnAfterInstallFailure(onlinePlayer, fallbackDropLocation, weaponToInstall, attachmentPayment);
                    return;
                }
                module.getAttachmentAuditLogger().log(onlinePlayer != null ? onlinePlayer : player, "install", weapon, attachment);

                int remaining = attachmentPayment.getAmount() - 1;
                attachmentPayment.setAmount(1);
                giveOrDrop(onlinePlayer, fallbackDropLocation, weaponToInstall);
                if (remaining > 0) {
                    ItemStack leftover = attachmentPayment.clone();
                    leftover.setAmount(remaining);
                    giveOrDrop(onlinePlayer, fallbackDropLocation, leftover);
                }
                if (onlinePlayer != null) {
                    onlinePlayer.playSound(onlinePlayer.getLocation(), Sound.BLOCK_ANVIL_USE, 0.7f, 1.4f);
                    onlinePlayer.sendActionBar(Component.text("Attachment installed.", NamedTextColor.GREEN));
                }
            }
        }.runTaskLater(module.getCore(), Math.max(1, attachment.getInstallTimeTicks()));
    }

    private void handleRemove(Player player, Inventory inv) {
        ItemStack weaponStack = inv.getItem(WEAPON_SLOT);
        WeaponDefinition weapon = module.getWeaponRegistry().getWeapon(weaponStack);
        if (weapon == null) {
            player.sendActionBar(Component.text("Invalid weapon.", NamedTextColor.RED));
            return;
        }

        AttachmentDefinition slotHint = module.getAttachmentRegistry().getAttachment(inv.getItem(ATTACHMENT_SLOT));
        if (slotHint != null) {
            removeSlot(player, weaponStack, slotHint.getSlot());
            updateControlItems(inv);
            return;
        }

        Map<AttachmentSlot, AttachmentDefinition> installed = module.getAttachmentManager().getInstalledAttachments(weaponStack);
        if (installed.isEmpty()) {
            player.sendActionBar(Component.text("Invalid attachment.", NamedTextColor.RED));
            return;
        }

        for (AttachmentSlot slot : installed.keySet()) {
            AttachmentDefinition removed = module.getAttachmentManager().removeAttachment(weaponStack, slot);
            if (removed != null) {
                module.getAttachmentAuditLogger().log(player, "remove", weapon, removed);
                giveOrDrop(player, module.getAttachmentRegistry().createItemStack(removed.getId()));
            }
        }
        player.playSound(player.getLocation(), Sound.ITEM_ARMOR_EQUIP_IRON, 0.7f, 1.2f);
        player.sendActionBar(Component.text("Attachment removed.", NamedTextColor.GREEN));
        updateControlItems(inv);
    }

    private void removeSlot(Player player, ItemStack weaponStack, AttachmentSlot slot) {
        AttachmentDefinition removed = module.getAttachmentManager().removeAttachment(weaponStack, slot);
        if (removed == null) {
            player.sendActionBar(Component.text("Invalid attachment.", NamedTextColor.RED));
            return;
        }
        WeaponDefinition weapon = module.getWeaponRegistry().getWeapon(weaponStack);
        module.getAttachmentAuditLogger().log(player, "remove", weapon, removed);
        giveOrDrop(player, module.getAttachmentRegistry().createItemStack(removed.getId()));
        player.playSound(player.getLocation(), Sound.ITEM_ARMOR_EQUIP_IRON, 0.7f, 1.2f);
        player.sendActionBar(Component.text("Attachment removed.", NamedTextColor.GREEN));
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
            return module.getWeaponRegistry().getWeapon(item) != null;
        }
        if (slot == ATTACHMENT_SLOT) {
            return module.getAttachmentRegistry().getAttachment(item) != null;
        }
        return false;
    }

    private void updateControlItems(Inventory inv) {
        inv.setItem(CONFIRM_SLOT, new ItemBuilder(Material.LIME_CONCRETE)
                .name(Component.text("Install", NamedTextColor.GREEN, TextDecoration.BOLD)
                        .decoration(TextDecoration.ITALIC, false))
                .build());
        inv.setItem(REMOVE_SLOT, removeButton(inv));
    }

    private ItemStack removeButton(Inventory inv) {
        ItemStack attachmentItem = inv.getItem(ATTACHMENT_SLOT);
        AttachmentDefinition attachment = module.getAttachmentRegistry().getAttachment(attachmentItem);
        if (attachment != null) {
            return new ItemBuilder(Material.REDSTONE_TORCH)
                    .name(Component.text("Remove " + AttachmentRegistry.slotDisplayName(attachment.getSlot()), NamedTextColor.RED)
                            .decoration(TextDecoration.ITALIC, false))
                    .lore(Component.text("Mounted item in this slot is returned.", NamedTextColor.GRAY)
                            .decoration(TextDecoration.ITALIC, false))
                    .build();
        }

        ItemStack weaponItem = inv.getItem(WEAPON_SLOT);
        Map<AttachmentSlot, AttachmentDefinition> installed = module.getAttachmentManager().getInstalledAttachments(weaponItem);
        String summary = installed.isEmpty()
                ? "None"
                : installed.values().stream().map(AttachmentDefinition::getDisplayName).collect(Collectors.joining(" / "));
        return new ItemBuilder(Material.HOPPER)
                .name(Component.text("Remove Mods", NamedTextColor.RED)
                        .decoration(TextDecoration.ITALIC, false))
                .lore(Component.text("Installed: " + summary, NamedTextColor.GRAY)
                        .decoration(TextDecoration.ITALIC, false))
                .build();
    }

    private void scheduleControlUpdate(Inventory inv) {
        Bukkit.getScheduler().runTask(module.getCore(), () -> updateControlItems(inv));
    }

    private void returnAfterInstallFailure(Player player, Location fallbackDropLocation, ItemStack weapon, ItemStack attachment) {
        giveOrDrop(player, fallbackDropLocation, weapon);
        giveOrDrop(player, fallbackDropLocation, attachment);
        if (player != null) {
            player.sendActionBar(Component.text("Incompatible attachment.", NamedTextColor.RED));
        }
    }

    private void giveOrDrop(Player player, ItemStack item) {
        if (player == null || item == null || item.getType().isAir()) {
            return;
        }
        Map<Integer, ItemStack> leftovers = player.getInventory().addItem(item);
        leftovers.values().forEach(leftover -> player.getWorld().dropItemNaturally(player.getLocation(), leftover));
    }

    private void giveOrDrop(Player player, Location fallbackDropLocation, ItemStack item) {
        if (item == null || item.getType().isAir()) {
            return;
        }
        if (player != null && player.isOnline()) {
            giveOrDrop(player, item);
            return;
        }
        if (fallbackDropLocation != null && fallbackDropLocation.getWorld() != null) {
            fallbackDropLocation.getWorld().dropItemNaturally(fallbackDropLocation, item);
        }
    }

    private boolean isEmpty(ItemStack item) {
        return item == null || item.getType().isAir();
    }

    private static final class WorkbenchHolder implements InventoryHolder {
        private final UUID ownerId;
        private Inventory inventory;
        private boolean installing;

        private WorkbenchHolder(UUID ownerId) {
            this.ownerId = ownerId;
        }

        @Override
        public Inventory getInventory() {
            return inventory;
        }

        private void setInventory(Inventory inventory) {
            this.inventory = inventory;
        }

        private boolean isInstalling() {
            return installing;
        }

        private void setInstalling(boolean installing) {
            this.installing = installing;
        }

        @SuppressWarnings("unused")
        private UUID getOwnerId() {
            return ownerId;
        }
    }
}
