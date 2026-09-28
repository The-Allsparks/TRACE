package org.allsparks.trace.session;

import org.allsparks.trace.ftc.FtcTelemetryAdapter;

/**
 * Live-only Driver Station packing. Several TRACE channels share a group name;
 * {@link #flush} sends one {@code key = a / b / c} line in add order. A
 * {@link FtcTelemetryAdapter#LINE} group is an unlabeled {@code addLine}.
 * Not written to {@code .tlog}. Fail-open when full.
 */
final class DsGroupBuffer {
    static final int MAX_GROUPS = 24;
    static final int MAX_COLUMNS = 8;
    static final int MAX_LINES = 16;
    static final String SEPARATOR = " / ";

    private final String[] captions = new String[MAX_GROUPS];
    private final String[][] values = new String[MAX_GROUPS][MAX_COLUMNS];
    private final int[] columnCount = new int[MAX_GROUPS];
    private final String[] lines = new String[MAX_LINES];
    private final StringBuilder pack = new StringBuilder(64);
    private int groupCount;
    private int lineCount;

    /**
     * Append one value to a group. Empty caption is ignored. Extra columns
     * past {@link #MAX_COLUMNS} or groups past {@link #MAX_GROUPS} are dropped.
     * {@link FtcTelemetryAdapter#LINE} is one unlabeled DS line, not a caption.
     */
    void offer(String caption, String rendered) {
        if (caption == null || caption.isEmpty()) {
            return;
        }
        if (FtcTelemetryAdapter.LINE.equals(caption)) {
            offerLine(rendered);
            return;
        }
        int group = indexOf(caption);
        if (group < 0) {
            if (groupCount >= MAX_GROUPS) {
                return;
            }
            group = groupCount++;
            captions[group] = caption;
            columnCount[group] = 0;
        }
        int column = columnCount[group];
        if (column >= MAX_COLUMNS) {
            return;
        }
        values[group][column] = rendered == null ? "" : rendered;
        columnCount[group] = column + 1;
    }

    /**
     * Pack lines then groups in add order and publish. Clears afterwards so a
     * second flush in the same cycle is a no-op.
     */
    void flush(FtcTelemetryAdapter adapter) {
        if (adapter == null || (groupCount == 0 && lineCount == 0)) {
            clear();
            return;
        }
        for (int i = 0; i < lineCount; i++) {
            adapter.publishLine(lines[i]);
        }
        for (int g = 0; g < groupCount; g++) {
            int columns = columnCount[g];
            if (columns <= 0) {
                continue;
            }
            pack.setLength(0);
            for (int column = 0; column < columns; column++) {
                if (column > 0) {
                    pack.append(SEPARATOR);
                }
                pack.append(values[g][column]);
            }
            adapter.publish(captions[g], pack.toString());
        }
        clear();
    }

    void clear() {
        groupCount = 0;
        lineCount = 0;
    }

    private void offerLine(String rendered) {
        if (lineCount >= MAX_LINES) {
            return;
        }
        lines[lineCount++] = rendered == null ? "" : rendered;
    }

    private int indexOf(String caption) {
        for (int i = 0; i < groupCount; i++) {
            if (caption.equals(captions[i])) {
                return i;
            }
        }
        return -1;
    }
}
