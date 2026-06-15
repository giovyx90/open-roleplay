package dev.openrp.vending.hook;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.bukkit.entity.Player;
import dev.openrp.vending.model.MachineProduct;
import dev.openrp.vending.model.VendingMachine;

/**
 * Holds the registered {@link VendingHook}s and folds their results. Gate methods short-circuit on
 * the first deny; resolver methods chain each hook's output into the next. A misbehaving hook that
 * throws is isolated so it cannot abort a transaction.
 */
public final class HookExecutor {

    private final List<VendingHook> hooks = new CopyOnWriteArrayList<>();
    private final java.util.logging.Logger logger;

    public HookExecutor(java.util.logging.Logger logger) {
        this.logger = logger;
    }

    public void register(VendingHook hook) {
        if (hook != null && !hooks.contains(hook)) {
            hooks.add(hook);
        }
    }

    public void unregister(VendingHook hook) {
        hooks.remove(hook);
    }

    public void clear() {
        hooks.clear();
    }

    public VendingDecision canUse(Player player, VendingMachine machine) {
        for (VendingHook hook : hooks) {
            VendingDecision decision = guard(() -> hook.canPlayerUseMachine(player, machine), "canPlayerUseMachine");
            if (decision != null && decision.denied()) {
                return decision;
            }
        }
        return VendingDecision.allow();
    }

    public VendingDecision canRestock(Player player, VendingMachine machine) {
        for (VendingHook hook : hooks) {
            VendingDecision decision = guard(() -> hook.canPlayerRestockMachine(player, machine), "canPlayerRestockMachine");
            if (decision != null && decision.denied()) {
                return decision;
            }
        }
        return VendingDecision.allow();
    }

    public VendingDecision canWithdraw(Player player, VendingMachine machine) {
        for (VendingHook hook : hooks) {
            VendingDecision decision = guard(() -> hook.canPlayerWithdrawCash(player, machine), "canPlayerWithdrawCash");
            if (decision != null && decision.denied()) {
                return decision;
            }
        }
        return VendingDecision.allow();
    }

    public double resolvePrice(Player player, VendingMachine machine, MachineProduct product, double basePrice) {
        double price = basePrice;
        for (VendingHook hook : hooks) {
            try {
                price = hook.resolveProductPrice(player, machine, product, price);
            } catch (RuntimeException exception) {
                warn("resolveProductPrice", exception);
            }
        }
        return Math.max(0.0, price);
    }

    public int resolveCompanyLimit(String companyId, int baseLimit) {
        int limit = baseLimit;
        for (VendingHook hook : hooks) {
            try {
                limit = hook.resolveCompanyLimit(companyId, limit);
            } catch (RuntimeException exception) {
                warn("resolveCompanyLimit", exception);
            }
        }
        return limit;
    }

    public VendingDecision beforePayment(PurchaseContext context) {
        for (VendingHook hook : hooks) {
            VendingDecision decision = guard(() -> hook.beforePayment(context), "beforePayment");
            if (decision != null && decision.denied()) {
                return decision;
            }
        }
        return VendingDecision.allow();
    }

    public void afterPayment(PurchaseContext context) {
        for (VendingHook hook : hooks) {
            try {
                hook.afterPayment(context);
            } catch (RuntimeException exception) {
                warn("afterPayment", exception);
            }
        }
    }

    private VendingDecision guard(java.util.function.Supplier<VendingDecision> call, String name) {
        try {
            return call.get();
        } catch (RuntimeException exception) {
            warn(name, exception);
            return VendingDecision.allow();
        }
    }

    private void warn(String method, RuntimeException exception) {
        if (logger != null) {
            logger.warning("[OpenVendingMachines] Hook " + method + " threw: " + exception.getMessage());
        }
    }
}
