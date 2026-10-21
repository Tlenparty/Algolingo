---
phase: 02-content-schema-engine
plan: 03
subsystem: engine
tags: [reducer, queue, grade, hints]
requires:
  - phase: 02-02
    provides: ExerciseEngine, seedOf
provides:
  - LessonReducer (start/reduce/result), LessonState, Phase, LessonAction, Grade, LessonResult, MAX_REPEATS
affects: [phase 3 ViewModel, phase 4 progress]
key-files:
  created:
    - shared/src/commonMain/kotlin/com/tlenparty/algolingo/lesson/LessonReducer.kt
    - shared/src/commonTest/kotlin/com/tlenparty/algolingo/lesson/ReducerTest.kt
key-decisions:
  - "Acceptable verdict counts as solved and clean (Q3)"
  - "Per-play bank seed = seedOf(id, attempts) xor lesson seed"
requirements-completed: [LESS-05]
duration: 8min
completed: 2026-10-21
---

# Phase 2 Plan 03: Lesson reducer Summary

**Pure reducer: wrong exercises requeue at the end (max 3 attempts, then Reveal unsolved), reshuffled per attempt, with Clean/Hinted/Failed grade.**

## Task Commits
1. Task 1: 92f27d5

## Verification
`./gradlew :shared:allTests` green on testAndroidHostTest and iosSimulatorArm64Test (simulator ran).

## Hand-off for Phase 3
- API: `LessonReducer.start(lesson, seed)`, `reduce(state, action)`, `result(state)`; ViewModel = MutableStateFlow<LessonState> + reduce. Pick `seed` per play session.
- `Phase.Feedback` carries the `CheckResult` (per-hole Ok/Acceptable/Wrong with why/correct).
- `Phase.Reveal(solved)` is entered automatically after a solve or the 3rd miss; there is no ShowAnswer action.
- "Filled + selected" visual is unreachable (see 02-02).
- XP is not computed here (Phase 4); `LessonResult.perExercise` has attempts and hintsShown.
- `progress` = done / (done + queue.size); Hint is a no-op when `hints` is empty.
- Phase 3 must add the loader: `Res.readBytes("files/content/lessons/l0.4.json")` then `Json { ignoreUnknownKeys = true }.decodeFromString<Lesson>` (deferred from Phase 2).

## Deviations from Plan
None.

## Known Stubs
None.

## Self-Check: PASSED
