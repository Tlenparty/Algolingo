---
phase: 05-level-0-content-end-to-end
plan: 02
subsystem: content
tags: [content, sidecars, l0.1, l0.2, l0.3, reference-tests]
requires: [tools/gen_content.py, content/authoring/l0.4.md]
provides: [content/authoring/l0.1-l0.3.md, lessons/l0.1-l0.3.json, 4-lesson index.json, 8 reference/variant tests]
affects: [05-03 (customer review), 05-04 (walkthrough)]
tech-stack:
  added: []
  patterns: [per-hole @H overrides for every cross-hole landing of a distractor that has a chip why]
key-files:
  created:
    - content/authoring/l0.1.md
    - content/authoring/l0.2.md
    - content/authoring/l0.3.md
    - shared/src/commonMain/composeResources/files/content/lessons/l0.1.json
    - shared/src/commonMain/composeResources/files/content/lessons/l0.2.json
    - shared/src/commonMain/composeResources/files/content/lessons/l0.3.json
  modified:
    - shared/src/commonMain/composeResources/files/content/index.json
    - shared/src/commonTest/kotlin/com/tlenparty/algolingo/lesson/ReferenceSolutionsTest.kt
decisions:
  - "l0.2 e3 uses holes l⟦++⟧; r⟦--⟧ instead of ⟦l++⟧; ⟦r--⟧: the checker is per-hole, so `r--; l++` (equivalent) would have been graded wrong"
  - "e3 of 0.2/0.3 keep the while hole strict (`l < r` only, per plan); `l <= r` is acceptable in e1/e2 of 0.2 and e1 of 0.3"
  - "Correct-elsewhere chips that only fail to compile in a foreign hole get no @H (shows «Верно: X»); semantic swaps get @H"
metrics:
  duration: ~15 min
  completed: 2026-10-22
  tasks: 2
  files: 8
---

# Phase 5 Plan 02: Level 0.1-0.3 Content Summary

Three sidecars (sum+max, reverse, palindrome) with 3 theory cards each, 1 → 2 → 4 hole ladders, distractors from «Грабли» with counterexample whys, and the «принято, но…» variants `Int.MIN_VALUE` (0.1) and `l <= r` (0.2, 0.3). The generator now produces a 4-lesson index (l0.1..l0.4) and 8 reference tests, all passing on androidHost and iosSimulatorArm64.

## Tasks

| # | Task | Commit |
|---|------|--------|
| 1 | l0.1 «Сумма и максимум» + regenerate (index [l0.1, l0.4], L0_1_v0/v1) | 782cb9c |
| 2 | l0.2 «Разворот строки», l0.3 «Палиндром» + 4-lesson catalog (L0_2_*, L0_3_*) | debbdbd |

## Verification

- `python3 tools/gen_content.py --check` exit 0; `python3 -m unittest tools/test_gen_content.py` 25 OK.
- `./gradlew :shared:allTests :androidApp:assembleDebug` green. ReferenceSolutionsTest 8/8 on host and iosSimulatorArm64 (L0_1_v0, L0_1_v1 `Int.MIN_VALUE`, L0_2_v0, L0_2_v1 `l <= r`, L0_3_v0, L0_3_v1 `l <= r`, L0_4_v0, L0_4_v1 `ushr`). LessonContentTest 4/4 (strict decode + validate() over all 4 ids).
- Hole counts [1, 2, 4] in every lesson; banks: 0.1 [5,5,7], 0.2 [4,6,6], 0.3 [4,4,7] (<= 10).
- No `>=` or `s.lastIndex` chip in 0.1-0.3. `l0.4.json` unchanged. `/Users/` grep over content + outputs: 0.

## Hand-traced counterexample log

Each why assumes all other holes are correct.

