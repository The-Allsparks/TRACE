package org.allsparks.trace.examples;

import java.nio.file.Path;
import org.allsparks.trace.TraceConfig;
import org.allsparks.trace.TraceMode;
import org.allsparks.trace.core.Units;
import org.allsparks.trace.session.TraceSession;

/**
 * Bounded on-robot flight recording sketch. Writes a compact {@code .tlog}
 * without blocking the caller's loop on file I/O.
 */
public final class BoundedRecordingExample {
    public static TraceSession run(Path directory) throws InterruptedException {
        TraceSession session = new TraceSession(TraceConfig.builder()
                .mode(TraceMode.FULL)
                .opModeName("BoundedRecordingExample")
                .storageDirectory(directory)
                .fileSink(true)
                .memorySink(true)
                .maxFileBytes(64 * 1024)
                .maxTotalBytes(256 * 1024)
                .queueCapacity(256)
                .build());
        session.onOpModeStart();
        for (int i = 0; i < 20; i++) {
            try (var cycle = session.beginCycle()) {
                cycle.recordInput("AMPER/Battery/Voltage", 13.2 - i * 0.01, Units.VOLTS);
                cycle.recordOutput("Drive/Command", 0.4, Units.DIMENSIONLESS);
            }
        }
        session.onOpModeStop();
        return session;
    }

    private BoundedRecordingExample() {}
}
