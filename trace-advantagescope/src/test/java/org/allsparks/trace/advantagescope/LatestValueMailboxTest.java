package org.allsparks.trace.advantagescope;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.allsparks.trace.core.Pose2d;
import org.allsparks.trace.core.RecordCategory;
import org.allsparks.trace.core.TracePriority;
import org.allsparks.trace.core.TraceQuality;
import org.allsparks.trace.core.TraceRecord;
import org.allsparks.trace.core.TypedValue;
import org.allsparks.trace.core.Units;
import org.junit.jupiter.api.Test;

class LatestValueMailboxTest {
    @Test
    void keepsLatestAndCountsSkippedFramesBySequence() {
        LatestValueMailbox mailbox = new LatestValueMailbox(16);
        mailbox.offer(record("Drive/Command", TypedValue.ofDouble(0.1), Units.DIMENSIONLESS));
        mailbox.offer(record("Drive/Command", TypedValue.ofDouble(0.9), Units.DIMENSIONLESS));
        assertEquals(2L, mailbox.sequence());
        assertEquals(1, mailbox.size());
        assertEquals(0.9, mailbox.entries().next().getValue().value().asDouble(), 1e-9);
    }

    @Test
    void skipsArraysAndMetricNamespace() {
        LatestValueMailbox mailbox = new LatestValueMailbox(16);
        mailbox.offer(record("TRACE/AdvantageScope/Enabled", TypedValue.ofBoolean(true), Units.NONE));
        mailbox.offer(record("Drive/Wheel", TypedValue.ofDoubles(1.0, 2.0), Units.NONE));
        assertEquals(0, mailbox.size());
        assertEquals(1L, mailbox.valuesSkipped());
        assertEquals(0L, mailbox.sequence());
    }

    @Test
    void refusesNewKeysPastMaxValues() {
        LatestValueMailbox mailbox = new LatestValueMailbox(1);
        mailbox.offer(record("A", TypedValue.ofDouble(1.0), Units.NONE));
        mailbox.offer(record("B", TypedValue.ofDouble(2.0), Units.NONE));
        mailbox.offer(record("A", TypedValue.ofDouble(3.0), Units.NONE));
        assertEquals(1, mailbox.size());
        assertEquals("A", mailbox.entries().next().getKey());
        assertEquals(3.0, mailbox.entries().next().getValue().value().asDouble(), 1e-9);
        assertTrue(mailbox.valuesSkipped() >= 1L);
    }

    @Test
    void poseStaysOnOriginalKeyUntilJsonExpansion() {
        LatestValueMailbox mailbox = new LatestValueMailbox(8);
        mailbox.offer(record(
                "Drive/Pose",
                TypedValue.ofPose(new Pose2d(1.0, 2.0, 0.5)),
                Units.METERS));
        assertEquals(1, mailbox.size());
        assertNull(LiveValueEncoder.scalar(mailbox.entries().next().getValue().value()));
    }

    private static TraceRecord record(String name, TypedValue value, Units units) {
        return TraceRecord.builder()
                .name(name)
                .category(RecordCategory.OUTPUT)
                .value(value)
                .units(units)
                .priority(TracePriority.NORMAL)
                .quality(TraceQuality.OK)
                .monotonicNanos(1L)
                .wallClockMillis(1_700_000_000_000L)
                .build();
    }
}
