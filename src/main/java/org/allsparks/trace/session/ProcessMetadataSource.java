package org.allsparks.trace.session;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

/**
 * Best-effort metadata source. Environment variables win. Git is not spawned
 * unless {@code TRACE_GIT_SPAWN} is {@code 1} or {@code true} (Control Hub-safe
 * default). {@link #buildTimestamp()} is empty unless {@code TRACE_BUILD_TIMESTAMP}
 * is set; session-start time is never treated as a build stamp.
 */
public final class ProcessMetadataSource implements SessionMetadata.MetadataSource {
    private final Function<String, String> environment;

    public ProcessMetadataSource() {
        this(System::getenv);
    }

    /**
     * @param environment environment lookup; typically {@code System::getenv}
     */
    public ProcessMetadataSource(Function<String, String> environment) {
        this.environment = Objects.requireNonNull(environment, "environment");
    }

    @Override
    public String gitCommitSha() {
        String env = env("TRACE_GIT_SHA");
        if (env != null) {
            return env;
        }
        if (!gitSpawnEnabled()) {
            return null;
        }
        return runGit("rev-parse", "HEAD");
    }

    @Override
    public boolean dirtyWorkingTree() {
        String env = environment.apply("TRACE_GIT_DIRTY");
        if (env != null) {
            return truthy(env);
        }
        if (!gitSpawnEnabled()) {
            return false;
        }
        String status = runGit("status", "--porcelain");
        return status != null && !status.trim().isEmpty();
    }

    @Override
    public String buildTimestamp() {
        return env("TRACE_BUILD_TIMESTAMP");
    }

    @Override
    public String traceVersion() {
        String env = env("TRACE_VERSION");
        if (env != null) {
            return env;
        }
        Package pack = TraceVersionHolder.PACKAGE;
        if (pack != null && pack.getImplementationVersion() != null) {
            return pack.getImplementationVersion();
        }
        return "0.1.0-SNAPSHOT";
    }

    @Override
    public String ftcSdkVersion() {
        String env = env("TRACE_FTC_SDK");
        return env == null ? "unknown" : env;
    }

    @Override
    public Map<String, String> libraryVersions() {
        LinkedHashMap<String, String> versions = new LinkedHashMap<>();
        versions.put("trace", traceVersion());
        String extra = env("TRACE_LIBRARY_VERSIONS");
        if (extra != null) {
            for (String part : extra.split(",")) {
                int eq = part.indexOf('=');
                if (eq > 0) {
                    versions.put(part.substring(0, eq).trim(), part.substring(eq + 1).trim());
                }
            }
        }
        return Collections.unmodifiableMap(versions);
    }

    private boolean gitSpawnEnabled() {
        return truthy(environment.apply("TRACE_GIT_SPAWN"));
    }

    private String env(String name) {
        String value = environment.apply(name);
        if (value == null || value.isEmpty()) {
            return null;
        }
        return value;
    }

    private static boolean truthy(String value) {
        return "1".equals(value) || Boolean.parseBoolean(value);
    }

    private static String runGit(String... args) {
        try {
            String[] command = new String[args.length + 1];
            command[0] = "git";
            System.arraycopy(args, 0, command, 1, args.length);
            Process process = new ProcessBuilder(command)
                    .redirectErrorStream(true)
                    .start();
            if (!process.waitFor(400, TimeUnit.MILLISECONDS)) {
                process.destroyForcibly();
                return null;
            }
            if (process.exitValue() != 0) {
                return null;
            }
            try (BufferedReader reader =
                    new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                StringBuilder output = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    if (output.length() > 0) {
                        output.append('\n');
                    }
                    output.append(line);
                }
                return output.toString();
            }
        } catch (Exception ignored) {
            return null;
        }
    }

    private static final class TraceVersionHolder {
        private static final Package PACKAGE = org.allsparks.trace.Trace.class.getPackage();
    }
}
