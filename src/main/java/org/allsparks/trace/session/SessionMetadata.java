package org.allsparks.trace.session;

import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.allsparks.trace.TraceConfig;
import org.allsparks.trace.core.SchemaVersion;

/**
 * Identifies the software and robot configuration that produced a session.
 * Collection degrades gracefully when Git or build information is unavailable:
 * {@code gitAvailable} and {@code buildInfoAvailable} are true only when a SHA
 * or build stamp was actually supplied, not inferred from session-start time.
 */
public final class SessionMetadata {
    private final String sessionId;
    private final String gitCommitSha;
    private final boolean dirtyWorkingTree;
    private final String buildTimestamp;
    private final String traceVersion;
    private final int traceSchemaVersion;
    private final int metadataSchemaVersion;
    private final String ftcSdkVersion;
    private final Map<String, String> libraryVersions;
    private final String robotConfigurationHash;
    private final String calibrationSetVersion;
    private final String opModeName;
    private final String recordingMode;
    private final String enabledIntegrations;
    private final String featureFlags;
    private final String robotName;
    private final String batteryId;
    private final String matchType;
    private final String matchNumber;
    private final String alliance;
    private final String station;
    private final String driverConfiguration;
    private final boolean gitAvailable;
    private final boolean buildInfoAvailable;

    private SessionMetadata(Builder builder) {
        this.sessionId = builder.sessionId;
        this.gitCommitSha = builder.gitCommitSha;
        this.dirtyWorkingTree = builder.dirtyWorkingTree;
        this.buildTimestamp = builder.buildTimestamp;
        this.traceVersion = builder.traceVersion;
        this.traceSchemaVersion = builder.traceSchemaVersion;
        this.metadataSchemaVersion = builder.metadataSchemaVersion;
        this.ftcSdkVersion = builder.ftcSdkVersion;
        this.libraryVersions = Collections.unmodifiableMap(new LinkedHashMap<>(builder.libraryVersions));
        this.robotConfigurationHash = builder.robotConfigurationHash;
        this.calibrationSetVersion = builder.calibrationSetVersion;
        this.opModeName = builder.opModeName;
        this.recordingMode = builder.recordingMode;
        this.enabledIntegrations = builder.enabledIntegrations;
        this.featureFlags = builder.featureFlags;
        this.robotName = builder.robotName;
        this.batteryId = builder.batteryId;
        this.matchType = builder.matchType;
        this.matchNumber = builder.matchNumber;
        this.alliance = builder.alliance;
        this.station = builder.station;
        this.driverConfiguration = builder.driverConfiguration;
        this.gitAvailable = builder.gitAvailable;
        this.buildInfoAvailable = builder.buildInfoAvailable;
    }

    public static SessionMetadata collect(TraceConfig config) {
        return collect(config, new ProcessMetadataSource());
    }

    public static SessionMetadata collect(TraceConfig config, MetadataSource source) {
        Objects.requireNonNull(config, "config");
        Objects.requireNonNull(source, "source");
        Builder builder = builder()
                .sessionId(UUID.randomUUID().toString())
                .traceVersion(source.traceVersion())
                .traceSchemaVersion(SchemaVersion.RECORD)
                .metadataSchemaVersion(SchemaVersion.METADATA)
                .opModeName(config.opModeName())
                .recordingMode(config.mode().name())
                .enabledIntegrations(String.join(",", config.enabledIntegrations()))
                .featureFlags(String.join(",", config.featureFlags()))
                .robotName(config.robotName())
                .ftcSdkVersion(orUnknown(source.ftcSdkVersion()));
        String sha = source.gitCommitSha();
        if (sha == null || sha.isEmpty()) {
            builder.gitCommitSha("unknown").gitAvailable(false).dirtyWorkingTree(false);
        } else {
            builder.gitCommitSha(sha).gitAvailable(true).dirtyWorkingTree(source.dirtyWorkingTree());
        }
        String built = source.buildTimestamp();
        if (built == null || built.isEmpty()) {
            builder.buildTimestamp(Instant.EPOCH.toString()).buildInfoAvailable(false);
        } else {
            builder.buildTimestamp(built).buildInfoAvailable(true);
        }
        builder.libraryVersions(source.libraryVersions());
        return builder.build();
    }

    public static Builder builder() {
        return new Builder();
    }

    public String sessionId() {
        return sessionId;
    }

    public String gitCommitSha() {
        return gitCommitSha;
    }

    public boolean dirtyWorkingTree() {
        return dirtyWorkingTree;
    }

    public String buildTimestamp() {
        return buildTimestamp;
    }

    public String traceVersion() {
        return traceVersion;
    }

    public int traceSchemaVersion() {
        return traceSchemaVersion;
    }

    public int metadataSchemaVersion() {
        return metadataSchemaVersion;
    }

    public String ftcSdkVersion() {
        return ftcSdkVersion;
    }

