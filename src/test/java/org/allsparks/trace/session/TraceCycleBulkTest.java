package org.allsparks.trace.session;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.stream.Collectors;
import org.allsparks.trace.TraceConfig;
import org.allsparks.trace.TraceMode;
import org.allsparks.trace.core.RecordCategory;
import org.allsparks.trace.core.TraceRecord;
import org.allsparks.trace.core.Units;
import org.junit.jupiter.api.Test;

class TraceCycleBulkTest {
    @Test
    void recordOutputsWritesEachNamedScalar() {
        TraceSession session = openSession();
        try (TraceCycle cycle = session.beginCycle()) {
            cycle.recordOutputs(
                    new String[] {"Drive/Command/FL", "Drive/Command/FR", "Drive/Pose/X"},
                    new double[] {0.1, 0.2, 12.0},
                    new Units[] {Units.DIMENSIONLESS, Units.DIMENSIONLESS, Units.INCHES});
        }
        List<TraceRecord> drive = driveRecords(session);
        assertEquals(3, drive.size());
        assertEquals(0.1, named(session, "Drive/Command/FL").value().asDouble(), 1e-9);
        assertEquals(12.0, named(session, "Drive/Pose/X").value().asDouble(), 1e-9);
        assertEquals(Units.INCHES, named(session, "Drive/Pose/X").units());
        session.close();
    }

    @Test
    void recordOutputsSkipsNullNameAndNaN() {
        TraceSession session = openSession();
        try (TraceCycle cycle = session.beginCycle()) {
            cycle.recordOutputs(
                    new String[] {"Drive/Command/FL", null, "Drive/Heading"},
                    new double[] {0.4, 0.5, Double.NaN},
                    Units.DIMENSIONLESS);
        }
        List<TraceRecord> drive = driveRecords(session);
        assertEquals(1, drive.size());
        assertEquals("Drive/Command/FL", drive.get(0).name().value());
        session.close();
    }

    @Test
    void reusedBatchClearsBetweenLoops() {
        TraceSession session = openSession();
        TraceBatch batch = new TraceBatch(4);
        try (TraceCycle cycle = session.beginCycle()) {
            batch.add("Drive/Command/FL", 0.1, Units.DIMENSIONLESS).add("Drive/Command/FR", 0.2, Units.DIMENSIONLESS);
            cycle.recordOutputs(batch);
        }
        batch.clear();
        try (TraceCycle cycle = session.beginCycle()) {
            batch.add("Drive/Command/BL", 0.3, Units.DIMENSIONLESS);
            cycle.recordOutputs(batch);
        }
        List<String> names = driveRecords(session).stream().map(r -> r.name().value()).collect(Collectors.toList());
        assertTrue(names.contains("Drive/Command/FL"));
        assertTrue(names.contains("Drive/Command/BL"));
        assertEquals(1, names.stream().filter(n -> n.equals("Drive/Command/BL")).count());
        session.close();
    }

    @Test
    void recordInputsKeepInputCategory() {
        TraceSession session = openSession();
        TraceBatch batch = new TraceBatch(2);
        batch.add("Drive/TranslationX", 0.8, Units.DIMENSIONLESS);
        try (TraceCycle cycle = session.beginCycle()) {
            cycle.recordInputs(batch);
        }
        TraceRecord record = named(session, "Drive/TranslationX");
        assertEquals(RecordCategory.INPUT, record.category());
        session.close();
    }

    @Test
    void addPastCapacityIsIgnored() {
        TraceBatch batch = new TraceBatch(1);
        batch.add("Drive/A", 1.0, Units.DIMENSIONLESS).add("Drive/B", 2.0, Units.DIMENSIONLESS);
        assertEquals(1, batch.size());
    }

    @Test
    void batchGroupAndSlotSurviveRecordOutputs() {
        TraceSession session = openSession();
        TraceBatch batch = new TraceBatch(2);
        batch.add("Drive/Command/FL", 0.1, Units.DIMENSIONLESS, "FL pwr/ticks")
                .add("Drive/Ticks/FL", 12.0, Units.COUNTS, "FL pwr/ticks");
        CapturingAdapter adapter = new CapturingAdapter();
        session.setTelemetryAdapter(adapter);
        try (TraceCycle cycle = session.beginCycle()) {
            cycle.recordOutputs(batch);
            session.publishTelemetry();
            assertEquals("FL pwr/ticks", adapter.published.keySet().iterator().next());
        }
        session.close();
    }

    private static final class CapturingAdapter implements org.allsparks.trace.ftc.FtcTelemetryAdapter {
        final java.util.Map<String, Object> published = new java.util.LinkedHashMap<>();

        @Override
        public void publish(String key, Object value) {
            published.put(key, value);
        }
    }

    private static TraceSession openSession() {
        return new TraceSession(TraceConfig.builder()
                .mode(TraceMode.FULL)
                .memorySink(true)
                .build());
    }

    private static List<TraceRecord> driveRecords(TraceSession session) {
        return session.recorded().stream()
                .filter(r -> r.name().value().startsWith("Drive/"))
                .collect(Collectors.toList());
    }

    private static TraceRecord named(TraceSession session, String name) {
        return session.recorded().stream()
                .filter(r -> r.name().value().equals(name))
                .findFirst()
                .orElseThrow(() -> new AssertionError("missing " + name));
    }
}
