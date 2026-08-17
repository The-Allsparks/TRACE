package org.allsparks.trace.storage;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.zip.CRC32;
import org.allsparks.trace.core.Pose2d;
import org.allsparks.trace.core.RecordCategory;
import org.allsparks.trace.core.TracePriority;
import org.allsparks.trace.core.TraceQuality;
import org.allsparks.trace.core.TraceRecord;
import org.allsparks.trace.core.TraceSeverity;
import org.allsparks.trace.core.TypedValue;
import org.allsparks.trace.core.Units;
import org.allsparks.trace.session.SessionMetadata;

/**
 * Compact little-endian TRACE log encoding. Each record is length-prefixed and
 * CRC32-protected so truncated or corrupt files can be read up to the last
 * intact record.
 */
public final class TlogCodec {
    public static final byte[] MAGIC = new byte[] {'T', 'L', 'O', 'G'};
    public static final int VERSION = 1;
    private static final int MAX_STRING = 8 * 1024;
    private static final int MAX_RECORD = 64 * 1024;

    private TlogCodec() {}

    public static byte[] encodeHeader(SessionMetadata metadata) {
        byte[] json = metadata.toJson().getBytes(StandardCharsets.UTF_8);
        ByteBuffer buffer = ByteBuffer.allocate(4 + 2 + 2 + 4 + json.length).order(ByteOrder.LITTLE_ENDIAN);
        buffer.put(MAGIC);
        buffer.putShort((short) VERSION);
        buffer.putShort((short) 0);
        buffer.putInt(json.length);
        buffer.put(json);
        return buffer.array();
    }

    public static byte[] encodeRecord(TraceRecord record) {
        byte[] payload = encodePayload(record);
        CRC32 crc32 = new CRC32();
        crc32.update(payload);
        ByteBuffer buffer = ByteBuffer.allocate(8 + payload.length).order(ByteOrder.LITTLE_ENDIAN);
        buffer.putInt(payload.length);
        buffer.putInt((int) crc32.getValue());
        buffer.put(payload);
        return buffer.array();
    }

    private static byte[] encodePayload(TraceRecord record) {
        byte[] source = utf8(record.source());
        byte[] name = utf8(record.name().value());
        byte[] units = utf8(record.units().symbol());
        byte[] message = utf8(record.message());
        byte[] valueBytes = encodeValue(record.value());
        ByteBuffer buffer = ByteBuffer.allocate(
                        1 + 8 + 8 + 8 + 1 + 1 + 1 + 2 + 2
                                + 2 + source.length
                                + 2 + name.length
                                + 2 + units.length
                                + 2 + message.length
                                + 1
                                + valueBytes.length)
                .order(ByteOrder.LITTLE_ENDIAN);
        buffer.put(record.category().wireId());
        buffer.putLong(record.monotonicNanos());
        buffer.putLong(record.wallClockMillis());
        buffer.putLong(record.cycle());
        buffer.put(record.priority().wireId());
        buffer.put(record.quality().wireId());
        buffer.put(record.severity().wireId());
        buffer.putShort((short) record.schemaVersion());
        putBytes(buffer, source);
        putBytes(buffer, name);
        putBytes(buffer, units);
        putBytes(buffer, message);
        buffer.put(record.value().kind().wireId());
        buffer.put(valueBytes);
        return buffer.array();
    }

