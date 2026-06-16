package dev.openrp.companies.config;

import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import org.bukkit.configuration.file.FileConfiguration;

/**
 * Typed, cached view over config.yml. Re-read on every {@code /company reload} so the rest of the
 * code never touches raw config keys (and so the keys live in exactly one place). All list-valued
 * settings are normalised to lower-case for case-insensitive matching.
 */
public final class CompaniesSettings {

    private static final Pattern DEFAULT_NAME_PATTERN = Pattern.compile("^[A-Za-z0-9 _-]+$");

    private CreationMode creationMode = CreationMode.PLAYER_DIRECT;
    private int maxOwnedPerPlayer = 1;
    private int maxMembersPerCompany = 30;
    private double creationCost = 0.0;
    private long creationCooldownMillis = 60_000L;
    private List<String> allowedTypes = List.of("generic");

    private int nameMinLength = 3;
    private int nameMaxLength = 32;
    private Pattern namePattern = DEFAULT_NAME_PATTERN;
    private List<String> reservedNames = List.of();

    private String economyAccount = "cash";
    private String currencySymbol = "$";
    private double demoStartingBalance = 1000.0;

    private String bankAccount = "bank";
    private List<Integer> denominations = List.of(1, 5, 10, 20, 50, 100, 500);
    private String banknoteMaterial = "PAPER";
    private long payrollIntervalSeconds = 86_400L;
    private long payrollTickSeconds = 300L;
    private double keypadMaxAmount = 1_000_000.0;
    private double transferFee = 0.0;
    private double atmWithdrawFee = 0.0;

    private boolean vendingIntegrationEnabled = true;
    private boolean vendingRestockRequiresManager = false;
    private int vendingDefaultMachineLimit = -1;

    private boolean debug = false;

    public void load(FileConfiguration config) {
        this.creationMode = CreationMode.fromString(config.getString("companies.creation.mode", "PLAYER_DIRECT"));
        this.maxOwnedPerPlayer = config.getInt("companies.creation.max-owned-per-player", 1);
        this.maxMembersPerCompany = Math.max(1, config.getInt("companies.creation.max-members-per-company", 30));
        this.creationCost = Math.max(0.0, config.getDouble("companies.creation.creation-cost", 0.0));
        this.creationCooldownMillis = Math.max(0L, config.getLong("companies.creation.cooldown-seconds", 60L)) * 1000L;
        this.allowedTypes = lowerCaseList(config.getStringList("companies.creation.allowed-types"), "generic");

        this.nameMinLength = Math.max(1, config.getInt("companies.creation.name.min-length", 3));
        this.nameMaxLength = Math.max(nameMinLength, config.getInt("companies.creation.name.max-length", 32));
        this.namePattern = compile(config.getString("companies.creation.name.allowed-regex"));
        this.reservedNames = lowerCaseList(config.getStringList("companies.creation.name.reserved"), null);

        this.economyAccount = config.getString("economy.account", "cash");
        this.currencySymbol = config.getString("economy.currency-symbol", "$");
        this.demoStartingBalance = config.getDouble("economy.demo-starting-balance", 1000.0);

        this.bankAccount = config.getString("finance.bank-account", "bank");
        this.denominations = sanitizeDenominations(config.getIntegerList("finance.currency.denominations"));
        this.banknoteMaterial = config.getString("finance.currency.banknote-material", "PAPER");
        this.payrollIntervalSeconds = Math.max(1L, config.getLong("finance.payroll.recurring-interval-seconds", 86_400L));
        this.payrollTickSeconds = Math.max(1L, config.getLong("finance.payroll.tick-seconds", 300L));
        this.keypadMaxAmount = Math.max(1.0, config.getDouble("finance.keypad.max-amount", 1_000_000.0));
        this.transferFee = Math.max(0.0, config.getDouble("finance.fees.transfer", 0.0));
        this.atmWithdrawFee = Math.max(0.0, config.getDouble("finance.fees.atm-withdraw", 0.0));

        this.vendingIntegrationEnabled = config.getBoolean("integration.vending.enabled", true);
        this.vendingRestockRequiresManager = config.getBoolean("integration.vending.restock-requires-manager", false);
        this.vendingDefaultMachineLimit = config.getInt("integration.vending.default-machine-limit", -1);

        this.debug = config.getBoolean("debug", false);
    }

