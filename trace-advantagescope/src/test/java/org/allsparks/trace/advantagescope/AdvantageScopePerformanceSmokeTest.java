package org.allsparks.trace.advantagescope;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.allsparks.trace.TraceConfig;
import org.allsparks.trace.TraceMode;
import org.allsparks.trace.core.Units;
import org.allsparks.trace.session.TraceSession;
import org.junit.jupiter.api.Test;

class AdvantageScopePerformanceSmokeTest {
    @Test
    void streamingDisabledMatchesCoreBudget() {
        measure(false);
    }

    @Test
    void streamingEnabledWithNoClientStaysUnderBudget() {
        measure(true);
    }

    @Test
    void streamingEnabledWithOneClientKeepsProducerUnderBudget() throws Exception {
        TraceSession session = new TraceSession(TraceConfig.builder()
                .mode(TraceMode.ESSENTIAL)
                .memorySink(true)
                .essentialSampleIntervalNanos(0)
                .memoryCapacity(4096)
                .advantageScopeStreaming(true)
                .advantageScopePort(0)
                .advantageScopeRateHz(20)
                .build());
        TestWsClient client = TestWsClient.connect(session.advantageScopeLive().listenPort());
        try {
            client.send("{\"type\":\"GET_ROBOT_STATUS\"}");
            client.waitFor("RECEIVE_ROBOT_STATUS", 2000);
            double nsPerLoop = loop(session, 3000);
            assertTrue(nsPerLoop < 5_000_000.0, "desktop per-loop recording exceeded 5 ms: " + nsPerLoop);
            System.out.println("TRACE+AS desktop per-loop ns="
                    + nsPerLoop
                    + " frames="
                    + session.advantageScopeLive().framesSent());
        } finally {
            client.close();
            session.close();
        }
    }

    private static void measure(boolean streaming) {
        TraceSession session = new TraceSession(TraceConfig.builder()
                .mode(TraceMode.ESSENTIAL)
                .memorySink(true)
                .essentialSampleIntervalNanos(0)
                .memoryCapacity(4096)
                .advantageScopeStreaming(streaming)
                .advantageScopePort(0)
                .build());
        try {
            double nsPerLoop = loop(session, 5000);
            assertTrue(nsPerLoop < 5_000_000.0, "desktop per-loop recording exceeded 5 ms: " + nsPerLoop);
            System.out.println("TRACE desktop streaming=" + streaming + " per-loop ns=" + nsPerLoop);
        } finally {
            session.close();
        }
    }

    private static double loop(TraceSession session, int iterations) {
        long start = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            try (var cycle = session.beginCycle()) {
                cycle.recordInput("AMPER/Battery/Voltage", 12.8, Units.VOLTS);
                cycle.recordOutput("Drive/Command", 0.3, Units.DIMENSIONLESS);
            }
        }
        return (System.nanoTime() - start) / (double) iterations;
    }
}