    public static TraceRecord decodePayload(byte[] payload) {
        ByteBuffer buffer = ByteBuffer.wrap(payload).order(ByteOrder.LITTLE_ENDIAN);
        RecordCategory category = RecordCategory.fromWire(buffer.get());
        long monotonic = buffer.getLong();
        long wall = buffer.getLong();
        long cycle = buffer.getLong();
        TracePriority priority = TracePriority.fromWire(buffer.get());
        TraceQuality quality = TraceQuality.fromWire(buffer.get());
        TraceSeverity severity = TraceSeverity.fromWire(buffer.get());
        int schema = buffer.getShort() & 0xFFFF;
        String source = getString(buffer);
        String name = getString(buffer);
        String units = getString(buffer);
        String message = getString(buffer);
        TypedValue.Kind kind = TypedValue.Kind.fromWire(buffer.get());
        TypedValue value = decodeValue(buffer, kind);
        return TraceRecord.builder()
                .category(category)
                .monotonicNanos(monotonic)
                .wallClockMillis(wall)
                .cycle(cycle)
                .priority(priority)
                .quality(quality)
                .severity(severity)
                .schemaVersion(schema)
                .source(source)
                .name(name)
                .units(Units.of(units))
                .message(message)
                .value(value)
                .build();
    }

    public static int crc32(byte[] payload) {
        CRC32 crc32 = new CRC32();
        crc32.update(payload);
        return (int) crc32.getValue();
    }

    public static int maxRecordBytes() {
        return MAX_RECORD;
    }

    private static byte[] encodeValue(TypedValue value) {
        switch (value.kind()) {
            case BOOLEAN:
                return new byte[] {(byte) (value.asBoolean() ? 1 : 0)};
            case LONG:
                return ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN).putLong(value.asLong()).array();
            case DOUBLE:
                return ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN).putDouble(value.asDouble()).array();
            case STRING:
                byte[] text = utf8(value.asString());
                ByteBuffer textBuf = ByteBuffer.allocate(2 + text.length).order(ByteOrder.LITTLE_ENDIAN);
                putBytes(textBuf, text);
                return textBuf.array();
            case POSE2D:
                Pose2d pose = value.asPose();
                return ByteBuffer.allocate(24)
                        .order(ByteOrder.LITTLE_ENDIAN)
                        .putDouble(pose.x())
                        .putDouble(pose.y())
                        .putDouble(pose.headingRad())
                        .array();
            case DOUBLE_ARRAY:
                double[] values = value.asDoubleArray();
                ByteBuffer arrayBuf = ByteBuffer.allocate(2 + 8 * values.length).order(ByteOrder.LITTLE_ENDIAN);
                arrayBuf.putShort((short) values.length);
                for (double item : values) {
                    arrayBuf.putDouble(item);
                }
                return arrayBuf.array();
            case NONE:
            default:
                return new byte[0];
        }
    }

    private static TypedValue decodeValue(ByteBuffer buffer, TypedValue.Kind kind) {
        switch (kind) {
            case BOOLEAN:
                return TypedValue.ofBoolean(buffer.get() != 0);
            case LONG:
                return TypedValue.ofLong(buffer.getLong());
            case DOUBLE:
                return TypedValue.ofDouble(buffer.getDouble());
            case STRING:
                return TypedValue.ofString(getString(buffer));
            case POSE2D:
                return TypedValue.ofPose(new Pose2d(buffer.getDouble(), buffer.getDouble(), buffer.getDouble()));
            case DOUBLE_ARRAY:
                int count = buffer.getShort() & 0xFFFF;
                double[] values = new double[count];
                for (int i = 0; i < count; i++) {
                    values[i] = buffer.getDouble();
                }
                return TypedValue.ofDoubles(values);
            case NONE:
            default:
                return TypedValue.none();
        }
    }

    private static void putBytes(ByteBuffer buffer, byte[] bytes) {
        buffer.putShort((short) bytes.length);
        buffer.put(bytes);
    }

    private static String getString(ByteBuffer buffer) {
        int length = buffer.getShort() & 0xFFFF;
        if (length > MAX_STRING) {
            throw new IllegalArgumentException("String exceeds limit: " + length);
        }
        byte[] bytes = new byte[length];
        buffer.get(bytes);
        return new String(bytes, StandardCharsets.UTF_8);
    }

    private static byte[] utf8(String value) {
        byte[] bytes = (value == null ? "" : value).getBytes(StandardCharsets.UTF_8);
        if (bytes.length > MAX_STRING) {
            throw new IllegalArgumentException("String exceeds limit");
        }
        return bytes;
    }
}
