package org.allsparks.trace.core;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Hierarchical signal names such as {@code Drive/Pose} or
 * {@code AMPER/Battery/Voltage}.
 */
public final class SignalName {
    private static final Pattern SEGMENT = Pattern.compile("[A-Za-z][A-Za-z0-9_]*");

    private final String value;

    private SignalName(String value) {
        this.value = value;
    }

    public static SignalName parse(String raw) {
        Objects.requireNonNull(raw, "name");
        String trimmed = raw.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("Signal name must not be empty");
        }
        if (trimmed.startsWith("/") || trimmed.endsWith("/")) {
            throw new IllegalArgumentException("Signal name must not start or end with '/': " + raw);
        }
        String[] parts = trimmed.split("/");
        if (parts.length == 0) {
            throw new IllegalArgumentException("Signal name has no segments: " + raw);
        }
        for (String part : parts) {
            if (!SEGMENT.matcher(part).matches()) {
                throw new IllegalArgumentException(
                        "Invalid signal segment '" + part + "' in '" + raw + "'");
            }
        }
        return new SignalName(trimmed);
    }

    public String value() {
        return value;
    }

    public String root() {
        int slash = value.indexOf('/');
        return slash < 0 ? value : value.substring(0, slash);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof SignalName)) {
            return false;
        }
        return value.equals(((SignalName) other).value);
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }

    @Override
    public String toString() {
        return value;
    }
}
