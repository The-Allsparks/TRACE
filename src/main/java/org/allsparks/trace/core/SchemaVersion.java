package org.allsparks.trace.core;

/**
 * Schema versions evolve without silently changing existing field meaning.
 * Bump {@link #RECORD} when the on-disk record layout changes, and
 * {@link #METADATA} when session metadata keys change meaning.
 */
public final class SchemaVersion {
    public static final int RECORD = 1;
    public static final int METADATA = 1;
    public static final String FORMAT_NAME = "TLOG";

    private SchemaVersion() {}
}
