package dev.openrp.politics.adapter;

import java.util.Optional;

/**
 * Bridge to a public-budget backend (e.g. Open Economy). Open Politics never holds money: it only
 * certifies <em>who</em> may access a government's public account through the {@code MANAGE_BUDGET}
 * capability, and reads the balance for display. The default is a no-op whose {@link #available()}
 * reports {@code false}, so a budget command degrades to "no economy connected".
 */
public interface EconomyAdapter {

    String id();

    boolean available();

    /** The public account id bound to a government, when the backend knows one. */
    Optional<String> budgetAccount(String governmentId);

    /** Balance of a public account, or empty when the backend cannot answer. */
    Optional<Long> balance(String account);
}
