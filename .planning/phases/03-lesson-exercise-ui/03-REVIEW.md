---
phase: 03-lesson-exercise-ui
reviewed: 2026-10-21T00:00:00Z
depth: standard
files_reviewed: 33
files_reviewed_list:
  - design/build.gradle.kts
  - settings.gradle.kts
  - shared/build.gradle.kts
  - gradle/libs.versions.toml
  - design/src/commonMain/kotlin/com/tlenparty/algolingo/design/AlgoTheme.kt
  - design/src/commonMain/kotlin/com/tlenparty/algolingo/design/Highlight.kt
  - design/src/commonMain/kotlin/com/tlenparty/algolingo/design/components/AlgoCard.kt
  - design/src/commonMain/kotlin/com/tlenparty/algolingo/design/components/ChunkyButton.kt
  - design/src/commonMain/kotlin/com/tlenparty/algolingo/design/components/CodeBlock.kt
  - design/src/commonMain/kotlin/com/tlenparty/algolingo/design/components/LessonProgressBar.kt
  - design/src/commonMain/kotlin/com/tlenparty/algolingo/design/components/TokenChip.kt
  - shared/src/commonMain/kotlin/com/tlenparty/algolingo/App.kt
  - shared/src/commonMain/kotlin/com/tlenparty/algolingo/home/HomeScreen.kt
  - shared/src/commonMain/kotlin/com/tlenparty/algolingo/lesson/LessonRepository.kt
  - shared/src/commonMain/kotlin/com/tlenparty/algolingo/lesson/LessonViewModel.kt
  - shared/src/commonMain/kotlin/com/tlenparty/algolingo/lesson/UiMapping.kt
  - shared/src/commonMain/kotlin/com/tlenparty/algolingo/lesson/Xp.kt
  - shared/src/commonMain/kotlin/com/tlenparty/algolingo/lesson/ui/ExerciseScreen.kt
  - shared/src/commonMain/kotlin/com/tlenparty/algolingo/lesson/ui/ExitGuard.kt
  - shared/src/commonMain/kotlin/com/tlenparty/algolingo/lesson/ui/LessonHost.kt
  - shared/src/commonMain/kotlin/com/tlenparty/algolingo/lesson/ui/LessonScreen.kt
  - shared/src/commonMain/kotlin/com/tlenparty/algolingo/lesson/ui/RevealScreen.kt
  - shared/src/commonMain/kotlin/com/tlenparty/algolingo/lesson/ui/SummaryScreen.kt
  - shared/src/commonMain/kotlin/com/tlenparty/algolingo/lesson/ui/TheoryScreen.kt
  - shared/src/commonMain/kotlin/com/tlenparty/algolingo/ui/components/CodeTemplate.kt
  - shared/src/commonMain/kotlin/com/tlenparty/algolingo/ui/components/LessonTopBar.kt
  - shared/src/commonMain/kotlin/com/tlenparty/algolingo/ui/components/ResultPanel.kt
  - shared/src/commonMain/kotlin/com/tlenparty/algolingo/kb/ui/BlockView.kt
  - shared/src/commonMain/kotlin/com/tlenparty/algolingo/kb/ui/CalloutView.kt
  - shared/src/commonMain/kotlin/com/tlenparty/algolingo/kb/ui/KbListScreen.kt
  - shared/src/commonMain/kotlin/com/tlenparty/algolingo/kb/ui/NoteScreen.kt
  - shared/src/commonTest/kotlin/com/tlenparty/algolingo/lesson/LessonRepositoryTest.kt
  - shared/src/commonTest/kotlin/com/tlenparty/algolingo/lesson/UiMappingTest.kt
  - shared/src/commonTest/kotlin/com/tlenparty/algolingo/lesson/XpTest.kt
findings:
  critical: 1
  warning: 5
  info: 7
  total: 13
status: issues_found
fix_status: all_fixed
fix_report: .planning/phases/03-lesson-exercise-ui/03-REVIEW-FIX.md
---

