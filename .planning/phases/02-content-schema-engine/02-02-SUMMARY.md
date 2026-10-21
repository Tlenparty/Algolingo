---
phase: 02-content-schema-engine
plan: 02
subsystem: engine
tags: [exercise-engine, seeded-shuffle, duplicate-chips]
requires:
  - phase: 02-01
    provides: Model.kt schema
provides:
  - seedOf / seededShuffle (deterministic, JVM + iOS identical)
  - ExerciseState, initial, tapChip, tapHole, check, CheckResult, HoleResult, Verdict
affects: [02-03 reducer, phase 3 UI]
key-files:
  created:
    - shared/src/commonMain/kotlin/com/tlenparty/algolingo/lesson/SeededShuffle.kt
    - shared/src/commonMain/kotlin/com/tlenparty/algolingo/lesson/ExerciseEngine.kt
    - shared/src/commonTest/kotlin/com/tlenparty/algolingo/lesson/ShuffleTest.kt
    - shared/src/commonTest/kotlin/com/tlenparty/algolingo/lesson/EngineTest.kt
    - shared/src/commonTest/kotlin/com/tlenparty/algolingo/lesson/DuplicateChipTest.kt
key-decisions:
  - "kotlin.random.Random(Int) parity (A1) confirmed: no LCG fallback needed"
  - "tapHole on a filled hole returns the chip and selects it (Q1)"
requirements-completed: [EXER-05]
duration: 10min
completed: 2026-10-21
---

# Phase 2 Plan 02: Engine and seeded shuffle Summary

**Pure hole-filling engine (index-based chip identity, per-hole Ok/Acceptable/Wrong check) plus a hand-written Fisher-Yates shuffle whose golden passes on JVM and iOS simulator.**

## Accomplishments
- Shuffle golden for seed 42 over 0..9 = `[5, 0, 4, 9, 2, 8, 1, 7, 6, 3]`, captured on JVM. It passes on `iosSimulatorArm64Test` (the simulator actually ran: ShuffleTest 4 tests, EngineTest 9, DuplicateChipTest 2, 0 failures). Assumption A1 holds.
- Engine never throws; out-of-range ids/holes return the state unchanged. State stores only bank indices.

## Task Commits
1. Task 1: a0d67cd
2. Task 2: cf8e05d

## Hand-off for Phase 3
The "Filled + selected" visual state is unreachable: `tapHole` on a filled hole clears it (the chip returns to the bank) and selects the hole. It can be dropped from the UI.

## Deviations from Plan
None. (Gradle needed `./gradlew --stop` once because of a stale proxy daemon.)

## Known Stubs
None.

## Self-Check: PASSED
