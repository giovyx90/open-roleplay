package dev.openrp.vending.config;

import org.bukkit.configuration.file.FileConfiguration;

/**
 * Typed, cached view over config.yml. Re-read on every {@code /ovm reload} so the rest of the code
 * never touches raw config keys (and so the keys live in exactly one place).
 */
public final class VendingSettings {

    private String paymentAccount = "cash";
    private String withdrawAccount = "bank";
    private String currencySymbol = "$";
    private double demoStartingBalance = 1000.0;

    private double maxInteractionDistance = 6.0;
    private long cooldownMillis = 750L;
    private boolean requireLineOfSight = false;
    private boolean failIfInventoryFull = true;

    private RestockMode restockMode = RestockMode.PLAYER_INVENTORY;
    private int restockMaxPerAction = 64;
    private boolean allowPriceEditing = true;

    private boolean depositToCompanyAccount = true;
    private boolean logWithdrawals = true;

    private int defaultCompanyLimit = 5;
    private double randomBreakdownChance = 0.0;
    private boolean seedFullOnCreate = true;
    private boolean placeIconBlock = true;
    private boolean furnitureEntities = false;
    private boolean debug = false;

    public void load(FileConfiguration config) {
        this.paymentAccount = config.getString("economy.payment-account", "cash");
        this.withdrawAccount = config.getString("economy.withdraw-account", "bank");
        this.currencySymbol = config.getString("economy.currency-symbol", "$");
        this.demoStartingBalance = config.getDouble("economy.demo-starting-balance", 1000.0);

        this.maxInteractionDistance = config.getDouble("purchase.max-interaction-distance", 6.0);
        this.cooldownMillis = config.getLong("purchase.cooldown-millis", 750L);
        this.requireLineOfSight = config.getBoolean("purchase.require-line-of-sight", false);
        this.failIfInventoryFull = config.getBoolean("purchase.fail-if-inventory-full", true);

        this.restockMode = RestockMode.fromString(config.getString("restock.mode", "player_inventory"));
        this.restockMaxPerAction = Math.max(1, config.getInt("restock.max-per-action", 64));
        this.allowPriceEditing = config.getBoolean("restock.allow-price-editing", true);

        this.depositToCompanyAccount = config.getBoolean("cash.deposit-to-company-account", true);
        this.logWithdrawals = config.getBoolean("cash.log-withdrawals", true);

        this.defaultCompanyLimit = config.getInt("machines.default-company-limit", 5);
        this.randomBreakdownChance = Math.max(0.0, Math.min(1.0, config.getDouble("machines.random-breakdown-chance", 0.0)));
        this.seedFullOnCreate = config.getBoolean("machines.seed-full-on-create", true);
        this.placeIconBlock = config.getBoolean("machines.place-icon-block", true);
        this.furnitureEntities = config.getBoolean("interaction.furniture-entities", false);
        this.debug = config.getBoolean("debug", false);
    }

    public String paymentAccount() {
        return paymentAccount;
    }

    public String withdrawAccount() {
        return withdrawAccount;
    }

    public String currencySymbol() {
        return currencySymbol;
    }

    public double demoStartingBalance() {
        return demoStartingBalance;
    }

    public double maxInteractionDistance() {
        return maxInteractionDistance;
    }

    public double maxInteractionDistanceSquared() {
        return maxInteractionDistance * maxInteractionDistance;
    }

    public long cooldownMillis() {
        return cooldownMillis;
    }

    public boolean requireLineOfSight() {
        return requireLineOfSight;
    }

    public boolean failIfInventoryFull() {
        return failIfInventoryFull;
    }

    public RestockMode restockMode() {
        return restockMode;
    }

    public int restockMaxPerAction() {
        return restockMaxPerAction;
    }

    public boolean allowPriceEditing() {
        return allowPriceEditing;
    }

    public boolean depositToCompanyAccount() {
        return depositToCompanyAccount;
    }

    public boolean logWithdrawals() {
        return logWithdrawals;
    }

    public int defaultCompanyLimit() {
        return defaultCompanyLimit;
    }

    public double randomBreakdownChance() {
        return randomBreakdownChance;
    }

    public boolean seedFullOnCreate() {
        return seedFullOnCreate;
    }

    public boolean placeIconBlock() {
        return placeIconBlock;
    }

    public boolean furnitureEntities() {
        return furnitureEntities;
    }

    public boolean debug() {
        return debug;
    }
}
