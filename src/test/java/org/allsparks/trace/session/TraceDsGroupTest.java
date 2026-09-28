package org.allsparks.trace.session;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.allsparks.trace.TraceConfig;
import org.allsparks.trace.TraceMode;
import org.allsparks.trace.core.TypedValue;
import org.allsparks.trace.core.Units;
import org.allsparks.trace.ftc.FtcTelemetryAdapter;
import org.junit.jupiter.api.Test;

class TraceDsGroupTest {
    @Test
    void packedCaptionJoinsInAddOrder() {
        CapturingAdapter adapter = new CapturingAdapter();
        TraceSession session = openSession(adapter);
        try (TraceCycle cycle = session.beginCycle()) {
            cycle.recordOutput("Drive/Command/FL", 0.1, Units.DIMENSIONLESS, "FL pwr/ticks/vel");
            cycle.recordOutput("Drive/Ticks/FL", 12.0, Units.COUNTS, "FL pwr/ticks/vel");
            cycle.recordOutput("PULSE/Bulk/front_left_drive/velocity", 42.0, Units.DIMENSIONLESS, "FL pwr/ticks/vel");
            cycle.publishTelemetry();
            assertEquals(packed(0.1, 12.0, 42.0), adapter.published.get("FL pwr/ticks/vel"));
            assertEquals(1, adapter.published.size());
        }
        session.close();
    }

    @Test
    void laterAddAppendsAfterEarlierAdd() {
        CapturingAdapter adapter = new CapturingAdapter();
        TraceSession session = openSession(adapter);
        try (TraceCycle cycle = session.beginCycle()) {
            cycle.recordOutput("Drive/Command/FL", 0.1, Units.DIMENSIONLESS, "FL pwr/ticks/vel");
            cycle.recordOutput("PULSE/Bulk/front_left_drive/velocity", 42.0, Units.DIMENSIONLESS, "FL pwr/ticks/vel");
            cycle.publishTelemetry();
            assertEquals(packed(0.1, 42.0), adapter.published.get("FL pwr/ticks/vel"));
        }
        session.close();
    }

    @Test
    void ungroupedRecordDoesNotPublish() {
        CapturingAdapter adapter = new CapturingAdapter();
        TraceSession session = openSession(adapter);
        try (TraceCycle cycle = session.beginCycle()) {
            cycle.recordOutput("Drive/MixX", 0.8, Units.DIMENSIONLESS);
            cycle.publishTelemetry();
            assertTrue(adapter.published.isEmpty());
        }
        assertEquals(0.8, named(session, "Drive/MixX").value().asDouble(), 1e-9);
        session.close();
    }

    @Test
    void endCycleDropsUnpublishedGroups() {
        CapturingAdapter adapter = new CapturingAdapter();
        TraceSession session = openSession(adapter);
        try (TraceCycle cycle = session.beginCycle()) {
            cycle.recordOutput("Drive/Command/FL", 0.1, Units.DIMENSIONLESS, "FL pwr/ticks/vel");
        }
        session.publishTelemetry();
        assertFalse(adapter.published.containsKey("FL pwr/ticks/vel"));
        session.close();
    }

    @Test
    void lineGroupPublishesUnlabeledLine() {
        CapturingAdapter adapter = new CapturingAdapter();
        TraceSession session = openSession(adapter);
        try (TraceCycle cycle = session.beginCycle()) {
            cycle.recordOutput("Init/Note/WalkBeside", "Pedro teleop.", FtcTelemetryAdapter.LINE);
            cycle.recordOutput("Drive/MaxSpeed", 0.33, Units.DIMENSIONLESS, "maxSpeed");
            cycle.publishTelemetry();
            assertEquals(1, adapter.lines.size());
            assertEquals("Pedro teleop.", adapter.lines.get(0));
            assertEquals(packed(0.33), adapter.published.get("maxSpeed"));
        }
        session.close();
    }

