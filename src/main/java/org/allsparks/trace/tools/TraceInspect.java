package org.allsparks.trace.tools;

import java.nio.file.Path;
import org.allsparks.trace.export.CsvExporter;
import org.allsparks.trace.export.HumanReadableExporter;
import org.allsparks.trace.storage.TlogReader;

/**
 * Desktop inspection utility for TRACE {@code .tlog} files.
 */
public final class TraceInspect {
    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            System.err.println("Usage: TraceInspect <file.tlog> [--csv|--as-csv]");
            System.exit(2);
            return;
        }
        TlogReader reader = TlogReader.read(Path.of(args[0]));
        System.out.println("session=" + reader.metadata().sessionId());
        System.out.println("mode=" + reader.metadata().recordingMode());
        System.out.println("git=" + reader.metadata().gitCommitSha());
        System.out.println("complete=" + reader.complete());
        System.out.println("corrupt=" + reader.corruptRecords());
        System.out.println("truncatedBytes=" + reader.truncatedBytes());
        if (args.length > 1 && "--csv".equals(args[1])) {
            System.out.print(new CsvExporter().export(reader.records()));
        } else if (args.length > 1 && "--as-csv".equals(args[1])) {
            System.out.print(new CsvExporter().exportAdvantageScopeList(reader.records()));
        } else {
            HumanReadableExporter exporter = new HumanReadableExporter();
            reader.records().forEach(record -> System.out.println(exporter.format(record)));
        }
    }
}
