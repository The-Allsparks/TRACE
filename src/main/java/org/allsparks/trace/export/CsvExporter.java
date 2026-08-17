package org.allsparks.trace.export;

import java.util.List;
import java.util.Locale;
import org.allsparks.trace.core.TraceRecord;

/**
 * Educational CSV export. CSV is an interchange format, not the canonical
 * on-disk format.
 */
public final class CsvExporter {
    public static final String HEADER =
            "timestamp_s,cycle,category,priority,quality,name,units,value,source,schema,severity,message";

    public String export(List<TraceRecord> records) {
        StringBuilder builder = new StringBuilder();
        builder.append(HEADER).append('\n');
        for (TraceRecord record : records) {
            builder.append(formatRow(record)).append('\n');
        }
        return builder.toString();
    }

    public String formatRow(TraceRecord record) {
        double seconds = record.monotonicNanos() / 1_000_000_000.0;
        return String.format(
                Locale.ROOT,
                "%.9f,%d,%s,%s,%s,%s,%s,%s,%s,%d,%s,%s",
                seconds,
                record.cycle(),
                record.category(),
                record.priority(),
                record.quality(),
                csv(record.name().value()),
                csv(record.units().symbol()),
                csv(record.value().render()),
                csv(record.source()),
                record.schemaVersion(),
                record.severity(),
                csv(record.message()));
    }

    /**
     * AdvantageScope "CSV (List)" interchange: Timestamp, Key, Value.
     * Documented as lossy relative to TRACE schema.
     */
    public String exportAdvantageScopeList(List<TraceRecord> records) {
        StringBuilder builder = new StringBuilder();
        builder.append("Timestamp, Key, Value\n");
        for (TraceRecord record : records) {
            double seconds = record.monotonicNanos() / 1_000_000_000.0;
            String value = record.category() == org.allsparks.trace.core.RecordCategory.EVENT
                    ? record.message()
                    : record.value().render();
            builder.append(String.format(Locale.ROOT, "%.6f, %s, %s\n", seconds, record.name().value(), csv(value)));
        }
        return builder.toString();
    }

    private static String csv(String value) {
        if (value == null) {
            return "";
        }
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
