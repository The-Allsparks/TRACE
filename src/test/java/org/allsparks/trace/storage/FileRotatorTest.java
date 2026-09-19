package org.allsparks.trace.storage;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class FileRotatorTest {
    @Test
    void quotaDoesNotDeleteAnotherPrefix(@TempDir Path dir) throws Exception {
        File directory = dir.toFile();
        long now = System.currentTimeMillis();
        File foreign = writeTlog(directory, "beta-001.tlog", 1800, now - 60_000);
        File oldAlpha = writeTlog(directory, "alpha-001.tlog", 1800, now - 30_000);
        File newAlpha = writeTlog(directory, "alpha-002.tlog", 1800, now - 1_000);
        File notes = writeBytes(directory, "notes.txt", 1800, now - 90_000);

        FileRotator rotator = new FileRotator(directory, "alpha", 1024, 2000);
        rotator.enforceQuota();

        assertTrue(foreign.isFile(), "a newer session must not delete another prefix's .tlog");
        assertTrue(notes.isFile(), "quota must not delete non-.tlog files");
        assertFalse(oldAlpha.isFile(), "oldest file of this prefix should rotate away");
        assertTrue(newAlpha.isFile(), "newest file of this prefix must remain");
    }

    @Test
    void prefixMatchDoesNotTreatAlphabetAsAlpha(@TempDir Path dir) throws Exception {
        File directory = dir.toFile();
        long now = System.currentTimeMillis();
        File alphabet = writeTlog(directory, "alphabet-001.tlog", 1800, now - 60_000);
        File oldAlpha = writeTlog(directory, "alpha-001.tlog", 1800, now - 30_000);
        File newAlpha = writeTlog(directory, "alpha-002.tlog", 1800, now - 1_000);

        FileRotator rotator = new FileRotator(directory, "alpha", 1024, 2000);
        rotator.enforceQuota();

        assertTrue(alphabet.isFile(), "alphabet-001.tlog is a different prefix than alpha");
        assertFalse(oldAlpha.isFile());
        assertTrue(newAlpha.isFile());
        assertTrue(rotator.isSessionFile("alpha-001.tlog"));
        assertFalse(rotator.isSessionFile("alphabet-001.tlog"));
        assertFalse(rotator.isSessionFile("alpha-extra-001.tlog"));
        assertFalse(rotator.isSessionFile("notes.txt"));
    }

    private static File writeTlog(File directory, String name, int bytes, long lastModified) throws Exception {
        return writeBytes(directory, name, bytes, lastModified);
    }

    private static File writeBytes(File directory, String name, int bytes, long lastModified) throws Exception {
        File file = new File(directory, name);
        Files.write(file.toPath(), new byte[bytes]);
        assertTrue(file.setLastModified(lastModified), "need distinct mtimes for rotation order");
        return file;
    }
}
