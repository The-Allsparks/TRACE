package org.allsparks.trace;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import java.util.Map;
import org.allsparks.trace.session.SessionMetadata;
import org.junit.jupiter.api.Test;

class MetadataTest {
    @Test
    void jsonRoundTripAndGracefulUnknownGit() {
        SessionMetadata original = SessionMetadata.builder()
                .sessionId("sid")
                .gitCommitSha("unknown")
                .gitAvailable(false)
                .dirtyWorkingTree(false)
                .buildTimestamp("2026-08-17T00:00:00Z")
                .buildInfoAvailable(true)
                .traceVersion("0.1.0-SNAPSHOT")
                .ftcSdkVersion("10.3")
                .libraryVersions(Map.of("trace", "0.1.0-SNAPSHOT"))
                .robotName("Spark")
                .opModeName("Auto")
                .recordingMode("ESSENTIAL")
                .enabledIntegrations("AMPER")
                .featureFlags("passive")
                .build();
        SessionMetadata restored = SessionMetadata.fromJson(original.toJson());
        assertEquals("sid", restored.sessionId());
        assertEquals("unknown", restored.gitCommitSha());
        assertFalse(restored.gitAvailable());
        assertEquals("Spark", restored.robotName());
        assertEquals("AMPER", restored.enabledIntegrations());
        assertTrue(original.compatibleWith(restored));
        SessionMetadata mismatch = SessionMetadata.builder()
                .gitCommitSha("other")
                .traceVersion("0.1.0-SNAPSHOT")
                .build();
        assertFalse(original.compatibleWith(mismatch));
    }

    @Test
    void collectUsesProvidedSource() {
        SessionMetadata.MetadataSource source = new SessionMetadata.MetadataSource() {
            @Override
            public String gitCommitSha() {
                return null;
            }

            @Override
            public boolean dirtyWorkingTree() {
                return false;
            }

            @Override
            public String buildTimestamp() {
                return "";
            }

            @Override
            public String traceVersion() {
                return "test";
            }

            @Override
            public String ftcSdkVersion() {
                return "";
            }

            @Override
            public Map<String, String> libraryVersions() {
                return Collections.emptyMap();
            }
        };
        SessionMetadata metadata = SessionMetadata.collect(TraceConfig.builder().mode(TraceMode.EVENTS).build(), source);
        assertFalse(metadata.gitAvailable());
        assertFalse(metadata.buildInfoAvailable());
        assertEquals("unknown", metadata.gitCommitSha());
        assertEquals("test", metadata.traceVersion());
    }
}
