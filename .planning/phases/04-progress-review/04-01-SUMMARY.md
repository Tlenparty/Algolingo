---
phase: 04-progress-review
plan: 01
subsystem: progress
tags: [progress, srs, streak, xp, kotlinx-datetime, datastore]
requires: [lesson/Xp.kt xpFor, lesson/LessonReducer.kt Grade/LessonResult]
provides: [Progress, LessonProgress, encodeProgress, decodeProgress, schedule, bumpStreak, visibleStreak, applyLessonResult, INTERVALS]
affects: [04-02 ProgressRepository, Home header/statuses, review]
tech-stack:
  added: [androidx.datastore:datastore-core 1.2.1, androidx.datastore:datastore-core-okio 1.2.1, org.jetbrains.kotlinx:kotlinx-datetime 0.8.0, org.jetbrains.kotlinx:kotlinx-coroutines-test 1.11.0]
  patterns: [pure domain functions with injected today LocalDate, versioned JSON document with encodeDefaults + ignoreUnknownKeys]
key-files:
  created:
    - shared/src/commonMain/kotlin/com/tlenparty/algolingo/progress/Progress.kt
    - shared/src/commonTest/kotlin/com/tlenparty/algolingo/progress/ProgressTest.kt
  modified:
    - gradle/libs.versions.toml
    - shared/build.gradle.kts
    - CLAUDE.md
decisions:
  - "minSdk raised 24 -> 26 instead of core library desugaring (kotlinx-datetime uses java.time)"
  - "SRS moves only on first pass and due/overdue reviews; replays before due or of mastered lessons keep step/due but still earn XP and streak (A2)"
  - "Streak keeps the max lastActiveDay, so a clock moved back changes nothing"
metrics:
  duration: ~10min
  completed: 2026-10-22
  tasks: 2
  files: 5
---

# Phase 4 Plan 01: Progress Engine Summary

A pure progress engine: one versioned `@Serializable Progress` document with a codec that returns null on garbage, plus XP (it reuses `xpFor`), a local-calendar-day streak, and Leitner 1/3/7 SRS. All of it takes an injected `today: LocalDate` and is covered by 21 fake-date tests that pass on the Android host and on iosSimulatorArm64.

## Tasks

| Task | Name | Commit |
|------|------|--------|
| 1 | Package legitimacy gate | n/a (user approval) |
| 2 | Deps + minSdk 26 | 8c1dd4d |
| 2 | RED: failing ProgressTest | b6db05c |
| 2 | GREEN: Progress.kt | 5e062e1 |
| 2 | Fix: clamp Hinted index | cabbfa1 |

## Package Legitimacy Gate (Task 1)

The orchestrator resolved this gate before execution started. It checked that the POMs for all four coordinates exist on Google Maven and Maven Central with official project URLs. The user approved them ("Да, добавляй"):
- androidx.datastore:datastore-core 1.2.1
- androidx.datastore:datastore-core-okio 1.2.1
- org.jetbrains.kotlinx:kotlinx-datetime 0.8.0
- org.jetbrains.kotlinx:kotlinx-coroutines-test 1.11.0

No desugar_jdk_libs was added.

## Verification

- `./gradlew :shared:allTests :androidApp:assembleDebug` is green. ProgressTest ran 21 tests with 0 failures on both testAndroidHostTest and iosSimulatorArm64Test.
- The grep gate finds no `kotlinx.datetime.Clock/Instant` and no `86400` in commonMain.
- Progress.kt has no compose, datastore or `kotlin.time.Clock` imports.
- `android-minSdk = "26"` is set and there is no desugar entry. The CLAUDE.md Android SDK row says minSdk **26**.

## TDD Gate Compliance

The RED commit `test(04-01)` (b6db05c) came before the GREEN commit `feat(04-01)` (5e062e1). The RED run failed to compile with an unresolved `Progress`. No refactor was needed.

## Deviations from Plan

- The dependency and catalog changes went into their own `chore(04-01)` commit (8c1dd4d), separate from the RED test commit, so each commit holds one kind of change. The content matches the plan.
- `schedule` uses `prev.step >= INTERVALS.lastIndex` rather than `==`, so an out-of-range step from a hand-edited file becomes mastered instead of throwing IndexOutOfBounds. The Hinted branch clamps its index the same way (cabbfa1) (T-04-02 hardening, [Rule 2]).

## Known Stubs

None.

## Self-Check: PASSED
