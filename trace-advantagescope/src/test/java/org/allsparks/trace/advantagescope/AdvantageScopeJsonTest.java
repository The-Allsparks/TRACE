package org.allsparks.trace.advantagescope;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.allsparks.trace.core.Pose2d;
import org.allsparks.trace.core.RecordCategory;
import org.allsparks.trace.core.TracePriority;
import org.allsparks.trace.core.TraceQuality;
import org.allsparks.trace.core.TraceRecord;
import org.allsparks.trace.core.TypedValue;
import org.allsparks.trace.core.Units;
import org.allsparks.trace.live.AdvantageScopeLiveStats;
import org.junit.jupiter.api.Test;

class AdvantageScopeJsonTest {
    @Test
    void telemetryUsesDashboardCompatibleShapeAndPoseSuffixes() {
        LatestValueMailbox mailbox = new LatestValueMailbox(16);
        mailbox.offer(TraceRecord.builder()
                .name("Battery/Voltage")
                .category(RecordCategory.OUTPUT)
                .value(TypedValue.ofDouble(12.6))
                .units(Units.VOLTS)
                .priority(TracePriority.NORMAL)
                .quality(TraceQuality.OK)
                .wallClockMillis(1_700_000_000_123L)
                .build());
        mailbox.offer(TraceRecord.builder()
                .name("Drive/Pose")
                .category(RecordCategory.OUTPUT)
                .value(TypedValue.ofPose(new Pose2d(1.0, 0.0, 1.57)))
                .units(Units.METERS)
                .priority(TracePriority.NORMAL)
                .quality(TraceQuality.OK)
                .wallClockMillis(1_700_000_000_123L)
                .build());
        String json = AdvantageScopeJson.telemetry(
                mailbox, AdvantageScopeLiveStats.inactive(), new StringBuilder());
        assertTrue(json.contains("\"type\":\"RECEIVE_TELEMETRY\""));
        assertTrue(json.contains("\"timestamp\":1700000000123"));
        assertTrue(json.contains("\"Battery/Voltage\":\"12.6\""));
        assertTrue(json.contains("\"Drive/Pose x\":\"39.37008\""));
        assertTrue(json.contains("\"Drive/Pose y\":\"0.0\""));
        assertTrue(json.contains("\"Drive/Pose heading\":\"1.57\""));
        assertTrue(json.contains("\"log\":[]"));
    }

    @Test
    void robotStatusAnswersHeartbeat() {
        String json = AdvantageScopeJson.robotStatus("TeleOp", true);
        assertTrue(json.contains("\"type\":\"RECEIVE_ROBOT_STATUS\""));
        assertTrue(json.contains("\"activeOpMode\":\"TeleOp\""));
        assertTrue(json.contains("\"activeOpModeStatus\":\"RUNNING\""));
        assertTrue(AdvantageScopeJson.isGetRobotStatus("{\"type\":\"GET_ROBOT_STATUS\"}"));
        assertEquals(false, AdvantageScopeJson.isGetRobotStatus("{\"type\":\"SAVE_CONFIG\"}"));
    }
}
