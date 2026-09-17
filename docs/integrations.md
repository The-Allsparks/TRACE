# Integrations

TRACE records evidence. It does not absorb other projects.

| Project | Owns | TRACE may record | TRACE must not |
|---------|------|------------------|----------------|
| [ViDAR](https://github.com/The-Allsparks/ViDAR) | What the robot sees | Frame age, target count, fused poses | Run detectors or store raw video by default |
| Pedro Pathing | Localization and path following | Pose, path id, follower errors | Compute wheel powers |
| [AMPER](https://github.com/The-Allsparks/AMPER) | Electrical demand | Voltage, current, intervention flags | Allocate motor power |
| [MIMIC](https://github.com/The-Allsparks/MIMIC) | Mechanism lifecycle | Positions, goals, interlocks | Home, limit, or command actuators |
| [BEACON](https://github.com/The-Allsparks/BEACON) | Communication health | DS health, safe-state flags | Decide comms-loss responses |

## allsparks-contracts

TRACE depends on [`allsparks-contracts`](https://github.com/The-Allsparks/allsparks-contracts) `0.1.0-rc.1` for shared `MonotonicClock`, `Validity`, and `HealthSeverity` envelopes. TRACE remains independently adoptable: that JAR does not pull in HELM, AMPER, or MIMIC.

`TraceClock` extends `MonotonicClock` (`nowNanos()` delegates to `nanoTime()`). Wall-clock millis stay TRACE-local. `TraceQuality`, `TraceSeverity`, `TraceClock`, and `TraceRecord` are still TRACE recording types. Edge mappings live in `org.allsparks.trace.contracts.TraceMappings`. `ESTIMATED` and `ASYNC` stay TRACE recording labels and do not map to a `Validity`.

Students without GitHub Packages credentials should keep a sibling `allsparks-contracts` checkout. `settings.gradle` `includeBuild`s it when present.

## Dependency rule

Other functional projects may depend on TRACE. TRACE must not depend on them (HELM, AMPER, MIMIC, ViDAR, BEACON, Pedro). Adapters live behind the Phase 4 approval gate and should be optional artifacts or team-side glue. `allsparks-contracts` is the shared envelope JAR, not a functional library.

## Recommended signal names

See [schema.md](schema.md). Each future adapter issue must list:

* Recorded inputs / outputs / events
* Recommended sampling
* Estimated data rate
* Privacy or storage concerns
* Minimum compatible project version

## Current adapter surface

`FtcTelemetryAdapter`, `DashboardTelemetryAdapter`, and `OpModeLifecycle` compile without the FTC SDK. Teams wrap `telemetry.addData` themselves. Live AdvantageScope is the optional `trace-advantagescope` module, not `FtcTelemetryAdapter`.
