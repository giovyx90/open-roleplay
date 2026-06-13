package dev.openrp.weapons.model;

/**
 * Visual states for weapon models. Each state can map to a different
 * CustomModelData value so that Blockbench models can show distinct
 * firstperson displays (e.g. idle in main hand, aimed in off hand,
 * reloading animation).
 *
 * <p>Weapons define their per-state CMD values in {@code weapons.yml}
 * under the {@code visual-states} section. Weapons without this section
 * keep a single static model.
 */
public enum WeaponVisualState {

    /** Default state — weapon held normally in main hand. */
    IDLE,

    /** Aiming / ADS — weapon is in off hand, model centered on crosshair via firstperson_lefthand display. */
    AIMING,

    /** Reloading — brief animation state while a magazine is being swapped. */
    RELOADING
}
