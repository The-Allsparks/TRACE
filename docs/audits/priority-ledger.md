# TRACE priority ledger

Living orchestrator view. Update after each issue/PR transition. Do not treat this table as permission to skip GitHub issue state.

**Updated:** 2026-08-17  
**Audited commit:** `55fa66625f55ff19fd44ba7e48b601046b44339e`  
**Open implementation PR:** [#14](https://github.com/The-Allsparks/TRACE/pull/14) (draft, CI green)  
**Automatic merge:** false  
**Active subagent:** none at ledger creation; see current row after selection

## Priority model

Safety impact → correctness impact → CI/build → unblocking value → architectural leverage → user value → learning value → risk reduction → confidence → effort → hardware/external dependency.

Default order: safety blockers, correctness blockers, CI failures, multi-issue blockers, seams, tests for upcoming work, small user-facing slices, measured performance, docs, optional advanced work, cosmetic cleanup.

## Current selection

| Field | Value |
|-------|--------|
| Selected issue | [#15](https://github.com/The-Allsparks/TRACE/issues/15) Closed TRACE sessions must reject further records and events |
| Why highest priority | Completes Phase 3 session finalization (audit C1); recording integrity; no hardware; not behind an approval gate |
| Why ready | Requirements and acceptance criteria are testable; no credentials or hardware; depends only on PR #14 code already on this branch |
| Dependencies | None other than the Phase 0–3 code on `phase-0-3-passive-foundation` |
| Expected deliverable | Closed sessions ignore events/records; `Trace.stop()` leaves disabled health; tests; troubleshooting note |
| Expected validation | `./gradlew.bat check --no-daemon` |
| Hardware required | No |

Work lands on the existing foundation branch / PR #14 rather than a second implementation PR, because #14 is the unresolved Phase 0–3 slice and #15 is a Phase 3 finalization hole in that slice.

## Ledger

| Issue | Priority | Readiness | Dependencies | Current status | Assigned subagent | Branch | Pull request | CI status | Merge status | Blocker | Next action |
|-------|----------|-----------|--------------|----------------|-------------------|--------|--------------|-----------|--------------|---------|-------------|
| [PR #14](https://github.com/The-Allsparks/TRACE/pull/14) | Foundation | In review | — | Draft | orchestrator | `phase-0-3-passive-foundation` | #14 | green before #15 | not authorized | human review + #15 | Land #15; do not merge without human approval |
| [#1](https://github.com/The-Allsparks/TRACE/issues/1) | Index | Open epic | — | Open | — | — | #14 partial | — | — | — | Keep open |
| [#2](https://github.com/The-Allsparks/TRACE/issues/2)–[#4](https://github.com/The-Allsparks/TRACE/issues/4) | Phases 0–2 | Waiting merge | — | Open | — | same | #14 | green | unmerged | PR #14 | Merge when authorized |
| [#5](https://github.com/The-Allsparks/TRACE/issues/5) | Phase 3 software | Partial | #4 | Open | — | same | #14 | green | unmerged | #15, #17, #13 | Finish finalization; no CH claim |
| [#15](https://github.com/The-Allsparks/TRACE/issues/15) | HIGH | Ready / selected | PR #14 code | In progress | one-shot implementer | `phase-0-3-passive-foundation` | #14 | pending | unmerged | — | Implement slice |
| [#16](https://github.com/The-Allsparks/TRACE/issues/16) | HIGH | Ready | — | Queued | — | — | — | — | — | single-subagent | After #15 |
| [#17](https://github.com/The-Allsparks/TRACE/issues/17) | HIGH | Ready | — | Queued | — | — | — | — | — | — | After #16 |
| [#18](https://github.com/The-Allsparks/TRACE/issues/18) | MEDIUM | Ready | — | Queued | — | — | — | — | — | — | Later |
| [#19](https://github.com/The-Allsparks/TRACE/issues/19) | MEDIUM | Ready | — | Queued | — | — | — | — | — | — | Later |
| [#20](https://github.com/The-Allsparks/TRACE/issues/20) | MEDIUM | Ready | — | Queued | — | — | — | — | — | — | Later |
| [#21](https://github.com/The-Allsparks/TRACE/issues/21) | MEDIUM | Ready | prefer after #15 | Queued | — | — | — | — | — | — | Later |
| [#22](https://github.com/The-Allsparks/TRACE/issues/22) | MEDIUM | Ready | — | Queued | — | — | — | — | — | — | Later |
| [#23](https://github.com/The-Allsparks/TRACE/issues/23) | HIGH | Blocked | human GitHub settings | Open | — | — | — | — | — | maintainer | Enable protection |
| [#13](https://github.com/The-Allsparks/TRACE/issues/13) | HIGH | Blocked | #5 | Open | — | — | — | — | — | hardware | Wait |
| [#6](https://github.com/The-Allsparks/TRACE/issues/6)/[#9](https://github.com/The-Allsparks/TRACE/issues/9) | Gates | Blocked | human | Open | — | — | — | — | — | approval-gate | Do not implement #7/#10 |

## Ready queue (code, no hardware, no approval gate)

1. [#15](https://github.com/The-Allsparks/TRACE/issues/15) session close (C1) — **selected**
2. [#16](https://github.com/The-Allsparks/TRACE/issues/16) honest writer tests (C2)
3. [#17](https://github.com/The-Allsparks/TRACE/issues/17) pre-fault buffer honesty (C7)
4. [#18](https://github.com/The-Allsparks/TRACE/issues/18) metadata / git spawn (C3)
5. [#19](https://github.com/The-Allsparks/TRACE/issues/19) quota prefix (C5)
6. [#20](https://github.com/The-Allsparks/TRACE/issues/20) OFF allocation (P2)
7. [#22](https://github.com/The-Allsparks/TRACE/issues/22) pin Actions (Dep2)
8. [#21](https://github.com/The-Allsparks/TRACE/issues/21) drop-summary race (C4)

## Stop conditions currently in force

* `AUTOMATIC_MERGE=false` — do not merge PR #14 from the orchestrator.
* Hardware for #13 is unavailable in this environment.
* Phase 4 and Phase 6 approval gates are closed.
