package dev.openrp.companies.model;

import java.util.Locale;

/**
 * Catalogue of physical company assets that can be registered in the world. The company core only
 * stores <em>where</em> an asset is and <em>which company</em> owns it, plus the minimum role level
 * required to use and to manage it. The actual behaviour of an asset (a working POS, a printer that
 * spits out documents, an LED panel that renders text, ...) is left to vertical modules that look the
 * asset up through {@link dev.openrp.companies.api.CompanyAssetService}.
 *
 * <p>Each type carries a sensible default {@code use} and {@code manage} role level so a server gets
 * working authorization out of the box; these can be overridden in config.</p>
 */
public enum CompanyAssetType {
    /** Company workstation / computer terminal. */
    COMPANY_TERMINAL(CompanyRole.EMPLOYEE, CompanyRole.MANAGER),
    HOLOGRAM_PROJECTOR(CompanyRole.EMPLOYEE, CompanyRole.MANAGER),
    LED_PANEL(CompanyRole.EMPLOYEE, CompanyRole.MANAGER),
    PRINTER(CompanyRole.EMPLOYEE, CompanyRole.MANAGER),
    POS(CompanyRole.EMPLOYEE, CompanyRole.MANAGER),
    CASH_REGISTER(CompanyRole.EMPLOYEE, CompanyRole.MANAGER),
    STORAGE(CompanyRole.EMPLOYEE, CompanyRole.MANAGER),
    SAFE(CompanyRole.MANAGER, CompanyRole.DIRECTOR),
    BADGE_READER(CompanyRole.EMPLOYEE, CompanyRole.MANAGER),
    RECEPTION_KIOSK(CompanyRole.TRAINING, CompanyRole.MANAGER),
    PRODUCT_DISPLAY(CompanyRole.EMPLOYEE, CompanyRole.MANAGER);

    private final CompanyRole defaultUseRole;
    private final CompanyRole defaultManageRole;

    CompanyAssetType(CompanyRole defaultUseRole, CompanyRole defaultManageRole) {
        this.defaultUseRole = defaultUseRole;
        this.defaultManageRole = defaultManageRole;
    }

    /** Lowest role allowed to use this asset, by default. */
    public CompanyRole defaultUseRole() {
        return defaultUseRole;
    }

    /** Lowest role allowed to register/move/remove this asset, by default. */
    public CompanyRole defaultManageRole() {
        return defaultManageRole;
    }

    public String key() {
        return name().toLowerCase(Locale.ROOT);
    }

    /** Lenient parser; returns {@code null} for unknown values. */
    public static CompanyAssetType fromString(String value) {
        if (value == null) {
            return null;
        }
        try {
            return valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }
}
