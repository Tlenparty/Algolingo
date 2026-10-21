---
phase: 02-content-schema-engine
reviewed: 2026-10-21T00:00:00Z
depth: standard
files_reviewed: 11
files_reviewed_list:
  - shared/src/androidHostTest/kotlin/com/tlenparty/algolingo/lesson/LessonContentTest.kt
  - shared/src/commonMain/composeResources/files/content/lessons/l0.4.json
  - shared/src/commonMain/kotlin/com/tlenparty/algolingo/lesson/ExerciseEngine.kt
  - shared/src/commonMain/kotlin/com/tlenparty/algolingo/lesson/LessonReducer.kt
  - shared/src/commonMain/kotlin/com/tlenparty/algolingo/lesson/Model.kt
  - shared/src/commonMain/kotlin/com/tlenparty/algolingo/lesson/SeededShuffle.kt
  - shared/src/commonTest/kotlin/com/tlenparty/algolingo/lesson/DuplicateChipTest.kt
  - shared/src/commonTest/kotlin/com/tlenparty/algolingo/lesson/EngineTest.kt
  - shared/src/commonTest/kotlin/com/tlenparty/algolingo/lesson/ModelTest.kt
  - shared/src/commonTest/kotlin/com/tlenparty/algolingo/lesson/ReducerTest.kt
  - shared/src/commonTest/kotlin/com/tlenparty/algolingo/lesson/ShuffleTest.kt
findings:
  critical: 0
  warning: 4
  info: 6
  total: 10
status: issues_found
---

# Phase 2: Code Review Report

**Reviewed:** 2026-10-21
**Depth:** standard
**Files Reviewed:** 11
**Status:** issues_found

## Narrative Findings (AI reviewer)

## Summary

I traced the engine (`tapChip`/`tapHole`/`check`), the reducer (queue, requeue, MAX_REPEATS, hints, grade, seed mixing), `validate()` and the l0.4 content against 02-RESEARCH.md and the User Decisions.

These parts are correct:
- Duplicate-chip identity: state holds only bank indices.
- The selection invariant: `selected` is always an empty hole or null.
- Three attempts, then `Reveal(false)`, with no 4th attempt.
- Acceptable counts as a Clean grade.
- Seed mixing: same lesson seed plus a different attempt always gives a different seed.
- Cross-target shuffle.

All three exercises rebuild exactly to `solution.code`. I checked this independently with a script. I hand-traced every counterexample in the l0.4 `why` texts and they hold.

Defects found:
- The reducer can soft-lock on a lesson that has theory but no playable exercises.
- `validate()` accepts a distractor whose only explanation is a `wrongWhy` entry in a different hole. It also never checks `wrongWhy` keys or blank explanations.
- In e3, the chips `0` and `-1` show an explanation about the `lo = mid + …` line even when they are placed in the `return` or `hi = mid - …` hole.

No blockers: the bundled content is valid and the soft-lock needs content that `validate()` rejects.

## Warnings

### WR-01: Theory-first lesson with no playable exercise soft-locks in `Phase.Exercise` with `current == null`

**File:** `shared/src/commonMain/kotlin/com/tlenparty/algolingo/lesson/LessonReducer.kt:62-66, 75-76`
**Issue:** `start()` falls back to `Phase.Done` only when `theory` is empty. If theory is non-empty and `enter()` returns null (no exercises, or the queue head is not found), the lesson starts in `Theory(0)`. After the last `NextTheory` it moves to `Phase.Exercise` with `current = null`. From there every action is a stray action (all branches require `cur != null` or another phase), so the UI is stuck with no way out except closing the lesson. `validate()` rejects this content, but the reducer header promises robustness ("never throws; stray actions return the state unchanged"). Phase 3's loader is not guaranteed to call `validate()`.
**Fix:**
```kotlin
a is LessonAction.NextTheory && p is Phase.Theory -> s.copy(
    phase = when {
        p.index + 1 < s.lesson.theory.size -> Phase.Theory(p.index + 1)
        s.current == null -> Phase.Done
        else -> Phase.Exercise
    }
)
```
Add a ReducerTest case: `start(lessonOf(theory = 1), 1)` → `NextTheory` → `Phase.Done`.