# Phase 03: Code Review Report

**Reviewed:** 2026-10-21
**Depth:** standard
**Files Reviewed:** 33 (plus the reducer, engine, model and l0.4.json, read for context)
**Status:** issues_found

## Summary

The pure layer is tidy and covered by tests: UiMapping, Xp, LessonRepository and the CancellationException rethrow in `loadLesson` all hold up. The kb token migration is mechanical and I found nothing wrong in it.

The main defect is at the boundary between the UI and the reducer. RESEARCH line 149 states that "stray actions return the same state, so double taps are harmless". That holds for some actions but not for `NextTheory` or `Continue`. Those actions carry no identity, and `AnimatedContent` keeps the outgoing page clickable, so a fast double tap skips a theory card, a feedback panel or a Reveal screen. Smaller problems:

- M3 colour slots that the theme never sets fall back to the purple baseline.
- All three known follow-ups are still open, and each has a specific root cause (see WR-02, WR-03, WR-04).
- The loader does not catch every error it can hit.
- The design module's API has some hygiene issues.

## Fix Status

All 13 findings fixed on 2026-10-21 (see `03-REVIEW-FIX.md`).

| ID | Status | Commit |
|----|--------|--------|
| CR-01 | fixed: requires human verification | edae97a |
| WR-01 | fixed | e03e2d0 |
| WR-02 | fixed: requires human verification | 99d5264 |
| WR-03 | fixed | f51be1b |
| WR-04 | fixed | d9b1cb9 |
| WR-05 | fixed | b34e4ee |
| IN-01 | fixed | 51fb8d5 |
| IN-02 | fixed | 05e1eaf |
| IN-03 | fixed | 6d219ef |
| IN-04 | fixed | 63d09b2 |
| IN-05 | fixed (item 4 documented, not changed) | cd072fd |
| IN-06 | fixed | 4694d60 |
| IN-07 | fixed | 091d76f |

## Critical Issues

### CR-01: Double tap skips theory cards, feedback and Reveal (actions carry no identity, outgoing pages stay interactive)

**Files:**
- `shared/src/commonMain/kotlin/com/tlenparty/algolingo/lesson/ui/LessonScreen.kt:54-69`
- `shared/src/commonMain/kotlin/com/tlenparty/algolingo/lesson/ui/ExerciseScreen.kt:107,120-123`
- `shared/src/commonMain/kotlin/com/tlenparty/algolingo/lesson/LessonReducer.kt:75-114`

**Issue:** `LessonAction.NextTheory` and `LessonAction.Continue` say nothing about which screen they came from. The reducer applies them to whatever the current phase is, and `vm::on` always dispatches against the latest state. The buttons that send them sit in the same bottom slot on consecutive screens. In addition, the outgoing `AnimatedContent` page keeps taking input for its 200 ms exit. Concrete failures:

- **Theory(0) → Theory(1):** a double tap on «Дальше» sends two `NextTheory` actions, so the user lands on Theory(2) or Solving and card 1 is never seen.
- **Feedback → Reveal:** a double tap on «Дальше» / «Понятно, дальше» sends `Continue` twice. The first moves to Reveal and the second moves past it. On the last exercise this also skips «Сложность», «Грабли» and «Вариации» and goes straight to the Summary.
- **Solving → Feedback:** the panel slides up into the slot where «Проверить» was. A second tap can hit the panel's button and discard the verdict before it can be read. With a Wrong verdict, the user never sees why.

The claim in RESEARCH that "double taps are harmless" is wrong for these transitions, and no test covers them.

**Fix:** guard at the single choke point, the reducer, so that every caller benefits. Make each action carry the screen it was issued from:

