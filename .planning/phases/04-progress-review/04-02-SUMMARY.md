---
phase: 04-progress-review
plan: 02
subsystem: progress
tags: [progress, datastore, okio, persistence, home-header]
requires: [04-01 Progress/applyLessonResult/visibleStreak]
provides: [ProgressSerializer, progressStore, ProgressRepository, localToday, AppGraph, App(dataDir), HomeScreen(progress, today, onStart), LessonViewModel onFinish]
affects: [04-03 Home statuses/review (reads progress.lessons), 04-04 device walkthrough]
tech-stack:
  added: []
  patterns: [typed Okio DataStore with ReplaceFileCorruptionHandler, repository-owned SupervisorJob write scope, process-singleton AppGraph]
key-files:
  created:
    - shared/src/commonMain/kotlin/com/tlenparty/algolingo/progress/ProgressRepository.kt
    - shared/src/commonTest/kotlin/com/tlenparty/algolingo/progress/ProgressRepositoryTest.kt
  modified:
    - shared/src/commonMain/kotlin/com/tlenparty/algolingo/lesson/LessonViewModel.kt
    - shared/src/commonMain/kotlin/com/tlenparty/algolingo/lesson/ui/LessonHost.kt
    - shared/src/commonMain/kotlin/com/tlenparty/algolingo/App.kt
    - shared/src/commonMain/kotlin/com/tlenparty/algolingo/home/HomeScreen.kt
    - shared/src/iosMain/kotlin/com/tlenparty/algolingo/MainViewController.kt
    - androidApp/src/main/kotlin/com/tlenparty/algolingo/MainActivity.kt
decisions:
  - "Typed OkioStorage DataStore works on iosSimulatorArm64, so the Preferences DataStore fallback was not needed"
  - "Progress is recorded on the non-Done to Done transition in the ViewModel, so an abandoned lesson (ExitGuard) records nothing"
metrics:
  duration: ~12min
  completed: 2026-10-22
  tasks: 2
  files: 8
---

# Phase 4 Plan 02: Progress Persistence and Home Header Summary

Progress is now stored in a typed DataStore (Okio) at `<dataDir>/progress.json`. There is one instance per process, held by `AppGraph`. Writes run in an app-lifetime `SupervisorJob` scope, and a corrupt file resets to zero. When a lesson reaches Done, the result is recorded exactly once. Home shows a header with `🔥 streak` on the left and `⭐ XP` on the right.

## Tasks

| Task | Name | Commit |
|------|------|--------|
| 1 | RED: failing ProgressRepository tests | db873d4 |
| 1 | GREEN: DataStore progress repository | 2ca1f3d |
| 2 | Record on Done, dataDir from platforms, Home header | 2d7c393 |

## Verification

- `:shared:allTests :shared:linkDebugFrameworkIosSimulatorArm64 :design:compileKotlinIosSimulatorArm64 :androidApp:assembleDebug` is green. ProgressRepositoryTest ran 7 tests with 0 failures on testAndroidHostTest and on iosSimulatorArm64Test. That includes the real-file restart test and the corrupt-file reset test.
- iOS `xcodebuild` (sim 4461DE0A..., CODE_SIGNING_ALLOWED=NO) printed BUILD SUCCEEDED.
- The literal gate is clean. There is no `viewModelScope` in progress code, and there is no `kotlinx.datetime.Clock`.
- emulator-5554 smoke check (`adb -s emulator-5554 install -r`): Home shows «🔥 0» on the left and «⭐ 0» on the right (/tmp/04-02-home.png).

## TDD Gate Compliance

RED `test(04-02)` (db873d4) failed to compile with an unresolved ProgressRepository/ProgressSerializer. It came before GREEN `feat(04-02)` (2ca1f3d). No refactor was needed.

## Deviations from Plan

- The doc comment in ProgressRepository.kt says "ViewModel scope" instead of `viewModelScope`, so the plan's `! grep viewModelScope` gate stays meaningful.
- Once progress loads, the header appears and the centered path shifts down by half the header height. Accepted, because 04-03 rewrites the path block.

## Known Stubs

- HomeScreen still has the hard-coded single l0.4 node. This is intentional, and 04-03 replaces it with index.json and statuses.

## Self-Check: PASSED
