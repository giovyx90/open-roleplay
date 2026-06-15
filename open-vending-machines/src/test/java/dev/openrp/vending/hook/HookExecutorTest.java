package dev.openrp.vending.hook;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.UUID;
import org.bukkit.entity.Player;
import org.junit.Test;
import dev.openrp.vending.model.MachineLocation;
import dev.openrp.vending.model.MachineProduct;
import dev.openrp.vending.model.VendingMachine;

public class HookExecutorTest {

    private VendingMachine machine() {
        return new VendingMachine(UUID.randomUUID(), "snack", new MachineLocation("world", 0, 0, 0), null);
    }

    @Test
    public void firstDenyWins() {
        HookExecutor executor = new HookExecutor(null);
        executor.register(new VendingHook() {
            @Override
            public VendingDecision canPlayerUseMachine(Player player, VendingMachine vendingMachine) {
                return VendingDecision.deny("nope");
            }
        });
        VendingDecision decision = executor.canUse(null, machine());
        assertTrue(decision.denied());
        assertEquals("nope", decision.reason());
    }

    @Test
    public void resolvePriceFoldsInRegistrationOrder() {
        HookExecutor executor = new HookExecutor(null);
        executor.register(new VendingHook() {
            @Override
            public double resolveProductPrice(Player player, VendingMachine vendingMachine, MachineProduct product, double current) {
                return current + 1;
            }
        });
        executor.register(new VendingHook() {
            @Override
            public double resolveProductPrice(Player player, VendingMachine vendingMachine, MachineProduct product, double current) {
                return current * 2;
            }
        });
        assertEquals(22.0, executor.resolvePrice(null, machine(), null, 10.0), 1e-9);
    }

    @Test
    public void resolvePriceNeverNegative() {
        HookExecutor executor = new HookExecutor(null);
        executor.register(new VendingHook() {
            @Override
            public double resolveProductPrice(Player player, VendingMachine vendingMachine, MachineProduct product, double current) {
                return -50.0;
            }
        });
        assertEquals(0.0, executor.resolvePrice(null, machine(), null, 10.0), 1e-9);
    }

    @Test
    public void resolveCompanyLimitFolds() {
        HookExecutor executor = new HookExecutor(null);
        executor.register(new VendingHook() {
            @Override
            public int resolveCompanyLimit(String companyId, int current) {
                return current + 5;
            }
        });
        assertEquals(8, executor.resolveCompanyLimit("acme", 3));
    }

    @Test
    public void throwingHookIsIsolated() {
        HookExecutor executor = new HookExecutor(null);
        executor.register(new VendingHook() {
            @Override
            public VendingDecision canPlayerUseMachine(Player player, VendingMachine vendingMachine) {
                throw new IllegalStateException("boom");
            }
        });
        assertFalse(executor.canUse(null, machine()).denied());
    }

    @Test
    public void unregisterStopsHook() {
        HookExecutor executor = new HookExecutor(null);
        VendingHook hook = new VendingHook() {
            @Override
            public VendingDecision canPlayerUseMachine(Player player, VendingMachine vendingMachine) {
                return VendingDecision.deny("x");
            }
        };
        executor.register(hook);
        assertTrue(executor.canUse(null, machine()).denied());
        executor.unregister(hook);
        assertFalse(executor.canUse(null, machine()).denied());
    }
}
