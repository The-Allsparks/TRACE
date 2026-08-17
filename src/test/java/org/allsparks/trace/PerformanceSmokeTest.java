package org.allsparks.trace;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.allsparks.trace.core.Units;
import org.allsparks.trace.session.TraceSession;
import org.junit.jupiter.api.Test;

class PerformanceSmokeTest {
    @Test
    void controlLoopRecordingOverheadIsBoundedOnDesktop() {
        TraceSession session = new TraceSession(TraceConfig.builder()
                .mode(TraceMode.ESSENTIAL)
                .memorySink(true)
                .essentialSampleIntervalNanos(0)
                .memoryCapacity(4096)
                .build());
        int iterations = 5000;
        long start = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            try (var cycle = session.beginCycle()) {
                cycle.recordInput("AMPER/Battery/Voltage", 12.8, Units.VOLTS);
                cycle.recordOutput("Drive/Command", 0.3, Units.DIMENSIONLESS);
            }
        }
        long elapsed = System.nanoTime() - start;
        session.close();
        double nsPerLoop = elapsed / (double) iterations;
        assertTrue(nsPerLoop < 5_000_000.0, "desktop per-loop recording exceeded 5 ms: " + nsPerLoop);
        System.out.println("TRACE desktop per-loop ns=" + nsPerLoop + " accepted=" + session.health().accepted());
    }
}
