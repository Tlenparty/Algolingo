---
phase: 09-language-switch-python-pilot
plan: 04
subsystem: ui
tags: [python, home, lang-switch, compositionlocal]
requires: [09-01, 09-02]
provides: ["LangSegment(selected, onSelect)", "HomeScreen(lang, onLang)", "LessonRoute.lang", "LessonHost(id, lang, review, ...)"]
affects: [App.kt, HomeScreen.kt, LessonHost.kt]
tech-stack:
  added: []
  patterns: ["language frozen in the nav route at lesson start", "LocalCodeLang provided once in LessonHost"]
key-files:
  created:
    - design/src/commonMain/kotlin/com/tlenparty/algolingo/design/components/LangSegment.kt
  modified:
    - shared/src/commonMain/kotlin/com/tlenparty/algolingo/App.kt
    - shared/src/commonMain/kotlin/com/tlenparty/algolingo/home/HomeScreen.kt
    - shared/src/commonMain/kotlin/com/tlenparty/algolingo/lesson/ui/LessonHost.kt
decisions:
  - "The language is stored in LessonRoute as a string id (lang: String = \"kotlin\"), so it is fixed when the lesson starts and survives process restore. It is mapped back with CodeLang.entries.firstOrNull { it.id == ... } ?: Kotlin. A string avoids depending on navigation's enum NavType on iOS."
  - "HomeScreen shows the segment only when onLang != null. HomeTab passes onLang only when byLang, so the SD tab has no segment."
  - "LangSegment uses Modifier.selectable(role = RadioButton) inside a selectableGroup Row. The label is CodeLang.name and the testTag is lang-<id>."
metrics:
  duration: ~10min
  completed: 2026-10-28
  tasks: 2
  files: 4
---

# Phase 9 Plan 04: Language switch UI Summary

Home now has a "Kotlin | Python" segment (`LangSegment` in design) above the lesson path. Tapping a segment calls `progressRepo.setLang`. Lessons that do not exist in the selected language are shown as `Soon`: the node looks like Locked, has the caption "Скоро на Python", and has no onClick. The language is fixed in `LessonRoute` when the lesson starts. `LessonHost` passes it to `LessonViewModel(lang = ...)` and provides `LocalCodeLang` once, around the whole when-block.

## Tasks

| # | Task | Commit |
|---|------|--------|
| 1 | LangSegment in design/components | bb8a292 |
| 2 | Home segment + Soon node; App/LessonHost pass lang into the lesson | 251f242 |

## Verification

- `./gradlew :shared:testAndroidHostTest :androidApp:assembleDebug :shared:compileKotlinIosSimulatorArm64` passed.
- `LocalCodeLang provides` appears once in LessonHost.kt. "Скоро на Python" is defined once, as `SOON` in HomeScreen.kt. `byLang = false` is still used for SdHome in App.kt.
- LangSegment has no dp/sp/Color(0x literals, and design does not import lesson or progress.
- The remaining `CodeLang.Kotlin` uses in App/HomeScreen are defaults or fallbacks, not when/if branches on the language.
- Nothing was run on a device, as instructed.

## Deviations from Plan

- **The language goes through the route, not as `p.lang` read during LessonRoute composition.** The plan says to pass the current `p.lang` to LessonHost and to put the language in the viewModel key. Reading `p.lang` while the lesson is composed would recreate the VM on a language change. Freezing the language in `LessonRoute` at navigate time gives the required "fixed at start" behavior and survives process restore. The VM key still includes the language (`$id/$review/$lang`).

## Known Stubs

None.

## Self-Check: PASSED
