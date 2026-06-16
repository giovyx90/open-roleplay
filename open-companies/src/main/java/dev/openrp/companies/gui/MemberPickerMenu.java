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
import dev.openrp.companies.core.CompanyResult;
import dev.openrp.companies.model.Company;
import dev.openrp.companies.model.CompanyMember;
import dev.openrp.companies.model.TransactionType;

/**
 * Picks the member to pay from the company terminal. Each member is a head button; choosing one opens
 * the keypad to enter a discretionary amount, which is paid from the treasury into that member's bank
 * account as a {@link TransactionType#SALARY} line. The amount is entirely the director's call - there
 * is no fixed wage.
 */
public final class MemberPickerMenu extends Menu {

    private final OpenCompaniesPlugin plugin;
    private final Player player;
    private final String companyId;
    private final Map<Integer, UUID> bySlot = new HashMap<>();

    public MemberPickerMenu(OpenCompaniesPlugin plugin, Player player, String companyId) {
        this.plugin = plugin;
        this.player = player;
        this.companyId = companyId;
        int size = 54;
        this.inventory = Bukkit.createInventory(this, size, plugin.messages().mini(player, "terminal.pick_member"));
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
            inventory.setItem(slot, Items.button(Material.PLAYER_HEAD,
                    Component.text(member.playerName(), NamedTextColor.WHITE),
                    Component.text(plugin.messages().text(player, member.role().messageKey()), NamedTextColor.GRAY)));
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
        UUID target = bySlot.get(slot);
        if (target == null) {
            return;
        }
        String targetName = plugin.companies().findById(companyId)
                .flatMap(c -> c.member(target)).map(CompanyMember::playerName).orElse(target.toString());
        long max = (long) plugin.settings().keypadMaxAmount();
        new KeypadMenu(plugin, player, "Bonifico a " + targetName, max, amount -> {
            CompanyResult result = plugin.treasury().transferToPlayer(companyId,
                    Bukkit.getOfflinePlayer(target), amount, TransactionType.SALARY,
                    player.getUniqueId(), "bonifico");
            if (result.success()) {
                plugin.messages().success(player, result.messageKey(), result.placeholders());
            } else {
                plugin.messages().error(player, result.messageKey(), result.placeholders());
            }
        }, () -> new MemberPickerMenu(plugin, player, companyId).open(player)).open(player);
    }
}
