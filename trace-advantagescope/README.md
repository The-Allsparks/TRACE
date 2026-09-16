# TRACE live streaming to AdvantageScope

Low-overhead live TRACE telemetry for AdvantageScope. This module speaks the FTC Dashboard-compatible WebSocket that AdvantageScope already supports. **FTC Dashboard itself is not required.** Panels remains the operational dashboard. `.tlog` remains the match record.

Public name: **TRACE live streaming to AdvantageScope**. Do not call it "FTC Dashboard streaming."

## What this is

```
                         ┌──────────────► Panels :8001
                         │                team dashboard / tuning
TeamCode ────────────────┤
                         ▼
                       TRACE (org.allsparks:trace)
                   ┌─────┴─────┐
                   │           │
                   ▼           ▼
                 .tlog   trace-advantagescope
              permanent     latest-value mailbox
                truth              │
                                   │ worker only serializes if a client is connected
                                   │ ws://<hub>:8000
                                   ▼
                              AdvantageScope
                    (live source labeled "FTC Dashboard")
```

| System | Role |
| ------ | ---- |
| Panels | Operational / development dashboard (unchanged) |
| TRACE / `.tlog` | Canonical observability and persistent record |
| `trace-advantagescope` | Best-effort live delivery to AdvantageScope |
| FTC Dashboard | Protocol/reference only. Not a product we run |

Credit: ACME Robotics defined the WebSocket messages (`GET_ROBOT_STATUS`, `RECEIVE_ROBOT_STATUS`, `RECEIVE_TELEMETRY`) that AdvantageScope's FTC Dashboard live source already understands. See [AdvantageScope live sources](https://docs.advantagescope.org/overview/live-sources/). TRACE implements a compatible subset. It does not include DashboardCore or the Dashboard UI.

## Enable it

Classpath presence alone does **not** start a server.

1. Depend on both artifacts:

```gradle
implementation 'org.allsparks:trace'
implementation 'org.allsparks:trace-advantagescope'
```

2. Turn it on in robot config (development only by default):

```java
Trace.configure(TraceConfig.builder()
        .mode(TraceMode.ESSENTIAL)
        .fileSink(true)
        .storageDirectory(new java.io.File("/sdcard/FIRST/trace"))
        .advantageScopeStreaming(true)  // default false
        .advantageScopeRateHz(20)
        .advantageScopePort(8000)
        .advantageScopeMaxValues(256)
        .build());
```

Competition: Panels as required, `.tlog` on, `advantageScopeStreaming(false)`.

Development: Panels on (8001), `.tlog` on, this module on (8000) when you want live graphs.

## Connect AdvantageScope

1. Enable `trace-advantagescope` as above.
2. Put the laptop on Control Hub Wi-Fi.
3. Start an OpMode so TRACE and the live server are active.
4. Open AdvantageScope.
5. **File → Connect to Robot**.
6. Select **FTC Dashboard**.
7. Enter the Control Hub address (typically `192.168.43.1`; TRACE does not hardcode it).
8. Connect. TRACE values should appear live.

**Why AdvantageScope says "FTC Dashboard":** that is the name of the compatible FTC live protocol. You connect AdvantageScope **directly to TRACE**. A separate FTC Dashboard application does not need to be running.

Live may drop frames. `.tlog` is the full history. Convert with `TraceInspect --wpilog` for offline graphs.

Pose on the FTC field uses `name x` / `name y` / `name heading` (inches + radians). TRACE still stores Pose2d as meters + `headingRad` in `.tlog`.

## Troubleshooting

- **Cannot connect:** laptop on robot Wi-Fi? module on classpath? `advantageScopeStreaming(true)`? OpMode started? port 8000 free? firewall? another compatible service already on 8000?
- **Why select FTC Dashboard?** Protocol compatibility only. TRACE is the server.
- **Can Panels and AdvantageScope run together?** Yes. Independent sockets: Panels 8001, this module 8000.
- **Need FTC Dashboard installed?** No. This module uses NanoWSD only, not DashboardCore or the UI.
- **Does live replace `.tlog`?** No. Keep `.tlog` → `.wpilog` for history, offline, and full fidelity.
- **Port conflict with real FTC Dashboard:** do not run both. Both want 8000. TRACE recording continues if this server fails to bind.
- **Match legality:** extra streaming during MATCH play can violate FTC R704. Keep this off in competition unless mentors accept the risk.

## Architecture notes

- `Trace.record` never serializes JSON or touches the network. It updates a latest-value mailbox.
- A `MIN_PRIORITY` daemon thread `trace-advantagescope` encodes and sends at the configured rate.
- No client: no JSON, no WebSocket frames.
- Slow or disconnected clients drop stale frames. There is no FIFO backlog.
- Self-metrics live under `TRACE/AdvantageScope/` in `.tlog` and on the live overlay.

See [ADR 0011](../docs/adr/0011-advantagescope-live-protocol.md).
