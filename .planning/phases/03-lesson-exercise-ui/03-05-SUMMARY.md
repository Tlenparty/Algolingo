---
phase: 03-lesson-exercise-ui
plan: 05
subsystem: lesson-screens
tags: [compose, theory, exercise, result-panel]
requires: [03-01, 03-02, 03-04]
provides: [ResultPanel, TheoryScreen, ExerciseScreen, richText]
key-files:
  created:
    - shared/src/commonMain/kotlin/com/tlenparty/algolingo/ui/components/ResultPanel.kt
    - shared/src/commonMain/kotlin/com/tlenparty/algolingo/lesson/ui/TheoryScreen.kt
    - shared/src/commonMain/kotlin/com/tlenparty/algolingo/lesson/ui/ExerciseScreen.kt
decisions:
  - richText(s) = inlineText(parseInline(s)) helper lives in ResultPanel.kt (internal), shared by both screens
  - HapticFeedbackType.Confirm/Reject compile in CMP 1.12.2, no LongPress fallback needed
metrics:
  tasks: 2
  completed: 2026-10-21
---

# Phase 3 Plan 05: Theory, Exercise, ResultPanel Summary

Stateless renderers of LessonState: theory card (hides empty sections), exercise screen (key(id) bank chips with ghosts, Check enabled only when all holes filled, hole recolor, hint cards derived from state.hintsShown, haptics) and a ResultPanel (Success/Amber/Danger) that replaces the bank/Check section with a 250ms slide+fade. Both targets compile; host tests green.

## Commits
- feat(03-05): ResultPanel and TheoryScreen
- feat(03-05): ExerciseScreen

## Deviations from Plan
- Exit animation of the panel is not played: the panel replaces the bank section directly when phase leaves Feedback (the host switches screens/play anyway). Enter animation (250ms slide+fade) is implemented via AnimatedVisibility with a MutableTransitionState.
- Hint auto-scroll animates to the top of the scroll area (cards sit above the code), not to maxValue.
- No previews or UI tests; behavior is verified end to end in plans 07/08.

## Known Stubs
None.

## Self-Check: PASSED
