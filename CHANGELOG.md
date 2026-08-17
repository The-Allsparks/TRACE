# Changelog

All notable changes to TRACE will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project aims to adhere to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added

- Initial public repository scaffold for The Allsparks FTC Team 36117.
- Phases 0–3 implemented on desktop: foundation vocabulary, event recorder, typed essential telemetry, and bounded `.tlog` flight recording.
- Source-backed FTC/FRC logging research, build-versus-adopt decision, architecture, ADRs, and student documentation.
- CI for compile, unit tests, example compilation, Javadoc, and relative documentation link checks.

### Safety

- TRACE remains observational. `TraceMode.REPLAY` cannot be enabled. No motor, servo, or mechanism commands are issued.
