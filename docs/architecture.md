# Architecture

TRACE is a **single Gradle Java library** (`org.allsparks.trace`) with internal packages instead of many published modules. This matches [AMPER](https://github.com/The-Allsparks/AMPER) and [MIMIC](https://github.com/The-Allsparks/MIMIC): students consume one artifact, while maintainers keep package boundaries.

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
| `org.allsparks.trace.export` | Human-readable and CSV interchange |
| `org.allsparks.trace.policy` | Sampling / downsampling / change-based recording |
| `org.allsparks.trace.ftc` | SDK-free OpMode and telemetry adapter interfaces |
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
    → TraceCycle.close() records loop duration / overrun
```

The control loop must not perform blocking file writes. Enqueueing a record is mutex + copy; the writer thread batches bytes.

## Modes

`OFF`, `EVENTS`, `ESSENTIAL`, `FULL` are implemented. `REPLAY` throws at configuration time (fail closed). No mode activates because ViDAR, Pedro, AMPER, MIMIC, or BEACON is present.

## What this library will not do in Phases 0–5

* Command motors, servos, or mechanisms
* Replace hardware reads during replay
* Depend on FTC SDK, FTC Dashboard, AdvantageScope, or PsiKit at compile time
* Store raw camera frames

## Approval gates

* **Integration gate** before Phase 4 adapters
* **Replay gate** before Phase 6 IO refactoring

Passive recording must be Control Hub proven before TRACE may influence robot behavior.
