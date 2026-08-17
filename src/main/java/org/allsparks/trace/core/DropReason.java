package org.allsparks.trace.core;

/**
 * Why a record was not retained. TRACE never silently discards data: drop
 * counts, categories, and reasons are always exposed through health and DROP
 * records.
 */
public enum DropReason {
    QUEUE_FULL,
    LOWER_PRIORITY_EVICTED,
    STORAGE_EXHAUSTED,
    FILE_SIZE_LIMIT,
    MODE_FILTERED,
    SAMPLE_SKIPPED,
    WRITER_FAILED,
    SHUTDOWN,
    INVALID_RECORD
}
