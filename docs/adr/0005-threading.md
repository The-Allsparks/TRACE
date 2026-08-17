# ADR 0005: Threading model

## Context

WPILib DataLog performs file I/O off the robot thread. FTC vision often runs asynchronously. Uncontrolled logging from worker threads breaks determinism.

## Decision

* Control loop may call `Trace` APIs.
* File I/O runs on `trace-writer`.
* The loop only enqueues.
* No hardware reads inside TRACE.
* Replay (future) uses a frozen clock and recorded inputs only.

## Alternatives considered

* Sync writes — Control Hub stalls.
* Disruptor-style wait-free queues — too much Phase 0 complexity.
* Allow any thread to advance cycles — nondeterministic.

## Consequences

Camera threads must hand results into the cycle snapshot (Phase 6). Until then, treat off-thread records as suspect.

## Student impact

Students keep calling TRACE from `loop` / `runOpMode`.

## Revisit conditions

Measured Control Hub contention requiring a different handoff.
