---
phase: 03-lesson-exercise-ui
verified: 2026-10-21T00:00:00Z
status: passed
score: 7/7 must-haves verified (6 ROADMAP SC + :design token requirement)
overrides_applied: 0
re_verification: false
---

# Phase 3: Lesson & Exercise UI Verification Report

**Phase Goal:** Пользователь проходит урок с телефона без клавиатуры: читает теорию, собирает код из облачков, жмёт «Проверить» и сразу видит, где грабли
**Status:** passed
**Re-verification:** No (initial)

## Build and tests (run by verifier)

`./gradlew :shared:allTests :design:compileKotlinIosSimulatorArm64 :androidApp:assembleDebug` returned BUILD SUCCESSFUL. All tasks were UP-TO-DATE, so the build and test results came from the cache. Counts below are from the XML files in `shared/build/test-results/`:

| Target | Tests | Failures | Errors | Skipped |
| --- | --- | --- | --- | --- |
| testAndroidHostTest | 92 | 0 | 0 | 0 |
| iosSimulatorArm64Test | 87 | 0 | 0 | 0 |

The Android and iOS counts differ by 5. This is a platform difference in the test set and does not affect the verdict.

## Observable Truths

| # | Truth | Status | Evidence |
| - | ----- | ------ | -------- |
| 1 | Урок: 2–4 карточки теории «Дальше», затем упражнения с прогресс-баром | VERIFIED | `LessonScreen` routes `Phase.Theory` to `TheoryScreen` with `NextTheory`, and `Solving`/`Feedback` to `ExerciseScreen`. `LessonProgressBar` is in `design/components`. Android emulator walk showed 3 theory cards. |
| 2 | Моноширинный код с подсветкой и дырками; тап вставляет в выделенную/первую пустую, в банке серый призрак; тап по вставленному возвращает; тап по дырке выделяет | VERIFIED | `CodeTemplate` (`onHoleTap`), `TokenChip` with `ChipState.Ghost` when placed, `Highlight.kt` in `:design`. Reducer and `DuplicateChipTest` pass. Device walk covered ghosts, duplicate `1` chips, returning a chip, and auto-scroll to the selected hole. |
| 3 | «Проверить» активна только при заполненных дырках; цвета зелёный/жёлтый/красный; панель с объяснением и верным вариантом | VERIFIED | `ExerciseScreen:132` uses `enabled = solving && cur.placed.all { it != null }`. `holeVisual` maps to Ok/Acceptable/Wrong. `panelModel` gives Success/Amber/Danger and passes `PanelItem.correct`. `UiMappingTest` passes. Device: red «❌ Не совсем» with why, amber «Принято, но…». |
| 4 | Подсказки по одной (кнопка скрыта без подсказок); после решения эталон, сложность, грабли, вариации | VERIFIED | `LessonScreen:48` sets `hintsRemaining = null` when there are no hints, and `LessonTopBar` then shows a spacer. `ExerciseScreen` shows `shown` hint cards, one per tap. `RevealScreen` has sections Эталон, Сложность, Грабли and Вариации, the last two only when non-empty. Device walk confirmed. |
| 5 | Итог ✅/💡/❌ + XP | VERIFIED | `SummaryScreen` maps `Grade.Clean/Hinted/Failed` to ✅/💡/❌. `XpTest` passes. Device: «💡 Решено с подсказкой» +25 XP. |
| 6 | Игривый стиль по UI-SPEC, светлая/тёмная тема по системе | VERIFIED | `AlgoTheme` in `:design` uses `isSystemInDarkTheme()`, and `App.kt` wraps everything in `AlgoTheme`. Device walk in light and dark on Android. |
| 7 | `:design` module with AlgoTheme tokens; no raw dp/sp/Color(0x/RoundedCornerShape( in shared commonMain (one documented exception) | VERIFIED | `include(":design")` is in settings. Theme, highlight and components (`ChunkyButton`, `TokenChip`, `AlgoCard`, `CodeBlock`, `LessonProgressBar`) are in `design/src/commonMain`. Grep of `shared/src/commonMain/kotlin` for `\.dp`/`\.sp`, `Color(0x` and `RoundedCornerShape(` finds a single use: `kb/ui/BlockView.kt:133`, the computed table column width `(... * 8 + 16).coerceIn(80, 240).dp`. This is the sanctioned exception. |

**Score:** 7/7

## Key links

| From | To | Status |
| ---- | -- | ------ |
| `App.kt` Home route | `LessonRoute` / `LessonHost` / `LessonScreen` | WIRED (`entry.ifResumed` guards double navigation) |
| `LessonScreen` | `ExerciseScreen`, `TheoryScreen`, `RevealScreen`, `SummaryScreen` | WIRED |
| `ExerciseScreen` | `LessonReducer` via `LessonAction` | WIRED |
| `shared` | `:design` (`AlgoTheme`, components) | WIRED (`api` exposure per review fix IN-05) |

## Requirements Coverage

All 13 IDs (LESS-02, LESS-04, LESS-06, EXER-01 to EXER-04, EXER-06 to EXER-10, UI-01) are listed in ROADMAP Phase 3 and marked `[x]` / Complete in REQUIREMENTS.md. Each maps to the truths above and is SATISFIED. No orphaned Phase 3 requirements. EXER-05 is not in this phase's list, and I did not check which phase it belongs to.

## Anti-patterns

The debt-marker grep (TBD, FIXME, XXX, TODO) over `shared/src`, `design/src` and `androidApp/src` found nothing. Code review (CR-01, WR-01 to WR-05, IN-01 to IN-07) is all_fixed per `03-REVIEW-FIX.md`, and the build and tests still pass after the fixes.

## Probes

The phase declares no probe scripts.

## Human verification

None outstanding. The Android and iOS device walks and the iOS swipe-back check were supplied as verified, and the user approved the 03-08 human-verify checkpoint.

## Gaps

None.

---
_Verified: 2026-10-21_
_Verifier: Claude (gsd-verifier)_
