package org.allsparks.trace.ftc;

/**
 * Optional Driver Station / dashboard telemetry hook. TRACE does not depend on
 * the FTC SDK; teams adapt {@code telemetry.addData} themselves.
 */
public interface FtcTelemetryAdapter {
    void publish(String key, Object value);
}
