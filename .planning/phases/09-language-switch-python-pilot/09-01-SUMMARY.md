---
phase: 09-language-switch-python-pilot
plan: 01
subsystem: design
tags: [highlight, python, compositionlocal]
requires: []
provides: [CodeLang, LocalCodeLang, "highlight(code, colors, lang)"]
affects: [CodeBlock, TokenChip, CodeTemplate, ResultPanel]
tech-stack:
  added: []
  patterns: ["language via CompositionLocal (LocalCodeLang), single when(lang) in Highlight.kt"]
key-files:
  created:
    - design/src/commonMain/kotlin/com/tlenparty/algolingo/design/CodeLang.kt
    - shared/src/commonTest/kotlin/com/tlenparty/algolingo/design/HighlightTest.kt
  modified:
    - design/src/commonMain/kotlin/com/tlenparty/algolingo/design/Highlight.kt
    - design/src/commonMain/kotlin/com/tlenparty/algolingo/design/components/CodeBlock.kt
    - design/src/commonMain/kotlin/com/tlenparty/algolingo/design/components/TokenChip.kt
    - shared/src/commonMain/kotlin/com/tlenparty/algolingo/ui/components/CodeTemplate.kt
    - shared/src/commonMain/kotlin/com/tlenparty/algolingo/ui/components/ResultPanel.kt
decisions:
  - "CodeLang(val id: String) enum in design without serialization; id is the JSON form kotlin/python for shared to map"
  - "Kotlin and Python regexes share one suffix (strings|numbers|identifiers); only the comment prefix differs (// vs #)"
metrics:
  duration: ~10min
  completed: 2026-10-28
  tasks: 2
  files: 7
---

# Phase 9 Plan 01: CodeLang and Python highlighting Summary

The design module now has `enum CodeLang(id) { Kotlin, Python }` and `LocalCodeLang` (Kotlin by default). `highlight(code, colors, lang = Kotlin)` contains a single `when (lang)` that picks the regex and keyword pair. Python comments start with `#`, and `//` in Python is not treated as a comment. The four code components read the language from `LocalCodeLang.current`, and their signatures are unchanged.

## Tasks

| # | Task | Commit |
|---|------|--------|
| 1 | CodeLang, LocalCodeLang, highlight(lang) (TDD) | fe0cb3a (RED), b22c027 (GREEN) |
| 2 | CodeBlock/TokenChip/CodeTemplate/ResultPanel read LocalCodeLang | 454fddb |

## Verification

- `HighlightTest`: 7 tests passed. Cases covered: Python keywords plus `#`, `//` in Python, `//` in Kotlin, default equals Kotlin, a 5000-character line (T-09-01), strings and numbers in both languages, `#` inside a Python string, and `id`.
- `./gradlew :shared:testAndroidHostTest :androidApp:assembleDebug` passed.
- `when (lang)` appears only in Highlight.kt. The only `highlight(` call without lang is BlockView, where KB stays Kotlin.

## Deviations from Plan

None, the plan was executed as written. The worktree branch started from the wrong base (9282369), so it was reset to 4cf70ea as the branch check prescribes.

## TDD Gate Compliance

RED fe0cb3a (compile failure: CodeLang did not exist), then GREEN b22c027.

## Self-Check: PASSED
