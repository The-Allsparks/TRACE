package org.allsparks.trace.live;

import org.allsparks.trace.TraceConfig;

/**
 * Reflective load of {@code trace-advantagescope}. Classpath presence alone
 * does not start a server; {@link TraceConfig#advantageScopeStreaming()} must
 * also be true.
 */
public final class AdvantageScopeLiveLoader {
    public static final String IMPLEMENTATION = "org.allsparks.trace.advantagescope.AdvantageScopePublisher";

    private AdvantageScopeLiveLoader() {}

    public static AdvantageScopeLive load(TraceConfig config, AdvantageScopeMetricSink metrics) {
        if (config == null || metrics == null || !config.advantageScopeStreaming() || !config.isEnabled()) {
            return null;
        }
        try {
            Class<?> type = Class.forName(IMPLEMENTATION);
            return (AdvantageScopeLive)
                    type.getConstructor(TraceConfig.class, AdvantageScopeMetricSink.class)
                            .newInstance(config, metrics);
        } catch (ClassNotFoundException absent) {
            return null;
        } catch (Throwable failed) {
            return null;
        }
    }
}
