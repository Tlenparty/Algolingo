---
phase: 02-content-schema-engine
fixed_at: 2026-10-21T13:57:36Z
review_path: .planning/phases/02-content-schema-engine/02-REVIEW.md
iteration: 1
findings_in_scope: 8
fixed: 8
skipped: 0
status: all_fixed
---

# Phase 2: Code Review Fix Report

**Fixed at:** 2026-10-21T13:57:36Z
**Source review:** .planning/phases/02-content-schema-engine/02-REVIEW.md
**Iteration:** 1

**Summary:**
- Findings in scope: 8 (WR-01..WR-04, IN-01, IN-02, IN-05, IN-06)
- Fixed: 8
- Skipped: 0 (IN-03 and IN-04 were left out of scope on purpose, see the end of this report)

`./gradlew :shared:allTests` passes: testAndroidHostTest ran 69 tests and iosSimulatorArm64Test ran 64, with 0 failures. The fixes were committed directly on `main`, which the caller allowed. A /tmp worktree would not have `local.properties` (sdk.dir), so Gradle could not run there.

## Fixed Issues

### WR-01: Theory-first lesson with no playable exercise soft-locks

**Files modified:** `shared/src/commonMain/kotlin/com/tlenparty/algolingo/lesson/LessonReducer.kt`, `shared/src/commonTest/kotlin/com/tlenparty/algolingo/lesson/ReducerTest.kt`
**Commit:** 931749c
**Applied fix:** After the last `NextTheory` the reducer now moves to `Phase.Done` when `current == null`. Before, it went to `Phase.Exercise` with no exercise. Added the test `theoryWithoutExercisesEndsInDone`.

### WR-02: Distractor explained in only one hole passes `validate()`

**Files modified:** `shared/src/commonMain/kotlin/com/tlenparty/algolingo/lesson/Model.kt`, `shared/src/commonTest/kotlin/com/tlenparty/algolingo/lesson/ModelTest.kt`
**Commit:** d8dfa21
**Applied fix:** New rule. A distractor needs a non-blank `Chip.why`. If it has none, every hole needs a non-blank `wrongWhy[text]`. This matches `check()`, which looks up `hole.wrongWhy[text]` and falls back to `chip.why`. The error message now names the hole. The "viaWrongWhy" test is now two cases: an override in one hole only is an error ("no why for hole 1"), and an override in every hole is valid. Also added a blank-`why` case.

### WR-03: `wrongWhy` keys never validated

**Files modified:** `shared/src/commonMain/kotlin/com/tlenparty/algolingo/lesson/Model.kt`, `shared/src/commonTest/kotlin/com/tlenparty/algolingo/lesson/ModelTest.kt`
**Commit:** f54a534
**Applied fix:** In each hole, every `wrongWhy` key must match a bank chip text. It must not be one of that hole's `ok`/`acceptable` texts, because such an entry can never fire. Its value must not be blank. Added the test `wrongWhyKeys`, which has one valid case and three error cases.

### WR-04: e3 `why` for `0`/`-1` explains the wrong line

**Files modified:** `shared/src/commonMain/composeResources/files/content/lessons/l0.4.json`
**Commit:** 87c2e7e
**Applied fix:** Added per-hole `wrongWhy` overrides wherever a chip-level `why` would explain a line the user did not fill:
- e3, `return ⟦⟧` hole:
  - `0`: «вернёт 0 вместо найденного индекса: [1, 3], target 3 даст 0»
  - `-1`: «скажет «не найдено»…: [5], target 5 даст -1»
- e3, `hi = mid - ⟦⟧` hole:
  - `0`: `hi = mid - 0` does not exclude mid. On [1, 3], target 0 the loop never ends.
  - `-1`: `mid + 1` does not narrow the range. On [1, 3], target 0, hi is 1 again and the loop never ends.
- Type-mismatch placements now say "не скомпилируется" instead of showing the wrong explanation:
  - e3 `while` hole: `0`, `-1`
  - e3 Int holes: `lo < hi`
  - e2 `while` hole: `(lo + hi) / 2`, `hi - lo / 2`
  - e2 `mid` hole: `lo < hi`
- Kept the chip-level `why` for `0`/`-1`, because it is correct for the `lo = mid + ⟦⟧` hole.

I traced every counterexample by hand. **Requires human review of the Russian wording.**

### IN-01: Cross-hole mistakes get no explanation

**Files modified:** `shared/src/commonMain/composeResources/files/content/lessons/l0.4.json`
**Commit:** d8d4ce3
**Applied fix:** Added e3 overrides:
- `1` in `return`: «вернёт константу 1…: [5], target 5 даст 1 — такого индекса нет»
- `mid` in `lo = mid + ⟦⟧`: `2·mid`. On [1, 3], target 3, lo stays 0 and the loop never ends.
- `mid` in `hi = mid - ⟦⟧`: always 0. On [1, 3, 5, 7, 9], target 3 it returns -1.

Wrong type placements of non-distractor chips (for example `mid` in `while`) still show only «Верно: …». This is legal by design. **Requires human review of wording.**

### IN-02: Garbled `why` for `lo != hi`

**Files modified:** `shared/src/commonMain/composeResources/files/content/lessons/l0.4.json`
**Commit:** 1ebbd6f
**Applied fix:** New text: «при lo == hi цикл останавливается, не проверив последний кандидат: [1, 3], target 3 вернёт -1; а если hi уйдёт ниже lo, условие останется истинным: [1, 3], target 0 зацикливается». I traced both cases. Kotlin `/` truncates, so `mid = 0 + (-1)/2 = 0` and hi stays -1. **Requires human review of wording.**

### IN-05: Dead setup steps in `selectedFilledHoleIsOverwritten`

**Files modified:** `shared/src/commonTest/kotlin/com/tlenparty/algolingo/lesson/EngineTest.kt`
**Commit:** c4a2d5f
**Applied fix:** Removed the setup lines that the next line overwrote. Kept the state built by hand, with a comment that the public API cannot reach it.

### IN-06: Name collisions likely to bite Phase 3

**Files modified:** `LessonReducer.kt`, `ExerciseEngine.kt`, `Model.kt` (comments), `DuplicateChipTest.kt`, `EngineTest.kt`, `ReducerTest.kt`
**Commit:** cccef2b
**Applied fix:** Renamed:
- `Phase.Exercise` → `Phase.Solving`
- `check(ex, s)` → `checkExercise`
- `initial(ex, seed)` → `initialExerciseState`

Phase 3 has not started, so the renames are cheap, and the compiler checked every call site. I kept `LessonAction.Check`, because the action name no longer collides with anything. The `.planning` docs (RESEARCH, PLAN, ARCHITECTURE) still use the old names as historical text. I did not edit them.

## Not in scope (left as-is)

- **IN-03** (`validate()` reports only the first acceptable that is also in `ok`, and does not check for duplicate `ok` texts): left out of scope as instructed. It is about 2 lines if wanted later.
- **IN-04** (`result()` called before `Done` grades unreached exercises as Failed): left out of scope. Phase 3 should call `result()` only in `Phase.Done`.

---

_Fixed: 2026-10-21T13:57:36Z_
_Fixer: Claude (gsd-code-fixer)_
_Iteration: 1_