```kotlin
// LessonAction
data class NextTheory(val index: Int) : LessonAction
data class Continue(val key: ScreenKey) : LessonAction

// reducer
a is LessonAction.NextTheory && p is Phase.Theory && a.index == p.index -> ...
a is LessonAction.Continue && a.key == screenKey(s) && p is Phase.Feedback -> ...
a is LessonAction.Continue && a.key == screenKey(s) && p is Phase.Reveal -> ...

// UI: inside AnimatedContent { s -> ... } pass screenKey(s) / p.index from the snapshot
TheoryScreen(it, onNext = { on(LessonAction.NextTheory(p.index)) })
```

This also makes taps on the outgoing snapshot harmless, because its key is stale. For the Check → panel case, you can additionally ignore input while the panel enter animation is running (`visible.isIdle`). Add a reducer test: `NextTheory(0)` applied twice from Theory(0) must leave the state at Theory(1).

## Warnings

### WR-01: M3 colour slots the theme does not set fall back to the purple baseline (NavigationBar, AlertDialog)

**File:** `design/src/commonMain/kotlin/com/tlenparty/algolingo/design/AlgoTheme.kt:60-71`

**Issue:** `lightColorScheme(...)` and `darkColorScheme(...)` set only `primary`, `background`, `surface`, `surfaceVariant`, `outline` and the matching `on*` colours. Every other slot keeps the M3 baseline values, which are lavender or purple. That affects:

- the `NavigationBar` container (`surfaceContainer`) and its selected-pill indicator (`secondaryContainer`) in `App.kt:65-77`;
- the `AlertDialog` container (`surfaceContainerHigh`) and its scrim in `ExitGuard.kt:28`;
- the `OutlinedTextField` and `TextButton` states in the knowledge base.

All of these render off-palette next to the Duolingo greens and greys, in both themes.

**Fix:** set the container family explicitly in both schemes:

```kotlin
surfaceContainerLowest = bg, surfaceContainerLow = bg, surfaceContainer = bg,
surfaceContainerHigh = bg, surfaceContainerHighest = surfaceVariant,
secondaryContainer = <successTint>, onSecondaryContainer = <successText>,
secondary = primary, tertiary = primary,
```

### WR-02: Known follow-up: closing braces scroll away and never come back; Wrong holes off-screen are not revealed

**File:** `shared/src/commonMain/kotlin/com/tlenparty/algolingo/ui/components/CodeTemplate.kt:52,95-96`

**Issue:** All lines share one `horizontalScroll`. `Hole` calls `bringIntoView()` only when it becomes `EmptySelected`. Once that pulls a right-hand hole into view, nothing scrolls back: filling the last hole leaves no selection, so the scroll stays offset and the short `}` lines at column 0–8 stay out of view.

The reverse case is also unhandled. In Feedback, a `Wrong` or `Acceptable` hole that is off-screen to the right is never brought into view, so the red hole the panel is talking about can be invisible.

**Fix:** hoist the `ScrollState` into `CodeTemplate` and drive it from the visuals:

```kotlin
val hScroll = rememberScrollState()
val anySelected = visuals.any { it == HoleVisual.EmptySelected }
val firstBad = visuals.indexOfFirst { it == HoleVisual.Wrong }
LaunchedEffect(anySelected, firstBad) { if (!anySelected && firstBad < 0) hScroll.animateScrollTo(0) }
// and in Hole: LaunchedEffect(visual) { if (visual == EmptySelected || visual == Wrong) inView.bringIntoView() }
```

`l0.4.json` has a 45-character line (`nums[mid] < target  -> lo = mid + 1`), which overflows a 360dp phone at 14sp. Alternatively, shorten the padding before `->` in the content.

### WR-03: Known follow-up: ▶ on the Home node renders as a colour emoji ▶️ on iOS

**File:** `shared/src/commonMain/kotlin/com/tlenparty/algolingo/home/HomeScreen.kt:50`

**Issue:** U+25B6 is not in Nunito, so text falls back to a system font. On iOS that fallback is Apple Color Emoji, which only has the colour glyph. Appending U+FE0E alone may not fix it, because the fallback chain still picks the emoji font when no text font has the glyph.

