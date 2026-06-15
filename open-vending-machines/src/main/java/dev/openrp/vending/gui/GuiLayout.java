package dev.openrp.vending.gui;

/** Slot layout constants for the default chest GUI (double chest, 54 slots). */
public final class GuiLayout {

    public static final int SIZE = 54;

    /** Products occupy the first five rows (slots 0-44). */
    public static final int PRODUCT_SLOTS = 45;

    // Purchase view controls (bottom row)
    public static final int MANAGE_BUTTON = 53;

    // Management view controls (bottom row)
    public static final int WITHDRAW_BUTTON = 45;
    public static final int INFO_BUTTON = 49;
    public static final int BACK_BUTTON = 53;

    private GuiLayout() {
    }
}
