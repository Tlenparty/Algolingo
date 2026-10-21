---
phase: 03-lesson-exercise-ui
plan: 01
subsystem: ui-theme
tags: [fonts, theme, compose]
requires: []
provides: [LocalAppText, LocalCodeFont, extended AppColors, bundled fonts]
affects: [all Phase 3 UI components, KB screens]
tech-stack:
  added: [Nunito (OFL), JetBrains Mono (OFL)]
  patterns: [CompositionLocal for text styles/code font]
key-files:
  created:
    - shared/src/commonMain/composeResources/font/nunito_regular.ttf
    - shared/src/commonMain/composeResources/font/nunito_bold.ttf
    - shared/src/commonMain/composeResources/font/jetbrains_mono_regular.ttf
    - shared/src/commonMain/composeResources/font/jetbrains_mono_bold.ttf
    - third_party/fonts/README.md
    - third_party/fonts/OFL-Nunito.txt
    - third_party/fonts/OFL-JetBrainsMono.txt
  modified:
    - shared/src/commonMain/kotlin/com/tlenparty/algolingo/ui/theme/Theme.kt
    - shared/src/commonMain/kotlin/com/tlenparty/algolingo/kb/ui/BlockView.kt
decisions:
  - Nunito static instances generated from the variable font with fonttools (throwaway venv, documented in README)
metrics:
  tasks: 2
  completed: 2026-10-21
---

# Phase 3 Plan 01: Fonts and Theme Summary

Nunito (UI) and JetBrains Mono (code) bundled as Compose resources; AppTheme now provides LocalAppText (8 styles, Normal/Bold only), LocalCodeFont and 10 extra AppColors fields, and applies Nunito to all M3 typography.

## Commits
- 623e428 fonts and OFL licenses
- 8d29eab theme extension

## Deviations from Plan
None - plan executed as written. `:shared:testAndroidHostTest` and iOS simulator compile pass.

## Known Stubs
None.

## Self-Check: PASSED
