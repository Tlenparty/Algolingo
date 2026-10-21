---
phase: 03-lesson-exercise-ui
fixed_at: 2026-10-21T00:00:00Z
review_path: .planning/phases/03-lesson-exercise-ui/03-REVIEW.md
iteration: 1
findings_in_scope: 13
fixed: 13
skipped: 0
status: all_fixed
---

# Phase 3: Code Review Fix Report

**Fixed at:** 2026-10-21
**Source review:** .planning/phases/03-lesson-exercise-ui/03-REVIEW.md
**Iteration:** 1

**Summary:**
- Findings in scope: 13 (CR-01, WR-01..05, IN-01..07)
- Fixed: 13
- Skipped: 0

**Verification:** `./gradlew :shared:allTests :design:compileKotlinIosSimulatorArm64 :androidApp:assembleDebug` passed. The APK was installed with `adb -s emulator-5554 install -r` and CR-01 was smoke-checked on the emulator:
- Two simultaneous taps on «Дальше» at Theory(0) landed on Theory(1) «Каркас».
- Two simultaneous taps on «Дальше» in the Feedback panel landed on Reveal and did not skip it.
- Home exposes one accessibility node, «Начать: 0.4 Бинарный поиск».

Taps sent with adb `input` arrive with several hundred ms of JVM-startup jitter, so these runs cannot reproduce an exact 100 ms double tap. That case is covered by `ReducerTest`.

## Fixed Issues

### CR-01: Double tap skips theory cards, feedback and Reveal

**Files modified:** `shared/.../lesson/LessonReducer.kt`, `lesson/ui/LessonScreen.kt`, `lesson/ui/ExerciseScreen.kt`, `lesson/ui/RevealScreen.kt`, `commonTest/.../ReducerTest.kt`, `commonTest/.../UiMappingTest.kt`
**Commit:** edae97a
**Applied fix:**
- The actions now carry the screen they came from: `NextTheory(index)` and `Continue(from: ScreenKey)`.
- The reducer ignores a `Continue` whose key differs from `screenKey(s)`, and a `NextTheory` whose index differs from the current Theory index.
- The UI takes the stamp from the `AnimatedContent` snapshot, so taps on the outgoing page are stale and do nothing.
- For Solving → Feedback, which shares one screen key, the panel button does nothing until the enter animation is idle.
- New tests: `doubleTapNextTheoryShowsEveryCard` and `doubleTapContinueDoesNotSkipReveal`.
- Phase 2 tests were updated to the new action signatures through `next()`/`cont()` helpers. Their assertions did not change.

**Status:** fixed: requires human verification (this is a state-machine logic change; the panel-idle guard is UI timing).

### WR-01: M3 colour slots fall back to the purple baseline

**Files modified:** `design/.../AlgoTheme.kt`
**Commit:** e03e2d0
**Applied fix:**
- One `algoScheme(...)` builder now sets every slot in both themes, using values from the UI-SPEC palette:
  - primary, secondary and tertiary (plus their containers) use the accent and the success tint/text;
  - error and errorContainer use the wrong-state red and its tint/text;
  - all `surfaceContainer*` slots use the background, except `surfaceContainerHighest`, which uses surfaceVariant;
  - `surfaceTint`, inverse colours, `outlineVariant`, and `surfaceBright`/`surfaceDim` are set as well.
- The NavigationBar indicator is now the success tint, and the dialog container is the background.

### WR-02: Template horizontal scroll never comes back; Wrong holes off-screen

**Files modified:** `shared/.../ui/components/CodeTemplate.kt`, `lesson/ui/ExerciseScreen.kt`
**Commit:** 99d5264
**Applied fix:**
- The `ScrollState` is hoisted into `CodeTemplate`. A new `selected` parameter is passed only while solving.
- `focus` is the first Wrong hole if there is one, otherwise the selected hole.
- Only the focused hole calls `bringIntoView`.
- When `focus == null` (last hole filled, or everything correct after Check), the template animates back to column 0.

**Status:** fixed: requires human verification (scroll behaviour should be checked on a narrow phone).

### WR-03: ▶ renders as the colour emoji on iOS

**Files modified:** `shared/.../home/HomeScreen.kt`, `design/.../AlgoTheme.kt`
**Commit:** f51be1b
**Applied fix:**
- The triangle is now drawn with `Canvas` + `Path` in `onPrimary`, so it no longer depends on any font.
- The unused `typography.nodeEmoji` token was replaced by a new `sizes.nodeGlyph` token (24dp), which has one call site.

### WR-04: Code in hints is not in the code font

**Files modified:** `shared/src/commonMain/composeResources/files/content/lessons/l0.4.json`
**Commit:** d9b1cb9
**Applied fix:**
- Code in every hint is now wrapped in backticks.
- The same was done for the theory «Каркас» body `[lo, hi]` and all chip `why` and hole `wrongWhy` texts (identifiers, expressions, sample arrays, values).
- The JSON was rewritten with the original formatting (indent 2, UTF-8). `LessonContentTest` stays green.
- The optional `validate()` check was not added. Matching `ok[0]` strings such as `1` or `mid` against prose gives false positives. Add a proper check when the Phase 5 generator exists.

