package org.allsparks.trace.export;

import org.allsparks.trace.core.TraceRecord;

/** One-line human-readable formatting for events and classroom review. */
public final class HumanReadableExporter {
    public String format(TraceRecord record) {
        StringBuilder builder = new StringBuilder();
        builder.append("t=").append(record.monotonicNanos());
        builder.append(" cycle=").append(record.cycle());
        builder.append(' ').append(record.category());
        builder.append(' ').append(record.priority());
        builder.append(' ').append(record.name().value());
        if (record.category() == org.allsparks.trace.core.RecordCategory.EVENT) {
            builder.append(" [").append(record.severity()).append("] ");
            builder.append(record.message());
        } else {
            builder.append('=').append(record.value().render());
            if (!record.units().symbol().equals("none")) {
                builder.append(' ').append(record.units().symbol());
            }
        }
        builder.append(" quality=").append(record.quality());
        builder.append(" src=").append(record.source());
        builder.append(" schema=").append(record.schemaVersion());
        return builder.toString();
    }
}
