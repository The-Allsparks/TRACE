# Build versus adopt

Decision date: **2026-08-17**. See also [ADR 0001](../adr/0001-build-versus-adopt.md).

## Question

Should TRACE adopt PsiKit (or another FTC logger) as its core, contribute upstream, build independently, or use a hybrid of replaceable sinks?

## Candidates

### 1. PsiKit

| Criterion | Finding | Evidence |
|-----------|---------|----------|
| Maintenance | Active in 2026; last push 2026-07-07 | GitHub repo metadata |
| License | AdvantageKit BSD-style; no AdvantageKit name endorsement | `LICENSE` in PsiLynx/PsiKit |
| Platforms | FTC Android + desktop replay notes | `docs/replay.md`, `ftc/` module |
| FTC SDK | compileOnly / compileSdk 30; Java 8 downgrade commit | changelog, 2026-07-07 commit |
| Control Hub | `.rlog` under `/storage/emulated/0/FIRST/PsiKit` | replay.md |
| AdvantageScope | RLOG live/file path | replay.md, AdvantageScope formats |
| Live telemetry | AdvantageScope via RLOG server (homepage feature list) | homepage; verify per release |
| On-robot files | Implemented in source (`RLOGWriter`) despite homepage “coming soon” | source vs homepage mismatch |
| Replay | Documented RLOG replay; wrappers required | replay.md, `LogReplaySource` |
| Student onramp | AdvantageKit IO/wrapper model; autolog now opt-in | changelog 0.1.0-beta2 |
| Allsparks schema/priorities/quotas | Not the product | inference |
| Upstream contribution | PRs welcomed | homepage |

**Inference:** PsiKit is the only serious FTC replay stack. It is not a gradual `Trace.event("...")` teaching API, and TRACE would inherit AdvantageKit architecture, Kotlin wrappers, and a still-beta surface. Contributing TRACE’s educational facade upstream is unlikely to land as PsiKit’s identity.

### 2. Road Runner FlightRecorder / FateWeaver

AdvantageScope already reads RR `.log`. FateWeaver adds typed channels. Neither provides TRACE’s input/output/event vocabulary, drop accounting, or Allsparks-neutral API. Adopting RR format as *core* couples TRACE to RR schemas.

### 3. Official FTC Datalogger

CSV teaching tool. Blocking or simplistic file I/O relative to WPILib’s documented separate-thread design. Wrong default directory (OnBot source tree).

### 4. AdvantageKit / DataLog / DogLog / Epilogue

FRC/WPILib. Not Control Hub runtimes.

### 5. Independent TRACE core

Cost: we maintain a codec. Benefit: student API, Control Hub policy, no circular deps, MIT license matching AMPER/MIMIC/ViDAR, fail-closed replay until approved.

## Chosen strategy

**Hybrid (option 4) with a narrow independent core (option 3):**

* TRACE owns the student API, schema, session metadata, priorities, and Control Hub writer.
* Visualization should reuse **AdvantageScope** via future converters (WPILOG, RLOG, Road Runner, or CSV).
* PsiKit is **not** a compile dependency. A future `trace-psikit` sink/source is allowed after the integration gate if PsiKit remains maintained.
* Upstream PsiKit contributions are welcome for FTC-wide replay, but TRACE’s onramp will not wait on them.

## Why not adopt PsiKit now

1. TRACE must be useful from `Trace.event` without hardware wrappers.
2. Homepage/source documentation disagreement means API stability is not yet a safe foundation.
3. Kotlin+wrapper architecture is a large student jump.
4. License branding constraints around AdvantageKit names.
5. TRACE must not pull ViDAR/Pedro/AMPER/MIMIC/BEACON, and those must not need PsiKit to log.

## Revisit if

* PsiKit ships a stable Java-8/11 MIT-or-BSD file+replay API with a no-wrapper event logger, or
* AdvantageScope adds a documented extension that makes TLOG native, or
* Maintainers agree to make TRACE a thin schema layer over RLOG.
