package org.allsparks.trace.storage;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.allsparks.trace.core.TraceRecord;
import org.allsparks.trace.session.SessionMetadata;

/**
 * Truncation-tolerant TRACE log reader. Stops at the first incomplete record
 * and reports corrupt records without throwing for the remainder of the file.
 */
public final class TlogReader {
    private final SessionMetadata metadata;
    private final List<TraceRecord> records;
    private final int truncatedBytes;
    private final int corruptRecords;
    private final boolean complete;

    private TlogReader(
            SessionMetadata metadata,
            List<TraceRecord> records,
            int truncatedBytes,
            int corruptRecords,
            boolean complete) {
        this.metadata = metadata;
        this.records = records;
        this.truncatedBytes = truncatedBytes;
        this.corruptRecords = corruptRecords;
        this.complete = complete;
    }

    public static TlogReader read(Path path) throws IOException {
        try (InputStream in = new BufferedInputStream(Files.newInputStream(path))) {
            return read(in);
        }
    }

    public static TlogReader read(InputStream in) throws IOException {
        byte[] magic = readFully(in, 4);
        if (magic.length < 4 || magic[0] != 'T' || magic[1] != 'L' || magic[2] != 'O' || magic[3] != 'G') {
            throw new IOException("Not a TRACE log (missing TLOG magic)");
        }
        byte[] versionBytes = readFully(in, 4);
        if (versionBytes.length < 4) {
            throw new IOException("Truncated TRACE header");
        }
        ByteBuffer versionBuffer = ByteBuffer.wrap(versionBytes).order(ByteOrder.LITTLE_ENDIAN);
        int version = versionBuffer.getShort() & 0xFFFF;
        versionBuffer.getShort();
        if (version != TlogCodec.VERSION) {
            throw new IOException("Unsupported TRACE log version: " + version);
        }
        byte[] lengthBytes = readFully(in, 4);
        if (lengthBytes.length < 4) {
            throw new IOException("Truncated TRACE header length");
        }
        int headerLength = ByteBuffer.wrap(lengthBytes).order(ByteOrder.LITTLE_ENDIAN).getInt();
        if (headerLength < 0 || headerLength > 1024 * 1024) {
            throw new IOException("Invalid header length: " + headerLength);
        }
        byte[] jsonBytes = readFully(in, headerLength);
        if (jsonBytes.length < headerLength) {
            throw new IOException("Truncated TRACE metadata");
        }
        SessionMetadata metadata = SessionMetadata.fromJson(new String(jsonBytes, java.nio.charset.StandardCharsets.UTF_8));
        List<TraceRecord> records = new ArrayList<>();
        int truncated = 0;
        int corrupt = 0;
        boolean complete = true;
        while (true) {
            byte[] prefix = readFully(in, 8);
            if (prefix.length == 0) {
                break;
            }
            if (prefix.length < 8) {
                truncated = prefix.length;
                complete = false;
                break;
            }
            ByteBuffer prefixBuffer = ByteBuffer.wrap(prefix).order(ByteOrder.LITTLE_ENDIAN);
            int payloadLength = prefixBuffer.getInt();
            int expectedCrc = prefixBuffer.getInt();
            if (payloadLength < 0 || payloadLength > TlogCodec.maxRecordBytes()) {
                corrupt++;
                complete = false;
                break;
            }
            byte[] payload = readFully(in, payloadLength);
            if (payload.length < payloadLength) {
                truncated = payload.length;
                complete = false;
                break;
            }
            if (TlogCodec.crc32(payload) != expectedCrc) {
                corrupt++;
                continue;
            }
            try {
                records.add(TlogCodec.decodePayload(payload));
            } catch (RuntimeException ignored) {
                corrupt++;
            }
        }
        return new TlogReader(metadata, records, truncated, corrupt, complete);
    }

    public SessionMetadata metadata() {
        return metadata;
    }

    public List<TraceRecord> records() {
        return records;
    }

    public int truncatedBytes() {
        return truncatedBytes;
    }

    public int corruptRecords() {
        return corruptRecords;
    }

    public boolean complete() {
        return complete;
    }

    private static byte[] readFully(InputStream in, int length) throws IOException {
        byte[] bytes = new byte[length];
        int offset = 0;
        while (offset < length) {
            int read = in.read(bytes, offset, length - offset);
            if (read < 0) {
                if (offset == 0) {
                    return new byte[0];
                }
                byte[] partial = new byte[offset];
                System.arraycopy(bytes, 0, partial, 0, offset);
                return partial;
            }
            offset += read;
        }
        return bytes;
    }
}
