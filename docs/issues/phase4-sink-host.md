# [phase] Sibling libraries emit through local sinks; TRACE stays independent

Draft for GitHub. Do not post until a maintainer asks. Reframes
[issue #6](https://github.com/The-Allsparks/TRACE/issues/6).

## Problem

Drive copies AMPER and other library snapshots into TRACE from `loop()`. A
bad TRACE signal name can throw on the OpMode thread. Sibling docs still say
those libraries may depend on a small TRACE API. That would make AMPER or
Pedro import TRACE.

Students cannot say: libraries stay usable with a no-op sink; TeamCode wires
TRACE at INIT; `loop()` only calls `update()` / `observe()`.

## Student learning objective

A student can point at INIT and say: SHIFT (or AMPER) got a TRACE adapter
here, or it got NOOP. They can say TRACE never reads Hub hardware. They can
say a bad channel name must not stop the wheels.

## Scope

- TRACE `record` / `event` fail open (drop `INVALID_RECORD`, do not throw).
- `wouldAccept` peek so adapters skip maps and strings when ESSENTIAL would
  drop the sample.
- `integrationEnabled(name)` so `enableIntegration` is a real adapter switch,
  not metadata only.
- `FailOpen` helper for TeamCode sink implementations.
- Docs: libraries expose sinks; TeamCode implements them. SHIFT is the
  reference. TRACE does not depend on AMPER, MIMIC, HELM, ViDAR, BEACON, or
  Pedro.

## Out of scope

- TRACE importing sibling library types
- A shared `Logger` in `allsparks-contracts`
- Deleting AMPER / MIMIC / BEACON in-memory CSV rings
- Replay, live AdvantageScope changes, commanding motors

## Acceptance criteria

- [ ] `session.record("not a name!", 1.0, Units.NONE)` does not throw
- [ ] The bad name increments `DropReason.INVALID_RECORD`
- [ ] `wouldAccept` is false for a downsampled analog without allocating a
      `TraceRecord`
- [ ] `integrationEnabled("AMPER")` is true only after `enableIntegration`
- [ ] README and `docs/integrations.md` say siblings do not depend on TRACE
- [ ] SHIFT + TeamCode `TraceShiftAdapter` remains the reference stitch

## Dependencies

Parent gate: [#6](https://github.com/The-Allsparks/TRACE/issues/6). Hub
recording proof ([#13](https://github.com/The-Allsparks/TRACE/issues/13))
still blocks competition-default TRACE, not this desktop host API.

## Architecture impact

TRACE is a sink *target*. Adapters live in TeamCode (or later optional
`*-trace-adapter` modules). TRACE core stays free of FTC sibling types.

## Implementation plan

1. Fail-open `TraceSession.record` / `event`.
2. Public `wouldAccept` / `integrationEnabled` / `FailOpen`.
3. Flip integration docs. Optional ADR 0012.

## Validation plan

Desktop unit tests. No Control Hub required for the host API.

## Documentation required

- [ ] README relationship to Allsparks projects
- [ ] `docs/integrations.md` cookbook
- [ ] Phase 4 student path
- [ ] CHANGELOG

## Hardware validation required

- [x] Desktop only
- [ ] Control Hub
- [ ] Robot
- [ ] Match

## Risks

Adapters that ignore `wouldAccept` still allocate. Fail-open can hide a
typo; health drop counts must stay visible.

## Rollback or disable strategy

`TraceConfig.off()` and `TeamTrace.MODE = OFF`. Sinks stay NOOP.

## Parent roadmap issue

[#6](https://github.com/The-Allsparks/TRACE/issues/6)

## Exclusions

No sibling compile dependencies. No contracts Logger SPI.

## This issue is a design review. Do not start implementation until maintainers accept the plan.