**Fix:** draw the triangle so it does not depend on any font:

```kotlin
Canvas(Modifier.size(AlgoTheme.sizes.node / 3)) {
    drawPath(Path().apply { moveTo(0f, 0f); lineTo(size.width, size.height / 2); lineTo(0f, size.height); close() }, color = ink)
}
```

A quicker but riskier option is `Text("▶︎", fontFamily = AlgoTheme.typography.code)`; check first that JetBrains Mono contains the glyph.

### WR-04: Known follow-up: code in hints is not in the code font (content markup)

**Files:**
- `shared/src/commonMain/composeResources/files/content/lessons/l0.4.json` (hints of e1, e2 and e3)
- consumer: `ExerciseScreen.kt:89`

**Issue:** The renderer already supports this: `richText` maps backticks to the JetBrains Mono `InlineCode` span with no ligatures. The hint strings simply have no backticks, for example `пока lo <= hi.`, `mid = lo + (hi - lo) / 2` and `lo = mid + 1`. Nothing stops the generator from emitting more of these.

**Fix:** wrap the code in backticks in the JSON and in the generator. Optionally, add a cheap check in `validate()`: if a hint contains an `ok[0]` string of any hole and that string is not inside backticks, report it as a content error.

### WR-05: Lesson loader does not handle every failure; process death leaves the exit dialog over a restarted lesson

**Files:**
- `shared/src/commonMain/kotlin/com/tlenparty/algolingo/lesson/LessonRepository.kt:26-32`
- `shared/src/commonMain/kotlin/com/tlenparty/algolingo/lesson/ui/LessonHost.kt:29`
- `shared/src/commonMain/kotlin/com/tlenparty/algolingo/lesson/ui/LessonScreen.kt:34`

**Issue (loader):** `loadLesson` catches only `MissingResourceException`. Any other read failure, such as a platform `IOException` or an `IllegalStateException` from the resource reader, propagates out of `produceState` and crashes the app, even though `LessonErrorScreen` exists for exactly this case. Rethrowing `CancellationException` is already correct.

**Issue (process death):** Restarting the lesson after process death is an accepted decision (RESEARCH line 257). But `confirm` is `rememberSaveable`, while the lesson state is not saved. If the process dies while «Выйти из урока?» is open, it comes back as a dialog over a fresh lesson with a new seed. In the same area, `produceState` re-reads the file after every configuration change, so rotation briefly shows a blank `Box` before the ViewModel is reused.

**Fix:**

```kotlin
} catch (e: CancellationException) { throw e
} catch (e: Exception) { return Result.failure(e) }
```

Use plain `remember` for `confirm`, which matches the decision not to save the lesson. Optionally, cache the decoded `Lesson` in the ViewModel store so rotation does not reload it.

## Info

### IN-01: `@OptIn(ExperimentalLayoutApi::class)` on stable FlowRow

**File:** `shared/src/commonMain/kotlin/com/tlenparty/algolingo/lesson/ui/ExerciseScreen.kt:13,44`

**Issue:** `FlowRow` is stable in Compose 1.8+, and CLAUDE.md's "What NOT to Use" table lists this opt-in.

**Fix:** remove both the import and the annotation.

### IN-02: ResultPanelHost exit animation is unreachable, and its comment is wrong

**File:** `shared/src/commonMain/kotlin/com/tlenparty/algolingo/lesson/ui/ExerciseScreen.kt:105-107,131-140`

**Issue:** The host is removed by `if (check != null)`, so `exit = slideOut... + fadeOut` never runs. The panel survives the exit only because the outgoing `AnimatedContent` snapshot still holds a non-null `check`, not because of anything in this code, so the comment "Keep the last check during exit animation" describes something the code does not do. The fully qualified names (`androidx.compose.runtime.remember`, `androidx.compose.ui.unit.Dp`) are inconsistent with the imports used elsewhere.

**Fix:** delete the `exit` parameter, fix the comment, and import the types normally.

