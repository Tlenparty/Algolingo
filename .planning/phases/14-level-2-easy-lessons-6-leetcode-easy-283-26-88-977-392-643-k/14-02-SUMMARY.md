---
phase: 14-level-2-easy-lessons
plan: 02
subsystem: content
tags: [lessons, level-2, two-pointers, kotlin, python]
requires: ["14-01: note 05 sections 2.1-2.3"]
provides:
  - "lessons l2.1 (283), l2.2 (26), l2.3 (88) in Kotlin + Python, langs [kotlin, python] in index.json"
affects: [14-03, 14-04]
tech-stack:
  added: []
  patterns: ["python lesson code carries the same // comments as Kotlin (as # lines)"]
key-files:
  created:
    - content/authoring/l2.1.md
    - content/authoring/l2.2.md
    - content/authoring/l2.3.md
    - content/authoring/py/l2.1.md
    - content/authoring/py/l2.2.md
    - content/authoring/py/l2.3.md
    - shared/src/commonMain/composeResources/files/content/lessons/l2.{1,2,3}.json
    - shared/src/commonMain/composeResources/files/content/lessons/py/l2.{1,2,3}.json
    - .planning/phases/14-level-2-easy-lessons-6-leetcode-easy-283-26-88-977-392-643-k/14-02-REVIEW-PENDING.md
  modified:
    - shared/src/commonMain/composeResources/files/content/index.json
    - shared/src/commonTest/kotlin/com/tlenparty/algolingo/lesson/ReferenceSolutionsTest.kt
    - shared/src/androidHostTest/resources/wrong_variants.json
key-decisions:
  - "l2.2: nums[fast - 1] in the compare hole is ~acceptable (canon nums[slow - 1] generalises to LeetCode 80)"
  - "l2.3: >= in the compare hole is ~acceptable (equal numbers are indistinguishable)"
  - "Python lessons of level 2 carry the same comments as Kotlin; idioms that bypass the technique only in variations"
metrics:
  duration: 75min
  completed: 2026-10-30
---

# Phase 14 Plan 02: Batch A — l2.1 Move Zeroes, l2.2 Remove Duplicates, l2.3 Merge Sorted Array Summary

Three level-2 lessons (LeetCode 283, 26, 88) ship in Kotlin and Python. Each has 3 steps, at most 7 chips per bank, and every wrong chip comes with a tagged why that has been proven against its counterexample.

## Tasks

| Task | Name | Commit |
|------|------|--------|
| 1 | Sidecars + generation l2.1-l2.3 (Kotlin + Python) | 69257a7 |
| 2 | Tests green + review render 14-02-REVIEW-PENDING.md | 4759cbb |
| 3 | Customer review (checkpoint) | approved, no edits |

Holes per step: l2.1 1/2/4, l2.2 1/2/4, l2.3 1/2/3. There are no [hang] tags in batch A.

## Verification

- `python3 tools/gen_content.py --check`: exit 0.
- `python3 tools/test_gen_content.py`: 82 tests OK.
- `./gradlew :shared:testAndroidHostTest`: exit 0. WrongChipsTest ran 328 programs with 0 findings; 75 of those programs belong to l2.x.
- I ran every Python why counterexample with python3 and confirmed it. The Kotlin claims were confirmed by WrongChipsTest.
- The orchestrator played batch A on `emulator-5554` with a debug build:
  - All 6 playthroughs passed via adb: l2.1–l2.3 in Kotlin and in Python.
  - Placing the wrong chip `>` in l2.1 shows its why.
  - The code lines in l2.3 are wide, so the code block scrolls horizontally. This works.

## Замечания заказчика

None. The customer replied «да все ок». All 8 decisions in 14-02-REVIEW-PENDING.md were accepted as they are:
- `~nums[fast - 1]` is acceptable in l2.2.
- `~>=` is acceptable in l2.3.
- The Python code carries the same comments as Kotlin.
- Python and Kotlin tag some wrong chips differently: `slow = 0`, `merge_index = index2 >= 0`, `while m + n - 1`.

For 14-03, keep the same style.

## Deviations from Plan

- **[Rule 2 - Content] l2.1 Kotlin `trains:` override:** the note's text carries a wikilink `[[04 …]]`, which would show up raw in the app.
- **[Rule 2 - Content] l2.1 statement:** «LeetCode 283» was added to the statement.
- **[Rule 2 - Content] l2.1 pitfalls and variations in the sidecar:** note 2.1 has no «Вариации», so I set them in the sidecar.
- I did not rename the review file. Phase 10 kept its `*-REVIEW-PENDING.md` names after approval, so 14-02 follows the same convention.

## Known Stubs

None.

## Self-Check: PASSED

- FOUND: content/authoring/l2.{1,2,3}.md, content/authoring/py/l2.{1,2,3}.md, lessons/l2.*.json, lessons/py/l2.*.json
- FOUND: commits 69257a7, 4759cbb
