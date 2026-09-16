package org.allsparks.trace.advantagescope;

import java.util.Iterator;
import java.util.Map;
import org.allsparks.trace.advantagescope.LatestValueMailbox.LiveSlot;
import org.allsparks.trace.core.Pose2d;
import org.allsparks.trace.core.TypedValue;
import org.allsparks.trace.live.AdvantageScopeLiveStats;

/**
 * Minimal JSON for the FTC Dashboard WebSocket messages AdvantageScope
 * consumes. Not a copy of ACME DashboardCore; field names match that protocol
 * so {@code File → Connect to Robot → FTC Dashboard} works.
 */
final class AdvantageScopeJson {
    private AdvantageScopeJson() {}

    static String robotStatus(String opModeName, boolean running) {
        StringBuilder json = new StringBuilder(256);
        json.append("{\"type\":\"RECEIVE_ROBOT_STATUS\",\"status\":{");
        json.append("\"enabled\":true,\"available\":true,");
        json.append("\"activeOpMode\":");
        quote(json, opModeName == null || opModeName.isEmpty() ? "unknown" : opModeName);
        json.append(",\"activeOpModeStatus\":");
        quote(json, running ? "RUNNING" : "STOPPED");
        json.append(",\"warningMessage\":\"\",\"errorMessage\":\"\",\"batteryVoltage\":0.0}}");
        return json.toString();
    }

    static String telemetry(
            LatestValueMailbox mailbox, AdvantageScopeLiveStats stats, StringBuilder buffer) {
        buffer.setLength(0);
        buffer.append("{\"type\":\"RECEIVE_TELEMETRY\",\"telemetry\":[{");
        long timestamp = 0L;
        buffer.append("\"data\":{");
        boolean first = true;
        int valuesSent = 0;
        Iterator<Map.Entry<String, LiveSlot>> entries = mailbox.entries();
        while (entries.hasNext()) {
            Map.Entry<String, LiveSlot> entry = entries.next();
            LiveSlot slot = entry.getValue();
            TypedValue value = slot.value();
            if (value == null) {
                continue;
            }
            if (slot.wallClockMillis() > timestamp) {
                timestamp = slot.wallClockMillis();
            }
            Pose2d pose = LiveValueEncoder.pose(value);
            if (pose != null) {
                first = append(buffer, first, entry.getKey() + " x", Double.toString(LiveValueEncoder.toInches(pose.x(), slot.units())));
                first = append(buffer, first, entry.getKey() + " y", Double.toString(LiveValueEncoder.toInches(pose.y(), slot.units())));
                first = append(buffer, first, entry.getKey() + " heading", Double.toString(pose.headingRad()));
                valuesSent += 3;
                continue;
            }
            String rendered = LiveValueEncoder.scalar(value);
            if (rendered == null) {
                continue;
            }
            first = append(buffer, first, entry.getKey(), rendered);
            valuesSent++;
        }
        first = overlay(buffer, first, stats, valuesSent);
        buffer.append("},\"timestamp\":").append(timestamp).append(",\"log\":[]}]}");
        return buffer.toString();
    }

    static boolean isGetRobotStatus(String message) {
        if (message == null) {
            return false;
        }
        return message.contains("GET_ROBOT_STATUS");
    }

    private static boolean overlay(
            StringBuilder buffer, boolean first, AdvantageScopeLiveStats stats, int valuesSent) {
        if (stats == null) {
            return first;
        }
        first = append(buffer, first, "TRACE/AdvantageScope/Enabled", stats.enabled() ? "true" : "false");
        first = append(buffer, first, "TRACE/AdvantageScope/ConfiguredRateHz", Integer.toString(stats.configuredRateHz()));
        first = append(buffer, first, "TRACE/AdvantageScope/ClientCount", Integer.toString(stats.clientCount()));
        first = append(buffer, first, "TRACE/AdvantageScope/FramesPrepared", Long.toString(stats.framesPrepared()));
        first = append(buffer, first, "TRACE/AdvantageScope/FramesSent", Long.toString(stats.framesSent()));
        first = append(buffer, first, "TRACE/AdvantageScope/FramesDropped", Long.toString(stats.framesDropped()));
        first = append(buffer, first, "TRACE/AdvantageScope/SendErrors", Long.toString(stats.sendErrors()));
        first = append(buffer, first, "TRACE/AdvantageScope/ValuesSent", Long.toString(valuesSent));
        first = append(buffer, first, "TRACE/AdvantageScope/ValuesSkipped", Long.toString(stats.valuesSkipped()));
        first = append(buffer, first, "TRACE/AdvantageScope/LastSendDurationMs", Long.toString(stats.lastSendDurationMs()));
        first = append(buffer, first, "TRACE/AdvantageScope/MaxSendDurationMs", Long.toString(stats.maxSendDurationMs()));
        first = append(buffer, first, "TRACE/AdvantageScope/SerializationDurationMs", Long.toString(stats.serializationDurationMs()));
        first = append(buffer, first, "TRACE/AdvantageScope/BytesSent", Long.toString(stats.bytesSent()));
        return append(buffer, first, "TRACE/AdvantageScope/ReconnectCount", Long.toString(stats.reconnectCount()));
    }

    private static boolean append(StringBuilder buffer, boolean first, String key, String value) {
        if (!first) {
            buffer.append(',');
        }
        quote(buffer, key);
        buffer.append(':');
        quote(buffer, value);
        return false;
    }

    static void quote(StringBuilder buffer, String text) {
        buffer.append('"');
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            switch (ch) {
                case '"':
                    buffer.append("\\\"");
                    break;
                case '\\':
                    buffer.append("\\\\");
                    break;
                case '\n':
                    buffer.append("\\n");
                    break;
                case '\r':
                    buffer.append("\\r");
                    break;
                case '\t':
                    buffer.append("\\t");
                    break;
                default:
                    if (ch < 0x20) {
                        buffer.append("\\u");
                        String hex = Integer.toHexString(ch);
                        for (int pad = hex.length(); pad < 4; pad++) {
                            buffer.append('0');
                        }
                        buffer.append(hex);
                    } else {
                        buffer.append(ch);
                    }
                    break;
            }
        }
        buffer.append('"');
    }
}
