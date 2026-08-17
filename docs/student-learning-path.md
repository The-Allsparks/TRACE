# Student learning path

TRACE is a ladder. Do not skip rungs.

```text
Observe → Record → Explain → Correlate → Reconstruct → Replay → Test → Predict
```

Each phase is useful alone, optional, reversible, and documented. Basic use must not require the full framework.

## Phase 0 — Foundation

**Learn:** the difference between a measurement, a decision, a command, and an event.

**Do:** read [data-model.md](data-model.md). Run desktop tests. Keep `TraceMode.OFF` on the robot.

**Checkpoint:** explain those four words to a teammate without using TRACE class names.

## Phase 1 — Event recorder

**Learn:** reconstruct what happened and in what order.

**Do:**

```java
Trace.configure(TraceConfig.builder().mode(TraceMode.EVENTS).memorySink(true).build());
Trace.event("Autonomous started");
```

**Checkpoint:** given a human-readable export, retell the match story in order.

## Phase 2 — Essential telemetry

**Learn:** graph a value over time and correlate it with behavior.

**Do:** `Trace.record("Battery/Voltage", voltage, Units.VOLTS);` then export CSV.

**Checkpoint:** point to a CSV row and say whether it is an input, output, or event.

## Phase 3 — Match flight recorder

**Learn:** retrieve a complete recording without blocking the control loop on disk I/O.

**Do:** enable `fileSink` with a bounded directory. Inspect with `TraceInspect`.

**Checkpoint:** explain what happens if the queue is full and why TRACE still reports drops.

## Phase 4 — Project adapters (approval gate)

**Learn:** see how ViDAR, Pedro, AMPER, MIMIC, and BEACON contributed to one behavior.

**Do not start** until mentors approve. TRACE still must not command hardware.

## Phase 5 — AdvantageScope

**Learn:** turn recordings into evidence (graphs, 2D pose, tables). TRACE will not ship a competing dashboard.

## Phase 6 — IO boundaries (approval gate)

**Learn:** the boundary between the physical robot and decision logic.

## Phase 7 — Deterministic replay

**Learn:** change an algorithm and test what it would have done in a recorded match. Physical outputs stay isolated.

## Phase 8 — Regression and fault injection

**Learn:** prove safety behavior still holds as code changes.

## Enablement rule

A student should be able to explain the current phase out loud before anyone enables the next one on a robot.