### IN-03: Haptic feedback replays on configuration change

**File:** `shared/src/commonMain/kotlin/com/tlenparty/algolingo/lesson/ui/ExerciseScreen.kt:61-67`

**Issue:** `LaunchedEffect(check)` runs again whenever the composition is recreated. Rotating the device during Feedback vibrates the phone again.

**Fix:** key the haptic on something saved, for example `rememberSaveable(screenKey) { mutableStateOf(false) }` as a fired flag. Alternatively, accept the replay and note it.

### IN-04: Leftover `Hello`, `parseHello` and `loadHello` dead code, plus the `files/hello.json` asset

**File:** `shared/src/commonMain/kotlin/com/tlenparty/algolingo/App.kt:32-39`

**Issue:** `HomeStub`, their only caller, was removed in this phase. Only `HelloTest` still references them. The JSON asset still ships in both apps.

**Fix:** delete the class, both functions, `HelloTest.kt` and `files/hello.json`.

### IN-05: Design module API hygiene

**Files:**
- `design/build.gradle.kts:28-34`
- `design/src/commonMain/kotlin/com/tlenparty/algolingo/design/components/ChunkyButton.kt:36-46,80-88`
- `design/src/commonMain/kotlin/com/tlenparty/algolingo/design/AlgoTheme.kt:43-58`

**Issue:**

1. The public signatures expose `Modifier`, `Color`, `Shape`, `Dp`, `TextStyle` and `ColumnScope`, but the module declares compose ui, foundation and material3 as `implementation`. `:shared` only compiles because it redeclares the same dependencies. These should be `api(...)`.
2. `ChunkyBox` is public and takes 8 positional parameters, including nullable colours and two lambdas. The call sites in `HomeScreen.kt:42,45` pass `true` and colours by position.
3. `ChunkyButton` destructures `listOf(...)` into `Color?` values and then uses `!!`. That is not type-safe; a `when` that returns a small data class would be.
4. `ChunkyButton` forces `fillMaxWidth()`. It works only because callers put `width()` first.
5. `LightApp` and `DarkApp` fill a 22-field `AlgoColors` positionally, so swapping two adjacent slots would compile without any error.

**Fix:** switch to `api(libs.compose.*)`. Use named arguments for the `AlgoColors` instances and the `ChunkyBox` call sites. Return a `data class Look(face, ledge, border, label)` from the `when`.

### IN-06: Accessibility gaps

**Files:**
- `design/.../LessonProgressBar.kt:18-23`
- `shared/.../ui/components/CodeTemplate.kt:89-93`
- `shared/.../home/HomeScreen.kt:42-56`

**Issue:**

- The progress bar has no `progressBarRangeInfo` semantics, so screen readers do not announce it.
- After Check, a hole's description does not include its verdict (`Ok`, `Acceptable` or `Wrong`), so the result is conveyed by colour alone. The panel text partly compensates.
- Home exposes two clickable elements for the same lesson (the «Начать» pill and the node), plus a caption that is announced separately, which adds redundant focus stops.

**Fix:**

- Add `Modifier.semantics { progressBarRangeInfo = ProgressBarRangeInfo(fraction, 0f..1f) }` to the progress bar.
- Append «: верно», «: принято» or «: ошибка» to the hole's `desc`.
- Remove the node from the focus order with `clearAndSetSemantics {}`, or merge it with the pill.

### IN-07: Fallback wrong-chip copy breaks if the correct answer contains a backtick

**File:** `shared/src/commonMain/kotlin/com/tlenparty/algolingo/lesson/UiMapping.kt:35`

**Issue:** The fallback interpolates `it.correct` into backtick markup. Any backtick in the answer would break the inline-code parsing. The current content has no such answers.

**Fix:** pass `correct` through `PanelItem.correct`, which already renders as code, and keep the fallback text free of markup.

---

_Reviewed: 2026-10-21_
_Reviewer: Claude (gsd-code-reviewer)_
_Depth: standard_
