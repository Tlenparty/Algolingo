---
phase: 09-language-switch-python-pilot
verified: 2026-10-28T00:00:00Z
status: passed
score: 9/9 must-haves verified
overrides_applied: 0
---

# Phase 9: Language Switch + Python Pilot Verification Report

**Phase Goal:** The user switches "Kotlin | Python" on Home, and pilot lesson l1.4 Two Sum is assembled from Python chips. Progress is shared and the language choice persists (spec components 1-7).
**Status:** passed. **Re-verification:** No (initial).

## Observable Truths

| # | Truth | Status | Evidence |
|---|-------|--------|----------|
| 1 | `CodeLang` enum in `design` and `Progress.lang` default Kotlin; unknown value falls back to Kotlin; schema stays 1 | VERIFIED | `design/.../CodeLang.kt`; `Progress.kt` `CodeLangSerializer` falls back to Kotlin; ProgressTest and ProgressRepositoryTest pass |
| 2 | `ProgressRepository.setLang` persists through the same `write` path as `record` | VERIFIED | `ProgressRepository.kt:80` `write { it.copy(lang = lang) }`; the language is read from Progress on Home, and the device restart showed Python still selected |
| 3 | `lessonPath/loadLesson(id, lang)`: sd ignores lang, Kotlin and `py/` paths | VERIFIED | `LessonRepository.kt:26-31`; LessonRepositoryTest passes |
| 4 | Catalog `langs`, `Soon` status, unlock chain over language-available lessons, `dueIds` filter, SD unfiltered (`lang=null`) | VERIFIED | `Catalog.kt` `lessonStatuses(available)`, `homeModel(lang)`; `App.kt` passes `byLang=false` for SdHome; l1.4 has `["kotlin","python"]` in `index.json`; CatalogTest passes |
| 5 | Home segment "Kotlin | Python", only on Home (not SdHome); Soon node is non-tappable | VERIFIED | `LangSegment` in design/components; `onLang = null` for SD; device logs: SD has no segment, tap on a Soon node does nothing |
| 6 | Highlight: Python keywords and `#` comment, `//` is not a comment; components read `LocalCodeLang`; `LessonHost` provides it once | VERIFIED | `Highlight.kt` `when(lang)` selects rules, one loop; `LessonHost.kt:35`; CodeBlock, TokenChip, CodeTemplate and ResultPanel read the local; HighlightTest passes |
| 7 | Language branching confined to `lessonPath`, `Highlight`, `homeModel` (`lang.id in langs`) | VERIFIED | grep of commonMain: the only `when(lang)` sites are `lessonPath` and `Highlight`. `LessonRoute.lang` is a string that is mapped back once in `App.kt`, which only freezes the language at lesson start. `BlockView` `block.lang=="kotlin"` is KB code from before this phase |
| 8 | Generator: py sidecar, `lessons/py`, `langs` update, `python3 -I` harness with checks; Kotlin output unchanged | VERIFIED | `python3 -I -m unittest discover -s tools` ran 74 tests, OK. Regeneration changed nothing in the tracked tree. `content/authoring/py/l1.4.md` and `lessons/py/l1.4.json` exist |
| 9 | Pilot l1.4 on Python works end to end, progress shared, language persists across restart | VERIFIED | `:shared:testAndroidHostTest` and `:design:allTests` pass (exit 0), including `scenarioPython`. 09-VERIFICATION-DEVICES.md records Android (Pixel 7) and iOS (simulator) runs: switch, build the solution, +30 XP, restart keeps Python, l1.4 Done on the Kotlin path. I did not re-run devices. Customer content review approved (09-05-SUMMARY) |

## CONTEXT decisions

Honored: branching confined to the allowed sites; `LocalCodeLang`; unlock chain over available lessons; SD unfiltered; unknown lang goes to Kotlin without a progress reset; an open lesson is not reloaded (the language is part of the route and the ViewModel key).

## Anti-patterns, requirements

No TBD/FIXME blockers found in the phase files that I inspected. No REQUIREMENTS.md IDs map to this phase (ad-hoc PY-* labels), so there are no orphaned requirements.

## Human verification

None outstanding. Device evidence is already recorded, and the customer review of l1.4 was approved.

## Notes (non-blocking)

The `#` comment color is not seen on devices because the pilot lesson has no `#` comment. HighlightTest covers it.

_Verifier: Claude (gsd-verifier)_
