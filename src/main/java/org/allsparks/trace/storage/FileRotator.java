package org.allsparks.trace.storage;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/** File-name and quota helpers for TRACE flight recordings. */
public final class FileRotator {
    private final File directory;
    private final String sessionPrefix;
    private final long maxFileBytes;
    private final long maxTotalBytes;
    private int index;

    public FileRotator(Path directory, String sessionPrefix, long maxFileBytes, long maxTotalBytes) {
        this(JavaIoFiles.toFile(directory), sessionPrefix, maxFileBytes, maxTotalBytes);
    }

    public FileRotator(File directory, String sessionPrefix, long maxFileBytes, long maxTotalBytes) {
        this.directory = directory;
        this.sessionPrefix = sanitize(sessionPrefix);
        this.maxFileBytes = maxFileBytes;
        this.maxTotalBytes = maxTotalBytes;
    }

    public File directory() {
        return directory;
    }

    public File nextFile() {
        index++;
        return new File(directory, sessionPrefix + "-" + String.format(Locale.ROOT, "%03d", index) + ".tlog");
    }

    public boolean exceedsFileLimit(long bytesWritten) {
        return bytesWritten >= maxFileBytes;
    }

    public long maxFileBytes() {
        return maxFileBytes;
    }

    public long maxTotalBytes() {
        return maxTotalBytes;
    }

    public long enforceQuota() throws IOException {
        if (!directory.isDirectory()) {
            JavaIoFiles.createDirectories(directory);
            return 0L;
        }
        List<File> files = JavaIoFiles.listTraceFiles(directory);
        long total = 0L;
        for (File file : files) {
            total += JavaIoFiles.size(file);
        }
        files.sort(Comparator.comparingLong(File::lastModified));
        int index = 0;
        while (total > maxTotalBytes && index < files.size() - 1) {
            File oldest = files.get(index);
            long size = JavaIoFiles.size(oldest);
            JavaIoFiles.deleteIfExists(oldest);
            total -= size;
            index++;
        }
        return total;
    }

    public static String sanitize(String raw) {
        if (raw == null || raw.isEmpty()) {
            return "trace-session";
        }
        String cleaned = raw.replaceAll("[^A-Za-z0-9._-]", "_");
        if (cleaned.length() > 48) {
            cleaned = cleaned.substring(0, 48);
        }
        return cleaned;
    }
}
