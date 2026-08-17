# ADR 0002: Canonical storage format

## Context

AdvantageScope already understands WPILOG, RLOG, Road Runner `.log`, and CSV. WPILib documents why binary logs plus a dedicated I/O thread matter.

## Decision

Canonical robot format is **TLOG1 (`.tlog`)**: length-prefixed, CRC-protected records with TRACE schema fields. CSV is educational interchange only.

## Alternatives considered

* WPILOG now — would pull WPILib or a reimplementation before Phase 5; Android/native risk unknown.
* RLOG now — couples to PsiKit/AdvantageKit 2022 legacy format.
* Road Runner `.log` now — channel/schema model is RR-centric.
* CSV canonical — text-only, no CRC, poor Control Hub density (FTC Datalogger lesson).

## Consequences

Desktop `TraceInspect` and `TlogReader` ship in Phase 3. AdvantageScope-native writers wait for Phase 5.

## Student impact

Students can still export CSV for spreadsheets immediately.

## Revisit conditions

If an upstream format gains TRACE category/priority/quality fields without extra dependencies.
