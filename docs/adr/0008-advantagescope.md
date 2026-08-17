# ADR 0008: AdvantageScope interoperability

## Context

AdvantageScope is the de facto FIRST analysis tool and already speaks FTC Dashboard and Road Runner logs. TRACE must not ship a competing dashboard.

## Decision

Phase 2/3: TRACE CSV and AdvantageScope list CSV (lossy). Phase 5: narrowest practical WPILOG, RLOG, or Road Runner export or converter. Live stream via existing FTC Dashboard adapter, not a new protocol, unless RLOG streaming is later chosen.

## Alternatives considered

* Custom TRACE GUI — rejected.
* Require PsiKit for visualization — rejected for the core.
* WPILOG-only now — delayed Phase 3.

## Consequences

Until Phase 5, students graph in spreadsheets or import lossy CSV.

## Student impact

They still get charts in Phase 2.

## Revisit conditions

AdvantageScope adds a documented TLOG reader.
