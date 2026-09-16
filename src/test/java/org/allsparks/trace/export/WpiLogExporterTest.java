package org.allsparks.trace.export;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.allsparks.trace.core.Pose2d;
import org.allsparks.trace.core.RecordCategory;
import org.allsparks.trace.core.TraceRecord;
import org.allsparks.trace.core.TypedValue;
import org.allsparks.trace.core.Units;
import org.allsparks.trace.session.SessionMetadata;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class WpiLogExporterTest {
    @Test
    void specExampleStartAndInt64Record() {
        WpiLogWriter writer = new WpiLogWriter("");
        int id = writer.start(1_000_000L, "test", "int64", "");
        assertEquals(1, id);
        writer.append(id, 1_000_000L, leLong(3L));
        byte[] bytes = writer.toByteArray();

        byte[] expectedHeader = new byte[] {
            'W', 'P', 'I', 'L', 'O', 'G', 0x00, 0x01, 0x00, 0x00, 0x00, 0x00
        };
        assertArrayEquals(expectedHeader, slice(bytes, 0, 12));

        byte[] start = slice(bytes, 12, 32);
        assertEquals(0x20, start[0] & 0xFF);
        assertEquals(0, start[1]);
        assertEquals(26, start[2]);
        assertEquals(1_000_000L, unsignedLe(start, 3, 3));
        assertEquals(0, start[6]);
        assertEquals(1, unsignedLe(start, 7, 4));
        assertEquals(4, unsignedLe(start, 11, 4));
        assertEquals("test", new String(slice(start, 15, 4), StandardCharsets.UTF_8));
        assertEquals(5, unsignedLe(start, 19, 4));
        assertEquals("int64", new String(slice(start, 23, 5), StandardCharsets.UTF_8));
        assertEquals(0, unsignedLe(start, 28, 4));

        byte[] data = slice(bytes, 12 + 32, 14);
        assertEquals(0x20, data[0] & 0xFF);
        assertEquals(1, data[1]);
        assertEquals(8, data[2]);
        assertEquals(1_000_000L, unsignedLe(data, 3, 3));
        assertEquals(3L, ByteBuffer.wrap(slice(data, 6, 8)).order(ByteOrder.LITTLE_ENDIAN).getLong());
    }

    @Test
    void exportsTypedRecordsForAdvantageScope(@TempDir Path temp) throws Exception {
        SessionMetadata metadata = SessionMetadata.builder()
                .sessionId("session-1")
                .robotName("BumbleBee")
                .opModeName("BumbleBee Drive")
                .recordingMode("ESSENTIAL")
                .build();
        List<TraceRecord> records = new ArrayList<>();
        records.add(TraceRecord.builder()
                .monotonicNanos(2_000_000L)
                .cycle(4)
                .source("Drive")
                .name("Drive/Command")
                .category(RecordCategory.OUTPUT)
                .value(TypedValue.ofDouble(0.25))
                .units(Units.DIMENSIONLESS)
                .build());
        records.add(TraceRecord.builder()
                .monotonicNanos(2_000_000L)
                .cycle(4)
                .source("Drive")
                .name("Drive/Pose")
                .category(RecordCategory.INPUT)
                .value(TypedValue.ofPose(new Pose2d(1.5, -0.5, 0.25)))
                .units(Units.METERS)
                .build());
        records.add(TraceRecord.builder()
                .monotonicNanos(3_000_000L)
                .cycle(5)
                .source("TRACE")
                .name("Match/Event")
                .category(RecordCategory.EVENT)
                .value(TypedValue.none())
                .message("Autonomous started")
                .build());

        Path out = temp.resolve("session.wpilog");
        new WpiLogExporter().export(out, metadata, records);
        assertTrue(Files.size(out) > 12);

        ParsedLog parsed = ParsedLog.parse(Files.readAllBytes(out));
        assertTrue(parsed.extraHeader.contains("session-1"));
        assertEquals("BumbleBee", parsed.lastString("/TRACE/Robot"));
        assertEquals("ESSENTIAL", parsed.lastString("/TRACE/Mode"));
        assertEquals(0.25, parsed.lastDouble("/Drive/Command"), 1e-9);
        assertEquals(1.5, parsed.lastDouble("/Drive/Pose/x"), 1e-9);
        assertEquals(-0.5, parsed.lastDouble("/Drive/Pose/y"), 1e-9);
        assertEquals(0.25, parsed.lastDouble("/Drive/Pose/headingRad"), 1e-9);
        assertArrayEquals(new double[] {1.5, -0.5, 0.25}, parsed.lastDoubleArray("/Drive/Pose"), 1e-9);
        assertEquals("Autonomous started", parsed.lastString("/Match/Event"));
        assertEquals(5L, parsed.lastInt64("/TRACE/Cycle"));
        assertEquals(2_000L, parsed.timestampsUs.get("/Drive/Command").get(0).longValue());
    }

    @Test
    void defaultOutputReplacesTlogSuffix() {
        Path tlog = Path.of("logs", "match-001.tlog");
        assertEquals(Path.of("logs", "match-001.wpilog"), WpiLogExporter.defaultOutput(tlog));
    }

    private static byte[] leLong(long value) {
        return ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN).putLong(value).array();
    }

    private static byte[] slice(byte[] bytes, int offset, int length) {
        byte[] out = new byte[length];
        System.arraycopy(bytes, offset, out, 0, length);
        return out;
    }

    private static long unsignedLe(byte[] bytes, int offset, int length) {
        long value = 0L;
        for (int i = 0; i < length; i++) {
            value |= ((long) bytes[offset + i] & 0xFFL) << (8 * i);
        }
        return value;
    }

    private static final class ParsedLog {
        private final String extraHeader;
        private final Map<Integer, String> names = new LinkedHashMap<>();
        private final Map<Integer, String> types = new LinkedHashMap<>();
        private final Map<String, List<byte[]>> payloads = new LinkedHashMap<>();
        private final Map<String, List<Long>> timestampsUs = new LinkedHashMap<>();

        private ParsedLog(String extraHeader) {
            this.extraHeader = extraHeader;
        }

        static ParsedLog parse(byte[] bytes) {
            ByteBuffer buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
            byte[] magic = new byte[6];
            buffer.get(magic);
            assertEquals("WPILOG", new String(magic, StandardCharsets.US_ASCII));
            assertEquals(0x0100, buffer.getShort() & 0xFFFF);
            int extraLen = buffer.getInt();
            byte[] extra = new byte[extraLen];
            buffer.get(extra);
            ParsedLog parsed = new ParsedLog(new String(extra, StandardCharsets.UTF_8));
            while (buffer.hasRemaining()) {
                int bitfield = buffer.get() & 0xFF;
                int idBytes = (bitfield & 0x3) + 1;
                int sizeBytes = ((bitfield >> 2) & 0x3) + 1;
                int timeBytes = ((bitfield >> 4) & 0x7) + 1;
                int entryId = (int) readUnsigned(buffer, idBytes);
                int size = (int) readUnsigned(buffer, sizeBytes);
                long timestamp = readUnsigned(buffer, timeBytes);
                byte[] payload = new byte[size];
                buffer.get(payload);
                if (entryId == 0) {
                    parsed.readControl(payload);
                } else {
                    String name = parsed.names.get(entryId);
                    parsed.payloads.computeIfAbsent(name, key -> new ArrayList<>()).add(payload);
                    parsed.timestampsUs.computeIfAbsent(name, key -> new ArrayList<>()).add(timestamp);
                }
            }
            return parsed;
        }

        private void readControl(byte[] payload) {
            ByteBuffer buffer = ByteBuffer.wrap(payload).order(ByteOrder.LITTLE_ENDIAN);
            int type = buffer.get() & 0xFF;
            if (type != 0) {
                return;
            }
            int entryId = buffer.getInt();
            names.put(entryId, readPrefixed(buffer));
            types.put(entryId, readPrefixed(buffer));
        }

        String lastString(String name) {
            byte[] payload = last(name);
            return new String(payload, StandardCharsets.UTF_8);
        }

        double lastDouble(String name) {
            return ByteBuffer.wrap(last(name)).order(ByteOrder.LITTLE_ENDIAN).getDouble();
        }

        long lastInt64(String name) {
            return ByteBuffer.wrap(last(name)).order(ByteOrder.LITTLE_ENDIAN).getLong();
        }

        double[] lastDoubleArray(String name) {
            byte[] payload = last(name);
            int count = payload.length / 8;
            double[] values = new double[count];
            ByteBuffer buffer = ByteBuffer.wrap(payload).order(ByteOrder.LITTLE_ENDIAN);
            for (int i = 0; i < count; i++) {
                values[i] = buffer.getDouble();
            }
            return values;
        }

        private byte[] last(String name) {
            List<byte[]> values = payloads.get(name);
            if (values == null || values.isEmpty()) {
                throw new AssertionError("missing entry " + name + ", have " + payloads.keySet());
            }
            return values.get(values.size() - 1);
        }

        private static String readPrefixed(ByteBuffer buffer) {
            int length = buffer.getInt();
            byte[] bytes = new byte[length];
            buffer.get(bytes);
            return new String(bytes, StandardCharsets.UTF_8);
        }

        private static long readUnsigned(ByteBuffer buffer, int bytes) {
            long value = 0L;
            for (int i = 0; i < bytes; i++) {
                value |= ((long) buffer.get() & 0xFFL) << (8 * i);
            }
            return value;
        }
    }
}
