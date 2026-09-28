package org.allsparks.trace.ftc;

/**
 * Optional Driver Station / dashboard telemetry hook. TRACE does not depend on
 * the FTC SDK; teams adapt {@code telemetry.addData} themselves.
 *
 * <p>Ungrouped records stay off this adapter. Channels recorded with a
 * {@code dsGroup} are packed and published at {@code Trace.publishTelemetry()}
 * (one caption, values joined with {@code " / "}). {@link #LINE} as a group
 * paints {@link #publishLine} instead (FTC {@code addLine}).
 */
public interface FtcTelemetryAdapter {
    /**
     * {@code dsGroup} that {@code publishTelemetry} sends as an unlabeled line.
     * Use for INIT help text, not packed captions.
     */
    String LINE = "\n";

    void publish(String key, Object value);

    /**
     * One unlabeled Driver Station line. Default writes the line as an
     * {@code addData} caption so adapters that only implement {@link #publish}
     * still show it.
     */
    default void publishLine(String line) {
        if (line != null && !line.isEmpty()) {
            publish(line, "");
        }
    }
}
