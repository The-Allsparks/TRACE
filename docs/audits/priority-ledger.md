# TRACE priority ledger

Living orchestrator view. Update after each issue/PR transition.

**Updated:** 2026-08-18  
**Main head:** `abd3d76` (PR #28 merged, closes #16)  
**Open implementation PR:** pending for #17  
**Automatic merge:** human said proceed for this cycle; still no blanket auto-merge of Dependabot or gated phases

## Current selection

| Field | Value |
|-------|--------|
| Selected issue | [#17](https://github.com/The-Allsparks/TRACE/issues/17) Document rolling pre-fault buffer as RAM-only |
| Why highest priority | Phase 3 overclaim (audit C7); honesty before more storage work |
| Why ready | #16 merged; Option A is docs + snapshot API; no hardware |
| Dependencies | None |
| Expected deliverable | Docs + `TraceSession.preFaultSnapshot()` + tests |
| Hardware required | No |

## Ledger

| Issue | Priority | Readiness | Status | Branch / PR | CI | Merge | Next action |
|-------|----------|-----------|--------|-------------|-----|-------|-------------|
| PR #14 | Foundation | **Merged** | Done | `main` | green | merged | — |
| #15 / #5 | HIGH | **Closed** | Done (desktop) | #14 | green | merged | Control Hub still #13 |
| #16 | HIGH | **Closed** | Done | [#28](https://github.com/The-Allsparks/TRACE/pull/28) | green | merged | — |
| **#17** | HIGH | **In progress** | Implemented | `docs/prefault-buffer-ram-only` | pending | — | Open PR |
| #18–#22 | MEDIUM | Ready | Queued | — | — | — | After #17 |
| #23 | HIGH | Partially done | Required CI on `main` exists | — | — | — | Confirm review policy |
| #13 | HIGH | Blocked | Hardware | — | — | — | Control Hub |
| #6 / #9 | Gates | Blocked | Approval | — | — | — | Do not implement #7/#10 |
| Dependabot #24–#27 | LOW | Blocked | Compatibility | — | — | — | Do not merge without analysis |

## Ready queue

1. **#17** — selected
2. #18 metadata / git spawn
3. #19 quota prefix scoping
4. #20 OFF allocation
5. #22 pin Actions
6. #21 drop-summary race

## Stop conditions

* #13 hardware unavailable here.
* Phase 4 / replay gates closed.
* Dependabot major bumps out of scope.
