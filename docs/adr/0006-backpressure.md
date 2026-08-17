# ADR 0006: Backpressure policy

## Context

Queues and disks fill. Silent loss is worse than visible loss.

## Decision

Bounded queues. If full, evict lower-priority retained records when the incoming record outranks them; otherwise drop incoming. Always increment `DroppedRecordStats` with a `DropReason`. Emit DROP records when the writer can. Never fsync per record.

## Alternatives considered

* Block the OpMode — can miss the rest of the match.
* Unbounded queues — Android OOM.
* Drop VERBOSE only without counters — hidden behavior.

## Consequences

FULL mode on a slow SD path will drop VERBOSE first. Health must be shown on DS telemetry.

## Student impact

Phase 3 checkpoint: explain a drop counter.

## Revisit conditions

If CRITICAL events are observed dropped on a Control Hub; then increase reserved CRITICAL slots.
