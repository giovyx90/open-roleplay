package dev.openrp.companies.gui;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import dev.openrp.companies.OpenCompaniesPlugin;
import dev.openrp.companies.model.CompanyTransaction;

/**
 * Read-only, paginated view of a company's treasury ledger (newest first), opened from the terminal.
 * Credits are shown green, debits red, each with its amount, reason, time and counterparty. It only
 * displays - money never moves from here.
 */
public final class LedgerMenu extends Menu {

    private static final int SIZE = 54;
    private static final int PAGE_SIZE = 45;
    private static final int PREV = 45;
    private static final int BACK = 49;
    private static final int NEXT = 53;
    private static final int MAX_LINES = 450;

    private static final DateTimeFormatter TIME = DateTimeFormatter
            .ofPattern("dd/MM HH:mm").withZone(ZoneId.systemDefault());

    private final OpenCompaniesPlugin plugin;
    private final Player player;
    private final String companyId;
    private final int page;
    private final List<CompanyTransaction> lines;

    public LedgerMenu(OpenCompaniesPlugin plugin, Player player, String companyId, int page) {
        this.plugin = plugin;
        this.player = player;
        this.companyId = companyId;
        this.lines = plugin.treasury().transactions(companyId, MAX_LINES);
        int maxPage = Math.max(0, (lines.size() - 1) / PAGE_SIZE);
        this.page = Math.max(0, Math.min(page, maxPage));
        this.inventory = Bukkit.createInventory(this, SIZE, plugin.messages().mini(player, "ledger.title"));
        render();
    }

    private void render() {
        int start = page * PAGE_SIZE;
        for (int i = 0; i < PAGE_SIZE; i++) {
            int index = start + i;
            inventory.setItem(i, index < lines.size() ? lineItem(lines.get(index)) : null);
        }
        inventory.setItem(BACK, Items.button(Material.ARROW, Component.text("Back", NamedTextColor.GRAY)));
        if (page > 0) {
            inventory.setItem(PREV, Items.button(Material.PAPER, Component.text("« Prev", NamedTextColor.YELLOW)));
        } else {
            inventory.setItem(PREV, null);
        }
        if ((page + 1) * PAGE_SIZE < lines.size()) {
            inventory.setItem(NEXT, Items.button(Material.PAPER, Component.text("Next »", NamedTextColor.YELLOW)));
        } else {
            inventory.setItem(NEXT, null);
        }
    }

    private org.bukkit.inventory.ItemStack lineItem(CompanyTransaction tx) {
        boolean credit = tx.type().credit();
        String sign = credit ? "+" : "-";
        String amount = sign + plugin.settings().currencySymbol() + String.format("%.2f", tx.amount());
        Component name = Component.text(amount, credit ? NamedTextColor.GREEN : NamedTextColor.RED);
        Component reason = Component.text(plugin.messages().text(player, "txtype." + tx.type().key()),
                NamedTextColor.GRAY);
        Component time = Component.text(TIME.format(Instant.ofEpochMilli(tx.timestamp())), NamedTextColor.DARK_GRAY);
        if (tx.note() != null && !tx.note().isBlank()) {
            return Items.button(Material.PAPER, name, reason,
                    Component.text(tx.note(), NamedTextColor.DARK_GRAY), time);
        }
        return Items.button(Material.PAPER, name, reason, time);
    }

    @Override
    public void onClick(InventoryClickEvent event) {
        int slot = event.getRawSlot();
        if (slot == BACK) {
            new TerminalMenu(plugin, player, companyId).open(player);
        } else if (slot == PREV && page > 0) {
            new LedgerMenu(plugin, player, companyId, page - 1).open(player);
        } else if (slot == NEXT && (page + 1) * PAGE_SIZE < lines.size()) {
            new LedgerMenu(plugin, player, companyId, page + 1).open(player);
        }
    }
}
