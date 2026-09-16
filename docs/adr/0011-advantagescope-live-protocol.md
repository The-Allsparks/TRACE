# ADR 0011: Minimal AdvantageScope live protocol

## Context

Students still have to pull a `.tlog`, convert it, and open AdvantageScope after the fact. AdvantageScope already has an experimental **FTC Dashboard** live source that talks to `ws://<robot>:8000`. TRACE must not become a second dashboard, must not put JSON or networking on the OpMode thread, and must not replace `.tlog`.

## Decision

Ship an optional module `org.allsparks:trace-advantagescope` that:

1. Listens with NanoWSD on port 8000 (configurable).
2. Answers `GET_ROBOT_STATUS` with `RECEIVE_ROBOT_STATUS`.
3. Pushes latest-value `RECEIVE_TELEMETRY` packets.
4. Is enabled only when `TraceConfig.advantageScopeStreaming(true)` **and** the module is on the classpath (`Class.forName`).

**Option A (reuse DashboardCore) is rejected.** It brings a telemetry FIFO, extra threads, Gson config/canvas, and Dashboard UI/OpMode/gamepad/camera. That is a second dashboard.

**Option B (minimal compatible server) is chosen.** Credit ACME Robotics as the protocol/reference. Do not copy Dashboard source. Do not depend on DashboardCore.

Panels stays on 8001 as the team dashboard. Live AdvantageScope is expendable. Robot control and `.tlog` are not.

## Alternatives considered

* Route TRACE → Panels → AdvantageScope — deferred. Preserve TRACE as an observable for a later Panels plugin; do not implement that plugin here.
* NT4 / native AdvantageScope protocol — extra FRC stack on the Hub.
* Reuse `FtcTelemetryAdapter` — it is synchronous on the OpMode thread.

## Consequences

TRACE becomes a small multi-project build. Artifact `org.allsparks:trace` is unchanged. A new daemon thread and listen port exist only when streaming is enabled. Competition default is off.

## Student impact

They can graph loop time, battery, and pose live during shop development, and still explain that live may drop frames.

## Revisit conditions

AdvantageScope documents a TRACE-native live protocol, or a real Hub session proves extra Dashboard messages are required beyond status + telemetry.
