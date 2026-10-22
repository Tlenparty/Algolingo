---
phase: 05-level-0-content-end-to-end
plan: 01
subsystem: content-pipeline
tags: [generator, python, content, l0.4, reference-tests]
requires: [lesson/Model.kt validate(), files/kb/n07ed966c.md, files/content/index.json]
provides: [tools/gen_content.py, sidecar grammar, generated ReferenceSolutionsTest.kt, catalog-wide LessonContentTest]
affects: [05-02 (authoring l0.1-l0.3), 05-03/05-04 (review, walkthrough)]
tech-stack:
  added: []
  patterns: [Python 3.9 stdlib generator with committed output, --check golden hook, generated commonTest per variant]
key-files:
  created:
    - tools/gen_content.py
    - tools/test_gen_content.py
    - content/authoring/l0.4.md
    - shared/src/commonTest/kotlin/com/tlenparty/algolingo/lesson/ReferenceSolutionsTest.kt
  modified:
    - shared/src/commonMain/composeResources/files/content/index.json
    - shared/src/androidHostTest/kotlin/com/tlenparty/algolingo/lesson/LessonContentTest.kt
    - README.md
decisions:
  - "Generator reads the bundled kb copy of note 05, never the vault; output is committed, Gradle never runs Python"
  - "index.json is now pretty-printed (indent=2) like lessons; content unchanged"
  - "Generated test file uses `import kotlin.test.*` so later lessons can use assertTrue/assertFalse in examples"
  - "Malformed sidecar syntax raises GenError with rule `syntax` (outside G1-G8)"
metrics:
  duration: ~5 min
  completed: 2026-10-22
  tasks: 3
  files: 7
---

# Phase 5 Plan 01: Content Generator Summary

`tools/gen_content.py` (stdlib Python 3.9) turns note 05 + `content/authoring/l0.4.md` into `index.json`, `lessons/l0.4.json` (byte-identical to the hand-written file) and a generated `ReferenceSolutionsTest.kt` (reference + `ushr` variant), with G1-G8 fail-closed validation and a `--check` mode.

## Tasks

| # | Task | Commit |
|---|------|--------|
| 1 | RED: golden note parser (0.1-0.4), G1-G8 negatives, byte-identical l0.4, --check | 422c7be |
| 2 | GREEN: gen_content.py + l0.4 sidecar; l0.4.json regenerated identically; ReferenceSolutionsTest green on host + iosSimulatorArm64 | 4da2355 |
| 3 | LessonContentTest `everyCatalogLessonIsStrictAndValid` + README «Контент уроков» | b620268 |

## Verification

- `python3 tools/gen_content.py` → `git diff --exit-code` on l0.4.json empty (identical on first run).
- `python3 tools/gen_content.py --check` exits 0; `python3 -m unittest tools/test_gen_content.py`: 25 tests OK.
- `./gradlew :shared:allTests` green; ReferenceSolutionsTest 2/2 on androidHost and iosSimulatorArm64; LessonContentTest 4/4.
- `/Users/` grep over all generated files, sidecar, generator: 0.

## Sidecar grammar (hand-off for 05-02)

Authoritative copy: module docstring of `tools/gen_content.py`. Summary:

```
# l0.N                          id == file stem (G7)
note: 0.N <heading>             `## 0.N <heading>` in note 05 (G7 if missing)
title: / statement: / trains:   optional one-line overrides
pitfalls: / variations:         optional bullet overrides (use pitfalls: for 0.2/0.3)
examples:                       >=1 Kotlin statements (G8), run in with(variant) { ... }
- assertEquals(...)

## theory <title>               keys signals: (bullets) skeleton: (``` fence) pitfalls: (bullets) body: (one line)

## eK                           one ```kotlin fence, FULL function, holes ⟦ok | ok2 | ~acceptable⟧
- `T`: why                      distractor + chip why (why needs a `backtick` span, G3)
- `T`                           distractor with no chip why -> needs @H for every hole (G2)
- @H `T`: why                   holes[H-1].wrongWhy[T]; T must be in bank and not correct for H (G6)
- ~`T`: note                    acceptable note (every ~T needs one, G6)
? hint
```

- Bank order: ok[0] per hole (with multiplicity), other ok/acceptable, then distractors in bullet order. Bank <= 10 (G7).
- A distractor bullet whose text is correct in any hole is rejected (G6); use `@H` to explain a correct-elsewhere chip.
- `//` comments are ignored in the drift check vs note (G5), but all exercises must yield the identical code, which becomes `solution.code`.
- wrongWhy key order = `@H` bullet order. Theory keys emit in order title, signals, skeleton, pitfalls, body.
- Lessons are ordered by numeric id; that is the index.json (unlock) order. Removing an id needs `--allow-removed <id>` (G4).

## Deviations from Plan

None - plan executed as written. Small additions: a hidden `--root` CLI option (used by tests to run the generator against a temp repo) and a `syntax` error class for malformed sidecar lines.

## TDD Gate Compliance

RED `test(05-01)` 422c7be → GREEN `feat(05-01)` 4da2355. No refactor commit needed.

## Known Stubs

None.

## Self-Check: PASSED

All created files exist; commits 422c7be, 4da2355, b620268 present in `git log`.
