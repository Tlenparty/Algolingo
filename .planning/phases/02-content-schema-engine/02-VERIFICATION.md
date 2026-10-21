---
phase: 02-content-schema-engine
verified: 2026-10-21T00:00:00Z
status: passed
score: 4/4 roadmap success criteria verified (plus plan truths)
overrides_applied: 0
---

# Phase 2: Content Schema & Engine Verification Report

**Phase Goal:** Схема контента и семантика проверки зафиксированы и доказаны тестами до UI и разметки
**Status:** passed
**Re-verification:** No, initial verification

## Test run (executed by verifier)

`./gradlew :shared:testAndroidHostTest :shared:iosSimulatorArm64Test --rerun` and `:shared:allTests`: BUILD SUCCESSFUL. Counts from `shared/build/test-results/*/*.xml`:

| Target | Tests | Failures | Errors | Skipped |
|--------|-------|----------|--------|---------|
| testAndroidHostTest | 69 | 0 | 0 | 0 |
| iosSimulatorArm64Test | 64 | 0 | 0 | 0 |

The 5 extra Android tests are the androidHostTest-only ones: LessonContentTest (2), KbContentTest (3), SharedLogicAndroidHostTest, offset by iOS-only SharedLogicIOSTest. These counts match the numbers in 02-REVIEW-FIX.md.

## Observable Truths

| # | Truth (ROADMAP SC) | Status | Evidence |
|---|--------------------|--------|----------|
| 1 | Hand-written `l0.4.json` parses into a Lesson: one task, 2-3 ladder exercises with growing hole count | VERIFIED | `shared/src/commonMain/composeResources/files/content/lessons/l0.4.json` (220 lines, ids l0.4.e1..e3). `LessonContentTest.l04IsValid` decodes it with strict `Json{ignoreUnknownKeys=false}`, asserts `validate()==[]` and hole counts `[1,2,4]`. Passes. |
| 2 | Engine inserts/removes chips, checks each hole against its correct set: ok / acceptable (with note) / wrong (with why); tests in commonTest pass | VERIFIED | `ExerciseEngine.kt`: `tapChip`, `tapHole`, `checkExercise` returning `HoleResult.Ok/Acceptable(note)/Wrong(why, correct)`; per-hole verdict. `EngineTest` (9 tests: checkThreeOutcomes, perHoleIndependence, etc.) pass on both targets. l0.4.json contains an `acceptable` entry. |
| 3 | Same-text chips (`mid`, `1`) behave as separate objects | VERIFIED | State holds `Int` bank indices only (`ExerciseState.placed: List<Int?>`). `DuplicateChipTest` (equalTextChipsAreIndependent, bothCheckOk) passes on both targets. l0.4.e3 bank has two separate `"1"` chips (`LessonContentTest.e3HasTwoSeparateOneChips`). |
| 4 | Reducer requeues wrong exercises at end (max 2 repeats), seeded deterministic shuffle, grade Clean/Hinted/Failed; lesson without hints valid | VERIFIED | `LessonReducer.kt`: `MAX_REPEATS=2`, `queue.drop(1)+id`, Reveal(false) after the 3rd miss, `result()` gives Grade Clean/Hinted/Failed. `seededShuffle`/`seedOf` use an explicit hash and a seeded `Random` (never `Random.Default`). `ReducerTest.wrongThreeTimesFails` shows the 4th attempt is impossible. `ShuffleTest.golden` and `deterministicAndSeedMixed` pass. Hints are optional (`hints` defaults to `emptyList()`; the reducer's Hint action is gated on `ex.hints.isNotEmpty()`). `ReducerTest.hints` and `gradeClean_acceptableCounts` pass. |

**Score:** 4/4

## Required Artifacts

| Artifact | Status | Details |
|----------|--------|---------|
| `lesson/Model.kt` (Lesson, Exercise, Hole, Chip, `validate`) | VERIFIED | Substantive: 12+ validation rules, never throws. Used by tests and content test. |
| `lesson/ExerciseEngine.kt` | VERIFIED | Pure functions, no stubs. |
| `lesson/LessonReducer.kt`, `SeededShuffle.kt` | VERIFIED | Pure state machine; `enter()` calls `initialExerciseState` with seed mix. |
| `l0.4.json` | VERIFIED | Passes strict decode plus `validate()` (segments + ok[0] equals solution.code). |
| Tests: Model, Engine, DuplicateChip, Reducer, Shuffle (commonTest), LessonContentTest (androidHostTest) | VERIFIED | All execute and pass. |

## Key Links

| From | To | Status |
|------|----|--------|
| `LessonReducer.enter` | `initialExerciseState` / `seededShuffle` | WIRED |
| `LessonReducer.reduce` (TapChip/TapHole/Check) | `tapChip`/`tapHole`/`checkExercise` | WIRED |
| `validate` | l0.4.json via `LessonContentTest` | WIRED |

Nothing in `App.kt` consumes the engine yet. That is intended: the UI is Phase 3, and the goal states "before UI".

## Requirements Coverage

| Requirement | Source Plan | Status | Evidence |
|-------------|-------------|--------|----------|
| LESS-03 (one task, 2-3 ladder exercises) | 02-01 | SATISFIED | `validate` enforces 2..3 exercises with strictly increasing hole counts; l0.4 is 1/2/4. |
| EXER-05 (duplicate chips are separate objects) | 02-02 | SATISFIED | Index identity plus DuplicateChipTest. |
| LESS-05 (wrong exercises requeued, max 2 repeats, shuffled bank, no lives) | 02-03 | SATISFIED | Reducer plus ReducerTest and ShuffleTest. No lives concept exists. |

REQUIREMENTS.md maps exactly LESS-03, LESS-05, EXER-05 to Phase 2 (all marked Complete). All three are claimed by plans, so there are no orphans.

## Anti-Patterns

No TBD/FIXME/XXX/TODO in `shared/src`. No stubs found in the lesson package. The review findings (WR-01..04, IN-01/02/05/06) were fixed. The IN-06 renames are consistent in the code and tests (Phase.Solving, checkExercise, initialExerciseState).

## Notes (non-blocking)

- `LessonContentTest` runs only on androidHostTest (it uses `java.io.File`). Parsing l0.4.json is therefore not exercised on iOS. Serialization of the model itself is covered on both targets by `ModelTest.inlineJsonDecodes`.
- The progress-bar value `progress` and the Phase 3 note about "Filled + selected" being unreachable are documented in code comments, not defects.

## Human Verification Required

None. The phase is pure logic and data, fully covered by automated tests.

## Gaps Summary

No gaps. The phase goal is achieved: the schema, the check semantics and the reducer are fixed in code and proven by 69 Android-host and 64 iOS-simulator passing tests.

---

_Verified: 2026-10-21_
_Verifier: Claude (gsd-verifier)_
