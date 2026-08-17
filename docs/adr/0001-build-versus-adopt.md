# ADR 0001: Build versus adopt

## Context

FTC already has PsiKit, Road Runner Flight Recorder, FateWeaver, FTC Dashboard, and the official CSV Datalogger. FRC has AdvantageKit, DataLog, Epilogue, and DogLog. TRACE must not duplicate a maintained project without cause.

## Decision

Build a **narrow independent TRACE core** with **replaceable sinks/exports**. Do not adopt PsiKit or WPILib DataLog as the runtime. Prefer AdvantageScope for visualization later.

## Alternatives considered

1. Adopt PsiKit and wrap it — rejected for student onramp, beta stability, Kotlin wrappers, stale homepage vs source.
2. Contribute only upstream — TRACE still needs Allsparks schema and phased docs now.
3. Adopt Road Runner logs as canonical — couples TRACE to RR.
4. Do nothing / CSV only — fails Control Hub performance and replay future.

## Consequences

TRACE maintains a `.tlog` codec. Phase 5 must convert or write an AdvantageScope-native format. A PsiKit adapter remains possible.

## Student impact

Beginners learn `Trace.event` / `Trace.record` without IO interfaces.

## Revisit conditions

PsiKit stable no-wrapper API, or AdvantageScope native TLOG support. Details in [build-vs-adopt.md](../research/build-vs-adopt.md).
