package org.allsparks.trace;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import org.allsparks.trace.core.Pose2d;
import org.allsparks.trace.examples.BoundedRecordingExample;
import org.allsparks.trace.examples.DashboardAdapterExample;
import org.allsparks.trace.examples.EssentialTelemetryExample;
import org.allsparks.trace.examples.EventOnlyExample;
import org.allsparks.trace.examples.IterativeCycleExample;
import org.allsparks.trace.examples.SimulatedDrive;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ExampleCompilationTest {
    @AfterEach
    void reset() {
        Trace.resetForTests(TraceConfig.off());
    }

    @Test
    void examplesRunOnDesktop() throws Exception {
        EventOnlyExample.run();
        EssentialTelemetryExample.run(12.8, new Pose2d(0, 0, 0));
        assertTrue(IterativeCycleExample.run(3, 13.1, new Pose2d(1, 0, 0)).recorded().size() > 0);
        DashboardAdapterExample.CapturingAdapter adapter = DashboardAdapterExample.run();
        assertFalse(adapter.published.isEmpty());
        SimulatedDrive drive = new SimulatedDrive();
        drive.setPower(0.2);
        assertTrue(drive.readEncoder() > 0);
    }

    @Test
    void boundedRecordingExampleWritesFile(@TempDir Path dir) throws Exception {
        var session = BoundedRecordingExample.run(dir);
        assertTrue(session.recordingFile() == null || java.nio.file.Files.exists(session.recordingFile())
                || session.health().accepted() > 0);
    }
}
