package dev.openrp.companies.api;

import dev.openrp.companies.adapter.AdapterRegistry;

/**
 * Public entry point, retrieved from Bukkit's services manager:
 * <pre>{@code
 * OpenCompaniesApi api = Bukkit.getServicesManager().load(OpenCompaniesApi.class);
 * api.adapters().setEconomy(new MyEconomyAdapter());
 * api.companies().createCompany(ownerUuid, ownerName, "Red Spot Foods", "food");
 * boolean canFire = api.companies().hasCapability(player.getUniqueId(), "red-spot-foods",
 *         CompanyCapability.FIRE);
 * }</pre>
 *
 * <p>Through {@link #adapters()} you replace any integration; through the three services you query
 * and mutate companies, the chamber and company assets along the same validated paths the commands
 * use.</p>
 */
public interface OpenCompaniesApi {

    /** Companies and membership. */
    CompanyService companies();

    /** Applications, status, licenses and headquarters. */
    ChamberService chamber();

    /** Physical company assets. */
    CompanyAssetService assets();

    /** Company treasury: balances, deposits/withdrawals, transfers and the ledger. */
    TreasuryService treasury();

    /** Personal banking: cash &harr; bank account and payment-card issuing. */
    BankingService banking();

    /** The live adapter set - swap any adapter here to integrate your own systems. */
    AdapterRegistry adapters();
}
