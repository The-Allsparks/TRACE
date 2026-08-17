package org.allsparks.trace.ftc;

import org.allsparks.trace.session.TraceSession;

/**
 * Maps FTC OpMode lifecycle callbacks onto TRACE events without importing the
 * FTC SDK.
 */
public final class OpModeLifecycle {
    private final TraceSession session;

    public OpModeLifecycle(TraceSession session) {
        this.session = session;
    }

    public void init() {
        session.onOpModeInit();
    }

    public void start() {
        session.onOpModeStart();
    }

    public void stop() {
        session.onOpModeStop();
    }

    public void onException(Throwable throwable) {
        session.recordException(throwable);
    }
}
