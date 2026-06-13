package dev.openrp.weapons.model;

public enum WeaponCategory {
    PISTOL("Pistol"),
    SMG("SMG"),
    ASSAULT_RIFLE("Assault Rifle"),
    SEMI_AUTO_RIFLE("Semi-Auto Rifle"),
    SNIPER("Sniper Rifle"),
    SHOTGUN("Shotgun"),
    TASER("Taser"),
    MELEE("Melee");

    private final String displayName;

    WeaponCategory(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
