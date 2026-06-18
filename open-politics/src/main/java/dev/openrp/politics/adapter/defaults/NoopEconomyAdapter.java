package dev.openrp.politics.adapter.defaults;

import java.util.Optional;
import dev.openrp.politics.adapter.EconomyAdapter;

/** No-op economy adapter: reports unavailable so budget access degrades to "no economy connected". */
public final class NoopEconomyAdapter implements EconomyAdapter {

    @Override
    public String id() {
        return "none";
    }

    @Override
    public boolean available() {
        return false;
    }

    @Override
    public Optional<String> budgetAccount(String governmentId) {
        return Optional.empty();
    }

    @Override
    public Optional<Long> balance(String account) {
        return Optional.empty();
    }
}
