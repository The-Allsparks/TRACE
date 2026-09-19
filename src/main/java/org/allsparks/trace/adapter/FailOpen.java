package org.allsparks.trace.adapter;

import org.allsparks.trace.Trace;

/**
 * Runs TeamCode sibling-sink work so a TRACE fault cannot stop drive.
 * TRACE does not import SHIFT, AMPER, or Pedro types. Wrap the adapter call,
 * not Hub hardware.
 */
public final class FailOpen {
    private FailOpen() {}

    public static void run(Runnable action) {
        if (action == null) {
            return;
        }
        try {
            action.run();
        } catch (RuntimeException | Error ex) {
            try {
                Trace.session().recordException(ex);
            } catch (RuntimeException | Error ignored) {
                // Recording the failure also failed. Drive still continues.
            }
        }
    }
}
