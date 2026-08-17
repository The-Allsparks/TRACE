package org.allsparks.trace.examples;

import org.allsparks.trace.Trace;
import org.allsparks.trace.TraceConfig;
import org.allsparks.trace.TraceMode;
import org.allsparks.trace.core.Pose2d;
import org.allsparks.trace.core.Units;
import org.allsparks.trace.ftc.OpModeLifecycle;
import org.allsparks.trace.session.TraceCycle;

/**
 * Event-only adoption sketch. This class compiles without the FTC SDK so
 * students can read it on a laptop. On a robot, call the same methods from
 * {@code LinearOpMode} or {@code OpMode} lifecycle methods.
 */
public final class EventOnlyExample {
    public static void run() {
        Trace.configure(TraceConfig.builder()
                .mode(TraceMode.EVENTS)
                .opModeName("EventOnlyExample")
                .memorySink(true)
                .build());
        OpModeLifecycle lifecycle = new OpModeLifecycle(Trace.session());
        lifecycle.init();
        lifecycle.start();
        Trace.event("Autonomous started");
        Trace.event("Path started");
        lifecycle.stop();
    }

    private EventOnlyExample() {}
}
