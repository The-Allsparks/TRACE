# ADR 0012: Sibling libraries emit through local sinks

## Context

SHIFT already accepts `ShiftEventSink` / `ShiftInputListener` with NOOP defaults. TeamCode implements those with TRACE at INIT. AMPER still copied snapshots from `loop()`. Docs said sibling projects may depend on a small TRACE API. That would put TRACE types in AMPER and Pedro.

## Decision

TRACE is a sink *target*. Functional libraries keep a local sink (events) and optional per-loop listener (signals), default NOOP. TeamCode (or a later optional `*-trace-adapter` module) implements that sink using TRACE. TRACE does not import AMPER, MIMIC, HELM, ViDAR, BEACON, SHIFT, or Pedro.

`record` / `event` fail open: a bad signal name drops `INVALID_RECORD` and does not throw. Adapters peek with `wouldAccept` before allocating snapshots. `enableIntegration` is a real adapter switch via `integrationEnabled`.

Signals use `TracePriority`. Events use `TraceSeverity`. DEBUG is retention, not severity.

## Alternatives considered

* Libraries call `Trace.record` — compile edge, independent adoption ends.
* Shared contracts `Logger` — string hot path, loses schema names, collides with `HealthSeverity`.
* TRACE implements sibling sink interfaces — TRACE would depend on those libraries.

## Consequences

`loop()` stays `capture` / `update` / `observe`. INIT stitches sinks. SHIFT remains the reference.

## Student impact

Point at INIT: TRACE adapter or NOOP. A TRACE typo must not stop the wheels.

## Revisit conditions

Published adapter modules when a mapping is too large for TeamCode.
