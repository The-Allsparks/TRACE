# TRACE priority ledger

Living orchestrator view. Update after each issue/PR transition.

**Updated:** 2026-09-17
**Main head at branch start:** `05e1bb3` (PR #42 contracts pilot)
**Open implementation PR:** `hygiene/foundation-19-22` (issues #19–#22, #32)
**Automatic merge:** false except when a human says proceed for a ready PR

## Current selection

| Field | Value |
|-------|--------|
| Selected issue | [#31](https://github.com/The-Allsparks/TRACE/issues/31) Package TRACE as the shared FTC observability foundation |
| Why highest priority | FTC install and sibling-safe packaging outrank replay, live polish, and Phase 4 adapters. Issue #32 records this order. |
| Why ready after this hygiene PR | #19 prefix quota, #20 OFF allocation, #21 drop-summary race, and #22 Action SHA pins are the remaining foundation-hardening leftovers. They do not need a Hub. |
| Dependencies | Hygiene PR on `hygiene/foundation-19-22` |
| Expected deliverable | Module (or equivalent) boundaries, FTC SDK compile of the robot artifact, documented OFF/stop lifecycle. Not a match-ready claim. |
| Hardware required | No for packaging software. Hub numbers stay on [#13](https://github.com/The-Allsparks/TRACE/issues/13). |

## Ledger

| Issue | Priority | Readiness | Status | Branch / PR | CI | Merge | Next action |
|-------|----------|-----------|--------|-------------|-----|-------|-------------|
| PR #14 | Foundation | **Merged** | Phases 0–3 desktop | `main` | green | merged | Close stale phase issues #2–#4 |
| #15 / #5 | HIGH | **Closed** | Desktop Phase 3 | #14 | green | merged | Hub remains #13 |
| #16–#18 | HIGH | **Closed** | Writer tests, pre-fault docs, honest metadata | #28–#30 | green | merged | — |
| PR #42 | Contracts | **Merged** | Clock/quality/severity mapped at the edge | [#42](https://github.com/The-Allsparks/TRACE/pull/42) | green | merged | Do not delete local recording types |
| **#19** | HIGH | **In progress** | Prefix-scoped quota | `hygiene/foundation-19-22` | pending | — | This PR |
| **#20** | HIGH | **In progress** | OFF allocates no recording sinks | `hygiene/foundation-19-22` | pending | — | This PR |
| **#21** | HIGH | **In progress** | Drop-summary happens-before | `hygiene/foundation-19-22` | pending | — | This PR |
| **#22** | HIGH | **In progress** | Pin Actions to SHAs | `hygiene/foundation-19-22` | pending | — | This PR |
| **#32** | Docs | **In progress** | Ledger lists #31 first | `hygiene/foundation-19-22` | pending | — | This PR |
| **#31** | P0 | Ready after hygiene | Packaging epic | — | — | — | Next implementation stream |
| #13 | HIGH | Unblocked for write-up | Hub validation | — | — | — | Cite 8 Sep / 15 Sep Drive `.tlog` pulls. Do not wait for a new shop night. Do not claim match-ready. |
| #37 | MEDIUM | Optional | Live AdvantageScope remainder | PR #40 merged the first slice | — | — | After #31. Off by default. |
| #38 | LOW | Review | Interval-gated GC diagnostic | — | — | — | Do not enable on the match robot |
| #23 | HIGH | Blocked | Required CI / branch protection | — | — | — | Maintainer policy |
| #6 / #9 | Gates | Blocked | Approval | — | — | — | Do not implement #7 / #10 |
| #10–#12 | Replay | Blocked | Replay gate | — | — | — | Closed until #9 |
| Dependabot #24 | LOW | Blocked | JUnit 6 major | — | — | — | Do not merge without analysis |

## Ready queue

1. **This PR** — #19, #20, #21, #22, #32
2. **#31** — FTC packaging (first implementation/readiness priority)
3. **#13** — write Hub evidence from existing `.tlog` pulls into `docs/performance.md`
4. #37 live remainder (optional, off by default)
5. #38 GC diagnostic review

## Phase 0–3 issue hygiene

Software from PR #14. Close when remaining ACs are desktop-complete:

| Issue | Software | Remaining AC | Close? |
|-------|----------|--------------|--------|
| #2 Phase 0 vocabulary | yes | none | **Yes** — implemented on `phase-0-3-passive-foundation` |
| #3 Phase 1 events | yes; AC checked | none | **Yes** |
| #4 Phase 2 telemetry | yes; AC checked | none | **Yes** |
| #5 Phase 3 recorder | yes | Hub | Already closed; Hub is #13 |

## Stop conditions

* Do not implement Phase 4 adapters (#6 / #7) or replay (#9–#12).
* Do not declare TRACE FTC-ready from desktop tests.
* Do not merge Dependabot major bumps without analysis.
* #13 may use existing Hub `.tlogs`. It still does not make TRACE match-tested.
