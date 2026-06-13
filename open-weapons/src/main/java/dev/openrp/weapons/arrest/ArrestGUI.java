package dev.openrp.weapons.arrest;

import it.meridian.core.gui.NexoUI;
import dev.openrp.weapons.module.WeaponsModule;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.wesjd.anvilgui.AnvilGUI;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.*;

/**
 * Modern Arrest GUI using custom Nexo UI icons and AnvilGUI for input.
 * Layout (54 slots):
 *   Row 1: Filler | Target Head (slot 4) | Filler
 *   Row 2: Filler
 *   Row 3: Time icon (slot 20) | Money icon (slot 22) | Book icon (slot 24)
 *   Row 4: Filler
 *   Row 5: Jail Region selector (slots 28-34)
 *   Row 6: Confirm (slot 48) | Cancel (slot 50)
 */
public class ArrestGUI implements Listener {

    private final WeaponsModule module;

    private static Component buildTitle() {
        return NexoUI.getGlyphTitle("arrest_gui", "Arrest");
    }


    // ── Slot positions ──
    private static final int SLOT_TARGET_HEAD = 4;

    private static final int SLOT_TIME   = 10;
    private static final int SLOT_MONEY  = 12;
    private static final int SLOT_REASON = 14;
    private static final int SLOT_REGION = 16;

    private static final int SLOT_CONFIRM = 21;
    private static final int SLOT_CANCEL  = 23;

    public ArrestGUI(WeaponsModule module) {
        this.module = module;
    }

    public static class ArrestSession {
        UUID targetUuid;
        String targetName;
        double jailTimeHours = -1;
        String jailRegion = null;
        double bailAmount = 0;
        String reason = null;

        public ArrestSession(UUID targetUuid, String targetName) {
            this.targetUuid = targetUuid;
            this.targetName = targetName;
        }
    }

    public static class ArrestGUIHolder implements org.bukkit.inventory.InventoryHolder {
        private final ArrestSession session;
        private Inventory inventory;

        public ArrestGUIHolder(ArrestSession session) {
            this.session = session;
        }

        @Override
        public Inventory getInventory() {
            return inventory;
        }

        public void setInventory(Inventory inventory) {
            this.inventory = inventory;
        }

        public ArrestSession getSession() {
            return session;
        }
    }

    public void open(Player officer, org.bukkit.OfflinePlayer target) {
        ArrestSession session = new ArrestSession(target.getUniqueId(), target.getName());
        openMainGUI(officer, session);
    }

