---
phase: 03-lesson-exercise-ui
plan: 06
subsystem: lesson-screens
tags: [compose, reveal, summary]
requires: [03-01, 03-02, 03-04]
provides: [RevealScreen, SummaryScreen]
requirements: [EXER-10, LESS-06]
key-files:
  created:
    - shared/src/commonMain/kotlin/com/tlenparty/algolingo/lesson/ui/RevealScreen.kt
    - shared/src/commonMain/kotlin/com/tlenparty/algolingo/lesson/ui/SummaryScreen.kt
decisions:
  - Reveal gating (A2): headline + highlighted reference on every reveal; Сложность/Грабли/Вариации only when isLastExercise(state); empty lists hide their heading
  - XP numbers come from xpFor, never literals
metrics:
  tasks: 2
  completed: 2026-10-21
---

# Phase 3 Plan 06: Reveal and Summary Summary

RevealScreen (headline with xpText-colored «+N XP» or «❌ Разберём решение», CodeBlock with solutionSpans highlights, last-exercise-only complexity/pitfalls/variations, null-safe when exercise is missing) and SummaryScreen (64sp grade emoji, title/subtitle per UI-SPEC grade table, total XP via xpFor(result), Продолжить button). Both targets compile; host tests green.

## Commits
- fdd000c feat(03-06): add RevealScreen
- bd1d3c1 feat(03-06): add SummaryScreen

## Deviations from Plan
None - plan executed exactly as written.

## Self-Check: PASSED
