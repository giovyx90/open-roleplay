package dev.openrp.vending.adapter;

/**
 * Audit sink for money- and stock-affecting actions. The core logs every purchase, restock,
 * withdrawal and lifecycle change here; route it to a file, the console, a database or nowhere.
 */
public interface LoggingAdapter {

    String id();

    /** {@code category} is a short tag such as PURCHASE, RESTOCK, WITHDRAW, CREATE, REMOVE, STATE. */
    void log(String category, String message);

    default void close() {
    }
}
