# Architecture

TRACE is a **small multi-project Gradle build**. Students still consume `org.allsparks:trace`. Optional live AdvantageScope delivery is a second artifact, `org.allsparks:trace-advantagescope`, loaded only when configured.

Future published splits (`trace-api`, `trace-storage`, `trace-replay`) remain possible. They are not created now because empty modules would not preserve a clearer boundary than packages.

## Package map

| Package | Responsibility |
|---------|----------------|
| `org.allsparks.trace` | Student facade (`Trace`, `TraceConfig`, `TraceMode`) |
| `org.allsparks.trace.core` | Categories, priorities, units, typed values, records |
| `org.allsparks.trace.clock` | Monotonic clock abstraction |
| `org.allsparks.trace.session` | Session lifecycle, metadata, health, cycles |
| `org.allsparks.trace.sink` | No-op, console, bounded memory, composite |
| `org.allsparks.trace.storage` | Async writer, `.tlog` codec, rotation, quotas |
| `org.allsparks.trace.export` | Human-readable, CSV, and WPILOG interchange |
| `org.allsparks.trace.policy` | Sampling / downsampling / change-based recording |
| `org.allsparks.trace.ftc` | SDK-free OpMode and telemetry adapter interfaces |
| `org.allsparks.trace.live` | Optional AdvantageScope load hook (`Class.forName`); no WebSocket in core |
| `org.allsparks.trace.tools` | Desktop `TraceInspect` |

## Runtime flow

```text
OpMode loop
    → Trace.beginCycle()
    → recordInput / record / event
    → SamplingPolicy
    → CompositeSink
         ├─ BoundedMemorySink (classroom + tests)
         ├─ ConsoleSink (optional)
         └─ AsyncBoundedWriter (file I/O on a dedicated thread)
    → optional LatestValueMailbox (map put + seq only)
         └─ trace-advantagescope worker (JSON/WebSocket, only if a client is connected)
    → TraceCycle.close() records loop duration / overrun
```

The control loop must not perform blocking file writes. Enqueueing a record is mutex + copy; the writer thread batches bytes. The writer opens files with `java.io.File` so Control Hub Android 7 does not need `File.toPath()`.

## Modes

`OFF`, `EVENTS`, `ESSENTIAL`, `FULL` are implemented. `REPLAY` throws at configuration time (fail closed). No mode activates because ViDAR, Pedro, AMPER, MIMIC, or BEACON is present.

## What this library will not do in Phases 0–5

* Command motors, servos, or mechanisms
* Replace hardware reads during replay
* Depend on FTC SDK, FTC Dashboard UI, AdvantageScope, or PsiKit at compile time in `org.allsparks:trace`. The optional `trace-advantagescope` module may depend on NanoWSD.
* Store raw camera frames

## Approval gates

* **Integration gate** before Phase 4 adapters
* **Replay gate** before Phase 6 IO refactoring

Passive recording must be Control Hub proven before TRACE may influence robot behavior.
