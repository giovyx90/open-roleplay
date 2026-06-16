package dev.openrp.companies.item;

import static org.junit.Assert.assertEquals;

import java.util.List;
import java.util.Map;
import org.junit.Test;

public class DenominationsTest {

    private static final List<Integer> DEFAULT = List.of(500, 100, 50, 20, 10, 5, 1);

    @Test
    public void greedyBreakdownIsExactWithDefaultSet() {
        Map<Integer, Integer> breakdown = Denominations.breakdown(1287, DEFAULT);
        assertEquals(1287, Denominations.total(breakdown));
        assertEquals(0, Denominations.remainder(1287, DEFAULT));
        // 1287 = 2x500 + 2x100 + 1x50 + 1x20 + 1x10 + 1x5 + 2x1
        assertEquals(Integer.valueOf(2), breakdown.get(500));
        assertEquals(Integer.valueOf(2), breakdown.get(100));
        assertEquals(Integer.valueOf(1), breakdown.get(50));
        assertEquals(Integer.valueOf(2), breakdown.get(1));
    }

    @Test
    public void zeroAndNegativeProduceNothing() {
        assertEquals(0, Denominations.total(Denominations.breakdown(0, DEFAULT)));
        assertEquals(0, Denominations.total(Denominations.breakdown(-50, DEFAULT)));
    }

    @Test
    public void remainderReportsUnrepresentableAmountWithoutOnes() {
        List<Integer> noOnes = List.of(10, 5);
        // 13 = 1x10 + ... 3 left, cannot be made from {10,5}
        assertEquals(3, Denominations.remainder(13, noOnes));
        assertEquals(0, Denominations.remainder(15, noOnes));
    }
}
