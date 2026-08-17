package org.allsparks.trace.session;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Best-effort metadata source. Environment variables win; Git process lookup
 * is skipped when unavailable (typical on a Control Hub).
 */
public final class ProcessMetadataSource implements SessionMetadata.MetadataSource {
    @Override
    public String gitCommitSha() {
        String env = System.getenv("TRACE_GIT_SHA");
        if (env != null && !env.isEmpty()) {
            return env;
        }
        return runGit("rev-parse", "HEAD");
    }

    @Override
    public boolean dirtyWorkingTree() {
        String env = System.getenv("TRACE_GIT_DIRTY");
        if (env != null) {
            return "1".equals(env) || Boolean.parseBoolean(env);
        }
        String status = runGit("status", "--porcelain");
        return status != null && !status.trim().isEmpty();
    }

    @Override
    public String buildTimestamp() {
        String env = System.getenv("TRACE_BUILD_TIMESTAMP");
        if (env != null && !env.isEmpty()) {
            return env;
        }
        return Instant.now().toString();
    }

    @Override
    public String traceVersion() {
        String env = System.getenv("TRACE_VERSION");
        if (env != null && !env.isEmpty()) {
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
        String env = System.getenv("TRACE_FTC_SDK");
        return env == null || env.isEmpty() ? "unknown" : env;
    }

    @Override
    public Map<String, String> libraryVersions() {
        LinkedHashMap<String, String> versions = new LinkedHashMap<>();
        versions.put("trace", traceVersion());
        String extra = System.getenv("TRACE_LIBRARY_VERSIONS");
        if (extra != null && !extra.isEmpty()) {
            for (String part : extra.split(",")) {
                int eq = part.indexOf('=');
                if (eq > 0) {
                    versions.put(part.substring(0, eq).trim(), part.substring(eq + 1).trim());
                }
            }
        }
        return Collections.unmodifiableMap(versions);
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