    public CreationMode creationMode() {
        return creationMode;
    }

    public int maxOwnedPerPlayer() {
        return maxOwnedPerPlayer;
    }

    public int maxMembersPerCompany() {
        return maxMembersPerCompany;
    }

    public double creationCost() {
        return creationCost;
    }

    public long creationCooldownMillis() {
        return creationCooldownMillis;
    }

    public List<String> allowedTypes() {
        return allowedTypes;
    }

    public boolean isTypeAllowed(String type) {
        return type != null && allowedTypes.contains(type.toLowerCase(Locale.ROOT));
    }

    public int nameMinLength() {
        return nameMinLength;
    }

    public int nameMaxLength() {
        return nameMaxLength;
    }

    public Pattern namePattern() {
        return namePattern;
    }

    public List<String> reservedNames() {
        return reservedNames;
    }

    public boolean isReservedName(String name) {
        return name != null && reservedNames.contains(name.trim().toLowerCase(Locale.ROOT));
    }

    public String economyAccount() {
        return economyAccount;
    }

    public String currencySymbol() {
        return currencySymbol;
    }

    public double demoStartingBalance() {
        return demoStartingBalance;
    }

    /** Economy-adapter account key for a player's personal bank account (the one a payment card draws on). */
    public String bankAccount() {
        return bankAccount;
    }

    /** Banknote denominations, descending, for cash issuing/change. Never empty. */
    public List<Integer> denominations() {
        return denominations;
    }

    public String banknoteMaterial() {
        return banknoteMaterial;
    }

    public long payrollIntervalSeconds() {
        return payrollIntervalSeconds;
    }

    public long payrollTickSeconds() {
        return payrollTickSeconds;
    }

    public double keypadMaxAmount() {
        return keypadMaxAmount;
    }

    public double transferFee() {
        return transferFee;
    }

    public double atmWithdrawFee() {
        return atmWithdrawFee;
    }

    public boolean vendingIntegrationEnabled() {
        return vendingIntegrationEnabled;
    }

    public boolean vendingRestockRequiresManager() {
        return vendingRestockRequiresManager;
    }

    public int vendingDefaultMachineLimit() {
        return vendingDefaultMachineLimit;
    }

    public boolean debug() {
        return debug;
    }

    private static List<String> lowerCaseList(List<String> values, String fallbackSingle) {
        if (values == null || values.isEmpty()) {
            return fallbackSingle == null ? List.of() : List.of(fallbackSingle);
        }
        return values.stream()
                .filter(value -> value != null && !value.isBlank())
                .map(value -> value.trim().toLowerCase(Locale.ROOT))
                .distinct()
                .toList();
    }

    /** Keeps only positive denominations, de-duplicated and sorted descending; falls back to a sane set. */
    private static List<Integer> sanitizeDenominations(List<Integer> values) {
        List<Integer> cleaned = values == null ? List.of() : values.stream()
                .filter(value -> value != null && value > 0)
                .distinct()
                .sorted((a, b) -> Integer.compare(b, a))
                .toList();
        return cleaned.isEmpty() ? List.of(500, 100, 50, 20, 10, 5, 1) : cleaned;
    }

    private static Pattern compile(String regex) {
        if (regex == null || regex.isBlank()) {
            return DEFAULT_NAME_PATTERN;
        }
        try {
            return Pattern.compile(regex);
        } catch (PatternSyntaxException ignored) {
            return DEFAULT_NAME_PATTERN;
        }
    }
}
