---
phase: 07-full-interview-knowledge-base
plan: 02
subsystem: kb
tags: [kb, search, compose, ui]
requires: []
provides: [collapsible-kb-sections, cached-search]
affects: [KbListScreen, searchNotes]
tech-stack:
  added: []
  patterns: [rememberSaveable List<String> for open sections, remember(query, notes) for derived search, lazy cached lowercase]
key-files:
  created: []
  modified:
    - shared/src/commonMain/kotlin/com/tlenparty/algolingo/kb/KbModels.kt
    - shared/src/commonMain/kotlin/com/tlenparty/algolingo/kb/KbRepository.kt
    - shared/src/commonMain/kotlin/com/tlenparty/algolingo/kb/ui/KbListScreen.kt
    - shared/src/commonTest/kotlin/com/tlenparty/algolingo/kb/KbSearchTest.kt
decisions:
  - "KB sections start collapsed; open set is a rememberSaveable List<String> of section names"
  - "Note caches lowerTitle/lowerRaw via body-level by lazy; constructor and equals unchanged"
metrics:
  duration: ~10m
  completed: 2026-10-22
requirements: [KB-07, KB-02, KB-06]
---

# Phase 7 Plan 02: Collapsible KB sections and cached search Summary

The KB list is now collapsible topic headers ("▸ Раздел (N)" / "▾ ..."), all collapsed by default, with the open set kept in rememberSaveable. Global search runs inside `remember(query, notes)` over lowercase note text cached with `by lazy`.

## Tasks

| Task | Name | Commit |
|------|------|--------|
| 1 | Cached lowercase text for search (+ new KbSearchTest case) | 3cc8776 |
| 2 | Collapsible section headers + remembered search | 8e21c41 |

## Verification

- `./gradlew :shared:allTests :androidApp:assembleDebug` is green.
- KbListScreen contains `remember(query`, `▸`, `▾` and `rememberSaveable`. App.kt is unchanged. No new dp/sp/Color values.
- Both commits are authored by tlenparty, with no trailers.

## Deviations from Plan

- I made two commits (one per task) instead of the single combined commit the plan named. This follows the per-task commit protocol.
- TDD: the new test case passed before the code changed. Task 1 is a pure performance refactor with unchanged behavior, so the test guards against regressions; it was never a failing RED test. Because of that, the test and the implementation share one commit.

## Threat mitigations

- T-07-06: search still uses a plain `indexOf` substring match. The `metacharactersLiteral` test is kept.
- T-07-07: search uses `remember(query, notes)` plus cached lowercase text.

## Known Stubs

None.

## Self-Check: PASSED
