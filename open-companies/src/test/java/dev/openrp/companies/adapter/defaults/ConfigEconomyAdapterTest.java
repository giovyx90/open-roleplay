package dev.openrp.companies.adapter.defaults;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Test;

public class ConfigEconomyAdapterTest {

    @Test
    public void withdrawRejectsNegativeAndInsufficient() {
        ConfigEconomyAdapter eco = new ConfigEconomyAdapter(0.0);
        assertFalse(eco.withdrawFromAccount("acme", -5.0));
        assertFalse(eco.withdrawFromAccount("acme", 1.0)); // empty account
        assertTrue(eco.depositToAccount("acme", 10.0));
        assertTrue(eco.withdrawFromAccount("acme", 4.0));
        assertEquals(6.0, eco.accountBalance("acme"), 1e-9);
    }

    @Test
    public void firstDepositPreservesStartingBalance() {
        ConfigEconomyAdapter eco = new ConfigEconomyAdapter(100.0);
        org.bukkit.OfflinePlayer player = mockPlayer();
        // A wallet that has never been touched is worth the starting balance; depositing must add to
        // it, not reset it to the deposit amount.
        assertTrue(eco.deposit(player, "cash", 50.0));
        assertEquals(150.0, eco.balance(player, "cash"), 1e-9);
    }

    @Test
    public void concurrentAccountWithdrawalsNeverOverdraw() throws Exception {
        ConfigEconomyAdapter eco = new ConfigEconomyAdapter(0.0);
        eco.depositToAccount("vault", 100.0);

        int threads = 16;
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threads);
        AtomicInteger succeeded = new AtomicInteger();
        for (int i = 0; i < threads; i++) {
            new Thread(() -> {
                try {
                    start.await();
                    for (int j = 0; j < 50; j++) {
                        if (eco.withdrawFromAccount("vault", 1.0)) {
                            succeeded.incrementAndGet();
                        }
                    }
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                } finally {
                    done.countDown();
                }
            }).start();
        }
        start.countDown();
        done.await();

        // Exactly 100 single-unit withdrawals may succeed; the balance can never go negative.
        assertEquals(100, succeeded.get());
        assertEquals(0.0, eco.accountBalance("vault"), 1e-9);
    }

    private static org.bukkit.OfflinePlayer mockPlayer() {
        java.util.UUID id = java.util.UUID.randomUUID();
        return (org.bukkit.OfflinePlayer) java.lang.reflect.Proxy.newProxyInstance(
                ConfigEconomyAdapterTest.class.getClassLoader(),
                new Class<?>[]{org.bukkit.OfflinePlayer.class},
                (proxy, method, args) -> {
                    if ("getUniqueId".equals(method.getName())) {
                        return id;
                    }
                    if ("equals".equals(method.getName())) {
                        return proxy == args[0];
                    }
                    if ("hashCode".equals(method.getName())) {
                        return System.identityHashCode(proxy);
                    }
                    Class<?> rt = method.getReturnType();
                    if (rt == boolean.class) {
                        return false;
                    }
                    if (rt.isPrimitive()) {
                        return 0;
                    }
                    return null;
                });
    }
}
