# Storage

## Canonical format

TRACE writes **`.tlog`** files (TLOG version 1). See [schema.md](schema.md).

CSV is for students and spreadsheets. AdvantageScope CSV list export (`Timestamp, Key, Value`) is lossy and does not preserve category, units, quality, or schema.

Desktop `WpiLogExporter` / `TraceInspect --wpilog` converts a `.tlog` into WPILOG 1.0 for AdvantageScope. That file is an interchange format. It is not the on-robot canonical log. Pose2d becomes a `double[]` plus `/x`, `/y`, `/headingRad` children. Entry metadata JSON carries TRACE category, units, quality, and source. Live WPILOG writing on the Control Hub is not implemented. Optional live graphs use `trace-advantagescope` (latest-value WebSocket), not a second on-robot file format.

## Writer

`AsyncBoundedWriter`:

* Dedicated daemon thread `trace-writer` at `Thread.MIN_PRIORITY`
* Bounded in-memory queue
* Batched writes
* File rotation when `maxFileBytes` is exceeded
* Oldest `.tlog` deletion for **this session prefix only** when that prefix's total exceeds `maxTotalBytes` (keeps the newest file of the prefix)
* Other prefixes in the same folder are not deleted. There is no directory-wide cap. Many session prefixes can grow the folder; an optional directory cap would need its own config flag later
* On-disk totals for one prefix may briefly reach `maxTotalBytes + maxFileBytes` while a new segment is opened before the next quota pass; `TraceConfig` requires `maxTotalBytes >= maxFileBytes`
* Creates directories and `.tlog` files with `java.io.File` (`mkdirs`, `FileOutputStream`), not `java.nio.file.Files`. Control Hub Android 7 has no `File.toPath()`.
* In-memory rolling pre-fault buffer of recent dequeued records (debug snapshot only; not durable)
* Best-effort flush on `close()`

The OpMode thread only offers records to the queue.

## Rolling pre-fault buffer

`RollingPreFaultBuffer` keeps a RAM-only copy of records the writer has dequeued. `TraceSession.preFaultSnapshot()` exposes it for tests and debugging. It is **not** written to disk, not dumped on writer failure, and not recovered after power loss. A JVM exit or power loss discards it.

Power-loss and crash recovery is `TlogReader` truncation tolerance of the `.tlog` file. The pre-fault buffer is not used for recovery.

## Locations

Teams should choose a Control Hub directory such as `/sdcard/FIRST/trace` in their OpMode. This library does not hard-code Android paths so desktop tests remain portable. Official FTC datalogs historically used `/sdcard/FIRST/java/src/Datalogs` ([FTC Datalogging wiki](https://github.com/FIRST-Tech-Challenge/FtcRobotController/wiki/Datalogging), accessed 2026-08-17). TRACE should **not** write into OnBot Java source folders by default.

## Filenames

`FileRotator.sanitize` strips characters other than `A-Za-z0-9._-` and caps length. Session ids are UUIDs. Do not put student names in filenames.

## Truncation and corruption

`TlogReader` stops at incomplete records and counts CRC failures. It does not use Java object deserialization. Incomplete trailing bytes are dropped; they are not reconstructed from the in-memory pre-fault buffer.

## Privacy

Do not log:

* Raw camera images/video
* Student names or contact data
* Wi-Fi passwords, tokens, or API keys
* Unredacted strategy documents

Match video should stay in a separate file and be aligned by timestamp in AdvantageScope later.
