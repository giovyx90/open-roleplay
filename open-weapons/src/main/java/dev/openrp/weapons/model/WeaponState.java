package dev.openrp.weapons.model;

public class WeaponState {
    private int currentAmmo;
    private boolean reloading;
    private long lastShotTime; // Current millis
    private FireMode fireMode;
    private boolean hasMagazine;

    public WeaponState(int maxAmmo) {
        this(maxAmmo, FireMode.SEMI);
    }

    public WeaponState(int maxAmmo, FireMode fireMode) {
        this.currentAmmo = maxAmmo;
        this.reloading = false;
        this.lastShotTime = 0;
        this.fireMode = fireMode;
        this.hasMagazine = true;
    }

    public int getCurrentAmmo() {
        return currentAmmo;
    }

    public void setCurrentAmmo(int currentAmmo) {
        this.currentAmmo = currentAmmo;
    }

    public boolean isReloading() {
        return reloading;
    }

    public void setReloading(boolean reloading) {
        this.reloading = reloading;
    }

    public long getLastShotTime() {
        return lastShotTime;
    }

    public void setLastShotTime(long lastShotTime) {
        this.lastShotTime = lastShotTime;
    }

    public FireMode getFireMode() {
        return fireMode;
    }

    public void setFireMode(FireMode fireMode) {
        this.fireMode = fireMode;
    }

    public boolean hasMagazine() {
        return hasMagazine;
    }

    public void setHasMagazine(boolean hasMagazine) {
        this.hasMagazine = hasMagazine;
    }
}
