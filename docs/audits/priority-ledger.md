# TRACE priority ledger

Living orchestrator view. Update after each issue/PR transition.

**Updated:** 2026-08-17 (post #14 merge)  
**Main head:** `9c1870b` (PR #14 merged)  
**Open implementation PR:** [#28](https://github.com/The-Allsparks/TRACE/pull/28) for #16  
**Automatic merge:** false  
**Active subagent:** none (issue #16 implemented, orchestrator integrating)

## Current selection

| Field | Value |
|-------|--------|
| Selected issue | [#16](https://github.com/The-Allsparks/TRACE/issues/16) Writer-failure and quota tests must be able to fail |
| Why highest priority | Correctness / test honesty (audit C2); unblocks confidence in Phase 3 storage claims; no hardware |
| Why ready | #14 merged; requirements clear; test-only slice |
| Dependencies | None |
| Expected deliverable | Non-tautological writer-failure and quota assertions |
| Expected validation | `./gradlew check` + CI |
| Hardware required | No |

## Ledger

| Issue | Priority | Readiness | Status | Branch / PR | CI | Merge | Next action |
|-------|----------|-----------|--------|-------------|-----|-------|-------------|
| PR #14 | Foundation | **Merged** | Done | `main` @ `9c1870b` | green | merged | — |
| #15 | HIGH | **Closed** | Done | #14 | green | merged | — |
| #5 | Phase 3 | **Closed** | Done (desktop) | #14 | green | merged | Control Hub still #13 |
| #2–#4 | Phases 0–2 | Delivered | Open on GitHub | #14 | — | unclosed | Close when convenient |
| **#16** | HIGH | **In PR** | Implemented | `fix/honest-writer-quota-tests` / [#28](https://github.com/The-Allsparks/TRACE/pull/28) | pending | not authorized | Wait CI, human merge |
| #17 | HIGH | Ready | Queued | — | — | — | After #16 |
| #18–#22 | MEDIUM | Ready | Queued | — | — | — | After #16 |
| #23 | HIGH | Blocked | Settings | — | — | — | Human: branch protection |
| #13 | HIGH | Blocked | Hardware | — | — | — | Control Hub |
| #6 / #9 | Gates | Blocked | Approval | — | — | — | Do not implement #7/#10 |
| Dependabot #24 | LOW | Blocked | JUnit 5→6 | — | — | — | Do not merge without Java 11 analysis |

## Ready queue

1. **#16** — in PR (selected)
2. #17 pre-fault buffer honesty
3. #18 metadata / git spawn
4. #19 quota prefix scoping
5. #20 OFF allocation
6. #22 pin Actions
7. #21 drop-summary race

## Stop conditions

* `AUTOMATIC_MERGE=false` — orchestrator does not merge #16 without human approval.
* #13 hardware unavailable here.
* Phase 4 / replay gates closed.
* Dependabot #24 (JUnit 6) out of scope.
