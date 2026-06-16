package dev.openrp.companies.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Proxy;
import java.util.List;
import java.util.UUID;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.Before;
import org.junit.Test;
import dev.openrp.companies.adapter.AdapterRegistry;
import dev.openrp.companies.adapter.defaults.ConfigEconomyAdapter;
import dev.openrp.companies.adapter.defaults.MemoryStorageAdapter;
import dev.openrp.companies.config.CompaniesSettings;
import dev.openrp.companies.model.CompanyTransaction;
import dev.openrp.companies.model.TransactionType;

/**
 * Exercises the pure treasury engine end to end with the in-memory storage and demo economy adapters:
 * treasury credits/debits, the insufficient-funds and invalid-amount guards, the atomic player bridges
 * ({@code collectFromPlayer}/{@code transferToPlayer}) and that every successful movement appends a
 * ledger line.
 */
public class TreasuryTest {

    private AdapterRegistry adapters;
    private CompanyManager companies;
    private LedgerManager ledger;
    private Treasury treasury;

    @Before
    public void setUp() {
        YamlConfiguration config = new YamlConfiguration();
        config.set("companies.creation.allowed-types", List.of("generic"));
        CompaniesSettings settings = new CompaniesSettings();
        settings.load(config);

        adapters = new AdapterRegistry();
        adapters.setStorage(new MemoryStorageAdapter());
        adapters.setEconomy(new ConfigEconomyAdapter(1000.0));

        companies = new CompanyManager(settings, new CompanyValidator(settings), adapters);
        ledger = new LedgerManager(adapters);
        treasury = new Treasury(companies, ledger, adapters, settings);
    }

    private String newCompany() {
        return companies.createAsAdmin(UUID.randomUUID(), "Owner", "Acme", "generic")
                .company().orElseThrow().id();
    }

    @Test
    public void depositAndWithdrawMoveBalanceAndWriteLedger() {
        String id = newCompany();

        assertTrue(treasury.deposit(id, 250.0, TransactionType.SALE_CASH, null, "sale").success());
        assertEquals(250.0, treasury.balance(id), 1e-9);

        assertTrue(treasury.withdraw(id, 100.0, TransactionType.TREASURY_WITHDRAW, null, "cash out").success());
        assertEquals(150.0, treasury.balance(id), 1e-9);

        List<CompanyTransaction> recent = treasury.transactions(id, 10);
        assertEquals(2, recent.size());
        // Newest first.
        assertEquals(TransactionType.TREASURY_WITHDRAW, recent.get(0).type());
        assertEquals(TransactionType.SALE_CASH, recent.get(1).type());
    }

    @Test
    public void withdrawRejectsInsufficientFundsWithoutLedgerLine() {
        String id = newCompany();
        CompanyResult result = treasury.withdraw(id, 10.0, TransactionType.TREASURY_WITHDRAW, null, "");
        assertTrue(result.failed());
        assertEquals("treasury.insufficient_funds", result.messageKey());
        assertEquals(0.0, treasury.balance(id), 1e-9);
        assertTrue(treasury.transactions(id, 10).isEmpty());
    }

    @Test
    public void invalidAmountsAreRejected() {
        String id = newCompany();
        assertEquals("treasury.invalid_amount", treasury.deposit(id, 0.0, TransactionType.SALE_CASH, null, "").messageKey());
        assertEquals("treasury.invalid_amount", treasury.deposit(id, -5.0, TransactionType.SALE_CASH, null, "").messageKey());
        assertEquals("treasury.invalid_amount",
                treasury.deposit(id, Double.NaN, TransactionType.SALE_CASH, null, "").messageKey());
    }

    @Test
    public void missingCompanyFails() {
        CompanyResult result = treasury.deposit("ghost", 10.0, TransactionType.SALE_CASH, null, "");
        assertTrue(result.failed());
        assertEquals("company.not_found", result.messageKey());
    }

    @Test
    public void collectFromPlayerDebitsCardAndCreditsTreasury() {
        String id = newCompany();
        OfflinePlayer payer = mockPlayer();

        CompanyResult result = treasury.collectFromPlayer(id, payer, 120.0, TransactionType.SALE_CARD, null, "pos");
        assertTrue(result.success());
        assertEquals(120.0, treasury.balance(id), 1e-9);
        // Card draws on the "bank" account, which started at 1000.
        assertEquals(880.0, adapters.economy().balance(payer, "bank"), 1e-9);
        assertEquals(payer.getUniqueId().toString(), treasury.transactions(id, 1).get(0).counterparty());
    }

    @Test
    public void collectFromPlayerFailsWhenCardCannotPay() {
        String id = newCompany();
        OfflinePlayer payer = mockPlayer();

        CompanyResult result = treasury.collectFromPlayer(id, payer, 5000.0, TransactionType.SALE_CARD, null, "pos");
        assertTrue(result.failed());
        assertEquals("treasury.payer_insufficient", result.messageKey());
        assertEquals(0.0, treasury.balance(id), 1e-9);
        assertEquals(1000.0, adapters.economy().balance(payer, "bank"), 1e-9);
    }

    @Test
    public void transferToPlayerDebitsTreasuryAndCreditsBank() {
        String id = newCompany();
        OfflinePlayer member = mockPlayer();
        treasury.deposit(id, 500.0, TransactionType.TREASURY_DEPOSIT, null, "float");

        CompanyResult result = treasury.transferToPlayer(id, member, 200.0, TransactionType.SALARY, null, "wage");
        assertTrue(result.success());
        assertEquals(300.0, treasury.balance(id), 1e-9);
        assertEquals(1200.0, adapters.economy().balance(member, "bank"), 1e-9);
    }

    @Test
    public void transferToPlayerFailsWhenTreasuryShort() {
        String id = newCompany();
        OfflinePlayer member = mockPlayer();
        treasury.deposit(id, 50.0, TransactionType.TREASURY_DEPOSIT, null, "float");

        CompanyResult result = treasury.transferToPlayer(id, member, 200.0, TransactionType.SALARY, null, "wage");
        assertTrue(result.failed());
        assertEquals("treasury.insufficient_funds", result.messageKey());
        assertEquals(50.0, treasury.balance(id), 1e-9);
        // The member's bank is untouched.
        assertEquals(1000.0, adapters.economy().balance(member, "bank"), 1e-9);
    }

    private static OfflinePlayer mockPlayer() {
        UUID id = UUID.randomUUID();
        return (OfflinePlayer) Proxy.newProxyInstance(
                TreasuryTest.class.getClassLoader(),
                new Class<?>[]{OfflinePlayer.class},
                (proxy, method, args) -> {
                    if ("getUniqueId".equals(method.getName())) {
                        return id;
                    }
                    if ("equals".equals(method.getName())) {
                        return proxy == args[0];
                    }
                    if ("hashCode".equals(method.getName())) {
                        return System.identityHashCode(proxy);
                    }
                    Class<?> rt = method.getReturnType();
                    if (rt == boolean.class) {
                        return false;
                    }
                    if (rt.isPrimitive()) {
                        return 0;
                    }
                    return null;
                });
    }
}
