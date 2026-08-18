# Troubleshooting

## Nothing is recorded

* Default mode is `OFF`. Call `Trace.configure` with `EVENTS`, `ESSENTIAL`, or `FULL`.
* `Trace.record` is ignored in `EVENTS` mode (signals are filtered). Use `Trace.event` or raise the mode.

## `REPLAY` throws

Expected. Replay is not implemented and fails closed. See [replay.md](replay.md).

## Files are missing on the Control Hub

* `fileSink(true)` requires `storageDirectory`.
* Writer failures set `TraceHealth.writerFailed()`. Check health after the OpMode.
* Quotas delete oldest `.tlog` files. Copy matches off the hub.

## Calls after stop are ignored

`Trace.stop()` and OpMode stop finalize the session, then ignore further `Trace.event` / `Trace.record` calls. Those calls do not throw and do not append to `recorded()` or `.tlog`. `Trace.health().enabled()` is false. `Trace.stop()` replaces the facade with `TraceMode.OFF`, so export CSV or inspect `recorded()` **before** `Trace.stop()`, or keep a `TraceSession` reference. Call `Trace.configure` to start a new session.

## Log looks truncated

Power loss or `close()` not called. `TlogReader` still returns intact prefix records and sets `complete=false`. Always stop the OpMode so TRACE can flush.

## Loop overruns after enabling FULL

Reduce mode, increase sample interval, enable change-based recording, or raise `minimumPriority`. TRACE should not flush per record. If overruns persist, disable TRACE (`OFF`) — it is reversible.

## CSV does not open in AdvantageScope as expected

Educational CSV is TRACE-native. Use `--as-csv` for the lossy `Timestamp, Key, Value` list. Native AdvantageScope writers are Phase 5.

## Git SHA is `unknown`

Normal on a Control Hub. TRACE does not spawn `git` unless `TRACE_GIT_SPAWN=1` (or `true`). Set `TRACE_GIT_SHA` at build time if you need exact provenance, and optionally `TRACE_GIT_DIRTY=1` when the tree was dirty. `gitAvailable` is true only when a SHA was supplied (env or opt-in git).

## Build time is epoch / `buildInfoAvailable` is false

Normal when `TRACE_BUILD_TIMESTAMP` is unset. TRACE does not use session-start time as a build stamp. Set `TRACE_BUILD_TIMESTAMP` to an ISO-8601 instant for real build provenance. `buildInfoAvailable` is true only when that stamp (or an injected source) provided a non-empty value. Session id and per-record wall-clock remain available.

## Broken relative docs

`DocLinkCheckerTest` fails CI when Markdown links point at missing files.