    private void openMainGUI(Player officer, ArrestSession session) {
        ArrestGUIHolder holder = new ArrestGUIHolder(session);
        Inventory gui = Bukkit.createInventory(holder, 27, buildTitle());
        holder.setInventory(gui);

        // ── Fill entire background ──
        ItemStack filler = NexoUI.getFiller();
        for (int i = 0; i < gui.getSize(); i++) {
            gui.setItem(i, filler);
        }

        // ── Build display strings ──
        String timeDisplay   = session.jailTimeHours < 0 ? "§cNot set" : "§a" + formatTime(session.jailTimeHours);
        String regionDisplay = session.jailRegion != null ? "§a" + session.jailRegion : "§cNot set";
        String bailDisplay   = session.bailAmount > 0 ? "§a$" + String.format("%.2f", session.bailAmount) : "§cNot set";
        String reasonDisplay = session.reason != null ? "§a" + session.reason : "§cNot set";

        // ── Row 1: Target Head (slot 4) ──
        gui.setItem(SLOT_TARGET_HEAD, createTargetHead(session.targetUuid, session.targetName,
                timeDisplay, regionDisplay, bailDisplay, reasonDisplay));

        // ── Row 3: Three action icons ──
        gui.setItem(SLOT_TIME, NexoUI.getArrestTimeButton(
                Component.text("ᴊᴀɪʟ ᴛɪᴍᴇ", NamedTextColor.GOLD).decoration(TextDecoration.ITALIC, false),
                List.of(
                        Component.text("Cʟɪᴄᴋ ᴛᴏ sᴇᴛ ᴊᴀɪʟ ᴅᴜʀᴀᴛɪᴏɴ", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
                        Component.empty(),
                        Component.text("Cᴜʀʀᴇɴᴛ: " + timeDisplay, NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false)
                )));

        gui.setItem(SLOT_MONEY, NexoUI.getArrestMoneyButton(
                Component.text("ʙᴀɪʟ ᴀᴍᴏᴜɴᴛ", NamedTextColor.GREEN).decoration(TextDecoration.ITALIC, false),
                List.of(
                        Component.text("Cʟɪᴄᴋ ᴛᴏ sᴇᴛ ʙᴀɪʟ", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
                        Component.empty(),
                        Component.text("Cᴜʀʀᴇɴᴛ: " + bailDisplay, NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false)
                )));

        gui.setItem(SLOT_REASON, NexoUI.getArrestReasonButton(
                Component.text("ᴀʀʀᴇsᴛ ʀᴇᴀsᴏɴ", NamedTextColor.AQUA).decoration(TextDecoration.ITALIC, false),
                List.of(
                        Component.text("Cʟɪᴄᴋ ᴛᴏ sᴇᴛ ʀᴇᴀsᴏɴ", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
                        Component.empty(),
                        Component.text("Cᴜʀʀᴇɴᴛ: " + reasonDisplay, NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false)
                )));

        // ── Row 2: Jail Region selector ──
        List<String> regions = module.getArrestManager().getJailRegions();
        if (regions.isEmpty()) {
            gui.setItem(SLOT_REGION, createSimpleItem(Material.BARRIER,
                    Component.text("ɴᴏ ᴊᴀɪʟ ʀᴇɢɪᴏɴs", NamedTextColor.RED).decoration(TextDecoration.ITALIC, false),
                    List.of(Component.text("Cʀᴇᴀᴛᴇ WᴏʀʟᴅGᴜᴀʀᴅ ʀᴇɢɪᴏɴs", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false))));
        } else {
            if (session.jailRegion == null) {
                session.jailRegion = regions.get(0);
            }
            gui.setItem(SLOT_REGION, createRegionItem(session.jailRegion, true));
        }

        // ── Row 6: Confirm / Cancel ──
        gui.setItem(SLOT_CONFIRM, NexoUI.getConfirmButton(
                Component.text("ᴄᴏɴꜰɪʀᴍ ᴀʀʀᴇsᴛ", NamedTextColor.GREEN, TextDecoration.BOLD)
                        .decoration(TextDecoration.ITALIC, false)));
        gui.setItem(SLOT_CANCEL, NexoUI.getCancelButton(
                Component.text("ᴄᴀɴᴄᴇʟ", NamedTextColor.RED, TextDecoration.BOLD)
                        .decoration(TextDecoration.ITALIC, false)));

        officer.openInventory(gui);
    }

    // ════════════════════════════════════════════════════════════════════════════
    //  AnvilGUI Openers
    // ════════════════════════════════════════════════════════════════════════════

    private void openTimeAnvil(Player officer, ArrestSession session) {
        new AnvilGUI.Builder()
                .onClick((slot, stateSnapshot) -> {
                    if (slot != AnvilGUI.Slot.OUTPUT) return Collections.emptyList();
                    String text = stateSnapshot.getText();
                    try {
                        double hours = Double.parseDouble(text.replace(",", ""));
                        if (hours <= 0) {
                            return Collections.singletonList(
                                    AnvilGUI.ResponseAction.replaceInputText("Must be > 0"));
                        }
                        session.jailTimeHours = hours;
                        return Collections.singletonList(AnvilGUI.ResponseAction.run(
                                () -> Bukkit.getScheduler().runTask(module.getCore(),
                                        () -> openMainGUI(officer, session))));
                    } catch (NumberFormatException e) {
                        return Collections.singletonList(
                                AnvilGUI.ResponseAction.replaceInputText("Not a number"));
                    }
                })
                .text("1.0")
                .title("Jail Time (hours)")
                .plugin(module.getCore())
                .open(officer);
    }

    private void openMoneyAnvil(Player officer, ArrestSession session) {
        new AnvilGUI.Builder()
                .onClick((slot, stateSnapshot) -> {
                    if (slot != AnvilGUI.Slot.OUTPUT) return Collections.emptyList();
                    String text = stateSnapshot.getText();
                    try {
                        double bail = Double.parseDouble(text.replace(",", "").replace("$", ""));
                        if (bail < 0) {
                            return Collections.singletonList(
                                    AnvilGUI.ResponseAction.replaceInputText("Must be >= 0"));
                        }
                        session.bailAmount = bail;
                        return Collections.singletonList(AnvilGUI.ResponseAction.run(
                                () -> Bukkit.getScheduler().runTask(module.getCore(),
                                        () -> openMainGUI(officer, session))));
                    } catch (NumberFormatException e) {
                        return Collections.singletonList(
                                AnvilGUI.ResponseAction.replaceInputText("Not a number"));
                    }
                })
                .text("0.00")
                .title("Bail Amount ($)")
                .plugin(module.getCore())
                .open(officer);
    }

    private void openReasonAnvil(Player officer, ArrestSession session) {
        new AnvilGUI.Builder()
                .onClick((slot, stateSnapshot) -> {
                    if (slot != AnvilGUI.Slot.OUTPUT) return Collections.emptyList();
                    String text = stateSnapshot.getText();
                    if (text == null || text.isBlank() || text.equals("Type reason...")) {
                        return Collections.singletonList(
                                AnvilGUI.ResponseAction.replaceInputText("Cannot be empty"));
                    }
                    session.reason = text;
                    return Collections.singletonList(AnvilGUI.ResponseAction.run(
                            () -> Bukkit.getScheduler().runTask(module.getCore(),
                                    () -> openMainGUI(officer, session))));
                })
                .text("Type reason...")
                .title("Arrest Reason")
                .plugin(module.getCore())
                .open(officer);
    }

    // ════════════════════════════════════════════════════════════════════════════
    //  Click Handler
    // ════════════════════════════════════════════════════════════════════════════

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player officer)) return;
        
        Inventory topInv = event.getView().getTopInventory();
        if (!(topInv.getHolder() instanceof ArrestGUIHolder holder)) {
            return;
        }

        event.setCancelled(true);
        
        if (event.getClickedInventory() != topInv) {
            return;
        }

        ArrestSession session = holder.getSession();
        if (session == null) return;

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta()) return;

