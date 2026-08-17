package org.allsparks.trace;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.allsparks.trace.core.DropReason;
import org.allsparks.trace.core.TracePriority;
import org.allsparks.trace.core.TraceQuality;
import org.allsparks.trace.core.TypedValue;
import org.allsparks.trace.core.Units;
import org.allsparks.trace.core.RecordCategory;
import org.allsparks.trace.session.TraceSession;
import org.junit.jupiter.api.Test;

class BoundedQueueTest {
    @Test
    void memorySinkPreservesHigherPriorityWhenFull() {
        TraceSession session = new TraceSession(TraceConfig.builder()
                .mode(TraceMode.FULL)
                .memorySink(true)
                .memoryCapacity(4)
                .essentialSampleIntervalNanos(0)
                .build());
        for (int i = 0; i < 8; i++) {
            session.record(
                    RecordCategory.OUTPUT,
                    "TRACE/Debug/Value",
                    TypedValue.ofLong(i),
                    Units.NONE,
                    TracePriority.VERBOSE,
                    TraceQuality.OK,
                    "");
        }
        session.record(
                RecordCategory.EVENT,
                "TRACE/Safety/Brownout",
                TypedValue.ofString("brownout"),
                Units.NONE,
                TracePriority.CRITICAL,
                TraceQuality.OK,
                "brownout");
        assertEquals(4, session.recorded().size());
        assertTrue(session.recorded().stream().anyMatch(r -> r.priority() == TracePriority.CRITICAL));
        assertTrue(session.drops().total() >= 5);
        assertTrue(session.drops().count(DropReason.QUEUE_FULL) + session.drops().count(DropReason.LOWER_PRIORITY_EVICTED) >= 5);
        session.close();
    }
}
