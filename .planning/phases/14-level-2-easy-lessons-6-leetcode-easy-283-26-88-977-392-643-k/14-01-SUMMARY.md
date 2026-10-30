---
phase: 14-level-2-easy-lessons
plan: 01
subsystem: content
tags: [kb-note, level-2, two-pointers, sliding-window]
requires: []
provides:
  - "note 05 headings ## 2.1-2.6 (Easy LeetCode 283, 26, 88, 977, 392, 643) for sidecars l2.* (14-02, 14-03)"
  - "old level-2 sections renumbered 2.7-2.13"
affects: [14-02, 14-03]
tech-stack:
  added: []
  patterns: ["Kotlin references with short // comments (<=36 chars incl. indent) above key lines"]
key-files:
  created: []
  modified:
    - shared/src/commonMain/composeResources/files/kb/n07ed966c.md
    - shared/src/commonMain/composeResources/files/kb/n17006052.md
    - "vault: 05 Тренажёр решений.md, 06 Движок «маркеры и дырки».md (outside repo)"
key-decisions:
  - "Merge Sorted Array reference uses index1/index2/mergeIndex instead of i/j (style rule: no one-letter names except i); loop condition is while (index2 >= 0)"
  - "moveZeroes tail loop split into multi-line body (behaviour unchanged) so sidecar holes can target single statements"
metrics:
  duration: 35min
  completed: 2026-10-30
---

# Phase 14 Plan 01: Note 05 level 2 renumbered + Easy sections 2.2-2.6 Summary

Note 05 level 2 now opens with six LeetCode Easy sections (283, 26, 88, 977, 392, 643) with commented, compile-tested Kotlin references. The old sections moved to 2.7-2.13, and note 06 links follow them. The bundle copies are byte-identical to the vault.

## Tasks

| Task | Name | Commit |
|------|------|--------|
| 1 | Renumber 2.2-2.8 -> 2.7-2.13 in vault 05/06 (headings, «Скелет 2.10», «скелет 2.8», ladder 2.1–2.13, progress table, day plan 2.7–2.8 / 2.10–2.12 / 2.9 + 3.1; 06 -> #2.7, #2.10 «и 2.11») | vault only (shipped in d648d62) |
| 2 | New sections 2.2-2.6, 2.1 moveZeroes restyled with 5 comments, 5 progress-table rows after 2.1 | vault only (shipped in d648d62) |
| 3 | cp vault -> bundle, regression | d648d62 |

## Verification

- Kotlin references were compiled with kotlin-compiler-embeddable 2.4.21 from the Gradle cache on JDK 21, then run:
  - LeetCode examples: 283 `[0,1,0,3,12]`, `[0]`; 26 `[1,1,2]`→2, `[0,0,1,1,1,2,2,3,3,4]`→5; 88 three examples plus the case where nums1 runs out first; 977 both examples; 392 `abc/ahbgdc`, `axc/ahbgdc`, empty `s`; 643 `[1,12,-5,-6,50,3],4`→12.75, `[5],1`→5.0, all-negative input→-1.5. All passed.
  - A brute-force oracle compared 2000 random small inputs for 283/26/88/977/643. All passed.
- Comment check: every reference has 5-6 `//` lines, each at most 36 chars including indentation. The only one-letter local is `i`, in the 643 index loops. The one-letter parameters (m, n, s, t, k) are the fixed LeetCode signatures.
- `cmp` vault vs bundle: 05 and 06 are identical.
- `python3 tools/gen_content.py --check`: exit 0 (2.5 min).
- `./gradlew :shared:testAndroidHostTest`: exit 0.
- `python3 tools/test_gen_content.py`: **FAILED. This failure already existed before this plan and is not caused by it.** The same failure appears with the HEAD bundle. The test fixture still uses `lo/hi` from note 0.4, which quick task 261029-pbe renamed. Logged in `deferred-items.md`.
- `kb/index.json` holds no hash or size, so it did not need an update.

## Deviations from Plan

- **[Style] Merge reference names:** the plan text says `while (j >= 0)`, but the style rule and the acceptance criteria forbid one-letter locals other than `i`. The reference uses `index1`/`index2`/`mergeIndex` with `while (index2 >= 0)`. The distractor idea from CONTEXT becomes `index1 >= 0` vs `index2 >= 0`.
- **[Out of scope] test_gen_content.py** fails before this plan. It is deferred and not fixed (see above).

## Known Stubs

None.

## Self-Check: PASSED

- FOUND: shared/src/commonMain/composeResources/files/kb/n07ed966c.md (13 `## 2.` headings, `## 2.13 Максимальная дистанция до соседа`)
- FOUND: commit d648d62
