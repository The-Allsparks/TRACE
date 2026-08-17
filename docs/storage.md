# Storage

## Canonical format

TRACE writes **`.tlog`** files (TLOG version 1). See [schema.md](schema.md).

CSV is for students and spreadsheets. AdvantageScope CSV list export (`Timestamp, Key, Value`) is lossy and does not preserve category, units, quality, or schema.

## Writer

`AsyncBoundedWriter`:

* Dedicated daemon thread `trace-writer`
* Bounded in-memory queue
* Batched writes
* File rotation when `maxFileBytes` is exceeded
* Oldest `.tlog` deletion when `maxTotalBytes` is exceeded (keeps the newest file)
* Rolling pre-fault buffer of recent records
* Best-effort flush on `close()`

The OpMode thread only offers records to the queue.

## Locations

Teams should choose a Control Hub directory such as `/sdcard/FIRST/trace` in their OpMode. This library does not hard-code Android paths so desktop tests remain portable. Official FTC datalogs historically used `/sdcard/FIRST/java/src/Datalogs` ([FTC Datalogging wiki](https://github.com/FIRST-Tech-Challenge/FtcRobotController/wiki/Datalogging), accessed 2026-08-17). TRACE should **not** write into OnBot Java source folders by default.

## Filenames

`FileRotator.sanitize` strips characters other than `A-Za-z0-9._-` and caps length. Session ids are UUIDs. Do not put student names in filenames.

## Truncation and corruption

`TlogReader` stops at incomplete records and counts CRC failures. It does not use Java object deserialization.

## Privacy

Do not log:

* Raw camera images/video
* Student names or contact data
* Wi-Fi passwords, tokens, or API keys
* Unredacted strategy documents

Match video should stay in a separate file and be aligned by timestamp in AdvantageScope later.
