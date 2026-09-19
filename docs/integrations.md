# Integrations

TRACE records evidence. It does not absorb other projects.

| Project | Owns | TRACE may record | TRACE must not |
|---------|------|------------------|----------------|
| [ViDAR](https://github.com/The-Allsparks/ViDAR) | What the robot sees | Frame age, target count, fused poses | Run detectors or store raw video by default |
| Pedro Pathing | Localization and path following | Pose, path id, follower errors | Compute wheel powers |
| [AMPER](https://github.com/The-Allsparks/AMPER) | Electrical demand | Voltage, current, intervention flags | Allocate motor power |
| [MIMIC](https://github.com/The-Allsparks/MIMIC) | Mechanism lifecycle | Positions, goals, interlocks | Home, limit, or command actuators |
| [BEACON](https://github.com/The-Allsparks/BEACON) | Communication health | DS health, safe-state flags | Decide comms-loss responses |
| [SHIFT](https://github.com/The-Allsparks/SHIFT) | Stick to named intent | Stick samples, profile events | Command motors |

## allsparks-contracts

TRACE depends on [`allsparks-contracts`](https://github.com/The-Allsparks/allsparks-contracts) `0.1.0-rc.1` for shared `MonotonicClock`, `Validity`, and `HealthSeverity` envelopes. TRACE remains independently adoptable: that JAR does not pull in HELM, AMPER, or MIMIC.

`TraceClock` extends `MonotonicClock` (`nowNanos()` delegates to `nanoTime()`). Wall-clock millis stay TRACE-local. `TraceQuality`, `TraceSeverity`, `TraceClock`, and `TraceRecord` are still TRACE recording types. Edge mappings live in `org.allsparks.trace.contracts.TraceMappings`. `ESTIMATED` and `ASYNC` stay TRACE recording labels and do not map to a `Validity`.

Students without GitHub Packages credentials should keep a sibling `allsparks-contracts` checkout. `settings.gradle` `includeBuild`s it when present.

## Dependency rule

Functional libraries must **not** depend on TRACE. TRACE must not depend on them (HELM, AMPER, MIMIC, ViDAR, BEACON, Pedro, SHIFT). Each library exposes a local sink (NOOP default). TeamCode implements that sink with TRACE at INIT. Optional `*-trace-adapter` modules (example: `trace-mimic-adapter`) are allowed when the mapping is large. `allsparks-contracts` is the shared envelope JAR, not a functional library. See [ADR 0012](adr/0012-sibling-sinks.md).

## Sink cookbook

1. Library-owned sink + NOOP. Skip snapshot allocation when the sink is NOOP.
2. Two emit kinds: **signals** (every-cycle numbers, `TracePriority`) and **events** (`TraceSeverity`). `DEBUG` is retention, not severity.
3. TeamCode constructs the TRACE adapter next to `TeamTrace.tryConfigure`. If TRACE is unavailable, leave NOOP.
4. Call `Trace.wouldAccept(name, category, priority)` before HashMaps or `String.format`.
5. Wrap adapter work with `org.allsparks.trace.adapter.FailOpen` so a TRACE typo cannot stop drive.
6. Wrap the OpMode loop in `Trace.beginCycle()` so adapter records inherit the cycle id.
7. Check `Trace.integrationEnabled("AMPER")` (or SHIFT, PULSE, ...) so `enableIntegration` is the switch.

Reference: [SHIFT integrations](https://github.com/The-Allsparks/SHIFT/blob/main/docs/integrations.md) and TeamCode `TraceShiftAdapter` wired on `FtcShift.builder().eventSink(...).inputListener(...)`.

## Recommended signal names

See [schema.md](schema.md). Each adapter should list:

* Recorded inputs / outputs / events
* Recommended sampling
* Estimated data rate
* Privacy or storage concerns
* Minimum compatible project version

## Current adapter surface

`FtcTelemetryAdapter`, `DashboardTelemetryAdapter`, and `OpModeLifecycle` compile without the FTC SDK. Teams wrap `telemetry.addData` themselves. Live AdvantageScope is the optional `trace-advantagescope` module, not `FtcTelemetryAdapter`. `FailOpen` and `wouldAccept` are the host API for sibling sinks. Draft design review: [docs/issues/phase4-sink-host.md](issues/phase4-sink-host.md).