    public Map<String, String> libraryVersions() {
        return libraryVersions;
    }

    public String robotConfigurationHash() {
        return robotConfigurationHash;
    }

    public String calibrationSetVersion() {
        return calibrationSetVersion;
    }

    public String opModeName() {
        return opModeName;
    }

    public String recordingMode() {
        return recordingMode;
    }

    public String enabledIntegrations() {
        return enabledIntegrations;
    }

    public String featureFlags() {
        return featureFlags;
    }

    public String robotName() {
        return robotName;
    }

    public String batteryId() {
        return batteryId;
    }

    public String matchType() {
        return matchType;
    }

    public String matchNumber() {
        return matchNumber;
    }

    public String alliance() {
        return alliance;
    }

    public String station() {
        return station;
    }

    public String driverConfiguration() {
        return driverConfiguration;
    }

    public boolean gitAvailable() {
        return gitAvailable;
    }

    public boolean buildInfoAvailable() {
        return buildInfoAvailable;
    }

    public String toJson() {
        StringBuilder json = new StringBuilder();
        json.append('{');
        json.append("\"sessionId\":\"").append(escape(sessionId)).append('"');
        appendString(json, "gitCommitSha", gitCommitSha);
        json.append(",\"dirtyWorkingTree\":").append(dirtyWorkingTree);
        appendString(json, "buildTimestamp", buildTimestamp);
        appendString(json, "traceVersion", traceVersion);
        json.append(",\"traceSchemaVersion\":").append(traceSchemaVersion);
        json.append(",\"metadataSchemaVersion\":").append(metadataSchemaVersion);
        appendString(json, "ftcSdkVersion", ftcSdkVersion);
        json.append(",\"libraryVersions\":{");
        boolean firstLib = true;
        for (Map.Entry<String, String> entry : libraryVersions.entrySet()) {
            if (!firstLib) {
                json.append(',');
            }
            firstLib = false;
            json.append('"').append(escape(entry.getKey())).append("\":\"").append(escape(entry.getValue())).append('"');
        }
        json.append('}');
        appendString(json, "robotConfigurationHash", robotConfigurationHash);
        appendString(json, "calibrationSetVersion", calibrationSetVersion);
        appendString(json, "opModeName", opModeName);
        appendString(json, "recordingMode", recordingMode);
        appendString(json, "enabledIntegrations", enabledIntegrations);
        appendString(json, "featureFlags", featureFlags);
        appendString(json, "robotName", robotName);
        appendString(json, "batteryId", batteryId);
        appendString(json, "matchType", matchType);
        appendString(json, "matchNumber", matchNumber);
        appendString(json, "alliance", alliance);
        appendString(json, "station", station);
        appendString(json, "driverConfiguration", driverConfiguration);
        json.append(",\"gitAvailable\":").append(gitAvailable);
        json.append(",\"buildInfoAvailable\":").append(buildInfoAvailable);
        json.append('}');
        return json.toString();
    }

    public static SessionMetadata fromJson(String json) {
        JsonMap map = JsonMap.parse(json);
        Builder builder = builder()
                .sessionId(map.string("sessionId", UUID.randomUUID().toString()))
                .gitCommitSha(map.string("gitCommitSha", "unknown"))
                .dirtyWorkingTree(map.bool("dirtyWorkingTree", false))
                .buildTimestamp(map.string("buildTimestamp", Instant.EPOCH.toString()))
                .traceVersion(map.string("traceVersion", "unknown"))
                .traceSchemaVersion(map.integer("traceSchemaVersion", SchemaVersion.RECORD))
                .metadataSchemaVersion(map.integer("metadataSchemaVersion", SchemaVersion.METADATA))
                .ftcSdkVersion(map.string("ftcSdkVersion", "unknown"))
                .robotConfigurationHash(map.string("robotConfigurationHash", "unknown"))
                .calibrationSetVersion(map.string("calibrationSetVersion", "unknown"))
                .opModeName(map.string("opModeName", "unknown"))
                .recordingMode(map.string("recordingMode", "OFF"))
                .enabledIntegrations(map.string("enabledIntegrations", ""))
                .featureFlags(map.string("featureFlags", ""))
                .robotName(map.string("robotName", "unknown"))
                .batteryId(map.string("batteryId", ""))
                .matchType(map.string("matchType", ""))
                .matchNumber(map.string("matchNumber", ""))
                .alliance(map.string("alliance", ""))
                .station(map.string("station", ""))
                .driverConfiguration(map.string("driverConfiguration", ""))
                .gitAvailable(map.bool("gitAvailable", false))
                .buildInfoAvailable(map.bool("buildInfoAvailable", false));
        builder.libraryVersions(map.object("libraryVersions"));
        return builder.build();
    }

    public boolean compatibleWith(SessionMetadata other) {
        return other != null
                && traceSchemaVersion == other.traceSchemaVersion
                && Objects.equals(traceVersion, other.traceVersion)
                && Objects.equals(gitCommitSha, other.gitCommitSha);
    }

