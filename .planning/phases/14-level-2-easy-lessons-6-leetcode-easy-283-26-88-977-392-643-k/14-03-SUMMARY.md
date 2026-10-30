---
phase: 14-level-2-easy-lessons
plan: 03
subsystem: content
tags: [lessons, level-2, two-pointers, sliding-window, kotlin, python]
requires: ["14-01: note 05 sections 2.4-2.6", "14-02: batch A style (approved)"]
provides:
  - "lessons l2.4 (977), l2.5 (392), l2.6 (643) in Kotlin + Python, langs [kotlin, python] in index.json"
affects: [14-04]
tech-stack:
  added: []
  patterns: ["Double results checked with absolute tolerance 1e-9 in checks (kotlin.test assertEquals(..., 1e-9) / abs(...) < 1e-9)"]
key-files:
  created:
    - content/authoring/l2.4.md
    - content/authoring/l2.5.md
    - content/authoring/l2.6.md
    - content/authoring/py/l2.4.md
    - content/authoring/py/l2.5.md
    - content/authoring/py/l2.6.md
    - shared/src/commonMain/composeResources/files/content/lessons/l2.{4,5,6}.json
    - shared/src/commonMain/composeResources/files/content/lessons/py/l2.{4,5,6}.json
    - .planning/phases/14-level-2-easy-lessons-6-leetcode-easy-283-26-88-977-392-643-k/14-03-REVIEW-PENDING.md
  modified:
    - shared/src/commonMain/composeResources/files/content/index.json
    - shared/src/commonTest/kotlin/com/tlenparty/algolingo/lesson/ReferenceSolutionsTest.kt
    - shared/src/androidHostTest/resources/wrong_variants.json
key-decisions:
  - "l2.4: >= in the square comparison is ~acceptable; loop uses the note's writeIndex downTo (G5), not resultIndex = lastIndex"
  - "l2.5: hang distractor modelled as a comparison chip in the tIndex++ hole (s and t pointers both stop on a mismatch)"
  - "l2.6: Double compared with tolerance 1e-9; Python first window is sum(nums[:k]) once before the loop"
metrics:
  duration: 70min
  completed: 2026-10-30
---

# Phase 14 Plan 03: Batch B — l2.4 Squares, l2.5 Is Subsequence, l2.6 Max Average Summary

Three more level-2 lessons (LeetCode 977, 392, 643) ship in Kotlin and Python. Each has 3 steps, at most 8 chips per bank, and every wrong chip has a tagged why whose counterexample was run.

## Tasks

| Task | Name | Commit |
|------|------|--------|
| 1 | Sidecars + generation l2.4-l2.6 (Kotlin + Python) | 09f7e64 |
| 2 | Tests green + review render 14-03-REVIEW-PENDING.md | 88260c2 |
| 3 | Customer review (checkpoint) | approved, no edits |

Holes per step: l2.4 1/2/3, l2.5 1/2/4, l2.6 1/2/3. There are two [hang] tags, both in l2.5 e3 (one in Kotlin, one in Python).

## Verification

- `python3 tools/gen_content.py --check`: exit 0.
- `python3 tools/test_gen_content.py`: 82 tests OK.
- `./gradlew :shared:testAndroidHostTest`: exit 0. WrongChipsTest ran 394 programs with 0 findings; 66 of those programs belong to l2.4-l2.6.
- I ran every why counterexample. Kotlin used the embedded kotlinc 2.4.21 (scratch script ktprobe.py), Python used python3. One text was corrected: l2.6 Python reports `UnboundLocalError`, not `NameError`.
- The orchestrator played batch B on `emulator-5554` with a debug build:
  - All 6 playthroughs passed via adb: l2.4-l2.6 in Kotlin and in Python.
  - Lessons unlock in order: l2.5 opens only after l2.4.
  - Comments fit on the screen.
  - Long code lines such as `windowSum += nums[i] - nums[i - k]` scroll horizontally.

## Замечания заказчика

None. The customer replied «ок». All 7 decisions in 14-03-REVIEW-PENDING.md were accepted as they are. l2.1-l2.3 needed no changes.

## Deviations from Plan

- **l2.4 loop form:** the plan suggested `resultIndex = nums.lastIndex`. I used the note's `for (writeIndex in nums.size - 1 downTo 0)` instead, because G5 requires the reference to match note 05 line by line.
- **l2.5 hang distractor:** the plan's "tIndex++ only on match" cannot be a single chip in the reference structure. In e3, a comparison chip placed in the `tIndex++` hole produces the same freeze (`"b"`, `"ab"`).
- **Titles shortened:** l2.5 is «Подпоследовательность» and l2.6 is «Максимальное среднее подмассива». Batch A did the same for l2.2.
- **Review file name:** I kept the `*-REVIEW-PENDING.md` name, following the phase convention.

## Known Stubs

None.

## Self-Check: PASSED

- FOUND: content/authoring/l2.{4,5,6}.md, content/authoring/py/l2.{4,5,6}.md, lessons/l2.{4,5,6}.json, lessons/py/l2.{4,5,6}.json
- FOUND: commits 09f7e64, 88260c2
