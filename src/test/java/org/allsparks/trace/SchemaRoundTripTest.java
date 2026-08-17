package org.allsparks.trace;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.allsparks.trace.core.Pose2d;
import org.allsparks.trace.core.SchemaVersion;
import org.allsparks.trace.core.TraceRecord;
import org.allsparks.trace.core.TypedValue;
import org.allsparks.trace.core.Units;
import org.allsparks.trace.storage.TlogCodec;
import org.junit.jupiter.api.Test;

class SchemaRoundTripTest {
    @Test
    void binaryRoundTripPreservesTypedValues() {
        TraceRecord original = TraceRecord.builder()
                .monotonicNanos(123)
                .wallClockMillis(456)
                .cycle(7)
                .source("Drive")
                .name("Drive/Pose")
                .value(TypedValue.ofPose(new Pose2d(1.5, 2.25, 0.1)))
                .units(Units.METERS)
                .schemaVersion(SchemaVersion.RECORD)
                .message("")
                .build();
        byte[] encoded = TlogCodec.encodeRecord(original);
        int length = java.nio.ByteBuffer.wrap(encoded).order(java.nio.ByteOrder.LITTLE_ENDIAN).getInt();
        byte[] payload = new byte[length];
        System.arraycopy(encoded, 8, payload, 0, length);
        TraceRecord decoded = TlogCodec.decodePayload(payload);
        assertEquals(original.name().value(), decoded.name().value());
        assertEquals(original.value().asPose(), decoded.value().asPose());
        assertEquals(original.units(), decoded.units());
        assertEquals(SchemaVersion.RECORD, decoded.schemaVersion());
        assertTrue(decoded.hasWallClock());
    }
}
