---
phase: 09-language-switch-python-pilot
plan: 05
subsystem: content
tags: [python, content, l1.4, pilot]
requires: [09-02, 09-03]
provides:
  - "lessons/py/l1.4.json: Python pilot lesson Two Sum (3 steps, 2/3/5 holes)"
  - "index.json: l1.4 langs [kotlin, python]"
affects: [content/authoring/py/l1.4.md, index.json, LessonContentTest]
tech-stack:
  added: []
  patterns: ["python sidecar with a why for every wrong (hole, chip) pair, checked by G12/G13"]
key-files:
  created:
    - content/authoring/py/l1.4.md
    - shared/src/commonMain/composeResources/files/content/lessons/py/l1.4.json
  modified:
    - shared/src/commonMain/composeResources/files/content/index.json
    - shared/src/androidHostTest/kotlin/com/tlenparty/algolingo/lesson/LessonContentTest.kt
decisions:
  - "SyntaxError placements are tagged [compile] (the program never runs)"
  - "nums.index(target - x) is correct in that position, so it is not a distractor; it is listed in the pitfalls as O(n²)"
  - "Hole counts are 2/3/5 so the step-3 bank holds 9 chips (limit 10)"
metrics:
  duration: ~25min
  completed: 2026-10-28
  tasks: 2
  files: 4
---

# Phase 9 Plan 05: Python pilot l1.4 Two Sum Summary

This plan adds the Python version of l1.4 Two Sum:

- **Theory:** 3 cards.
- **Steps:** 3, with 2, 3 and 5 holes.
- **Code:** the reference uses `seen = {}` and `enumerate`.
- **Why texts:** every wrong (hole, chip) pair has a why with a concrete counterexample, `[2, 7]` with target `9`.

The lesson passes G12 and G13. The customer approved it with no changes.

## Tasks

| # | Task | Commit |
|---|------|--------|
| 1 | Sidecar `py/l1.4.md`, generation, `index.json` langs, load test | 5646542 |
| 2 | Customer content review (checkpoint) | approved, no changes |

## Verification

- `python3 tools/gen_content.py --check` returns 0.
- G12 passes for the reference. G13 passes: all 55 unique wrong programs fail the checks.
- `python3 -m unittest tools/test_gen_content.py`: 74 tests OK.
- `./gradlew :shared:testAndroidHostTest` passes.
- Each why text was checked by running its wrong program on `[2, 7], 9`. The exception or output matches the text.
- In `index.json`, the only change is the `langs` line on l1.4. The Kotlin `lessons/l1.4.json` is unchanged.
- The sidecar has no imports (T-09-12).

## Deviations from Plan

**1. [Rule 3 - Blocking] The load test moved to androidHostTest.** `Res.readBytes` throws "No instrumentation registered" on the Android host JVM, so `loadLesson` in commonTest fails there. The test is now `LessonContentTest.pythonPilotLoadsBesideKotlin`. It reads `lessonPath("l1.4", lang)` from disk and calls `parseLesson`, with the same assertions as planned: Python contains `def two_sum` and has 2..3 exercises, and Kotlin contains `fun twoSum`.

**2. Distractor list adjusted to what the gates allow.**
- `nums.index(target - x)` returns a correct answer, so G13 would reject it as a wrong chip. It is covered in the pitfalls as O(n²).
- Putting `seen[x] = i` before the check is a line order, not a chip, so it is also in the pitfalls, with the counterexample `[3, 2, 4]`, `6` → `(0, 0)`.

**3. Hole counts are 2/3/5 instead of 2/4/6.** This keeps the bank at 10 chips or fewer while every (hole, chip) pair still gets its own why.

## Known Stubs

None.

## Self-Check: PASSED

- content/authoring/py/l1.4.md and lessons/py/l1.4.json exist; commit 5646542 exists.
