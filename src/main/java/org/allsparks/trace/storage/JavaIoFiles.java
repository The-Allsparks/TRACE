package org.allsparks.trace.storage;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Control Hub Android 7 (API 24) file helpers.
 *
 * <p>Do not call {@code File.toPath()} or {@code java.nio.file.Files} here.
 * {@code File.toPath()} is missing on the Hub, and desugar cannot add methods
 * to the platform {@code File} class. AMPER already records with
 * {@code File.mkdirs()} and {@code FileOutputStream}; TRACE uses the same APIs.
 */
final class JavaIoFiles {
    private JavaIoFiles() {}

    static File toFile(Path path) {
        if (path == null) {
            return null;
        }
        return new File(path.toString());
    }

    static void createDirectories(File directory) throws IOException {
        if (directory == null) {
            throw new IOException("storage directory is null");
        }
        if (directory.isDirectory()) {
            return;
        }
        if (directory.exists()) {
            throw new IOException("not a directory: " + directory.getAbsolutePath());
        }
        if (!directory.mkdirs() && !directory.isDirectory()) {
            throw new IOException("could not create directory: " + directory.getAbsolutePath());
        }
    }

    static OutputStream newOutputStream(File file) throws IOException {
        File parent = file.getParentFile();
        if (parent != null) {
            createDirectories(parent);
        }
        return new FileOutputStream(file);
    }

    static long size(File file) {
        return file.length();
    }

    static void deleteIfExists(File file) throws IOException {
        if (file.exists() && !file.delete()) {
            throw new IOException("could not delete: " + file.getAbsolutePath());
        }
    }

    static List<File> listTraceFiles(File directory) {
        File[] children = directory.listFiles((dir, name) -> name.endsWith(".tlog"));
        List<File> files = new ArrayList<>();
        if (children == null) {
            return files;
        }
        for (File child : children) {
            if (child.isFile()) {
                files.add(child);
            }
        }
        return files;
    }
}
