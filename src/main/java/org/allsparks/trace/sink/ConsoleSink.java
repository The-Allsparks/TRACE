package org.allsparks.trace.sink;

import java.io.PrintStream;
import java.util.Objects;
import org.allsparks.trace.core.TraceRecord;
import org.allsparks.trace.export.HumanReadableExporter;

/** Test and classroom sink that prints records to a stream. */
public final class ConsoleSink implements TraceSink {
    private final PrintStream out;
    private final HumanReadableExporter exporter = new HumanReadableExporter();

    public ConsoleSink() {
        this(System.out);
    }

    public ConsoleSink(PrintStream out) {
        this.out = Objects.requireNonNull(out, "out");
    }

    @Override
    public void accept(TraceRecord record) {
        out.println(exporter.format(record));
    }
}
