---
phase: 03-lesson-exercise-ui
plan: 02
subsystem: lesson-ui-state
tags: [tdd, pure-functions, xp]
requires: []
provides: [xpFor, XP_CLEAN, XP_ASSISTED, holeVisual, panelModel, templateLines, solutionSpans, hintsRemaining, isLastExercise, screenKey]
affects: [Phase 3 lesson/exercise screens]
key-files:
  created:
    - shared/src/commonMain/kotlin/com/tlenparty/algolingo/lesson/Xp.kt
    - shared/src/commonMain/kotlin/com/tlenparty/algolingo/lesson/UiMapping.kt
    - shared/src/commonTest/kotlin/com/tlenparty/algolingo/lesson/XpTest.kt
    - shared/src/commonTest/kotlin/com/tlenparty/algolingo/lesson/UiMappingTest.kt
decisions:
  - XP clean 10 / assisted 5 / unsolved 0 (RESEARCH A1)
metrics:
  tasks: 2
  completed: 2026-10-21
---

# Phase 3 Plan 02: Pure UI mapping and XP Summary

Pure, Compose-free functions for XP, hole visuals, result panel content, template lines, reveal spans, hint counter and a stable per-play ScreenKey, all unit-tested on Android host and iOS simulator.

## Commits
- 15fd4b8 Xp.kt
- 6228013 UiMapping.kt

## Deviations from Plan
- Plan said l0.4 e3 template gives 12 lines; counting its segments gives 13 (the last segment adds 4 line breaks after hole 3). Test asserts 13. Plan number was a miscount.
- RED/GREEN were committed together per task, not as separate test/feat commits (tests written first, run only once green).
- The test uses a hand-built copy of the e3 segments rather than parsing the JSON; solutionSpans is checked on that fixture and a malformed exercise.

## Known Stubs
None.

## Self-Check: PASSED
