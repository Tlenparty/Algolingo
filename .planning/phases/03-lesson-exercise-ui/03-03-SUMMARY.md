---
phase: 03-lesson-exercise-ui
plan: 03
subsystem: lesson-plumbing
tags: [back-handler, loader, viewmodel]
requires: []
provides: [ExitBackHandler, ExitDialog, parseLesson, loadLesson, LessonViewModel]
affects: [Phase 3 lesson screens]
key-files:
  created:
    - shared/src/commonMain/kotlin/com/tlenparty/algolingo/lesson/ui/ExitGuard.kt
    - shared/src/commonMain/kotlin/com/tlenparty/algolingo/lesson/LessonRepository.kt
    - shared/src/commonMain/kotlin/com/tlenparty/algolingo/lesson/LessonViewModel.kt
    - shared/src/commonTest/kotlin/com/tlenparty/algolingo/lesson/LessonRepositoryTest.kt
  modified:
    - shared/build.gradle.kts
    - gradle/libs.versions.toml
decisions:
  - Used CMP common BackHandler (ui-backhandler 1.12.2, first-party), opt-in ExperimentalComposeUiApi; iOS swipe still to be verified on a screen (plans 07/08)
metrics:
  tasks: 2
  completed: 2026-10-21
---

# Phase 3 Plan 03: ExitGuard, loader, ViewModel Summary

Exit guard isolated in one file (BackHandler + AlertDialog with UI-SPEC copy), `parseLesson`/`loadLesson` returning Result with validate() errors, and a thin `LessonViewModel` over LessonReducer. Both targets compile; 4 loader tests pass.

## Commits
- a406238 ExitGuard + ui-backhandler dependency
- e9b644e LessonRepository, LessonViewModel, tests

## Deviations from Plan
- ui-backhandler was not on the classpath transitively (assumption A5 false); added via version catalog at 1.12.2.
- BackHandler compiles on Android and iOS but is marked deprecated ("Use NavigationEventHandler") and experimental. Kept per plan; if the iOS swipe is not intercepted in plans 07/08, swap only the ExitBackHandler body to the navigation-event API.
- Test fixture needed segments rebuilding to solution.code to satisfy validate().
- RED/GREEN committed together.

## Known Stubs
None.

## Self-Check: PASSED
