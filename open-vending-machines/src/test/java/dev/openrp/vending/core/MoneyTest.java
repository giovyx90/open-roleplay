package dev.openrp.vending.core;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class MoneyTest {

    @Test
    public void formatsTwoDecimals() {
        assertEquals("3.00", Money.format(3.0));
        assertEquals("2.50", Money.format(2.5));
        assertEquals("10.00", Money.format(10.0));
    }

    @Test
    public void roundsToCents() {
        assertEquals(3.46, Money.round(3.456), 1e-9);
        assertEquals(3.45, Money.round(3.454), 1e-9);
    }
}
