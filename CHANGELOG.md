# Changelog

All notable changes to TRACE will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project aims to adhere to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added

- `FailOpen` helper so TeamCode sibling-sink calls cannot throw into `update()` / `observe()`.
- `TraceSession.wouldAccept` / `Trace.wouldAccept` peek before adapter allocation.
- `TraceSession.integrationEnabled` so `enableIntegration` is an adapter switch, not metadata only.
- Draft design-review issue [docs/issues/phase4-sink-host.md](docs/issues/phase4-sink-host.md) (not posted).
- Optional `trace-advantagescope` module: latest-value live telemetry to AdvantageScope using a minimal FTC Dashboard-compatible WebSocket. Off by default. Core `org.allsparks:trace` still has no WebSocket dependency. NanoHTTPD is `compileOnly` so the FTC APK can reuse RobotCore's copy.
- Desktop WPILOG 1.0 exporter (`WpiLogExporter`) and `TraceInspect --wpilog [out.wpilog]`. Converts `.tlog` / recorded sessions for AdvantageScope without a WPILib dependency. On-robot storage remains `.tlog`.
- `TraceSession.preFaultSnapshot()` returns an in-memory copy of the file writer's rolling pre-fault buffer, or empty when `fileSink` is disabled.
- Initial public repository scaffold for The Allsparks FTC Team 36117.
- Phases 0–3 implemented on desktop: foundation vocabulary, event recorder, typed essential telemetry, and bounded `.tlog` flight recording.
- Source-backed FTC/FRC logging research, build-versus-adopt decision, architecture, ADRs, and student documentation.
- CI for compile, unit tests, example compilation, Javadoc, and relative documentation link checks.
- Initial deep audit and priority ledger (`docs/audits/`).

### Changed

- File rotation quota deletes only `{sessionPrefix}-{digits}.tlog` for this session. Other prefixes in the same folder stay. `maxTotalBytes` is a per-prefix cap, not a directory-wide cap.
- `TraceMode.OFF` does not construct memory, console, or file sinks, even when builder flags request them. The unconfigured facade stays allocation-free of recording sinks.
- CI pins `actions/checkout` and `actions/setup-java` to full commit SHAs. Dependabot still opens pin-update PRs from the version comments.
- `beginCycle()` emits `TRACE/Loop/Begin` only in `FULL`. ESSENTIAL and EVENTS already stamp `cycle` on every record; the per-loop Begin event was unsampled allocation on the OpMode thread.
- Signal `record()` checks the ESSENTIAL sample interval before allocating `TypedValue` / `TraceRecord`. Skipped samples still count as `SAMPLE_SKIPPED`.
- The `trace-writer` disk thread runs at `Thread.MIN_PRIORITY` so Control Hub file I/O yields to the OpMode loop. The loop still only enqueues; the bounded RAM queue is unchanged.

### Documentation

- Storage and README state that quota is per session prefix and that `OFF` does not allocate recording sinks.
- `TraceSession.record` / `event` catch `RuntimeException` and `Error`, count `INVALID_RECORD`, and return. A bad signal name must not stop drive.
- Integrations, README, architecture, and Phase 4: sibling libraries expose local sinks; TeamCode implements with TRACE. SHIFT is the reference. TRACE does not depend on HELM, AMPER, MIMIC, ViDAR, BEACON, Pedro, or SHIFT. ADR 0012.
- The rolling pre-fault buffer is an in-memory debug snapshot only. It is not dumped on writer failure and is not power-loss durable. Power-loss recovery remains `TlogReader` truncation tolerance of the `.tlog` file.
- Phase 5 WPILOG converter usage is in the README, storage, student path, and troubleshooting docs.
- Optional live AdvantageScope streaming is documented in `trace-advantagescope/README.md` and ADR 0011.

### Fixed

- Writer drop-summary fields (`pendingDropCount` / category / reason) are published under the queue lock. The OpMode thread and `trace-writer` share that happens-before edge. `TRACE/Health/Dropped` rows may still coalesce; `DroppedRecordStats` remains the accurate count.
- On-robot `.tlog` writing uses `java.io.File` (`mkdirs`, `FileOutputStream`) instead of `java.nio.file.Files`. Control Hub Android 7 crashes on `File.toPath()`; the writer thread now also treats `Error` as `writerFailed`.
- Session metadata no longer treats session-start time as a build stamp. `buildInfoAvailable` is true only when `TRACE_BUILD_TIMESTAMP` (or an injected source) supplies a value; otherwise the timestamp is Unix epoch and the flag is false.
- Git is not spawned during session construction unless `TRACE_GIT_SPAWN=1` (or `true`). Prefer `TRACE_GIT_SHA` and `TRACE_GIT_DIRTY`. Control Hub-safe default: no git process.
- Closed sessions reject further events and records. `Trace.stop()` leaves TRACE disabled (`health().enabled()` is false); calls after stop are ignored.

### Tests

- `FileRotatorTest` keeps a second prefix's `.tlog` when enforcing this session's quota. `OffModeAllocationTest` requires OFF sessions not to retain recording sinks. Overflow handoff still requires `accepted + dropped >= produced`.
- `TraceMappingsTest` covers quality → `Validity` and severity → `HealthSeverity`, including unmapped `ESTIMATED` / `ASYNC`. `ClockAndCycleTest` requires `nowNanos()` to match `nanoTime()` while wall-clock stays on `TraceClock`.
- Metadata tests cover default `ProcessMetadataSource` honesty when git spawn is off and no build stamp is set, plus env overrides without spawning git.
- Writer-failure tests now require `health().writerFailed()` and/or `WRITER_FAILED` drops instead of a tautology. Storage quota tests fail if `.tlog` totals exceed `maxTotalBytes + maxFileBytes` (documented rotation slack), not a silent 2x fudge. Quota tests wait up to 5 s for the writer to release files so Windows `@TempDir` cleanup can succeed.
- `preFaultSnapshot()` is non-empty after file-sink recording and empty when `fileSink` is false.
- Writer-thread test asserts `trace-writer` is a daemon at `Thread.MIN_PRIORITY` while the file sink is open.
- WPILOG exporter tests encode the published 1.0 spec examples and round-trip TRACE doubles, poses, and events.
- Live AdvantageScope tests cover mailbox latest-value semantics, Dashboard-compatible JSON, WebSocket handshake, no-client idle, reconnect, port conflict, shutdown, and producer isolation.
- `ClockAndCycleTest` requires `TRACE/Loop/Begin` in FULL and forbids it in ESSENTIAL. `EventRecorderTest` forbids it in EVENTS.

### Safety

- TRACE remains observational. `TraceMode.REPLAY` cannot be enabled. No motor, servo, or mechanism commands are issued.
