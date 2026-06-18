package dev.openrp.politics.adapter.defaults;

import dev.openrp.politics.adapter.CompanyAdapter;

/** No-op company adapter: reports unavailable; licence-revocation recognition has nothing to act on. */
public final class NoopCompanyAdapter implements CompanyAdapter {

    @Override
    public String id() {
        return "none";
    }

    @Override
    public boolean available() {
        return false;
    }

    @Override
    public String companyName(String companyId) {
        return companyId == null ? "" : companyId;
    }

    @Override
    public boolean companyExists(String companyId) {
        return false;
    }
}
