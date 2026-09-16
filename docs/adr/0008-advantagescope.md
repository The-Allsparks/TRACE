# ADR 0008: AdvantageScope interoperability

## Context

AdvantageScope is the de facto FIRST analysis tool and already speaks FTC Dashboard and Road Runner logs. TRACE must not ship a competing dashboard.

## Decision

Phase 2/3: TRACE CSV and AdvantageScope list CSV (lossy). Phase 5 converter: desktop `WpiLogExporter` writes WPILOG 1.0 from `.tlog` / in-memory records **without** a WPILib dependency. The Hub still writes `.tlog`. Phase 5 live remainder: optional `trace-advantagescope` module streams latest values to AdvantageScope using a minimal FTC Dashboard-compatible WebSocket (see [ADR 0011](0011-advantagescope-live-protocol.md)). RLOG and Road Runner `.log` remain deferred.

## Alternatives considered

* Custom TRACE GUI — rejected.
* Require PsiKit for visualization — rejected for the core.
* WPILOG-only now — delayed Phase 3.

## Consequences

Students graph in AdvantageScope by converting `.tlog` → `.wpilog` on a laptop, or by enabling optional live streaming in the shop. Spreadsheets and `--as-csv` remain available. On-robot format is still TLOG1. Live is not the match record.

## Student impact

They still get charts in Phase 2.

## Revisit conditions

AdvantageScope adds a documented TLOG reader.
