package org.allsparks.trace.export;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.allsparks.trace.core.Pose2d;
import org.allsparks.trace.core.RecordCategory;
import org.allsparks.trace.core.TraceRecord;
import org.allsparks.trace.core.TypedValue;
import org.allsparks.trace.session.SessionMetadata;

/**
 * Converts TRACE records into WPILOG 1.0 bytes for AdvantageScope. Canonical
 * on-robot storage remains {@code .tlog}; this class is a desktop exporter.
 *
 * <p>Pose2d is written as {@code double[]} {@code [x, y, headingRad]} plus
 * {@code /x}, {@code /y}, {@code /headingRad} children so line graphs work
 * without struct schemas. Per-sample quality and category are stored in each
 * entry's JSON metadata from the first sample of that name.
 */
public final class WpiLogExporter {
    private static final String TYPE_BOOLEAN = "boolean";
    private static final String TYPE_INT64 = "int64";
    private static final String TYPE_DOUBLE = "double";
    private static final String TYPE_STRING = "string";
    private static final String TYPE_DOUBLE_ARRAY = "double[]";

    public byte[] export(SessionMetadata metadata, List<TraceRecord> records) {
        Objects.requireNonNull(metadata, "metadata");
        Objects.requireNonNull(records, "records");
        WpiLogWriter writer = new WpiLogWriter(metadata.toJson());
        Map<String, Integer> entries = new LinkedHashMap<>();
        writeSessionKeys(writer, metadata, entries);
        for (TraceRecord record : records) {
            writeRecord(writer, entries, record);
        }
        return writer.toByteArray();
    }

    public void export(Path output, SessionMetadata metadata, List<TraceRecord> records) throws IOException {
        Files.write(output, export(metadata, records));
    }

    public static Path defaultOutput(Path tlog) {
        String fileName = tlog.getFileName().toString();
        String stem = fileName.endsWith(".tlog")
                ? fileName.substring(0, fileName.length() - 5)
                : fileName;
        Path parent = tlog.getParent();
        Path name = Path.of(stem + ".wpilog");
        return parent == null ? name : parent.resolve(name);
    }

    private static void writeSessionKeys(
            WpiLogWriter writer, SessionMetadata metadata, Map<String, Integer> entries) {
        writeString(writer, entries, "/TRACE/SessionId", 0L, metadata.sessionId(), sessionMeta("sessionId"));
        writeString(writer, entries, "/TRACE/Robot", 0L, metadata.robotName(), sessionMeta("robotName"));
        writeString(writer, entries, "/TRACE/OpMode", 0L, metadata.opModeName(), sessionMeta("opModeName"));
        writeString(writer, entries, "/TRACE/Mode", 0L, metadata.recordingMode(), sessionMeta("recordingMode"));
    }

    private static void writeRecord(WpiLogWriter writer, Map<String, Integer> entries, TraceRecord record) {
        long timestampUs = record.monotonicNanos() / 1000L;
        String root = wpiName(record.name().value());
        writeInt64(writer, entries, "/TRACE/Cycle", timestampUs, record.cycle(), sessionMeta("cycle"));
        TypedValue value = record.value();
        String meta = entryMetadata(record, value.kind().name());
        if (record.category() == RecordCategory.EVENT || value.kind() == TypedValue.Kind.NONE) {
            String text = record.message() == null || record.message().isEmpty()
                    ? record.name().value()
                    : record.message();
            writeString(writer, entries, root, timestampUs, text, meta);
            return;
        }
        switch (value.kind()) {
            case BOOLEAN:
                writeBoolean(writer, entries, root, timestampUs, value.asBoolean(), meta);
                break;
            case LONG:
                writeInt64(writer, entries, root, timestampUs, value.asLong(), meta);
                break;
            case DOUBLE:
                writeDouble(writer, entries, root, timestampUs, value.asDouble(), meta);
                break;
            case STRING:
                writeString(writer, entries, root, timestampUs, value.asString(), meta);
                break;
            case DOUBLE_ARRAY:
                writeDoubleArray(writer, entries, root, timestampUs, value.asDoubleArray(), meta);
                break;
            case POSE2D:
                Pose2d pose = value.asPose();
                writeDoubleArray(writer, entries, root, timestampUs, pose.toArray(), meta);
                writeDouble(writer, entries, root + "/x", timestampUs, pose.x(), meta);
                writeDouble(writer, entries, root + "/y", timestampUs, pose.y(), meta);
                writeDouble(writer, entries, root + "/headingRad", timestampUs, pose.headingRad(), meta);
                break;
            default:
                writeString(writer, entries, root, timestampUs, value.render(), meta);
                break;
        }
    }

