# Performance

TRACE is designed for FTC Android hardware, not FRC roboRIO-class assumptions.

## Requirements

* No blocking file writes in the OpMode control loop
* Bounded memory, queues, and file sizes
* Storage quotas and log rotation
* Batched, asynchronous storage
* Configurable sampling, downsampling, and optional change-based recording
* Explicit backpressure and priority handling
* Dropped-record counters with reasons
* Graceful storage exhaustion and best-effort final flush
* Truncation-tolerant reads after power loss

## Priorities

```text
CRITICAL > HIGH > NORMAL > DEBUG > VERBOSE
```

When a queue is full, TRACE evicts a lower-priority retained record if the incoming record outranks it; otherwise the incoming record is dropped. Either way, a reason is counted. DROP summary records are written when the writer can do so.

## Default budgets (software policy, not hardware measurements)

| Limit | Default |
|-------|---------|
| Memory sink | 2048 records |
| Writer queue | 4096 records |
| File size | 8 MiB |
| Total logs | 32 MiB |
| Write batch | 16 KiB |
| Essential sample interval | 50 ms |
| Shutdown flush | 250 ms |
| Loop overrun budget | 30 ms |

These numbers are **engineering inference** from Control Hub storage and typical 20–30 ms FTC loops. They are **not** Control Hub measurements.

## Validation classes

| Claim | Status |
|-------|--------|
| Unit tested | Yes (desktop JVM) |
| Desktop simulated | Yes (file writer, quotas, truncation) |
| Android compiled | No (FTC SDK not on classpath) |
| Control Hub tested | **No** |
| Robot tested | **No** |
| Match tested | **No** |

## Desktop smoke result

`PerformanceSmokeTest` measures per-loop recording overhead on the CI/desktop JVM and asserts it stays under 5 ms/iteration for 5000 essential-mode cycles. That is **not** a Control Hub result.

## Allocation

Records are immutable and currently allocate per call. Object pooling is future work. `TraceMode.OFF` returns before building records.
