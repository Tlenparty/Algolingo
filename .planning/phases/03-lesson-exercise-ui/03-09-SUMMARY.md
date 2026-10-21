---
phase: 03-lesson-exercise-ui
plan: 09
subsystem: design-system
tags: [kmp, gradle-module, compose-resources, theme, design-tokens]
requires: [03-07]
provides: [":design module", AlgoTheme, AlgoColors, AlgoTypography, AlgoSpacing, AlgoSizes, AlgoShapes, highlight, NO_LIGATURES]
affects: [03-10]
requirements: [UI-01]
key-files:
  created:
    - design/build.gradle.kts
    - design/src/commonMain/kotlin/com/tlenparty/algolingo/design/AlgoTheme.kt (git mv from shared ui/theme/Theme.kt)
    - design/src/commonMain/kotlin/com/tlenparty/algolingo/design/Highlight.kt (git mv from shared ui/theme/Highlight.kt)
    - design/src/commonMain/composeResources/font/*.ttf (git mv from shared)
  modified:
    - settings.gradle.kts
    - shared/build.gradle.kts
    - 15 shared UI files (App, Home, lesson/ui/*, kb/ui/*, ui/components/*)
    - CLAUDE.md
    - .planning/research/STACK.md
    - README.md
decisions:
  - M3 ColorScheme stays the source for M3 slots (private LightScheme/DarkScheme in AlgoTheme.kt); AlgoTheme.colors holds only non-M3 colors
  - Spacing/sizes/shapes are single-instance data classes whose defaults are the token values; their locals are not re-provided in AlgoTheme() because the defaults are the same instances' values
  - design's Res class stays internal (generated package algolingo.design.generated.resources); consumers get fonts via AlgoTheme.typography
  - Static Shared framework links :design without export; Compose resources from :design reach both the APK and the iOS .app with no extra config
metrics:
  tasks: 3
  completed: 2026-10-21
---

# Phase 3 Plan 09: :design Module and AlgoTheme Summary

A new KMP library module `:design` (Android, iosArm64, iosSimulatorArm64, namespace `com.tlenparty.algolingo.design`) owns the theme. The old `AppTheme` and its locals became `AlgoTheme`, which exposes `colors`, `typography` (with `code` font family and the emoji sizes `iconEmoji`/`nodeEmoji`/`heroEmoji`), `spacing`, `sizes` and `shapes`, all with the exact values from the plan and UI-SPEC. Nunito and JetBrains Mono, `highlight()` and `NO_LIGATURES` moved too. `shared` now depends on `:design`, and every call site uses `AlgoTheme.*`. Colors, fonts and NO_LIGATURES are unchanged, so the app should look exactly as before.

## Commits
- c2650d4 feat(03-09): add :design module with AlgoTheme tokens and migrate shared (Tasks 1+2, one atomic commit as the plan requires)
- c22b14a docs(03-09): document shared + design module layout

## Verification
- `./gradlew :shared:allTests :design:compileKotlinIosSimulatorArm64 :androidApp:assembleDebug` passed.
- After a clean build, the APK contains only `assets/composeResources/algolingo.design.generated.resources/font/{jetbrains_mono_*,nunito_*}.ttf`. The first incremental APK also had stale shared font copies, which a `:shared:clean` removed.
- An xcodebuild simulator build (destination id 4461DE0A-…, `-derivedDataPath build/ios`) ended with BUILD SUCCEEDED. `Algolingo.app/compose-resources/composeResources/algolingo.design.generated.resources/font/` contains all 4 fonts.
- `grep -rnE 'ui\.theme|LocalAppColors|LocalAppText|LocalCodeFont|AppTheme|AppTextStyles|AppColors' shared/src` finds 0 matches. `\b(24|28|64)\.sp\b` in shared also finds 0 matches.
- No device install was done (no installDebug, no adb).

## Docs
- The "Project Template / Module Layout" section in CLAUDE.md now describes shared + design + androidApp/iosApp, says shared depends on design, and adds the rule against raw dp/sp/Color in shared. The user approved this change. The "What NOT to Use" row now reads "in shared and design".
- `.planning/research/STACK.md` received the same two edits, because the CLAUDE.md stack section is generated from it and would otherwise regenerate "one shared module".
- README.md has a new "## Модули" section.

## Deviations from Plan
- STACK.md was updated alongside CLAUDE.md, as the orchestrator instructed. Without that, the next CLAUDE.md regeneration would bring the old rule back.
- Otherwise the plan was executed as written.

## Known Stubs
None. Raw dp literals in shared are intentionally left for 03-10.

## Self-Check: PASSED
- design/build.gradle.kts, AlgoTheme.kt, Highlight.kt and the 4 fonts exist. shared/.../ui/theme and shared composeResources/font are gone.
- Commits c2650d4 and c22b14a exist, both authored by tlenparty with no AI trailers.
