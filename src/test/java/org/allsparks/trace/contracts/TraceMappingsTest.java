package org.allsparks.trace.contracts;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.EnumSet;
import java.util.Optional;
import org.allsparks.contracts.health.HealthSeverity;
import org.allsparks.contracts.observation.Validity;
import org.allsparks.trace.core.TraceQuality;
import org.allsparks.trace.core.TraceSeverity;
import org.junit.jupiter.api.Test;

class TraceMappingsTest {
    @Test
    void qualityMapsOntoValidity() {
        assertEquals(Optional.of(Validity.VALID), TraceMappings.toValidity(TraceQuality.OK));
        assertEquals(Optional.of(Validity.STALE), TraceMappings.toValidity(TraceQuality.STALE));
        assertEquals(Optional.of(Validity.INVALID), TraceMappings.toValidity(TraceQuality.INVALID));
        assertEquals(Optional.of(Validity.MISSING), TraceMappings.toValidity(TraceQuality.MISSING));
    }

    @Test
    void estimatedAndAsyncStayTraceRecordingLabels() {
        assertEquals(Optional.empty(), TraceMappings.toValidity(TraceQuality.ESTIMATED));
        assertEquals(Optional.empty(), TraceMappings.toValidity(TraceQuality.ASYNC));
    }

    @Test
    void everyQualityIsMappedOrExplicitlyUnmapped() {
        EnumSet<TraceQuality> unmapped = EnumSet.of(TraceQuality.ESTIMATED, TraceQuality.ASYNC);
        for (TraceQuality quality : TraceQuality.values()) {
            Optional<Validity> mapped = TraceMappings.toValidity(quality);
            if (unmapped.contains(quality)) {
                assertFalse(mapped.isPresent(), quality.name());
            } else {
                assertTrue(mapped.isPresent(), quality.name());
            }
        }
    }

    @Test
    void severityMapsOntoHealthSeverity() {
        assertEquals(HealthSeverity.INFO, TraceMappings.toHealthSeverity(TraceSeverity.INFO));
        assertEquals(HealthSeverity.INFO, TraceMappings.toHealthSeverity(TraceSeverity.NOTICE));
        assertEquals(HealthSeverity.WARNING, TraceMappings.toHealthSeverity(TraceSeverity.WARNING));
        assertEquals(HealthSeverity.DEGRADED, TraceMappings.toHealthSeverity(TraceSeverity.ERROR));
        assertEquals(HealthSeverity.STOP_COMPONENT, TraceMappings.toHealthSeverity(TraceSeverity.FAULT));
    }

    @Test
    void everySeverityIsMapped() {
        for (TraceSeverity severity : TraceSeverity.values()) {
            TraceMappings.toHealthSeverity(severity);
        }
    }

    @Test
    void nullInputsAreRejected() {
        assertThrows(NullPointerException.class, () -> TraceMappings.toValidity(null));
        assertThrows(NullPointerException.class, () -> TraceMappings.toHealthSeverity(null));
    }
}
