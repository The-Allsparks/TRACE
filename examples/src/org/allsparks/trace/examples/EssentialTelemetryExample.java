package org.allsparks.trace.examples;

import org.allsparks.trace.Trace;
import org.allsparks.trace.TraceConfig;
import org.allsparks.trace.TraceMode;
import org.allsparks.trace.core.Pose2d;
import org.allsparks.trace.core.Units;

/**
 * Essential telemetry sketch for graphing a few operational signals.
 */
public final class EssentialTelemetryExample {
    public static void run(double voltage, Pose2d pose) {
        Trace.configure(TraceConfig.builder()
                .mode(TraceMode.ESSENTIAL)
                .opModeName("EssentialTelemetryExample")
                .essentialSampleIntervalNanos(50_000_000L)
                .memorySink(true)
                .build());
        Trace.event("Autonomous started");
        Trace.record("Battery/Voltage", voltage, Units.VOLTS);
        Trace.record("Drive/Pose", pose);
        Trace.recordInput("AMPER/Battery/Voltage", voltage, Units.VOLTS);
        Trace.stop();
    }

    private EssentialTelemetryExample() {}
}
