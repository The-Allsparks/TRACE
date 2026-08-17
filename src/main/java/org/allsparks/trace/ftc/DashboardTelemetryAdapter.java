package org.allsparks.trace.ftc;

/**
 * Marker for an optional FTC Dashboard packet publisher. TRACE never requires
 * FTC Dashboard; this interface exists so later phases can attach an adapter
 * without coupling the core to ACME Robotics.
 */
public interface DashboardTelemetryAdapter extends FtcTelemetryAdapter {
    default void put(String key, Object value) {
        publish(key, value);
    }
}
