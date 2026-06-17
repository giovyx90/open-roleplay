package dev.openrp.crime.module;

/**
 * A toggleable subsystem (syndicate, production, traffic, laundering, racket). Each is conceptually a
 * separate plugin that depends on the core; here they ship as one configurable plugin and are turned
 * on or off through {@code config.yml}'s {@code modules:} block. A disabled module registers nothing.
 */
public interface CrimeModule {

    String id();

    /** Wires the module's commands, listeners and tasks. Called once on enable if the module is on. */
    void enable();

    /** Cancels tasks and releases anything the module started. Called on disable / reload. */
    void disable();
}
