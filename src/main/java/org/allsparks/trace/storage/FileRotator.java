package org.allsparks.trace.storage;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
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

    /**
     * Deletes oldest {@code .tlog} files for <em>this session prefix only</em>
     * until their total is at most {@code maxTotalBytes}. Always keeps the
     * newest remaining file for this prefix.
     *
     * <p>Other prefixes in the same directory are not deleted. There is no
     * directory-wide cap; many session prefixes can grow the folder. A later
     * explicit directory cap would need its own config flag.
     */
    public long enforceQuota() throws IOException {
        if (!directory.isDirectory()) {
            JavaIoFiles.createDirectories(directory);
            return 0L;
        }
        List<File> files = sessionTraceFiles();
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

    private List<File> sessionTraceFiles() {
        List<File> files = JavaIoFiles.listTraceFiles(directory);
        List<File> mine = new ArrayList<>();
        for (File file : files) {
            if (isSessionFile(file.getName())) {
                mine.add(file);
            }
        }
        return mine;
    }

    /**
     * True for {@code {prefix}-{digits}.tlog} only. {@code alphabet-001.tlog}
     * does not belong to prefix {@code alpha}.
     */
    boolean isSessionFile(String name) {
        if (name == null) {
            return false;
        }
        String expected = sessionPrefix + "-";
        if (!name.startsWith(expected) || !name.endsWith(".tlog")) {
            return false;
        }
        String indexPart = name.substring(expected.length(), name.length() - ".tlog".length());
        if (indexPart.isEmpty()) {
            return false;
        }
        for (int i = 0; i < indexPart.length(); i++) {
            if (!Character.isDigit(indexPart.charAt(i))) {
                return false;
            }
        }
        return true;
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
