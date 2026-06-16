package dev.openrp.companies.gui;

import java.util.function.LongConsumer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import dev.openrp.companies.OpenCompaniesPlugin;

/**
 * The on-screen numeric keypad - the single, reusable way a player enters an amount on any device
 * (POS, company terminal, ATM). It looks and works like an ATM keypad: digits, clear, backspace and a
 * confirm button, with a live display of the running amount. Confirming hands the whole-unit amount to
 * a {@link LongConsumer} callback; the caller decides what the amount means (a sale, a salary, a
 * withdrawal) and what screen to show next. This keeps amount entry diegetic - no chat, no command.
 */
public final class KeypadMenu extends Menu {

    private static final int SIZE = 54;
    private static final int DISPLAY = 4;
    private static final int CLEAR = 39;
    private static final int ZERO = 40;
    private static final int BACKSPACE = 41;
    private static final int BACK = 45;
    private static final int CONFIRM = 53;
    // 1-9 laid out as a 3x3 numpad in the centre columns.
    private static final int[] DIGIT_SLOTS = {ZERO, 12, 13, 14, 21, 22, 23, 30, 31, 32};

    private final OpenCompaniesPlugin plugin;
    private final Player player;
    private final String title;
    private final long max;
    private final LongConsumer onConfirm;
    private final Runnable onBack;
    private long value;

    public KeypadMenu(OpenCompaniesPlugin plugin, Player player, String title, long max,
                      LongConsumer onConfirm, Runnable onBack) {
        this.plugin = plugin;
        this.player = player;
        this.title = title;
        this.max = Math.max(1L, max);
        this.onConfirm = onConfirm;
        this.onBack = onBack;
        this.inventory = Bukkit.createInventory(this, SIZE, Component.text(title));
        render();
    }

    private void render() {
        for (int slot = 0; slot < SIZE; slot++) {
            inventory.setItem(slot, Items.filler());
        }
        inventory.setItem(DISPLAY, Items.button(Material.PAPER,
                Component.text(plugin.settings().currencySymbol() + value, NamedTextColor.GREEN),
                Component.text(title, NamedTextColor.GRAY)));
        for (int digit = 1; digit <= 9; digit++) {
            inventory.setItem(DIGIT_SLOTS[digit], Items.button(Material.PAPER,
                    Component.text(String.valueOf(digit), NamedTextColor.WHITE)));
        }
        inventory.setItem(ZERO, Items.button(Material.PAPER, Component.text("0", NamedTextColor.WHITE)));
        inventory.setItem(CLEAR, Items.button(Material.RED_DYE, Component.text("Clear", NamedTextColor.RED)));
        inventory.setItem(BACKSPACE, Items.button(Material.IRON_NUGGET,
                Component.text("←", NamedTextColor.YELLOW)));
        inventory.setItem(CONFIRM, Items.button(Material.LIME_DYE, Component.text("OK", NamedTextColor.GREEN)));
        if (onBack != null) {
            inventory.setItem(BACK, Items.button(Material.ARROW, Component.text("Back", NamedTextColor.GRAY)));
        }
    }

    @Override
    public void onClick(InventoryClickEvent event) {
        int slot = event.getRawSlot();
        if (slot == CONFIRM) {
            if (value <= 0) {
                return;
            }
            long confirmed = value;
            player.closeInventory();
            onConfirm.accept(confirmed);
            return;
        }
        if (slot == BACK) {
            if (onBack != null) {
                onBack.run();
            } else {
                player.closeInventory();
            }
            return;
        }
        if (slot == CLEAR) {
            value = 0;
            render();
            return;
        }
        if (slot == BACKSPACE) {
            value /= 10;
            render();
            return;
        }
        for (int digit = 0; digit <= 9; digit++) {
            if (DIGIT_SLOTS[digit] == slot) {
                append(digit);
                render();
                return;
            }
        }
    }

    private void append(int digit) {
        long next = value * 10 + digit;
        // Guard against overflow and clamp to the configured ceiling.
        value = next < 0 || next > max ? max : next;
    }
}
