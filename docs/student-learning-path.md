# Student learning path

TRACE is a ladder. Do not skip rungs.

```text
Observe → Record → Explain → Correlate → Reconstruct → Replay → Test → Predict
```

Each phase is useful alone, optional, reversible, and documented. Basic use must not require the full framework.

## Phase 0 — Foundation

**Learn:** the difference between a measurement, a decision, a command, and an event.

**Do:** read [data-model.md](data-model.md). Run desktop tests. BumbleBee configures `TraceMode.ESSENTIAL` by default; set `TraceMode.OFF` only to disable recording.

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

**Learn:** SHIFT (or AMPER) got a TRACE adapter at INIT, or it got NOOP. `loop()` only calls `update()` / `observe()`. TRACE never reads Hub hardware.

**Do:** implement the library's sink in TeamCode. Call `wouldAccept` before building snapshots. Wrap with `FailOpen`. Mentors still approve competition-default channels.

TRACE still must not command hardware. See [integrations.md](integrations.md) and [ADR 0012](adr/0012-sibling-sinks.md).

## Phase 5 — AdvantageScope

**Learn:** turn recordings into evidence (graphs, 2D pose, tables). TRACE will not ship a competing dashboard.

**Do:** pull the `.tlog` off the Hub, then convert on a laptop:

```powershell
java -jar build\libs\trace-0.1.0-SNAPSHOT.jar session.tlog --wpilog
```

Open `session.wpilog` in AdvantageScope. Pose is a `double[]` plus `/x` `/y` `/headingRad`.

Optional shop live view: add `trace-advantagescope`, set `advantageScopeStreaming(true)`, then in AdvantageScope choose **File → Connect to Robot → FTC Dashboard**. That label is the protocol name; FTC Dashboard does not need to be installed. Live may drop frames. `.tlog` is still the record.

**Checkpoint:** open a converted log and point to one TRACE signal by name. If live is on, say out loud that live is expendable.

## Phase 6 — IO boundaries (approval gate)

**Learn:** the boundary between the physical robot and decision logic.

## Phase 7 — Deterministic replay

**Learn:** change an algorithm and test what it would have done in a recorded match. Physical outputs stay isolated.

## Phase 8 — Regression and fault injection

**Learn:** prove safety behavior still holds as code changes.

## Enablement rule

A student should be able to explain the current phase out loud before anyone enables the next one on a robot.