**l0.1** `sumAndMax`
- `0` (max): max=0; -5>0, -2>0 false → `[-5,-2]` max 0 ✓
- `Int.MAX_VALUE` (max): no x > MAX → `[1,2]` max 2147483647 ✓
- `nums[1]` (max): `[7]` → index 1 out of bounds ✓
- `Int.MIN_VALUE` acceptable: empty array → (0, -2147483648), no exception ✓
- e2 `@1 nums[0]`: 1+1+2+3 = 7 ✓; `@1 Int.MIN_VALUE`: MIN+6 = -2147483642 ✓; `@1 1`: 1+6 = 7 ✓; `@1 Int.MAX_VALUE`: MAX+1 = -2147483648 on `[1]` ✓; `@2 0`: as above ✓; `@2 1`: `[-5,-2]` max 1 ✓
- e3 `=` (sum): `sum = x` → `[1,2]` sum 2 ✓; `<` (cmp): `[3,1]` max=3, 1<3 → max 1 ✓; `@3 <`: `sum < x` is a discarded expression → sum 0 ✓; `=`/`<` in value holes and `=` in `if` → compile errors ✓

**l0.2** `reverse`
- `l > r`: 0>3 false → `abcd` unchanged ✓
- `l != r`: abcd (0,3)→dbca, (1,2)→dcba, (2,1)→dbca, (3,0)→abcd, (4,-1) → `s[4]` ✓
- `l <= r` acceptable: `abc` extra iteration (1,1) swaps `b` with itself ✓
- `s.size` (r): first `s[r]` = `s[4]` on `abcd` ✓
- e3 `s[l]` (swap): t=a, s0=b, s1=s[0]=b → `bb` ✓
- e3 `@3 --` (`l--; r--`): after (0,3), l=-1 → `s[-1]` ✓; `@4 ++` (`l++; r++`): (1,4) → `s[4]` ✓
- All other foreign-hole landings (`s[l]` in while/`++`/`--` holes, `l > r` in `s[r] =`/operator holes, Boolean in `var r =`, `s.size` in while) → compile errors, stated as such ✓

**l0.3** `isPalindrome`
- `l > r`: loop skipped → `"ab"` true ✓
- `l != r`: `"abba"` (0,3),(1,2),(2,1),(3,0) all equal, (4,-1) → `s[4]` ✓
- `l <= r` acceptable: `"aba"` extra comparison at (1,1) ✓
- e2 `==`: `"ab"` a==b false → loop ends → true; `"aba"` a==a → false ✓; `true` (return): `"ab"` → true ✓
- e3 `s.length` (r): `"aba"` s[0] vs s[3] → out of bounds ✓; `==` (cmp): `"aba"` → false ✓
- e3 `@2 false`: `while (false)` → `"ab"` true ✓; `@4 false`: `"aba"` false ✓; `@2 true`: `"aba"` (2,0) equal, (3,-1) → `s[3]` ✓; `@4 l < r`: after loop l >= r → `"aba"` false ✓
- Remaining foreign-hole landings → compile errors, stated as such ✓

## Deviations from Plan

**1. [Rule 1 - Bug] l0.2 e3 operator holes instead of statement holes**
- **Found during:** Task 2
- **Issue:** Plan's `⟦l++⟧; ⟦r--⟧` lets the user place `r--; l++`, which is equivalent and correct, but `checkExercise` grades each hole independently and would mark it wrong (P3 / T-05-05).
- **Fix:** holes `l⟦++⟧; r⟦--⟧`; distractors `l--`/`r++` became the `--`/`++` swap with `@3`/`@4` overrides (`s[-1]`, `s[4]`); `l > r` added as while-hole distractor.
- **Files:** content/authoring/l0.2.md, l0.2.json
- **Commit:** debbdbd

## Decisions for the customer review (05-03)

- `Int.MIN_VALUE` note wording (0.1); `l <= r` notes (0.2, 0.3) and the theory-card sentence that 04 §3 calls `<=` wrong while these lessons accept it.
- l0.2 e3 hole shape `l⟦++⟧; r⟦--⟧` (see deviation).

## Known Stubs

None. `solution.variations` is empty for 0.1/0.3 because note 05 has none for them.

## Self-Check: PASSED

All created files exist; commits 782cb9c, debbdbd present in `git log`.
