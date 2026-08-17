# Examples

Each example is independent. The first tutorial does not require replay or adapters.

| Example | Teaches |
|---------|---------|
| `EventOnlyExample` | Phase 1 events |
| `EssentialTelemetryExample` | Phase 2 signals + units |
| `IterativeCycleExample` | `TraceCycle` structured API |
| `BoundedRecordingExample` | Phase 3 `.tlog` session |
| `DashboardAdapterExample` | Optional DS/dashboard hook |
| `SimulatedDrive` | Hardware vs decision-logic boundary |

These classes compile on the desktop JVM without the FTC SDK. On a robot, call the same TRACE methods from `init`, `start`, `loop`/`runOpMode`, and `stop`.

LinearOpMode sketch:

```java
@Override
public void runOpMode() {
    Trace.configure(TraceConfig.builder()
            .mode(TraceMode.EVENTS)
            .opModeName("Auto")
            .memorySink(true)
            .build());
    new OpModeLifecycle(Trace.session()).init();
    waitForStart();
    Trace.event("Autonomous started");
    // ...
    Trace.stop();
}
```

AdvantageScope analysis of `.tlog` files is Phase 5. Today, export CSV or run `TraceInspect`.
