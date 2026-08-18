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

### Documentation

- The rolling pre-fault buffer is an in-memory debug snapshot only. It is not dumped on writer failure and is not power-loss durable. Power-loss recovery remains `TlogReader` truncation tolerance of the `.tlog` file.

### Fixed

- Closed sessions reject further events and records. `Trace.stop()` leaves TRACE disabled (`health().enabled()` is false); calls after stop are ignored.

### Tests

- Writer-failure tests now require `health().writerFailed()` and/or `WRITER_FAILED` drops instead of a tautology. Storage quota tests fail if `.tlog` totals exceed `maxTotalBytes + maxFileBytes` (documented rotation slack), not a silent 2x fudge. Quota tests wait up to 5 s for the writer to release files so Windows `@TempDir` cleanup can succeed.
- `preFaultSnapshot()` is non-empty after file-sink recording and empty when `fileSink` is false.

### Safety

- TRACE remains observational. `TraceMode.REPLAY` cannot be enabled. No motor, servo, or mechanism commands are issued.
