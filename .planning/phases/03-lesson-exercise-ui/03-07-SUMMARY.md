---
phase: 03-lesson-exercise-ui
plan: 07
subsystem: lesson-flow
tags: [compose, navigation, back-handler, vertical-slice]
requires: [03-01, 03-03, 03-05, 03-06]
provides: [LessonScreen, LessonHost, HomeScreen, LessonRoute]
requirements: [LESS-02, LESS-04, LESS-06, EXER-09, UI-01]
key-files:
  created:
    - shared/src/commonMain/kotlin/com/tlenparty/algolingo/lesson/ui/LessonScreen.kt
    - shared/src/commonMain/kotlin/com/tlenparty/algolingo/lesson/ui/LessonHost.kt
    - shared/src/commonMain/kotlin/com/tlenparty/algolingo/home/HomeScreen.kt
  modified:
    - shared/src/commonMain/kotlin/com/tlenparty/algolingo/App.kt
    - shared/src/commonMain/kotlin/com/tlenparty/algolingo/lesson/ui/ExitGuard.kt
    - shared/src/commonMain/kotlin/com/tlenparty/algolingo/ui/components/ChunkyButton.kt
decisions:
  - AnimatedContent targets the whole LessonState with contentKey = screenKey, so the outgoing page keeps its own snapshot (no stale/new-phase flash) and Solving->Feedback updates in place
  - Summary has no top bar and the exit BackHandler is disabled there; NavHost pops straight to Home
  - ChunkyBox uses propagateMinConstraints so full-width buttons get a full-width face (wrap-content chips unaffected)
metrics:
  tasks: 2
  completed: 2026-10-21
---

# Phase 3 Plan 07: Lesson Flow Wiring Summary

The vertical slice now works end to end. Home shows the single l0.4 node with a «Начать» pill. Tapping it opens LessonHost, which loads the lesson and shows an error screen on failure. LessonScreen then plays theory, exercises, reveal and summary with a top bar, a 200ms slide+fade transition and an exit guard, and finishes back on Home. The bottom bar is hidden on LessonRoute, and HomeStub is removed.

## Commits
- f659fe4 feat(03-07): add LessonScreen and LessonHost
- dc5cde6 fix(03-07): make ChunkyBox face honor fixed/full width
- 779641d feat(03-07): wire Home «Начать» to the lesson route

## Emulator smoke (emulator-5554 only; user's phone untouched)
Full lesson walked from Home back to Home:
- Home: node ▶ with ledge, «Начать» pill and caption «0.4 Бинарный поиск»; bottom nav visible. No header and no «Повтор сегодня», as planned.
- «Начать» opens theory card 1 («Когда бинпоиск»). The bottom nav is hidden, the top bar shows ✕ and the progress bar, and the 💡 slot is a spacer.
- **BACK key on a theory card shows «Выйти из урока?».** This is the Android half of the back-handling spike and it passes. «Остаться» closes the dialog and keeps the card.
- Theory cards 2 («Каркас») and 3 («Грабли») display, then e1 with 💡1.
- e1 answered wrong (`lo < hi`): the hole turns red, the «❌ Не совсем» panel shows the why text and the correct answer, and 💡 is greyed. «Понятно, дальше» requeues e1.
- e2 (💡2): the hint shows the «Подсказка 1 из 2» card and the counter drops to 1. Correct answer gives «✅ Отлично!», then Reveal «✅ Решено! +5 XP» with the highlighted reference.
- Progress animates to 1/3, then e3 (4 holes) is solved, then 2/3 and the requeued e1 comes back with a fresh hole and a reshuffled bank. Solving it shows the final reveal with «Сложность» and «Грабли».
- Summary: 💡 «Решено с подсказкой», +20 XP. **BACK on the summary goes straight Home with no dialog.**
- Restarting the lesson gives a fresh state. ✕ → «Выйти» goes back to Home.

Visual notes for the plan 08 human check (not fixed; outside this plan):
1. The code font renders ligatures: `<=` shows as `≤`, `!=` as `≠`, `==` as a long equals sign, `->` as an arrow, in chips, templates and reveal. Learners may not recognize the Kotlin source tokens. Disabling them (`fontFeatureSettings = "liga 0, calt 0"` on the code styles) is a one-liner if wanted.
2. CodeTemplate lines are spaced widely (about 44dp per line, every line as tall as a hole). In e3 the holes after `return`, `lo = mid +` and `hi = mid -` are clipped off the right edge, so you only reach them by scrolling horizontally. Filling still works because selection auto-advances, but you cannot see what you placed without scrolling.
3. On the reveal, the highlight on `lo <= hi` looks slightly offset from the ligature glyph.
4. The emulator was heavily loaded: «System UI isn't responding» came up repeatedly, partly triggered by `uiautomator dump`. This is the emulator, not the app; the app had no crash or ANR.

The iOS edge swipe-back has not been checked yet; it is still flagged for plan 08.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] ChunkyButton face did not fill its width**
- **Found during:** Task 2 emulator run
- **Issue:** ChunkyBox's outer Box loosened min constraints, so with `fillMaxWidth` the green face only wrapped its label (the «Дальше» face covered just the text, with the ledge across the full width). The dialog's «Остаться» had the same problem.
- **Fix:** `Box(..., propagateMinConstraints = true)` in ChunkyBox. Wrap-content callers (chips, Home pill/node) are unchanged.
- **Files modified:** ui/components/ChunkyButton.kt
- **Commit:** dc5cde6

**2. [Plan-directed] ExitDialog «Остаться» restyled** as a Secondary ChunkyButton (140dp) per the LOCKED UI-SPEC. This was in ExitGuard.kt, which is not listed in files_modified (commit f659fe4).

## Known Stubs
- HomeScreen's lesson list is a hard-coded `listOf(HomeLesson("l0.4", ...))`. This is intentional per RESEARCH A6; Phase 4 replaces it with index.json and statuses.

## Self-Check: PASSED
