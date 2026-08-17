# Replay

Deterministic replay is **not implemented**. This document records the intended contract so Phases 6–7 do not surprise students.

## Why replay is deferred

Passive recording must be reliable on a Control Hub before TRACE is allowed to replace hardware inputs or suppress outputs. That is an explicit approval gate.

## Required properties (future)

* A single authoritative control-cycle context
* One monotonic TRACE clock and cycle number
* Immutable per-cycle input snapshots
* No direct hardware reads during replay
* No physical motor/servo/mechanism commands
* Fail closed if output isolation cannot be verified
* Warn when git SHA, TRACE version, or schema does not match the log

## Nondeterministic sources that invalidate replay

* Unrecorded sensor reads
* Unrecorded gamepad, dashboard, or file configuration
* Unrecorded randomness
* Vision results consumed off-cycle without timestamps
* Unstable iteration order
* Logging from arbitrary worker threads into decision logic
* Wall-clock waits

## Current fail-closed behavior

`TraceConfig.Builder.mode(TraceMode.REPLAY)` throws `IllegalArgumentException`.

## Isolation slogan

Replay may reconstruct decisions. It must never energize the robot.
