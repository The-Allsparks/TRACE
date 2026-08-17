package org.allsparks.trace.examples;

import java.util.LinkedHashMap;
import java.util.Map;
import org.allsparks.trace.TraceConfig;
import org.allsparks.trace.TraceMode;
import org.allsparks.trace.ftc.DashboardTelemetryAdapter;
import org.allsparks.trace.ftc.FtcTelemetryAdapter;
import org.allsparks.trace.session.TraceSession;

/**
 * Shows how to attach Driver Station telemetry or FTC Dashboard without TRACE
 * depending on those libraries.
 */
public final class DashboardAdapterExample {
    public static final class CapturingAdapter implements DashboardTelemetryAdapter, FtcTelemetryAdapter {
        public final Map<String, Object> published = new LinkedHashMap<>();

        @Override
        public void publish(String key, Object value) {
            published.put(key, value);
        }
    }

    public static CapturingAdapter run() {
        CapturingAdapter adapter = new CapturingAdapter();
        TraceSession session = new TraceSession(TraceConfig.builder()
                .mode(TraceMode.ESSENTIAL)
                .opModeName("DashboardAdapterExample")
                .memorySink(true)
                .build());
        session.setTelemetryAdapter(adapter);
        session.event("Autonomous started");
        session.record("Drive/Command", 0.25, org.allsparks.trace.core.Units.DIMENSIONLESS);
        session.close();
        return adapter;
    }

    private DashboardAdapterExample() {}
}
