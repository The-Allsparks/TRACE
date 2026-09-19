package org.allsparks.trace;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.allsparks.trace.core.Pose2d;
import org.allsparks.trace.core.RecordCategory;
import org.allsparks.trace.core.TraceQuality;
import org.allsparks.trace.core.Units;
import org.allsparks.trace.session.TraceSession;
import org.junit.jupiter.api.Test;

class SignalTypingTest {
    @Test
    void typedSignalsPreserveUnitsAndCategory() {
        TraceSession session = new TraceSession(TraceConfig.builder()
                .mode(TraceMode.ESSENTIAL)
                .memorySink(true)
                .essentialSampleIntervalNanos(0)
                .build());
        session.record("Battery/Voltage", 12.6, Units.VOLTS);
        session.record("Drive/Pose", new Pose2d(1, 2, 0.5));
        session.recordInput("AMPER/Battery/Voltage", 12.6, Units.VOLTS);
        assertTrue(session.recorded().stream().anyMatch(r ->
                r.name().value().equals("Battery/Voltage")
                        && r.units().equals(Units.VOLTS)
                        && r.value().asDouble() == 12.6
                        && r.quality() == TraceQuality.OK));
        assertTrue(session.recorded().stream().anyMatch(r ->
                r.name().value().equals("Drive/Pose") && r.value().asPose().x() == 1.0));
        assertTrue(session.recorded().stream().anyMatch(r ->
                r.name().value().equals("AMPER/Battery/Voltage") && r.category() == RecordCategory.INPUT));
        session.close();
    }

    @Test
    void invalidSignalNameRejected() {
        TraceSession session = new TraceSession(TraceConfig.builder().mode(TraceMode.ESSENTIAL).memorySink(true).build());
        session.record("not a name", 1.0, Units.NONE);
        assertTrue(session.recorded().isEmpty());
        assertTrue(session.drops().count(org.allsparks.trace.core.DropReason.INVALID_RECORD) >= 1);
        session.close();
    }
}
