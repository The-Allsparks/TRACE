# TRACE

**Telemetry, Recording, Analysis, and Control Events**

TRACE is an FTC-first robot flight recorder and observability framework. It records what the robot sensed, what its software believed, what it decided, what it commanded, and what safety or control events occurred.

It is designed for gradual student adoption. A team can get useful results from basic event logging without restructuring its robot. Later phases add structured snapshots, visualization exports, deterministic replay, regression tests, and fault injection.

---

## Built by The Allsparks

TRACE is created and maintained by **[The Allsparks](https://github.com/The-Allsparks)** (FTC Team **#36117**).

Repository: **[The-Allsparks/TRACE](https://github.com/The-Allsparks/TRACE)**

> **Disclaimer:** TRACE is community-developed and unofficial. It is **not** affiliated with or endorsed by FIRST, REV Robotics, CTRE, NI, Mechanical Advantage, ACME Robotics, or other referenced vendors. Teams must verify legality and performance against the current-season FTC Game Manual.

---

## What TRACE is

* A passive recorder of **inputs**, **outputs**, and **events**
* A student learning path from Observe → Record → Explain → Correlate → Reconstruct → Replay → Test
* A Control Hub-aware design: bounded memory, async writes, quotas, drop accounting
* A schema and session-metadata layer that other Allsparks projects can emit into

## What TRACE is not

* Not a vision system ([ViDAR](https://github.com/The-Allsparks/ViDAR) owns perception)
* Not a path follower (Pedro Pathing owns localization and chassis motion)
* Not a power manager ([AMPER](https://github.com/The-Allsparks/AMPER) owns electrical demand)
* Not a mechanism safety framework ([MIMIC](https://github.com/The-Allsparks/MIMIC) owns mechanism lifecycle)
* Not a communications health owner ([BEACON](https://github.com/The-Allsparks/BEACON) owns DS/RC link observation)
* Not a dashboard. Visualization should reuse [AdvantageScope](https://docs.advantagescope.org/)
* Not AdvantageKit, PsiKit, WPILib DataLog, or Road Runner. See [build-versus-adopt](docs/research/build-vs-adopt.md)

TRACE may provide shared clocks, schemas, sinks, and adapters. It must not become the decision-making owner for those systems.

---

## Current status

| Item | Status |
|------|--------|
| **Version** | `0.1.0-SNAPSHOT` |
| **Implemented phases** | **Phase 0** foundation, **Phase 1** event recorder, **Phase 2** essential telemetry, **Phase 3** match flight recorder (desktop-validated) |
| **Phases 4–5** | Designed; behind the **integration approval gate** |
| **Phases 6–8** | Designed; behind the **replay approval gate** |
| **Physical outputs** | **Never commanded.** TRACE is observational in Phases 0–5 |
| **Hardware validation** | **Not performed** on a Control Hub or robot |

**No phase should be enabled in competition without testing on your robot.**

Supported targets for this scaffold:

* **FTC SDK:** current public [FtcRobotController](https://github.com/FIRST-Tech-Challenge/FtcRobotController) season releases (Java TeamCode integration sketches; this library compiles without the SDK on the classpath)
* **Hardware:** REV Control Hub file storage and OpMode lifecycle, accessed through team-owned adapters
* **Library build:** Java 11 source/target; CI uses Temurin 17 to compile and test

### Current limitations

* Phase 0–3 provide vocabulary, events, typed signals, CSV export, and a compact `.tlog` writer. They do **not** replay robot code or suppress hardware outputs, because replay is not implemented.
* Selecting `TraceMode.REPLAY` fails closed.
* AdvantageScope-native WPILOG/RLOG/Road Runner writers are Phase 5 work. CSV list export is available now as a lossy interchange format.
* Per-cycle allocation is not yet object-pooled. Desktop smoke tests exist; Control Hub timing is unmeasured.
* Git metadata collection degrades to `unknown` when Git is unavailable, which is expected on a Control Hub.

---

## Why an FTC team would use it

FTC robots fail in ways that are hard to reconstruct from Driver Station telemetry alone: a brownout, a missed target, a loop overrun, a stale vision observation, a mechanism that moved when it should not have. TRACE keeps an ordered evidence trail so students can explain *what happened* before they try to make the robot smarter.

---

## Quick start (desktop)

```powershell
git clone https://github.com/The-Allsparks/TRACE.git
cd TRACE
.\gradlew.bat test
```

On Linux/macOS:

```bash
./gradlew test
```

Student onramp:

```java
Trace.configure(TraceConfig.builder().mode(TraceMode.EVENTS).memorySink(true).build());
Trace.event("Autonomous started");
Trace.record("Battery/Voltage", voltage, Units.VOLTS);
Trace.record("Drive/Pose", pose);
```

Structured cycle API:

```java
try (TraceCycle cycle = Trace.beginCycle()) {
    cycle.recordInput("Drive", driveInputs);
    cycle.recordInput("Power", powerInputs);
    cycle.recordOutput("Drive/Command", command);
}
```

---

## Platform support and runtime cost

| Environment | Support |
|-------------|---------|
| Desktop JVM unit tests | Implemented and CI-tested |
| Android / FTC SDK compile | Adapter sketches only; SDK is not on this library's compile classpath |
| Control Hub | Designed for; **not hardware-tested** |
| Robot / match | Not tested |

`TraceMode.OFF` is the default: no recording and negligible work. Event and essential modes keep work in the calling thread except for optional async file writes. Do not flush storage after every record.

---

## Data ownership and privacy

* Logs belong to the team that recorded them.
* Do not store raw camera video by default.
* Do not record student names, emails, Wi-Fi passwords, tokens, or secrets.
* Filenames are sanitized.
* Video sync should reference an external recording, not embed video in the robot log.
* See [SECURITY.md](SECURITY.md) and [docs/storage.md](docs/storage.md).

---

## Relationship to other logging tools

| Tool | TRACE relationship |
|------|-------------------|
| **AdvantageKit** | FRC logging/replay model we learned from. Not used at runtime. |
| **AdvantageScope** | Preferred visualizer. Phase 5 will export or convert; TRACE will not ship a competing dashboard. |
| **PsiKit** | Viable FTC AdvantageKit port. Evaluated and **not adopted as the core**. Future adapter possible. |
| **Road Runner Flight Recorder** | Mature FTC `.log` format that AdvantageScope already reads. TRACE does not wrap it yet. |
| **FTC Dashboard** | Live telemetry transport. Optional adapter interface only. |
| **WPILib DataLog / Epilogue / DogLog** | FRC-only or WPILib-backed. Not runnable on the Control Hub as-is. |
| **Official FTC Datalogger** | CSV teaching sample. Inspired Phase 2 educational CSV, not the canonical format. |

Details: [docs/research/ecosystem-review.md](docs/research/ecosystem-review.md), [docs/research/build-vs-adopt.md](docs/research/build-vs-adopt.md).

---

## Relationship to Allsparks projects

TRACE records evidence produced by ViDAR, Pedro Pathing, AMPER, MIMIC, and BEACON. Those projects must depend only on a small TRACE API if they integrate. TRACE does not require them to be installed. Adapter work is **Phase 4** and needs an explicit approval gate.

---

## Project maturity

This is an initial public scaffold. Phases 0–3 are implemented as desktop-tested vertical slices. They are not match-proven. Passive recording must be proven on a Control Hub before TRACE is permitted to influence robot behavior, and Phases 0–5 never command outputs.

---

## Documentation

| Doc | Purpose |
|-----|---------|
| [Student learning path](docs/student-learning-path.md) | Observe → improve, with checkpoints |
| [Mentor guide](docs/mentor-guide.md) | How to teach TRACE without boiling the ocean |
| [Architecture](docs/architecture.md) | Module boundaries |
| [Data model](docs/data-model.md) | Inputs, outputs, events |
| [Schema](docs/schema.md) | Record layout and naming |
| [Performance](docs/performance.md) | Control Hub constraints |
| [Storage](docs/storage.md) | `.tlog`, quotas, truncation |
| [Replay](docs/replay.md) | Future replay isolation |
| [Integrations](docs/integrations.md) | ViDAR / Pedro / AMPER / MIMIC / BEACON |
| [Troubleshooting](docs/troubleshooting.md) | Common failures |
| [Research](docs/research/ecosystem-review.md) | Source-backed ecosystem review |
| [ADRs](docs/adr/README.md) | Architecture decisions |
| [Examples](examples/README.md) | Independent onramps |

---

## License

MIT — same open-source license family as [AMPER](https://github.com/The-Allsparks/AMPER) and [ViDAR](https://github.com/The-Allsparks/ViDAR). See [LICENSE](LICENSE).

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md), [CODE_OF_CONDUCT.md](CODE_OF_CONDUCT.md), and [SECURITY.md](SECURITY.md).
