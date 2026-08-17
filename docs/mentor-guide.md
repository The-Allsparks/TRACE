# Mentor guide

## Goal

Students grow into TRACE. They should not have to understand clocks, CRC, and replay isolation on day one.

## Teaching sequence

1. Watch a match and write the story on paper (events).
2. Add `Trace.event` only.
3. Graph one signal (battery or pose).
4. Introduce input vs output when a student asks “but did we measure that or decide it?”
5. Only then turn on file recording.

## What to protect

* Never let TRACE become a reason to skip mechanical or electrical debugging.
* Never enable replay-like code that can command motors.
* Keep logs free of student PII and secrets.
* Treat Control Hub storage as finite. Set quotas before a competition.

## Review questions

* Can this be disabled by setting `TraceMode.OFF`?
* Does this PR command hardware?
* Are drops visible in health?
* Is this claim Control Hub tested or only desktop tested?

## Relationship to other Allsparks work

If AMPER, MIMIC, BEACON, or ViDAR already logs a value, TRACE should record that evidence rather than re-own the subsystem. See [integrations.md](integrations.md).

## Approval gates

Do not start Phase 4 adapters or Phase 6 IO refactors because the repository exists. Those are separate decisions after Phase 3 is reliable on hardware.
