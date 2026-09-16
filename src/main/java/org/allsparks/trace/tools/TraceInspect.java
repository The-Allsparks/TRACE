package org.allsparks.trace.tools;

import java.nio.file.Path;
import org.allsparks.trace.export.CsvExporter;
import org.allsparks.trace.export.HumanReadableExporter;
import org.allsparks.trace.export.WpiLogExporter;
import org.allsparks.trace.storage.TlogReader;

/**
 * Desktop inspection utility for TRACE {@code .tlog} files. Can also convert
 * a recording to WPILOG for AdvantageScope.
 */
public final class TraceInspect {
    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            System.err.println("Usage: TraceInspect <file.tlog> [--csv|--as-csv|--wpilog [out.wpilog]]");
            System.exit(2);
            return;
        }
        Path input = Path.of(args[0]);
        TlogReader reader = TlogReader.read(input);
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
        } else         if (args.length > 1 && "--wpilog".equals(args[1])) {
            Path output;
            if (args.length > 2) {
                output = Path.of(args[2]);
            } else {
                output = WpiLogExporter.defaultOutput(input);
            }
            new WpiLogExporter().export(output, reader.metadata(), reader.records());
            System.out.println("wpilog=" + output.toAbsolutePath());
            System.out.println("records=" + reader.records().size());
        } else {
            HumanReadableExporter exporter = new HumanReadableExporter();
            reader.records().forEach(record -> System.out.println(exporter.format(record)));
        }
    }
}