    private static void appendString(StringBuilder json, String key, String value) {
        json.append(",\"").append(key).append("\":\"").append(escape(value == null ? "" : value)).append('"');
    }

    private static String escape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static String orUnknown(String value) {
        return value == null || value.isEmpty() ? "unknown" : value;
    }

    public static final class Builder {
        private String sessionId = UUID.randomUUID().toString();
        private String gitCommitSha = "unknown";
        private boolean dirtyWorkingTree;
        private String buildTimestamp = Instant.EPOCH.toString();
        private String traceVersion = "0.1.0-SNAPSHOT";
        private int traceSchemaVersion = SchemaVersion.RECORD;
        private int metadataSchemaVersion = SchemaVersion.METADATA;
        private String ftcSdkVersion = "unknown";
        private Map<String, String> libraryVersions = new LinkedHashMap<>();
        private String robotConfigurationHash = "unknown";
        private String calibrationSetVersion = "unknown";
        private String opModeName = "unknown";
        private String recordingMode = "OFF";
        private String enabledIntegrations = "";
        private String featureFlags = "";
        private String robotName = "unknown";
        private String batteryId = "";
        private String matchType = "";
        private String matchNumber = "";
        private String alliance = "";
        private String station = "";
        private String driverConfiguration = "";
        private boolean gitAvailable;
        private boolean buildInfoAvailable;

        public Builder sessionId(String sessionId) {
            this.sessionId = sessionId;
            return this;
        }

        public Builder gitCommitSha(String gitCommitSha) {
            this.gitCommitSha = gitCommitSha;
            return this;
        }

        public Builder dirtyWorkingTree(boolean dirtyWorkingTree) {
            this.dirtyWorkingTree = dirtyWorkingTree;
            return this;
        }

        public Builder buildTimestamp(String buildTimestamp) {
            this.buildTimestamp = buildTimestamp;
            return this;
        }

        public Builder traceVersion(String traceVersion) {
            this.traceVersion = traceVersion;
            return this;
        }

        public Builder traceSchemaVersion(int traceSchemaVersion) {
            this.traceSchemaVersion = traceSchemaVersion;
            return this;
        }

        public Builder metadataSchemaVersion(int metadataSchemaVersion) {
            this.metadataSchemaVersion = metadataSchemaVersion;
            return this;
        }

        public Builder ftcSdkVersion(String ftcSdkVersion) {
            this.ftcSdkVersion = ftcSdkVersion;
            return this;
        }

        public Builder libraryVersions(Map<String, String> libraryVersions) {
            this.libraryVersions = new LinkedHashMap<>(libraryVersions);
            return this;
        }

        public Builder robotConfigurationHash(String robotConfigurationHash) {
            this.robotConfigurationHash = robotConfigurationHash;
            return this;
        }

        public Builder calibrationSetVersion(String calibrationSetVersion) {
            this.calibrationSetVersion = calibrationSetVersion;
            return this;
        }

        public Builder opModeName(String opModeName) {
            this.opModeName = opModeName;
            return this;
        }

        public Builder recordingMode(String recordingMode) {
            this.recordingMode = recordingMode;
            return this;
        }

        public Builder enabledIntegrations(String enabledIntegrations) {
            this.enabledIntegrations = enabledIntegrations;
            return this;
        }

        public Builder featureFlags(String featureFlags) {
            this.featureFlags = featureFlags;
            return this;
        }

        public Builder robotName(String robotName) {
            this.robotName = robotName;
            return this;
        }

        public Builder batteryId(String batteryId) {
            this.batteryId = batteryId;
            return this;
        }

        public Builder matchType(String matchType) {
            this.matchType = matchType;
            return this;
        }

        public Builder matchNumber(String matchNumber) {
            this.matchNumber = matchNumber;
            return this;
        }

        public Builder alliance(String alliance) {
            this.alliance = alliance;
            return this;
        }

        public Builder station(String station) {
            this.station = station;
            return this;
        }

        public Builder driverConfiguration(String driverConfiguration) {
            this.driverConfiguration = driverConfiguration;
            return this;
        }

        public Builder gitAvailable(boolean gitAvailable) {
            this.gitAvailable = gitAvailable;
            return this;
        }

        public Builder buildInfoAvailable(boolean buildInfoAvailable) {
            this.buildInfoAvailable = buildInfoAvailable;
            return this;
        }

        public SessionMetadata build() {
            return new SessionMetadata(this);
        }
    }

    /**
     * Abstraction so Control Hub deployments can supply Git/build data without
     * spawning processes.
     */
    public interface MetadataSource {
        String gitCommitSha();

        boolean dirtyWorkingTree();

        String buildTimestamp();

        String traceVersion();

        String ftcSdkVersion();

        Map<String, String> libraryVersions();
    }
}
