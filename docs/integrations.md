# Integrations

TRACE records evidence. It does not absorb other projects.

| Project | Owns | TRACE may record | TRACE must not |
|---------|------|------------------|----------------|
| [ViDAR](https://github.com/The-Allsparks/ViDAR) | What the robot sees | Frame age, target count, fused poses | Run detectors or store raw video by default |
| Pedro Pathing | Localization and path following | Pose, path id, follower errors | Compute wheel powers |
| [AMPER](https://github.com/The-Allsparks/AMPER) | Electrical demand | Voltage, current, intervention flags | Allocate motor power |
| [MIMIC](https://github.com/The-Allsparks/MIMIC) | Mechanism lifecycle | Positions, goals, interlocks | Home, limit, or command actuators |
| [BEACON](https://github.com/The-Allsparks/BEACON) | Communication health | DS health, safe-state flags | Decide comms-loss responses |

## Dependency rule

Other projects may depend on TRACE. TRACE must not depend on them. Adapters live behind the Phase 4 approval gate and should be optional artifacts or team-side glue.

## Recommended signal names

See [schema.md](schema.md). Each future adapter issue must list:

* Recorded inputs / outputs / events
* Recommended sampling
* Estimated data rate
* Privacy or storage concerns
* Minimum compatible project version

## Current adapter surface

`FtcTelemetryAdapter`, `DashboardTelemetryAdapter`, and `OpModeLifecycle` compile without the FTC SDK. Teams wrap `telemetry.addData` or `FtcDashboard.getInstance()` themselves.
