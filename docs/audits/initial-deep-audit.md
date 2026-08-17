# TRACE initial deep audit

| Field | Value |
|-------|--------|
| **Date of audit** | 2026-08-17 |
| **Audited commit SHA** | `55fa66625f55ff19fd44ba7e48b601046b44339e` |
| **Repository** | [The-Allsparks/TRACE](https://github.com/The-Allsparks/TRACE) |
| **Default branch** | `main` |
| **Audited ref** | `phase-0-3-passive-foundation` (open draft PR [#14](https://github.com/The-Allsparks/TRACE/pull/14)) |
| **Auditor identity** | `TA-C-GHill` |
| **Hardware validation performed** | None |

This audit inspects the current implementation and the original Phase 0–8 promise. Findings require evidence. Severity is not used to express preference.

---

## Executive summary

TRACE is an FTC-first **observational** telemetry, event, and flight-recording library. Phases 0–3 exist as a desktop-tested Java 11 library with research, ADRs, student docs, and CI. Phases 4–8 are designed and explicitly gated. The library does not command motors, servos, or mechanisms. `TraceMode.REPLAY` fails closed.

`main` currently contains only `LICENSE` and `README.md`. The implementation lives on PR #14. CI on that PR is green (Ubuntu test, Windows test, docs-structure). Branch protection is **not** enabled. Automatic merge is **not** authorized.

The original product promise is intact: TRACE records evidence; it does not absorb ViDAR, Pedro Pathing, AMPER, MIMIC, BEACON, or robot application ownership. No compile-time dependencies on those projects exist.

The largest gaps versus the Phase 0–3 promise are **lifecycle finalization**, **test honesty**, and **unconsumed pre-fault buffering** — not missing active-control features. Replay and adapters must stay gated until Control Hub recording is proven ([#13](https://github.com/The-Allsparks/TRACE/issues/13)).

**Highest-priority ready work:** closed sessions must reject further records and events, and `Trace.stop()` must leave a defined disabled state.

---

## Project purpose

TRACE records what the robot sensed, what software believed, what it decided, what it commanded, and which safety or control events occurred. Intended users are beginning students, advanced students, mentors, and downstream Allsparks libraries that may later emit into a small TRACE API.

It is **not** a vision system, path follower, power manager, mechanism safety framework, communications owner, or dashboard.

## Current maturity

| Claim | Evidence |
|-------|----------|
| Version `0.1.0-SNAPSHOT` | `build.gradle` |
| Phases 0–3 implemented on desktop | Source, tests, README status table |
| Phases 4–5 designed, integration gate | Issues [#6](https://github.com/The-Allsparks/TRACE/issues/6), [#7](https://github.com/The-Allsparks/TRACE/issues/7), [#8](https://github.com/The-Allsparks/TRACE/issues/8) |
| Phases 6–8 designed, replay gate | Issues [#9](https://github.com/The-Allsparks/TRACE/issues/9)–[#12](https://github.com/The-Allsparks/TRACE/issues/12) |
| Physical outputs never commanded | Facade and session APIs are record/export only; no hardware SDK types |
| Control Hub / robot / match untested | README, `docs/performance.md`, issue #13 |
| Not a published Maven release | No releases; SNAPSHOT version |

**Implementation phase:** Phase 3 desktop vertical slice, pending merge of PR #14 and hardware validation.

## Implemented capabilities

* `TraceMode.OFF`, `EVENTS`, `ESSENTIAL`, `FULL`
* Student facade `Trace` plus `TraceConfig` / `TraceSession` / `TraceCycle`
* Typed records (`TypedValue`, units, categories, priority, quality, severity)
* Bounded memory sink, optional console sink, composite fan-out
* Sampling / interval / optional change-based recording
* Async bounded `.tlog` writer, rotation, quotas, CRC records, truncation-tolerant reader
* Session metadata JSON header with graceful unknown Git
* CSV and human-readable export; lossy AdvantageScope list CSV
* SDK-free `OpModeLifecycle` and telemetry adapter interfaces
* Desktop `TraceInspect`
* Desktop examples compiled in CI
* Research, ADRs 0001–0010, student/mentor docs

## Documented but unimplemented capabilities

| Capability | Where promised | Status |
|------------|----------------|--------|
| Deterministic replay / IO substitution | `docs/replay.md`, Phase 7 | Fail-closed only |
| Project adapters (ViDAR/Pedro/AMPER/MIMIC/BEACON) | `docs/integrations.md`, Phase 4 | Interfaces only; no adapters |
| AdvantageScope-native WPILOG/RLOG/RR writers | README, Phase 5 | Lossy CSV list only |
| Control Hub measurements | `docs/performance.md` | Desktop smoke only |
| Object pooling | `docs/performance.md` | Not implemented |
| Rolling pre-fault buffer surviving a writer fault | `docs/storage.md`, issue #5 scope | Buffer is filled in RAM and never dumped, exported, or flushed to disk |
| `enabledIntegrations` / `featureFlags` behavior | `TraceConfig` | Stored in metadata only |

---

## Architecture findings

| ID | Severity | Type | Finding |
|----|----------|------|---------|
| A1 | INFORMATIONAL | ARCHITECTURE | Single Gradle Java library with package boundaries matches AMPER/MIMIC and ADR 0010. Future `trace-api` / `trace-storage` / `trace-replay` splits are correctly deferred. |
| A2 | LOW | ARCHITECTURE | `Trace` is a process-global singleton. That is the student facade, but `Trace.session()` exposes the live `TraceSession` (examples and `OpModeLifecycle` rely on it). |
| A3 | MEDIUM | ARCHITECTURE | `TraceSession` owns config, sampling, sinks, file writer, health, cycle numbering, and telemetry fan-out. Large, but still observational. Not a god-object for robot control. |
| A4 | INFORMATIONAL | ARCHITECTURE | Dependency direction is correct: TRACE does not depend on ViDAR, Pedro, AMPER, MIMIC, BEACON, FTC SDK, FTC Dashboard, or AdvantageScope at compile time (`build.gradle` test-only JUnit). |
| A5 | MEDIUM | ARCHITECTURE | `RollingPreFaultBuffer` is unreachable product behavior: offered records, public getter, no consumer. See C7. |
| A6 | LOW | ARCHITECTURE | `currentCycle` on `TraceSession` is written and never read. Nested `beginCycle()` is undefined. |

## Correctness findings

| ID | Severity | Type | Finding | Evidence |
|----|----------|------|---------|----------|
| C1 | HIGH | CORRECTNESS | After `close()`, `event()` does not check `open`. Further events can be published to closed sinks. `record()` uses `allows()`, which does check `open`. Asymmetric shutdown. | `TraceSession.event` vs `allows`; `Trace.stop()` only calls `session.close()` and does not install `TraceConfig.off()`. |
| C2 | HIGH | TESTING | `WriterFailureAndQuotaTest.writerFailureIsAccounted` asserts `writerFailed \|\| dropped > 0 \|\| accepted >= 0`. The last clause is always true. | `WriterFailureAndQuotaTest.java` |
| C3 | MEDIUM | CORRECTNESS | `ProcessMetadataSource.buildTimestamp()` returns `Instant.now()` when the env var is unset, so `SessionMetadata.collect` marks `buildInfoAvailable=true` with a session-start wall clock, not a build stamp. | `ProcessMetadataSource.java`, `SessionMetadata.collect` |
| C4 | MEDIUM | CORRECTNESS | `AsyncBoundedWriter.noteDrop` mutates `pendingDropCount` / reason / category from the OpMode thread, including **outside** `lock`, while the writer thread reads them in `maybeEmitDropRecord`. | `AsyncBoundedWriter.accept` / `noteDrop` / `maybeEmitDropCount` |
| C5 | MEDIUM | CORRECTNESS | `FileRotator.enforceQuota` lists every `*.tlog` in the directory, not this session prefix. A shared folder can delete another session's logs. | `FileRotator.listTraceFiles` |
| C6 | MEDIUM | CORRECTNESS | `flush()` only `notifyAll()`. `close()` joins with a 250 ms timeout then `closeQuietly()` from both the caller and the writer `finally`. Timeout can close the stream under a live writer. | `AsyncBoundedWriter.flush` / `close` / `run` |
| C7 | HIGH | CORRECTNESS | Rolling pre-fault buffer never leaves RAM. Power-loss truncation recovery is the `.tlog` reader, not this buffer. Phase 3 scope overclaims. | Grep: `rollingBuffer` only in writer + buffer class |
| C8 | LOW | CORRECTNESS | `StorageAndTruncationTest` corrupt path asserts `corruptRecords() >= 0`, which cannot fail. | test file |
| C9 | LOW | CORRECTNESS | `TlogReader` continues after CRC failure assuming length was honest. A corrupted length that still looks in-range can desynchronize the remainder. Bounded by `MAX_RECORD`. | `TlogReader.read` |
| C10 | LOW | CORRECTNESS | Change-based sampling uses exact `Pose2d.equals`, not the configured epsilon. | `TypedValue.approximatelyEquals` POSE2D branch |
| C11 | MEDIUM | CORRECTNESS | TLOG header JSON has length but no CRC. Truncated headers fail hard; bitflipped headers may parse silently. | `TlogCodec.encodeHeader` |
| C12 | LOW | CORRECTNESS | `TraceMode.recordsEvents()` includes `REPLAY` even though REPLAY cannot be constructed. Harmless today. | `TraceMode.java` |

Do not treat C1 as a robot-safety defect: TRACE cannot command hardware. It is a recording-integrity defect in the Phase 3 finalization promise.

## Safety findings

| ID | Severity | Type | Finding |
|----|----------|------|---------|
| S1 | INFORMATIONAL | SAFETY | No motor/servo/DcMotor/Servo types or SDK hardware calls in `src/main`. Passive modes cannot command hardware. |
| S2 | INFORMATIONAL | SAFETY | `TraceConfig.Builder.mode(REPLAY)` throws. Documented fail-closed. |
| S3 | INFORMATIONAL | SAFETY | Replay cannot produce physical outputs because replay is unimplemented. |
| S4 | LOW | SAFETY | `SECURITY.md` asks reporters to email a contact that is not published. GitHub Security Advisories are mentioned "when available." |
| S5 | INFORMATIONAL | SAFETY | Logging of secrets is policy (`SECURITY.md`, schema naming rules), not an automated redaction filter. Appropriate for Phase 3. |
| S6 | MEDIUM | SAFETY | Console sink writes on the calling thread. If enabled in a match loop, `PrintStream` can stall control. Optional and off by default. |

No path was found that energizes hardware or bypasses the replay gate.

## Performance findings

| ID | Severity | Type | Finding | Measured? |
|----|----------|------|---------|-----------|
| P1 | HIGH | PERFORMANCE | Control Hub loop cost, allocation, writer throughput, and SD stalls are **unknown**. Desktop smoke only. | No. Issue #13 exists. |
| P2 | MEDIUM | PERFORMANCE | `TraceConfig.off()` still defaults `memorySink=true`, so OFF sessions allocate a 2048-slot deque. | Predicted from constructor |
| P3 | MEDIUM | PERFORMANCE | `beginCycle()` emits `TRACE/Loop/Begin` as an unsampled HIGH event every loop when events are enabled. | Code path; desktop smoke uses ESSENTIAL |
| P4 | MEDIUM | PERFORMANCE | `ProcessMetadataSource` may spawn `git` twice with a 400 ms wait each during session construction. On a hub without git this should fail fast; if `git` exists and hangs, init stalls. | Predicted |
| P5 | LOW | PERFORMANCE | Per-record allocation (immutable records, several `ByteBuffer`s in the codec). Documented; pooling deferred. | Predicted |
| P6 | LOW | PERFORMANCE | Queue eviction scans the full `ArrayDeque` on the calling thread when full. | Predicted |
| P7 | INFORMATIONAL | PERFORMANCE | Desktop smoke: `PerformanceSmokeTest` asserts &lt; 5 ms/loop for 5000 essential cycles. PR #14 reports ~135 µs/loop on a development JVM. **Not** Control Hub. | Desktop only |

## API/usability findings

| ID | Severity | Type | Finding |
|----|----------|------|---------|
| U1 | INFORMATIONAL | USABILITY | `Trace.record` always stores `OUTPUT` by design (`docs/data-model.md`). Beginners can graph without IO vocabulary. |
| U2 | LOW | USABILITY | Phase 2 student checkpoint asks students to classify a CSV row as input/output/event while the quick-start `Trace.record` path always emits OUTPUT. Mentor guide should call this out more loudly. |
| U3 | LOW | USABILITY | Pose helpers default `Units.METERS`. `Units.INCHES` exists. FTC teams often use inches; docs should show an explicit unit at first pose example. |
| U4 | INFORMATIONAL | USABILITY | Progressive adoption is real: OFF → events → essential → file sink. Examples are independent. |
| U5 | LOW | USABILITY | `Trace.session()` is required for `OpModeLifecycle`. Fine for mentors; beginners copy it from examples. |

## Testing findings

| ID | Severity | Type | Finding |
|----|----------|------|---------|
| T1 | HIGH | TESTING | Writer-failure test cannot fail (C2). |
| T2 | MEDIUM | TESTING | No test that `close()` / `Trace.stop()` rejects subsequent events and records, or that `health().enabled()` becomes false after stop. |
| T3 | LOW | TESTING | Corrupt-record assertion cannot fail (C8). Shutdown test accepts `accepted >= 1 \|\| !isOpen()`. |
| T4 | INFORMATIONAL | TESTING | Coverage that **does** exist and is meaningful: OFF/EVENTS filtering, clock/cycle, sampling, schema round-trip, concurrent accounting, truncation prefix, metadata JSON, example compilation, doc links, Javadoc, desktop performance bound. |
| T5 | INFORMATIONAL | TESTING | Tests run in CI on Ubuntu and Windows. They do not require hardware. None are disabled. |
| T6 | MEDIUM | TESTING | No test that quota deletion is limited to the current session prefix. |
| T7 | INFORMATIONAL | TESTING | Tests assert some implementation details (human-readable substring matching) but generally check observable behavior. |

## Documentation findings

| ID | Severity | Type | Finding |
|----|----------|------|---------|
| D1 | INFORMATIONAL | DOCUMENTATION | README, CONTRIBUTING, SECURITY, CODE_OF_CONDUCT, LICENSE, architecture, schema, storage, replay, integrations, student path, mentor guide, troubleshooting, research, ADRs are present and mostly match the code. |
| D2 | MEDIUM | DOCUMENTATION | `docs/storage.md` describes a rolling pre-fault buffer as a writer feature without saying it is RAM-only and unused on failure. |
| D3 | LOW | DOCUMENTATION | Issue #1 still has an unchecked "child issues exist" box; children #2–#12 exist. |
| D4 | INFORMATIONAL | DOCUMENTATION | Maturity language is honest: desktop-tested, not match-proven. |
| D5 | LOW | DOCUMENTATION | No `docs/audits/` existed before this audit. CI `docs-structure` does not require audit files yet. |

## Dependency findings

| ID | Severity | Type | Finding |
|----|----------|------|---------|
| Dep1 | INFORMATIONAL | SECURITY | Runtime dependencies: none. Test: JUnit 5.10.2 BOM. Low supply-chain surface. |
| Dep2 | MEDIUM | SECURITY | GitHub Actions use floating tags `actions/checkout@v4` and `actions/setup-java@v4`, not commit SHAs. Dependabot watches Actions monthly. |
| Dep3 | INFORMATIONAL | COMPATIBILITY | Java 11 source/target; CI Temurin 17. Matches Allsparks convention (ADR 0010). |
| Dep4 | INFORMATIONAL | COMPATIBILITY | Gradle wrapper 8.7 is committed (`gradle-wrapper.jar` present, `!` exception in `.gitignore`). |
| Dep5 | INFORMATIONAL | SECURITY | No secrets in repo. `.env` gitignored. Custom JSON parser avoids Java deserialization of logs. |
| Dep6 | LOW | SECURITY | No Dependabot grouping for Gradle beyond monthly; only JUnit would update. Acceptable. |

## Repository-health findings

| ID | Severity | Type | Finding |
|----|----------|------|---------|
| R1 | HIGH | SECURITY | `main` is **not** branch-protected. No required checks, no required reviews, `deleteBranchOnMerge=false`. Merge methods: merge commit, squash, and rebase all allowed. |
| R2 | INFORMATIONAL | INTEGRATION | Draft PR #14 implements Phases 0–3. CI green. No reviews. Intentionally not using `Closes` because #13 remains. |
| R3 | HIGH | INTEGRATION | `main` is not a usable library until #14 merges. Continuing Phase 4+ on `main` is impossible; stacking more features onto #14 increases review risk. |
| R4 | INFORMATIONAL | DOCUMENTATION | Issue templates, PR template, CODE_OF_CONDUCT, Dependabot, phase labels, and milestones 1–9 exist. |
| R5 | LOW | DOCUMENTATION | Missing labels for audit severity/readiness (`severity:*`, `status:ready` / `blocked`). |
| R6 | LOW | SECURITY | No CODEOWNERS. |
| R7 | INFORMATIONAL | INTEGRATION | No GitHub releases. Changelog has `[Unreleased]` only. Appropriate for SNAPSHOT. |
| R8 | INFORMATIONAL | INTEGRATION | Identity `TA-C-GHill` is authenticated with `repo` and `workflow` scopes; admin permission on this repository. |

## Cross-project integration findings

| ID | Severity | Type | Finding |
|----|----------|------|---------|
| X1 | INFORMATIONAL | INTEGRATION | Conceptual chain ViDAR/Pedro/AMPER/MIMIC/BEACON → TRACE → HELM is documented. TRACE does not compile against them. |
| X2 | INFORMATIONAL | INTEGRATION | Signal-name recommendations exist in `docs/schema.md`. Adapters are Phase 4 and correctly blocked on issue #6. |
| X3 | LOW | INTEGRATION | `enableIntegration` only records a string in metadata. Teams cannot accidentally activate AMPER/MIMIC behavior by setting it. Good. |
| X4 | INFORMATIONAL | INTEGRATION | No circular dependency with other Allsparks repos in this tree. Other repos were not modified. |

---

## Readiness assessment

| Gate | Status |
|------|--------|
| Phase 0–3 desktop software | Implemented on PR #14; not on `main` |
| Phase 3 Control Hub proof | **Not met** (#13). Blocks match claims and later active phases |
| Integration approval (Phase 4) | **Not granted** (#6) |
| Replay approval (Phase 6) | **Not granted** (#9) |
| Release readiness | Not a release. No versioned artifact |
| Safety for observational use on a laptop | Acceptable after C1/C2 hardening |
| Safety for match recording | Not demonstrated |
| Safety for replay or output influence | Must remain disabled |

TRACE's own readiness slogan holds: **record reliably before enabling deterministic replay.** Recording is not yet reliable enough to call Phase 3 complete (C1, C7, #13).

## Recommended work order

1. Close the session-lifecycle hole (C1, T2) as a small vertical slice on the foundation branch.
2. Make writer-failure and quota tests able to fail (C2, T1).
3. Either persist/expose the pre-fault buffer or document it as unused RAM (C7, D2).
4. Metadata honesty and no surprise `git` spawn (C3, P4).
5. Merge authorization for PR #14 after review (**human**). Enable branch protection (R1).
6. Control Hub validation (#13) before any match-readiness language.
7. Pin Actions SHAs (Dep2).
8. Quota scope, flush semantics, OFF-mode allocation (C5, C6, P2).
9. Do **not** start Phase 4–8 until gates #6 / #9 and #13 policy are satisfied.

## Deferred or rejected ideas

| Idea | Disposition |
|------|-------------|
| Implement replay now because docs exist | **Rejected.** Gate #9. Fail-closed remains. |
| Add ViDAR/AMPER compile dependencies | **Rejected.** CONTRIBUTING rule 3. |
| Split the library into multiple published modules | **Deferred.** Empty modules would not clarify boundaries. |
| Object pooling / Disruptor queues | **Deferred** until Control Hub measurements exist. |
| Native AdvantageScope writers | **Deferred** to Phase 5. |
| Broad dependency upgrades | **Rejected.** No runtime deps to upgrade. |
| Treat `Trace.record` → OUTPUT as a bug | **Rejected.** Documented scaffolding (data-model.md). |
| Merge PR #14 from this orchestrator run | **Stopped.** `AUTOMATIC_MERGE` is false. |

## Evidence and references

* Repository metadata: `gh repo view The-Allsparks/TRACE`
* Branch protection: GitHub API 404 `Branch not protected`; rulesets `[]`
* PR #14: CI `test (ubuntu-latest)`, `test (windows-latest)`, `docs-structure` all `SUCCESS`
* Source: `src/main/java/org/allsparks/trace/**`
* Tests: `src/test/java/org/allsparks/trace/**`
* Docs: `README.md`, `docs/**`, `CONTRIBUTING.md`, `SECURITY.md`
* Issues: #1–#13 open; none closed at audit time
* `main` tree: `LICENSE`, `README.md` only

## Classification legend

* **BLOCKER** — Prevents safe or meaningful continued development.
* **CRITICAL** — Unsafe robot behavior, corrupt fundamental results, or invalidated architecture.
* **HIGH** — Materially affects reliability, integration, or major project goals.
* **MEDIUM** — Important, does not block the next safe vertical slice.
* **LOW** — Local improvement.
* **INFORMATIONAL** — Observation, no current action.

No BLOCKER or CRITICAL safety findings were identified in this observational Phase 0–3 tree.