### WR-02: `validate()` treats a distractor as explained if ANY hole has a `wrongWhy` for it, but `check()` reads only the current hole's `wrongWhy`

**File:** `shared/src/commonMain/kotlin/com/tlenparty/algolingo/lesson/Model.kt:97-101` (vs `ExerciseEngine.kt:59`)
**Issue:** `ex.holes.none { it.wrongWhy.containsKey(c.text) }` passes when hole 0 has `wrongWhy["Z"]` and `Z` has no `Chip.why`. If the user drops `Z` into hole 1, `check()` returns `Wrong("Z", why = null, …)` and the panel has no explanation. CONT-02 requires one (an explanation plus a counterexample). The `ModelTest.distractorNeedsWhy` "viaWrongWhy" case encodes this gap as valid.
**Fix:** require coverage per hole:
```kotlin
for (c in ex.bank) if (c.text !in used && c.why.isNullOrBlank()) {
    ex.holes.forEachIndexed { h, hole ->
        if (hole.wrongWhy[c.text].isNullOrBlank()) errors += "$e: distractor '${c.text}' has no why for hole $h"
    }
}
```
Using `isNullOrBlank()` also catches `"why": ""`, which currently passes.

### WR-03: `wrongWhy` keys are never validated (typos and dead entries go silent)

**File:** `shared/src/commonMain/kotlin/com/tlenparty/algolingo/lesson/Model.kt:77-81`
**Issue:** A `wrongWhy` key that matches no bank chip, for example a typo `"mid +1"`, can never fire. A key equal to one of that hole's `ok`/`acceptable` texts is also dead, because `check()` matches ok/acceptable first. Strict JSON decoding does not help here: these are map keys, not field names. Because of WR-02, a typo'd key in one hole can still mark the distractor as covered.
**Fix:** inside the per-hole loop:
```kotlin
hole.wrongWhy.keys.forEach { k ->
    if (k !in bankTexts) errors += "$e: hole $h wrongWhy key '$k' not in bank"
    if (k in hole.ok || hole.acceptable.any { it.text == k }) errors += "$e: hole $h wrongWhy key '$k' is a correct answer"
}
```

### WR-04: e3 `why` for chips `0` and `-1` explains the wrong line when they are placed in the `return` or `hi` hole

**File:** `shared/src/commonMain/composeResources/files/content/lessons/l0.4.json:168-175`
**Issue:** e3 has four holes, and three of them accept a number-like chip (`return ⟦mid⟧`, `lo = mid + ⟦1⟧`, `hi = mid - ⟦1⟧`). The chip-level `why` for `0` is «lo = mid + 0 зацикливается…», and for `-1` it is «lo = mid + -1 уводит границу назад…». Both only describe hole 2. If the user places `0` into `hi = mid - ⟦ ⟧` or `return ⟦ ⟧`, the panel explains a line the user did not touch. This is exactly the hole-dependent `why` case that RESEARCH says `wrongWhy` exists for. Real behavior of those placements:
- `hi = mid - 0` on `[1, 3]`, target 0: `hi` stays 0 and the loop never ends.
- `hi = mid - -1` widens the range and also loops.
- `return 0` / `return -1` returns a constant instead of the found index.
**Fix:** keep the chip `why` generic, or add per-hole overrides:
```json
{ "ok": ["mid"], "wrongWhy": { "0": "вернёт 0 вместо найденного индекса: [1, 3], target 3 даст 0", "-1": "скажет «не найдено», хотя элемент есть: [5], target 5 даст -1" } },
{ "ok": ["1"] },
{ "ok": ["1"], "wrongWhy": { "0": "hi = mid - 0 зацикливается: [1, 3], target 0, hi остаётся 0", "-1": "hi = mid + 1 расширяет диапазон: [1, 3], target 0, цикл бесконечный" } }
```

## Info

### IN-01: Correct-elsewhere chips give a Wrong with no `why` in e2/e3

