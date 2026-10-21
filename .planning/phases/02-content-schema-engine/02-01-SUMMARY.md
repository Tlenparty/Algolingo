---
phase: 02-content-schema-engine
plan: 01
subsystem: content
tags: [kotlinx-serialization, lesson-schema, validate, l0.4]
requires:
  - phase: 01.1
    provides: kb content layout and KbContentTest file-reading convention
provides:
  - Lesson/Exercise/Hole/Chip schema and validate() in commonMain
  - Hand-written l0.4.json (binary search, ladder 1/2/4 holes)
affects: [02-02 engine, 02-03 reducer, phase 3 UI, phase 5 generator]
tech-stack:
  added: []
  patterns: ["validate() returns messages, never throws", "chip identity = bank index", "strict decode in tests, lenient at runtime"]
key-files:
  created:
    - shared/src/commonMain/kotlin/com/tlenparty/algolingo/lesson/Model.kt
    - shared/src/commonMain/composeResources/files/content/lessons/l0.4.json
    - shared/src/commonTest/kotlin/com/tlenparty/algolingo/lesson/ModelTest.kt
    - shared/src/androidHostTest/kotlin/com/tlenparty/algolingo/lesson/LessonContentTest.kt
  modified: []
key-decisions:
  - "No Chip id field; identity is bank index (EXER-05)"
  - "validate() uses only equality/list ops (no Regex)"
requirements-completed: [LESS-03]
duration: 15min
completed: 2026-10-21
---

# Phase 2 Plan 01: Content schema and l0.4 Summary

**Per-hole lesson schema with a non-throwing validate() and a strict-decoded, validated binary-search lesson (1 -> 2 -> 4 holes).**

## Accomplishments
- Model.kt: schema per RESEARCH plus `validate()` covering segment/hole shape, bank membership, multiset assignability, distractor `why`, bank <= 10, reference drift, ladder, ids, schema, acceptable/ok overlap.
- ModelTest (commonTest) covers every rule plus inline-JSON decode.
- l0.4.json generated from the note's code (segments rebuilt exactly); e3 has two separate "1" chips.
- LessonContentTest decodes with `ignoreUnknownKeys = false`, validate() empty, hole counts [1,2,4].

## Task Commits
1. Task 1: 0e0c9e0
2. Task 2: 6af598f

## Deviations from Plan
None. `:shared:testAndroidHostTest` is green. iOS test run not part of this plan.

## Notes
- The `(lo + hi) ushr 1` acceptable note is left for customer review (CONT-06, assumption A4).
- Known stubs: none.

## Self-Check: PASSED
