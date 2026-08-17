# ADR 0003: Clock and cycle model

## Context

Replay needs one timeline. FTC loops are not FPGA-timestamped like WPILib DataLog on a roboRIO.

## Decision

* One `TraceClock` (monotonic nanos; optional wall millis)
* One monotonically increasing cycle number per session
* `TraceCycle` marks loop start/end and can emit loop duration / overrun events
* Tests inject `ManualClock`

## Alternatives considered

* Wall clock only — jumps and is not monotonic.
* Per-thread clocks — splits the story.
* Implicit cycle from timestamps — harder to teach and to join vision frames.

## Consequences

Worker threads must not advance the cycle. Async data is quality `ASYNC` or waits for Phase 6 snapshots.

## Student impact

`try (TraceCycle cycle = Trace.beginCycle())` is the advanced onramp; beginners can ignore it.

## Revisit conditions

FTC SystemCore or future control systems providing a shared FPGA-like timestamp.