### WR-05: Loader does not catch every failure; exit dialog after process death; reload on rotation

**Files modified:** `shared/.../lesson/LessonRepository.kt`, `lesson/LessonViewModel.kt`, `lesson/ui/LessonHost.kt`, `lesson/ui/LessonScreen.kt`
**Commit:** b34e4ee
**Applied fix:**
- `loadLesson` still rethrows `CancellationException` and now turns any other `Exception` into a `Result.failure`, which shows `LessonErrorScreen`.
- `LessonViewModel(id, seed, load = ::loadLesson)` loads the lesson once in `viewModelScope` and exposes `StateFlow<Result<LessonState>?>`. After rotation the ViewModel is reused, so there is no reload and no blank frame.
- `confirm` changed from `rememberSaveable` to `remember`. This is the smaller correct fix, consistent with the accepted decision that the lesson restarts after process death.

### IN-01: `@OptIn(ExperimentalLayoutApi::class)` on stable FlowRow

**Files modified:** `shared/.../lesson/ui/ExerciseScreen.kt`
**Commit:** 51fb8d5
**Applied fix:** Removed the import and the annotation.

### IN-02: ResultPanelHost exit animation unreachable, comment wrong

**Files modified:** `shared/.../lesson/ui/ExerciseScreen.kt`
**Commit:** 05e1eaf
**Applied fix:**
- Removed the `exit` parameter and its imports.
- Rewrote the comment to say that the panel leaves together with its page snapshot.
- Replaced the fully qualified `remember`, `MutableTransitionState`, `Dp` and `CheckResult` with normal imports.

### IN-03: Haptic replays on configuration change

**Files modified:** `shared/.../lesson/ui/ExerciseScreen.kt`
**Commit:** 6d219ef
**Applied fix:** A `buzzed` flag saved with `rememberSaveable(screenKey(state))` means the haptic fires once per play, even if the composition is recreated.

### IN-04: Dead Hello code and hello.json

**Files modified:** `shared/.../App.kt`, deleted `shared/src/commonTest/.../HelloTest.kt` and `shared/src/commonMain/composeResources/files/hello.json`
**Commit:** 63d09b2
**Applied fix:**
- Removed `Hello`, `parseHello`, `loadHello`, and the unused `Res`/`Json` imports.
- What HelloTest covered is still tested elsewhere: `ignoreUnknownKeys` by `LessonRepositoryTest.validRoundtripIgnoresUnknownKeys`, and reading JSON from resources by `LessonContentTest`/`KbContentTest`.
- The Phase-1 per-platform test harness smoke tests (`SharedCommonTest`, `SharedLogicAndroidHostTest`, `SharedLogicIOSTest`) were kept.

### IN-05: Design module API hygiene

**Files modified:** `design/build.gradle.kts`, `design/.../AlgoTheme.kt`, `design/.../components/ChunkyButton.kt`, `shared/.../home/HomeScreen.kt`
**Commit:** cd072fd
**Applied fix:**
- compose runtime, foundation, material3 and ui are now `api` in `:design`. Resources stay `implementation`, because they are not part of the public API.
- `LightApp` and `DarkApp` are filled with named arguments.
- The Home `ChunkyBox` call sites and the call inside `ChunkyButton` use named arguments.
- The `listOf` destructuring with `!!` was replaced by a private `data class Look`.
- Item 4 (`ChunkyButton` forcing `fillMaxWidth`) was documented in KDoc rather than changed, because every call site depends on full width.

### IN-06: Accessibility gaps

**Files modified:** `design/.../components/LessonProgressBar.kt`, `shared/.../ui/components/CodeTemplate.kt`, `shared/.../home/HomeScreen.kt`
**Commit:** 4694d60
**Applied fix:**
- The progress bar now has `progressBarRangeInfo`, using the target fraction rather than the animated value.
- After Check, a hole's description ends with «: верно», «: принято» or «: ошибка».
- On Home, the «Начать» pill is the only focus stop, with the description «Начать: <caption>». The node and caption use `clearAndSetSemantics {}`. Verified with uiautomator on the emulator.

### IN-07: Fallback wrong-chip copy breaks on a backtick in the answer

**Files modified:** `shared/.../lesson/UiMapping.kt`, `commonTest/.../UiMappingTest.kt`
**Commit:** 091d76f
**Applied fix:** The fallback is now `PanelItem(text, why ?: "Здесь нужно другое.", correct)`. ResultPanel already renders `correct` as code under «Верно:». The test was updated to match.

---

_Fixed: 2026-10-21_
_Fixer: Claude (gsd-code-fixer)_
_Iteration: 1_