        int slot = event.getRawSlot();

        switch (slot) {
            // ── Time icon → AnvilGUI ──
            case SLOT_TIME -> {
                officer.closeInventory();
                Bukkit.getScheduler().runTaskLater(module.getCore(),
                        () -> openTimeAnvil(officer, session), 2L);
                return;
            }
            // ── Money icon → AnvilGUI ──
            case SLOT_MONEY -> {
                officer.closeInventory();
                Bukkit.getScheduler().runTaskLater(module.getCore(),
                        () -> openMoneyAnvil(officer, session), 2L);
                return;
            }
            // ── Reason icon → AnvilGUI ──
            case SLOT_REASON -> {
                officer.closeInventory();
                Bukkit.getScheduler().runTaskLater(module.getCore(),
                        () -> openReasonAnvil(officer, session), 2L);
                return;
            }
            // ── Confirm ──
            case SLOT_CONFIRM -> {
                if (session.jailTimeHours < 0) {
                    officer.sendMessage(Component.text("You must set a jail time!", NamedTextColor.RED));
                    return;
                }
                if (session.jailRegion == null) {
                    officer.sendMessage(Component.text("You must select a jail region!", NamedTextColor.RED));
                    return;
                }
                if (session.reason == null) {
                    officer.sendMessage(Component.text("You must set a reason!", NamedTextColor.RED));
                    return;
                }

                org.bukkit.OfflinePlayer target = Bukkit.getOfflinePlayer(session.targetUuid);
                if (target == null) {
                    officer.sendMessage(Component.text("The target player could not be found.", NamedTextColor.RED));
                    officer.closeInventory();
                    return;
                }

                ArrestRecord record = new ArrestRecord(
                        target.getUniqueId(), target.getName(),
                        officer.getUniqueId(), officer.getName(),
                        session.jailRegion, session.reason,
                        session.jailTimeHours, session.bailAmount
                );

                module.getArrestManager().arrest(record);
                officer.closeInventory();

                String targetName = target.getName() != null ? target.getName() : "Unknown";
                String timeStr = formatTime(session.jailTimeHours);
                officer.sendMessage(Component.text("Successfully arrested " + targetName
                        + " for " + timeStr + ".", NamedTextColor.GREEN));
                return;
            }
            // ── Cancel ──
            case SLOT_CANCEL -> {
                officer.closeInventory();
                officer.sendMessage(Component.text("Arrest cancelled.", NamedTextColor.RED));
                return;
            }
        }

