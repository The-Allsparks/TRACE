# ADR 0010: Android and Control Hub compatibility

## Context

AMPER/MIMIC compile as Java 11 libraries without the FTC SDK on the test classpath. Control Hubs are Android devices with limited storage and sudden power loss.

## Decision

* Java 11 source/target; CI Temurin 17 (Allsparks convention)
* No FTC SDK Maven dependency in this repo
* No hardcoded `/sdcard` paths in the library
* Length-prefixed CRC records for truncation
* Gradle single module like AMPER
* MIT license

**Intentional deviations from AMPER:** TRACE adds a binary log, async writer, and ADRs because observability needs them; AMPER’s CSV/event logger is not sufficient here. TRACE does not copy AMPER’s power-management docs layout.

## Alternatives considered

* Android library module now — blocks desktop CI without SDK.
* Java 8 like PsiKit’s 2026-07 downgrade — Allsparks Java 11 is already the org standard.
* Embed FTC SDK — version coupling.

## Consequences

Example OpModes are sketches plus SDK-free compiled examples. Android compilation is a later TeamCode integration task.

## Student impact

They can run tests on laptops the same way as AMPER.

## Revisit conditions

Publishing an AAR that TeamCode consumes from Maven.
