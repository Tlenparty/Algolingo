---
phase: 03-lesson-exercise-ui
plan: 04
subsystem: ui-components
tags: [compose, components]
requires: [03-01, 03-02]
provides: [ChunkyButton, ChunkyStyle, TokenChip, ChipState, LessonProgressBar, LessonTopBar, CodeTemplate, CodeBlock]
key-files:
  created:
    - shared/src/commonMain/kotlin/com/tlenparty/algolingo/ui/components/ChunkyButton.kt
    - shared/src/commonMain/kotlin/com/tlenparty/algolingo/ui/components/TokenChip.kt
    - shared/src/commonMain/kotlin/com/tlenparty/algolingo/ui/components/LessonProgressBar.kt
    - shared/src/commonMain/kotlin/com/tlenparty/algolingo/ui/components/LessonTopBar.kt
    - shared/src/commonMain/kotlin/com/tlenparty/algolingo/ui/components/CodeTemplate.kt
    - shared/src/commonMain/kotlin/com/tlenparty/algolingo/ui/components/CodeBlock.kt
decisions:
  - Internal ChunkyBox in ChunkyButton.kt is the shared 3D press primitive reused by TokenChip
metrics:
  tasks: 2
  completed: 2026-10-21
---

# Phase 3 Plan 04: Shared UI components Summary

Six stateless components per UI-SPEC: 3D ChunkyButton (4 styles, 80ms press) and TokenChip (ghost keeps size), progress bar, top bar (hint button omitted when hintsRemaining is null), CodeTemplate (single horizontal scroll, 40dp line, 40x48 hole touch area over a 32dp face, getOrNull for hole data) and CodeBlock (clamped highlight ranges). Both targets compile; host tests green.

## Commits
- 02f7f34 ChunkyButton, TokenChip, LessonProgressBar, LessonTopBar
- CodeTemplate, CodeBlock (second feat commit)

## Deviations from Plan
None of substance. Added an internal ChunkyBox helper (shared by button and chip) instead of duplicating the press logic. No previews added (optional). Visual check on a device is left to the screen plans.

## Known Stubs
None.

## Self-Check: PASSED