    @Test
    void stringRecordPublishesItsGroup() {
        CapturingAdapter adapter = new CapturingAdapter();
        TraceSession session = openSession(adapter);
        try (TraceCycle cycle = session.beginCycle()) {
            cycle.recordOutput("Drive/Frame", "field", "drive.frame");
            cycle.publishTelemetry();
            assertEquals("field", adapter.published.get("drive.frame"));
            assertEquals(1, adapter.published.size());
        }
        session.close();
    }

    @Test
    void publishTelemetryEmitsOnlyGroupsFromThisCycle() {
        CapturingAdapter adapter = new CapturingAdapter();
        TraceSession session = openSession(adapter);
        try (TraceCycle cycle = session.beginCycle()) {
            cycle.recordOutput("Drive/Command/FL", 0.1, Units.DIMENSIONLESS, "FL pwr/ticks");
            cycle.publishTelemetry();
            assertEquals(1, adapter.published.size());
            assertTrue(adapter.published.containsKey("FL pwr/ticks"));
        }
        adapter.published.clear();
        try (TraceCycle cycle = session.beginCycle()) {
            cycle.recordOutput("Drive/Command/FR", 0.2, Units.DIMENSIONLESS, "FR pwr/ticks");
            cycle.publishTelemetry();
            assertEquals(1, adapter.published.size());
            assertTrue(adapter.published.containsKey("FR pwr/ticks"));
            assertFalse(adapter.published.containsKey("FL pwr/ticks"));
        }
        adapter.published.clear();
        try (TraceCycle cycle = session.beginCycle()) {
            cycle.recordOutput("Drive/Command/FL", 0.3, Units.DIMENSIONLESS, "FL pwr/ticks");
            cycle.publishTelemetry();
        }
        assertFalse(adapter.published.containsKey("TRACE/Health/Dropped"));
        assertFalse(adapter.published.containsKey("TRACE/Loop/DurationNs"));
        session.close();
    }

    @Test
    void batchRowCarriesGroup() {
        CapturingAdapter adapter = new CapturingAdapter();
        TraceSession session = openSession(adapter);
        TraceBatch batch = new TraceBatch(4);
        try (TraceCycle cycle = session.beginCycle()) {
            batch.add("Drive/Command/FL", 0.1, Units.DIMENSIONLESS, "FL pwr/ticks")
                    .add("Drive/Ticks/FL", 12.0, Units.COUNTS, "FL pwr/ticks")
                    .add("Drive/MixX", 0.8, Units.DIMENSIONLESS);
            cycle.recordOutputs(batch);
            cycle.publishTelemetry();
            assertEquals(packed(0.1, 12.0), adapter.published.get("FL pwr/ticks"));
            assertFalse(adapter.published.containsKey("Drive/MixX"));
        }
        session.close();
    }

    private static String packed(double... values) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < values.length; i++) {
            if (i > 0) {
                builder.append(" / ");
            }
            builder.append(TypedValue.ofDouble(values[i]).render());
        }
        return builder.toString();
    }

    private static TraceSession openSession(FtcTelemetryAdapter adapter) {
        TraceSession session = new TraceSession(TraceConfig.builder()
                .mode(TraceMode.FULL)
                .memorySink(true)
                .build());
        session.setTelemetryAdapter(adapter);
        return session;
    }

    private static org.allsparks.trace.core.TraceRecord named(TraceSession session, String name) {
        return session.recorded().stream()
                .filter(r -> r.name().value().equals(name))
                .findFirst()
                .orElseThrow(() -> new AssertionError("missing " + name));
    }

    private static final class CapturingAdapter implements FtcTelemetryAdapter {
        final Map<String, Object> published = new LinkedHashMap<>();
        final List<String> lines = new ArrayList<String>();

        @Override
        public void publish(String key, Object value) {
            published.put(key, value);
        }

        @Override
        public void publishLine(String line) {
            lines.add(line);
        }
    }
}
