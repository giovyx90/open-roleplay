package dev.openrp.fdo.adapter;

/**
 * Communications and interception. Replaces the hard dependency on a chat plugin: the physical
 * interception device placed in a region stays a roleplay object, but the message transport flows
 * through this adapter. Absent -> no interception; everything else still works.
 */
public interface RadioAdapter {

    String id();

    /** Registers a listener on a frequency, bound to a region. */
    void interceptFrequency(String frequency, String regionId, MessageSink sink);

    void stopIntercept(String frequency, String regionId);

    /** Receives an intercepted message. */
    @FunctionalInterface
    interface MessageSink {
        void accept(String sender, String message);
    }
}
