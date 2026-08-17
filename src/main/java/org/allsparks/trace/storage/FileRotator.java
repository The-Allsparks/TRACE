package org.allsparks.trace.storage;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/** File-name and quota helpers for TRACE flight recordings. */
public final class FileRotator {
    private final Path directory;
    private final String sessionPrefix;
    private final long maxFileBytes;
    private final long maxTotalBytes;
    private int index;

    public FileRotator(Path directory, String sessionPrefix, long maxFileBytes, long maxTotalBytes) {
        this.directory = directory;
        this.sessionPrefix = sanitize(sessionPrefix);
        this.maxFileBytes = maxFileBytes;
        this.maxTotalBytes = maxTotalBytes;
    }

    public Path directory() {
        return directory;
    }

    public Path nextFile() {
        index++;
        return directory.resolve(sessionPrefix + "-" + String.format(Locale.ROOT, "%03d", index) + ".tlog");
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
        if (!Files.isDirectory(directory)) {
            Files.createDirectories(directory);
            return 0L;
        }
        List<Path> files = listTraceFiles();
        long total = 0L;
        for (Path file : files) {
            total += Files.size(file);
        }
        files.sort(Comparator.comparingLong(this::lastModified));
        int index = 0;
        while (total > maxTotalBytes && index < files.size() - 1) {
            Path oldest = files.get(index);
            long size = Files.size(oldest);
            Files.deleteIfExists(oldest);
            total -= size;
            index++;
        }
        return total;
    }

    private List<Path> listTraceFiles() throws IOException {
        List<Path> files = new ArrayList<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(directory, "*.tlog")) {
            for (Path path : stream) {
                files.add(path);
            }
        }
        return files;
    }

    private long lastModified(Path path) {
        try {
            return Files.getLastModifiedTime(path).toMillis();
        } catch (IOException exception) {
            return 0L;
        }
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