        // ── Jail Region selection ──
        if (slot == SLOT_REGION) {
            List<String> regions = module.getArrestManager().getJailRegions();
            if (!regions.isEmpty()) {
                int currentIndex = regions.indexOf(session.jailRegion);
                int nextIndex = (currentIndex + 1) % regions.size();
                session.jailRegion = regions.get(nextIndex);
            }
        }

        // Refresh GUI to show updated state
        openMainGUI(officer, session);
    }

    /**
     * Legacy chat input handler – no longer needed since we use AnvilGUI now.
     * Kept for backward compatibility but always returns false.
     */
    public boolean handleChatInput(Player officer, String message) {
        return false;
    }

    // ════════════════════════════════════════════════════════════════════════════
    //  Helpers
    // ════════════════════════════════════════════════════════════════════════════

    private String formatTime(double hours) {
        if (hours < 1) {
            return String.format("%.0f ᴍɪɴᴜᴛᴇs", hours * 60);
        } else if (hours == (int) hours) {
            return String.format("%.0f ʜᴏᴜʀ(s)", hours);
        } else {
            return String.format("%.1f ʜᴏᴜʀ(s)", hours);
        }
    }

    private ItemStack createRegionItem(String regionName, boolean selected) {
        ItemStack item = new ItemStack(Material.IRON_BARS);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text(regionName.toUpperCase(), NamedTextColor.AQUA)
                    .decoration(TextDecoration.ITALIC, false));
            List<Component> lore = new ArrayList<>();
            lore.add(Component.text("ᴊᴀɪʟ ʀᴇɢɪᴏɴ", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
            lore.add(Component.empty());
            lore.add(Component.text("Cʟɪᴄᴋ ᴛᴏ ᴄʜᴀɴɢᴇ ʀᴇɢɪᴏɴ", NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false));
            if (selected) {
                meta.setEnchantmentGlintOverride(true);
            }
            meta.lore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createSimpleItem(Material material, Component name, List<Component> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(name);
            meta.lore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createTargetHead(UUID targetUuid, String targetName,
                                       String time, String region, String bail, String reason) {
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) item.getItemMeta();
        if (meta != null) {
            meta.setOwningPlayer(Bukkit.getOfflinePlayer(targetUuid));
            meta.displayName(Component.text("ᴀʀʀᴇsᴛ: " + targetName, NamedTextColor.GOLD)
                    .decoration(TextDecoration.ITALIC, false));
            List<Component> lore = new ArrayList<>();
            lore.add(Component.empty());
            lore.add(Component.text("Tɪᴍᴇ: " + time, NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
            lore.add(Component.text("Rᴇɢɪᴏɴ: " + region, NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
            lore.add(Component.text("Bᴀɪʟ: " + bail, NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
            lore.add(Component.text("Rᴇᴀsᴏɴ: " + reason, NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
            meta.lore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }
}
