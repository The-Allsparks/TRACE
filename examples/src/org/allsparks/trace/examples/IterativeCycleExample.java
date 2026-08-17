package org.allsparks.trace.examples;

import org.allsparks.trace.core.Pose2d;
import org.allsparks.trace.core.Units;
import org.allsparks.trace.session.TraceCycle;
import org.allsparks.trace.session.TraceSession;
import org.allsparks.trace.TraceConfig;
import org.allsparks.trace.TraceMode;

/**
 * Iterative-style loop using {@link TraceCycle}. Decision logic stays in the
 * team's controller; TRACE only records.
 */
public final class IterativeCycleExample {
    public static TraceSession run(int loops, double voltage, Pose2d pose) {
        TraceSession session = new TraceSession(TraceConfig.builder()
                .mode(TraceMode.FULL)
                .opModeName("IterativeCycleExample")
                .memorySink(true)
                .build());
        session.onOpModeInit();
        session.onOpModeStart();
        for (int i = 0; i < loops; i++) {
            try (TraceCycle cycle = session.beginCycle()) {
                cycle.recordInput("Drive/Pose", pose);
                cycle.recordInput("AMPER/Battery/Voltage", voltage, Units.VOLTS);
                double command = voltage > 12.0 ? 1.0 : 0.5;
                cycle.recordOutput("Drive/Command", command, Units.DIMENSIONLESS);
            }
        }
        session.onOpModeStop();
        return session;
    }

    private IterativeCycleExample() {}
}