    private static void writeBoolean(
            WpiLogWriter writer,
            Map<String, Integer> entries,
            String name,
            long timestampUs,
            boolean value,
            String metadata) {
        int id = entry(writer, entries, name, TYPE_BOOLEAN, metadata, timestampUs);
        writer.append(id, timestampUs, new byte[] {value ? (byte) 1 : (byte) 0});
    }

    private static void writeInt64(
            WpiLogWriter writer,
            Map<String, Integer> entries,
            String name,
            long timestampUs,
            long value,
            String metadata) {
        int id = entry(writer, entries, name, TYPE_INT64, metadata, timestampUs);
        writer.append(id, timestampUs, leLong(value));
    }

    private static void writeDouble(
            WpiLogWriter writer,
            Map<String, Integer> entries,
            String name,
            long timestampUs,
            double value,
            String metadata) {
        int id = entry(writer, entries, name, TYPE_DOUBLE, metadata, timestampUs);
        writer.append(id, timestampUs, leDouble(value));
    }

    private static void writeString(
            WpiLogWriter writer,
            Map<String, Integer> entries,
            String name,
            long timestampUs,
            String value,
            String metadata) {
        int id = entry(writer, entries, name, TYPE_STRING, metadata, timestampUs);
        byte[] payload = value == null ? new byte[0] : value.getBytes(StandardCharsets.UTF_8);
        writer.append(id, timestampUs, payload);
    }

    private static void writeDoubleArray(
            WpiLogWriter writer,
            Map<String, Integer> entries,
            String name,
            long timestampUs,
            double[] values,
            String metadata) {
        int id = entry(writer, entries, name, TYPE_DOUBLE_ARRAY, metadata, timestampUs);
        ByteBuffer buffer = ByteBuffer.allocate(values.length * 8).order(ByteOrder.LITTLE_ENDIAN);
        for (double value : values) {
            buffer.putDouble(value);
        }
        writer.append(id, timestampUs, buffer.array());
    }

    private static int entry(
            WpiLogWriter writer,
            Map<String, Integer> entries,
            String name,
            String type,
            String metadata,
            long timestampUs) {
        String key = name + "\0" + type;
        Integer existing = entries.get(key);
        if (existing != null) {
            return existing;
        }
        int id = writer.start(timestampUs, name, type, metadata);
        entries.put(key, id);
        return id;
    }

    static String wpiName(String traceName) {
        if (traceName == null || traceName.isEmpty()) {
            return "/TRACE/Unnamed";
        }
        return traceName.charAt(0) == '/' ? traceName : "/" + traceName;
    }

    private static String sessionMeta(String field) {
        return "{\"source\":\"TRACE\",\"field\":\"" + jsonEscape(field) + "\"}";
    }

    private static String entryMetadata(TraceRecord record, String kind) {
        return "{\"source\":\""
                + jsonEscape(record.source())
                + "\",\"category\":\""
                + record.category().name()
                + "\",\"units\":\""
                + jsonEscape(record.units().symbol())
                + "\",\"quality\":\""
                + record.quality().name()
                + "\",\"priority\":\""
                + record.priority().name()
                + "\",\"traceKind\":\""
                + kind
                + "\"}";
    }

    private static String jsonEscape(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static byte[] leLong(long value) {
        return ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN).putLong(value).array();
    }

    private static byte[] leDouble(double value) {
        return ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN).putDouble(value).array();
    }
}
