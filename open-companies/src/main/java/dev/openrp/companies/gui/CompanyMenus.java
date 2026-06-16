package dev.openrp.companies.gui;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.entity.Player;
import dev.openrp.companies.OpenCompaniesPlugin;
import dev.openrp.companies.model.CompanyAsset;
import dev.openrp.companies.model.CompanyCapability;

/**
 * Maps a physical company asset to the on-screen menu it opens, and holds the small amount of
 * transient state device screens share (currently the pending charge a POS cashier has rung up,
 * waiting for a customer to pay). This is the one place that knows which device shows which screen, so
 * {@link dev.openrp.companies.listener.AssetInteractionListener} stays a thin "resolve the clicked
 * block, hand it here" listener. Assets with no economic screen fall through with no effect.
 */
public final class CompanyMenus {

    private final OpenCompaniesPlugin plugin;
    /** POS asset id -> amount a cashier rang up, awaiting a customer payment. Transient by design. */
    private final Map<UUID, Long> pendingCharges = new ConcurrentHashMap<>();

    public CompanyMenus(OpenCompaniesPlugin plugin) {
        this.plugin = plugin;
    }

    /** Opens the screen for the asset the player interacted with, if it has one. */
    public void open(Player player, CompanyAsset asset) {
        switch (asset.type()) {
            case POS, CASH_REGISTER -> openPos(player, asset);
            case COMPANY_TERMINAL -> openTerminal(player, asset);
            case ATM -> new AtmMenu(plugin, player).open(player);
            default -> {
                // No interactive economic screen for this asset type.
            }
        }
    }

    /**
     * POS flow. A cashier (a member allowed to use the asset) rings up an amount on the keypad; that
     * becomes a pending charge on the device. The next person who is <em>not</em> the cashier sees a
     * pay screen and settles it by card or cash.
     */
    private void openPos(Player player, CompanyAsset asset) {
        boolean operator = plugin.assets().canUseAsset(player.getUniqueId(), asset);
        if (operator) {
            long max = (long) plugin.settings().keypadMaxAmount();
            new KeypadMenu(plugin, player, "Importo", max, amount -> {
                pendingCharges.put(asset.id(), amount);
                plugin.messages().success(player, "pos.charge_set",
                        "amount", plugin.settings().currencySymbol() + amount);
            }, null).open(player);
            return;
        }
        Long charge = pendingCharges.get(asset.id());
        if (charge == null) {
            plugin.messages().warning(player, "pos.no_charge");
            return;
        }
        new PosPayMenu(plugin, player, asset, charge, this).open(player);
    }

    /** The company terminal is the treasury control panel: opening it needs MANAGE_FINANCE. */
    private void openTerminal(Player player, CompanyAsset asset) {
        if (!plugin.companies().hasCapability(player.getUniqueId(), asset.companyId(), CompanyCapability.MANAGE_FINANCE)) {
            plugin.messages().error(player, "terminal.no_permission");
            return;
        }
        new TerminalMenu(plugin, player, asset.companyId()).open(player);
    }

    /** Clears a settled (or cancelled) pending charge. */
    void clearCharge(UUID assetId) {
        pendingCharges.remove(assetId);
    }
}
