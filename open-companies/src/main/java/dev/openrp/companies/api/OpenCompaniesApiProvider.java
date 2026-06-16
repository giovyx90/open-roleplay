package dev.openrp.companies.api;

import dev.openrp.companies.OpenCompaniesPlugin;
import dev.openrp.companies.adapter.AdapterRegistry;

/** Default API implementation; simply exposes the plugin's services and adapter registry. */
public final class OpenCompaniesApiProvider implements OpenCompaniesApi {

    private final OpenCompaniesPlugin plugin;

    public OpenCompaniesApiProvider(OpenCompaniesPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public CompanyService companies() {
        return plugin.companies();
    }

    @Override
    public ChamberService chamber() {
        return plugin.chamber();
    }

    @Override
    public CompanyAssetService assets() {
        return plugin.assets();
    }

    @Override
    public TreasuryService treasury() {
        return plugin.treasury();
    }

    @Override
    public BankingService banking() {
        return plugin.banking();
    }

    @Override
    public AdapterRegistry adapters() {
        return plugin.adapters();
    }
}
