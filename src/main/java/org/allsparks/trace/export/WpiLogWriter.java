package org.allsparks.trace.export;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

/**
 * WPILib Data Log (WPILOG) version 1.0 writer. Encodes the published binary
 * layout without depending on WPILib. Used only for desktop AdvantageScope
 * export; the Control Hub still writes {@code .tlog}.
 *
 * <p>Spec: WPILib Data Log File Format Specification, Version 1.0 (0x0100).
 */
final class WpiLogWriter {
    static final byte[] MAGIC = new byte[] {'W', 'P', 'I', 'L', 'O', 'G'};
    static final int VERSION = 0x0100;

    private final ByteArrayOutputStream out = new ByteArrayOutputStream();
    private int nextEntryId = 1;

    WpiLogWriter(String extraHeader) {
        byte[] extra = extraHeader == null
                ? new byte[0]
                : extraHeader.getBytes(StandardCharsets.UTF_8);
        out.write(MAGIC, 0, MAGIC.length);
        writeUnsignedLe(VERSION, 2);
        writeUnsignedLe(extra.length, 4);
        out.write(extra, 0, extra.length);
    }

    int start(long timestampUs, String name, String type, String metadata) {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(type, "type");
        if (metadata == null) {
            metadata = "";
        }
        int entryId = nextEntryId++;
        byte[] nameBytes = name.getBytes(StandardCharsets.UTF_8);
        byte[] typeBytes = type.getBytes(StandardCharsets.UTF_8);
        byte[] metaBytes = metadata.getBytes(StandardCharsets.UTF_8);
        ByteArrayOutputStream payload = new ByteArrayOutputStream();
        payload.write(0);
        writeUnsignedLe(payload, entryId, 4);
        writeUnsignedLe(payload, nameBytes.length, 4);
        payload.write(nameBytes, 0, nameBytes.length);
        writeUnsignedLe(payload, typeBytes.length, 4);
        payload.write(typeBytes, 0, typeBytes.length);
        writeUnsignedLe(payload, metaBytes.length, 4);
        payload.write(metaBytes, 0, metaBytes.length);
        writeRecord(0, timestampUs, payload.toByteArray());
        return entryId;
    }

    void append(int entryId, long timestampUs, byte[] payload) {
        writeRecord(entryId, timestampUs, payload);
    }

    byte[] toByteArray() {
        return out.toByteArray();
    }

    private void writeRecord(int entryId, long timestampUs, byte[] payload) {
        if (payload == null) {
            payload = new byte[0];
        }
        if (timestampUs < 0L) {
            timestampUs = 0L;
        }
        int idBytes = unsignedBytes32(entryId);
        int sizeBytes = unsignedBytes32(payload.length);
        int timestampBytes = unsignedBytes64(timestampUs);
        int bitfield = (idBytes - 1) | ((sizeBytes - 1) << 2) | ((timestampBytes - 1) << 4);
        out.write(bitfield);
        writeUnsignedLe(entryId, idBytes);
        writeUnsignedLe(payload.length, sizeBytes);
        writeUnsignedLe(timestampUs, timestampBytes);
        out.write(payload, 0, payload.length);
    }

    private void writeUnsignedLe(long value, int bytes) {
        writeUnsignedLe(out, value, bytes);
    }

    static void writeUnsignedLe(ByteArrayOutputStream sink, long value, int bytes) {
        for (int i = 0; i < bytes; i++) {
            sink.write((int) (value & 0xFFL));
            value >>>= 8;
        }
    }

    static int unsignedBytes32(int value) {
        if (value < 0) {
            throw new IllegalArgumentException("value must be unsigned 32-bit: " + value);
        }
        if (value <= 0xFF) {
            return 1;
        }
        if (value <= 0xFFFF) {
            return 2;
        }
        if (value <= 0xFFFFFF) {
            return 3;
        }
        return 4;
    }

    static int unsignedBytes64(long value) {
        if (value < 0L) {
            throw new IllegalArgumentException("value must be unsigned 64-bit: " + value);
        }
        int bytes = 1;
        long max = 0xFFL;
        while (bytes < 8 && value > max) {
            bytes++;
            max = (max << 8) | 0xFFL;
        }
        return bytes;
    }
}
