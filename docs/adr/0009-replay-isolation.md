# ADR 0009: Replay isolation

## Context

Replay that can energize motors is a safety defect. CTRE Hoot Replay still logs, but FRC robots have different enable interlocks than FTC.

## Decision

Phases 0–5 never command outputs. `TraceMode.REPLAY` throws. Future replay must fail closed if output isolation is unverified, clearly identify replay mode, and compare original vs recalculated outputs without touching hardware.

## Alternatives considered

* Soft “please don’t enable motors” comments — insufficient.
* Immediate IO interfaces — too much adoption cost before recording is proven.

## Consequences

Deterministic replay waits for two gates: hardware-proven recording, then IO boundary work.

## Student impact

They cannot accidentally “replay” on a robot sitting on a stand with batteries connected via TRACE APIs today.

## Revisit conditions

Phase 6 approval after Control Hub recording evidence.
