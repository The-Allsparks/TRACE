# Schema

Canonical on-disk format: **TLOG version 1** (`.tlog`). CSV is educational interchange, not canonical.

## What every record answers

| Question | Field |
|----------|-------|
| When? | `monotonicNanos`, optional `wallClockMillis` |
| Which session/cycle? | session metadata + `cycle` |
| Which subsystem? | `source` (first path segment) |
| What was recorded? | hierarchical `name` |
| Type and units? | `TypedValue` + `Units` |
| Fresh and trustworthy? | `TraceQuality` |
| Input, output, or event? | `RecordCategory` |
| Which schema? | `schemaVersion` (currently `1`) |

## Naming

```text
TRACE/Loop/Duration
Drive/Pose
Drive/Command
ViDAR/Camera/Front/FrameAge
ViDAR/Fusion/TargetCount
AMPER/Battery/Voltage
AMPER/Intervention/Active
MIMIC/Arm/Position
MIMIC/Arm/Goal
BEACON/DriverStation/Health
BEACON/SafeState/Active
```

Rules:

* Segments match `[A-Za-z][A-Za-z0-9_]*`
* `/` separates hierarchy
* No leading or trailing slash
* Do not put secrets, student names, or Wi-Fi SSIDs in names or values

## Versioning

* `SchemaVersion.RECORD = 1` — on-disk record payload
* `SchemaVersion.METADATA = 1` — session header JSON

Existing fields must not change meaning. Add fields or bump the version. Readers reject unknown TLOG magic/version numbers and skip CRC-failing records.

## Binary layout (TLOG1)

Header:

1. Magic `TLOG`
2. `uint16` version
3. `uint16` flags
4. `uint32` JSON length
5. UTF-8 session metadata JSON

Each record:

1. `uint32` payload length
2. `uint32` CRC32 of payload
3. Payload: category, timestamps, cycle, priority, quality, severity, schema, source, name, units, message, typed value

Incomplete trailing bytes are reported as truncation. CRC mismatches increment `corruptRecords` and are skipped.

## Session metadata

See `SessionMetadata`: git SHA (or `unknown`), dirty tree, build time, TRACE version, schema versions, FTC SDK version, library versions, robot configuration hash, calibration set, OpMode name, mode, integrations, feature flags, robot name, optional match/alliance/battery/driver config.

Replay (future) must warn when code or schema does not match the recorded session (`SessionMetadata.compatibleWith`).
