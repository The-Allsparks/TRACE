package org.allsparks.trace;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.allsparks.trace.session.ProcessMetadataSource;
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

    @Test
    void defaultCollectDoesNotClaimBuildInfoOrGitWithoutEnv() {
        assumeFalse(envSet("TRACE_BUILD_TIMESTAMP"));
        assumeFalse(envSet("TRACE_GIT_SHA"));
        assumeFalse(envTruthy("TRACE_GIT_SPAWN"));

        SessionMetadata metadata = SessionMetadata.collect(TraceConfig.off());
        assertFalse(metadata.buildInfoAvailable());
        assertEquals(Instant.EPOCH.toString(), metadata.buildTimestamp());
        assertFalse(metadata.gitAvailable());
        assertEquals("unknown", metadata.gitCommitSha());
        assertFalse(metadata.dirtyWorkingTree());
    }

    @Test
    void defaultSourceReturnsNullBuildStampWhenEnvUnset() {
        assumeFalse(envSet("TRACE_BUILD_TIMESTAMP"));
        ProcessMetadataSource source = new ProcessMetadataSource();
        assertNull(source.buildTimestamp());
    }

    @Test
    void defaultSourceDoesNotNeedGitWhenSpawnDisabled() {
        assumeFalse(envTruthy("TRACE_GIT_SPAWN"));
        ProcessMetadataSource source = new ProcessMetadataSource();
        String envSha = System.getenv("TRACE_GIT_SHA");
        if (envSha == null || envSha.isEmpty()) {
            assertNull(source.gitCommitSha());
        } else {
            assertEquals(envSha, source.gitCommitSha());
        }
        String envDirty = System.getenv("TRACE_GIT_DIRTY");
        if (envDirty == null) {
            assertFalse(source.dirtyWorkingTree());
        } else {
            assertEquals("1".equals(envDirty) || Boolean.parseBoolean(envDirty), source.dirtyWorkingTree());
        }
    }

    @Test
    void processSourceHonorsEnvOverridesWithoutGitSpawn() {
        Map<String, String> env = new HashMap<>();
        env.put("TRACE_GIT_SHA", "abc123def");
        env.put("TRACE_GIT_DIRTY", "1");
        env.put("TRACE_BUILD_TIMESTAMP", "2026-01-02T03:04:05Z");
        env.put("TRACE_VERSION", "9.9.9");
        env.put("TRACE_FTC_SDK", "10.3");
        ProcessMetadataSource source = new ProcessMetadataSource(env::get);

        assertEquals("abc123def", source.gitCommitSha());
        assertTrue(source.dirtyWorkingTree());
        assertEquals("2026-01-02T03:04:05Z", source.buildTimestamp());
        assertEquals("9.9.9", source.traceVersion());
        assertEquals("10.3", source.ftcSdkVersion());

        SessionMetadata metadata = SessionMetadata.collect(TraceConfig.off(), source);
        assertTrue(metadata.gitAvailable());
        assertEquals("abc123def", metadata.gitCommitSha());
        assertTrue(metadata.dirtyWorkingTree());
        assertTrue(metadata.buildInfoAvailable());
        assertEquals("2026-01-02T03:04:05Z", metadata.buildTimestamp());
        assertEquals("9.9.9", metadata.traceVersion());
        assertEquals("10.3", metadata.ftcSdkVersion());
    }

    @Test
    void processSourceWithEmptyEnvDoesNotClaimProvenance() {
        ProcessMetadataSource source = new ProcessMetadataSource(name -> null);
        assertNull(source.gitCommitSha());
        assertFalse(source.dirtyWorkingTree());
        assertNull(source.buildTimestamp());

        SessionMetadata metadata = SessionMetadata.collect(TraceConfig.off(), source);
        assertFalse(metadata.gitAvailable());
        assertFalse(metadata.buildInfoAvailable());
        assertEquals("unknown", metadata.gitCommitSha());
        assertEquals(Instant.EPOCH.toString(), metadata.buildTimestamp());
    }

    private static boolean envSet(String name) {
        String value = System.getenv(name);
        return value != null && !value.isEmpty();
    }

    private static boolean envTruthy(String name) {
        String value = System.getenv(name);
        return "1".equals(value) || Boolean.parseBoolean(value);
    }
}
