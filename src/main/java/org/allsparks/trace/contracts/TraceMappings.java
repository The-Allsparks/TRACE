package org.allsparks.trace.contracts;

import java.util.Objects;
import java.util.Optional;
import org.allsparks.contracts.health.HealthSeverity;
import org.allsparks.contracts.observation.Validity;
import org.allsparks.trace.core.TraceQuality;
import org.allsparks.trace.core.TraceSeverity;

/**
 * Edge mappings from TRACE recording labels onto allsparks-contracts envelopes.
 *
 * <p>Local {@link TraceQuality} and {@link TraceSeverity} stay on {@code TraceRecord}.
 * {@link TraceQuality#ESTIMATED} and {@link TraceQuality#ASYNC} are TRACE recording
 * labels and do not map to a {@link Validity}.
 */
public final class TraceMappings {
    private TraceMappings() {}

    /**
     * Maps a recorded quality onto shared {@link Validity} when the meaning matches.
     *
     * @param quality TRACE recording quality
     * @return mapped validity, or empty for {@code ESTIMATED} and {@code ASYNC}
     */
    public static Optional<Validity> toValidity(TraceQuality quality) {
        Objects.requireNonNull(quality, "quality");
        switch (quality) {
            case OK:
                return Optional.of(Validity.VALID);
            case STALE:
                return Optional.of(Validity.STALE);
            case INVALID:
                return Optional.of(Validity.INVALID);
            case MISSING:
                return Optional.of(Validity.MISSING);
            case ESTIMATED:
            case ASYNC:
                return Optional.empty();
            default:
                throw new IllegalArgumentException("Unknown quality: " + quality);
        }
    }

    /**
     * Maps a recorded event severity onto shared {@link HealthSeverity}.
     *
     * <p>{@link TraceSeverity#NOTICE} is informational. {@link TraceSeverity#ERROR}
     * is degraded health, not a commanded stop. {@link TraceSeverity#FAULT} maps
     * to {@link HealthSeverity#STOP_COMPONENT} as a recommended impact only.
     *
     * @param severity TRACE event severity
     * @return shared health severity
     */
    public static HealthSeverity toHealthSeverity(TraceSeverity severity) {
        Objects.requireNonNull(severity, "severity");
        switch (severity) {
            case INFO:
            case NOTICE:
                return HealthSeverity.INFO;
            case WARNING:
                return HealthSeverity.WARNING;
            case ERROR:
                return HealthSeverity.DEGRADED;
            case FAULT:
                return HealthSeverity.STOP_COMPONENT;
            default:
                throw new IllegalArgumentException("Unknown severity: " + severity);
        }
    }
}