**File:** `shared/src/commonMain/composeResources/files/content/lessons/l0.4.json:150-176`
**Issue:** Wrong placements such as `mid` ↔ `1` swapped in e3, `1` in the return hole, or `(lo + hi) ushr 1` in the `while` hole of e2 produce `Wrong(why = null)`, so only «Верно: …» is shown. This is allowed by design (RESEARCH line 146), but the most likely e3 mistake, `return 1`, gets no explanation.
**Fix:** add `wrongWhy` for the plausible cross-hole mistakes (at least `"1"` in the return hole).

### IN-02: Garbled `why` text for `lo != hi`

**File:** `shared/src/commonMain/composeResources/files/content/lessons/l0.4.json:56`
**Issue:** «то же, что lo < hi для lo <= hi» reads as nonsense. It also understates the bug: once `hi < lo`, `lo != hi` stays true and the loop keeps running. For example, on `[1, 3]` with target 0, `hi` becomes -1, `mid` stays 0, and the loop never ends.
**Fix:** «при lo == hi цикл не зайдёт ([5], target 5 → -1), а когда hi уходит ниже lo, условие остаётся истинным: [1, 3], target 0 зацикливается».

### IN-03: `validate()` reports only the first acceptable that is also in `ok`, and does not check duplicate texts within `ok`/`acceptable`

**File:** `shared/src/commonMain/kotlin/com/tlenparty/algolingo/lesson/Model.kt:79-80`
**Fix:** `hole.acceptable.filter { it.text in hole.ok }.forEach { … }`, and flag `hole.ok.size != hole.ok.toSet().size`.

### IN-04: `result()` grades unreached exercises as Failed when called before `Done`

**File:** `shared/src/commonMain/kotlin/com/tlenparty/algolingo/lesson/LessonReducer.kt:115-125`
**Issue:** If Phase 3 or Phase 4 calls it mid-lesson (for example on exit), it reports `Grade.Failed`. This is harmless if it is only called on `Done`, but nothing enforces that.
**Fix:** document "call only in Phase.Done", or return `LessonResult?` that is null when `s.phase != Phase.Done`.

### IN-05: Dead setup steps in `selectedFilledHoleIsOverwritten`

**File:** `shared/src/commonTest/kotlin/com/tlenparty/algolingo/lesson/EngineTest.kt:49-52`
**Issue:** Lines 49-51 build a state that line 52 fully overwrites with `copy(placed = …, selected = 0)`, so they test nothing. The test only checks overwrite on a hand-built state, which is unreachable through the public API.
**Fix:** delete lines 49-51 and keep the explicit state.

### IN-06: Name collisions likely to bite Phase 3

**File:** `shared/src/commonMain/kotlin/com/tlenparty/algolingo/lesson/LessonReducer.kt:10`, `ExerciseEngine.kt:11,50`
**Issue:**
- `Phase.Exercise` shadows the model class `Exercise` wherever `Phase.*` is imported.
- The top-level `check(ex, s)` overloads stdlib `check(Boolean)`.
- `LessonAction.Check` sits next to `check`.
- The top-level `initial(...)` is a very generic package-level name.
**Fix:** consider `Phase.Solving`, `checkExercise`, and `initialExerciseState` before Phase 3 starts importing them.

---

_Reviewed: 2026-10-21_
_Reviewer: Claude (gsd-code-reviewer)_
_Depth: standard_

## Fix Status

See `02-REVIEW-FIX.md` (iteration 1).

| ID | Status | Commit |
|----|--------|--------|
| WR-01 | fixed | 931749c |
| WR-02 | fixed | d8dfa21 |
| WR-03 | fixed | f54a534 |
| WR-04 | fixed (content: human review of wording) | 87c2e7e |
| IN-01 | fixed (content: human review of wording) | d8d4ce3 |
| IN-02 | fixed (content: human review of wording) | 1ebbd6f |
| IN-03 | skipped (out of scope) | — |
| IN-04 | skipped (out of scope) | — |
| IN-05 | fixed | c4a2d5f |
| IN-06 | fixed | cccef2b |
