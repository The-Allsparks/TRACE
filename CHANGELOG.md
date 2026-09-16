# Changelog

All notable changes to TRACE will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project aims to adhere to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added

- `TraceSession.preFaultSnapshot()` returns an in-memory copy of the file writer's rolling pre-fault buffer, or empty when `fileSink` is disabled.
- Initial public repository scaffold for The Allsparks FTC Team 36117.
- Phases 0–3 implemented on desktop: foundation vocabulary, event recorder, typed essential telemetry, and bounded `.tlog` flight recording.
- Source-backed FTC/FRC logging research, build-versus-adopt decision, architecture, ADRs, and student documentation.
- CI for compile, unit tests, example compilation, Javadoc, and relative documentation link checks.
- Initial deep audit and priority ledger (`docs/audits/`).

### Changed

- `beginCycle()` emits `TRACE/Loop/Begin` only in `FULL`. ESSENTIAL and EVENTS already stamp `cycle` on every record; the per-loop Begin event was unsampled allocation on the OpMode thread.
- Signal `record()` checks the ESSENTIAL sample interval before allocating `TypedValue` / `TraceRecord`. Skipped samples still count as `SAMPLE_SKIPPED`.
- The `trace-writer` disk thread runs at `Thread.MIN_PRIORITY` so Control Hub file I/O yields to the OpMode loop. The loop still only enqueues; the bounded RAM queue is unchanged.

### Documentation

- The rolling pre-fault buffer is an in-memory debug snapshot only. It is not dumped on writer failure and is not power-loss durable. Power-loss recovery remains `TlogReader` truncation tolerance of the `.tlog` file.

### Fixed

- On-robot `.tlog` writing uses `java.io.File` (`mkdirs`, `FileOutputStream`) instead of `java.nio.file.Files`. Control Hub Android 7 crashes on `File.toPath()`; the writer thread now also treats `Error` as `writerFailed`.
- Session metadata no longer treats session-start time as a build stamp. `buildInfoAvailable` is true only when `TRACE_BUILD_TIMESTAMP` (or an injected source) supplies a value; otherwise the timestamp is Unix epoch and the flag is false.
- Git is not spawned during session construction unless `TRACE_GIT_SPAWN=1` (or `true`). Prefer `TRACE_GIT_SHA` and `TRACE_GIT_DIRTY`. Control Hub-safe default: no git process.
- Closed sessions reject further events and records. `Trace.stop()` leaves TRACE disabled (`health().enabled()` is false); calls after stop are ignored.

### Tests

- Metadata tests cover default `ProcessMetadataSource` honesty when git spawn is off and no build stamp is set, plus env overrides without spawning git.
- Writer-failure tests now require `health().writerFailed()` and/or `WRITER_FAILED` drops instead of a tautology. Storage quota tests fail if `.tlog` totals exceed `maxTotalBytes + maxFileBytes` (documented rotation slack), not a silent 2x fudge. Quota tests wait up to 5 s for the writer to release files so Windows `@TempDir` cleanup can succeed.
- `preFaultSnapshot()` is non-empty after file-sink recording and empty when `fileSink` is false.
- Writer-thread test asserts `trace-writer` is a daemon at `Thread.MIN_PRIORITY` while the file sink is open.
- `ClockAndCycleTest` requires `TRACE/Loop/Begin` in FULL and forbids it in ESSENTIAL. `EventRecorderTest` forbids it in EVENTS.

### Safety

- TRACE remains observational. `TraceMode.REPLAY` cannot be enabled. No motor, servo, or mechanism commands are issued.
