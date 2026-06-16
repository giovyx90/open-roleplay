package dev.openrp.companies.gui;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import dev.openrp.companies.OpenCompaniesPlugin;
import dev.openrp.companies.model.Company;
import dev.openrp.companies.model.CompanyMember;
import dev.openrp.companies.model.RecurringPayment;

/**
 * Configures recurring salaries from the company terminal. Each member is shown with their current
 * recurring amount (if any); choosing one opens the keypad to set a new amount - entering {@code 0}
 * clears the recurring salary. The cadence is the configured default ({@code finance.payroll
 * .recurring-interval-seconds}); the amount stays entirely the director's choice.
 */
public final class RecurringMenu extends Menu {

    private final OpenCompaniesPlugin plugin;
    private final Player player;
    private final String companyId;
    private final Map<Integer, UUID> bySlot = new HashMap<>();

    public RecurringMenu(OpenCompaniesPlugin plugin, Player player, String companyId) {
        this.plugin = plugin;
        this.player = player;
        this.companyId = companyId;
        this.inventory = Bukkit.createInventory(this, 54, plugin.messages().mini(player, "recurring.title"));
        render();
    }

    private void render() {
        Company company = plugin.companies().findById(companyId).orElse(null);
        if (company == null) {
            return;
        }
        int slot = 0;
        for (CompanyMember member : company.members()) {
            if (slot >= 53) {
                break;
            }
            double current = plugin.recurring().get(companyId, member.playerUuid())
                    .map(RecurringPayment::amount).orElse(0.0);
            String label = current > 0
                    ? "Ricorrente: " + plugin.settings().currencySymbol() + String.format("%.2f", current)
                    : "Nessuna ricorrenza";
            inventory.setItem(slot, Items.button(Material.PLAYER_HEAD,
                    Component.text(member.playerName(), NamedTextColor.WHITE),
                    Component.text(label, NamedTextColor.GRAY)));
            bySlot.put(slot, member.playerUuid());
            slot++;
        }
        inventory.setItem(53, Items.button(Material.ARROW, Component.text("Back", NamedTextColor.GRAY)));
    }

    @Override
    public void onClick(InventoryClickEvent event) {
        int slot = event.getRawSlot();
        if (slot == 53) {
            new TerminalMenu(plugin, player, companyId).open(player);
            return;
        }
        UUID member = bySlot.get(slot);
        if (member == null) {
            return;
        }
        long max = (long) plugin.settings().keypadMaxAmount();
        new KeypadMenu(plugin, player, "Stipendio ricorrente", max, amount -> {
            if (amount <= 0) {
                plugin.recurring().remove(companyId, member);
                plugin.messages().success(player, "recurring.cleared");
            } else {
                long interval = plugin.settings().payrollIntervalSeconds();
                long nextDue = System.currentTimeMillis() + interval * 1000L;
                plugin.recurring().set(new RecurringPayment(companyId, member, amount, interval, nextDue));
                plugin.messages().success(player, "recurring.set",
                        "amount", plugin.settings().currencySymbol() + amount);
            }
        }, () -> new RecurringMenu(plugin, player, companyId).open(player)).open(player);
    }
}
