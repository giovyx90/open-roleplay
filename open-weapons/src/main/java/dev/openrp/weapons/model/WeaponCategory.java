package dev.openrp.weapons.model;

public enum WeaponCategory {
    PISTOL("Pistola"),
    SMG("SMG"),
    ASSAULT_RIFLE("Fucile d'assalto"),
    SEMI_AUTO_RIFLE("Fucile semiautomatico"),
    SNIPER("Fucile sniper"),
    SHOTGUN("Shotgun"),
    TASER("Taser"),
    MELEE("Corpo a corpo");

    private final String displayName;

    WeaponCategory(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
