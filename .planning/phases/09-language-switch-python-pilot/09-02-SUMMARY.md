---
phase: 09-language-switch-python-pilot
plan: 02
subsystem: progress, lesson-loading, catalog
tags: [python, progress, catalog, lessonPath]
requires: [09-01]
provides:
  - "Progress.lang (JSON \"kotlin\"/\"python\", unknown -> Kotlin)"
  - "ProgressRepository.setLang(lang): Job"
  - "lessonPath(id, lang), loadLesson(id, lang)"
  - "LessonViewModel(lang = ...)"
  - "CatalogEntry.langs, LessonStatus.Soon, homeModel(..., lang: CodeLang? = p.lang)"
affects: [App.kt HomeTab, HomeScreen PathNode]
tech-stack:
  added: []
  patterns: ["language as data: availability predicate passed into lessonStatuses/dueIds"]
key-files:
  created: []
  modified:
    - shared/src/commonMain/kotlin/com/tlenparty/algolingo/progress/Progress.kt
    - shared/src/commonMain/kotlin/com/tlenparty/algolingo/progress/ProgressRepository.kt
    - shared/src/commonMain/kotlin/com/tlenparty/algolingo/progress/Catalog.kt
    - shared/src/commonMain/kotlin/com/tlenparty/algolingo/lesson/LessonRepository.kt
    - shared/src/commonMain/kotlin/com/tlenparty/algolingo/lesson/LessonViewModel.kt
    - shared/src/commonMain/kotlin/com/tlenparty/algolingo/App.kt
    - shared/src/commonMain/kotlin/com/tlenparty/algolingo/home/HomeScreen.kt
    - shared/src/androidHostTest/kotlin/com/tlenparty/algolingo/lesson/LessonContentTest.kt
    - shared/src/commonTest/kotlin/com/tlenparty/algolingo/progress/ProgressTest.kt
    - shared/src/commonTest/kotlin/com/tlenparty/algolingo/progress/ProgressRepositoryTest.kt
    - shared/src/commonTest/kotlin/com/tlenparty/algolingo/progress/CatalogTest.kt
    - shared/src/commonTest/kotlin/com/tlenparty/algolingo/lesson/LessonRepositoryTest.kt
    - shared/src/commonTest/kotlin/com/tlenparty/algolingo/lesson/LessonViewModelTest.kt
    - shared/src/commonTest/kotlin/com/tlenparty/algolingo/e2e/E2eScenarios.kt
decisions:
  - "CodeLang is stored in Progress through CodeLangSerializer in Progress.kt (string id, unknown -> Kotlin); PROGRESS_SCHEMA stays 1"
  - "record and setLang share one private write() with retry + saveFailed"
  - "lessonStatuses/dueIds take an optional available predicate (default: everything), so existing calls are unchanged"
  - "HomeTab gets byLang: Boolean = true; the SD tab passes byLang = false, which becomes homeModel(..., lang = null)"
metrics:
  duration: ~15min
  completed: 2026-10-28
  tasks: 3
  files: 14
---

# Phase 9 Plan 02: Language in progress, loading and catalog Summary

`Progress` now remembers the language. It is stored as `"kotlin"`/`"python"`, and an unknown value reads as Kotlin without resetting the rest. `setLang` writes through the same retry and `saveFailed` path as `record`. Lessons load from `lessons/py/` when the language is Python. On Home, lessons that do not exist in the selected language get the `Soon` status and drop out of the unlock chain and the due list. The SD tab ignores the language.

## Tasks

| # | Task | Commits |
|---|------|---------|
| 1 | Progress.lang, Kotlin fallback on decode, setLang | 12135fa (RED), 73e85fa (GREEN) |
| 2 | lessonPath/loadLesson by language, LessonViewModel(lang) | 88522b5 (RED), 6342138 (GREEN) |
| 3 | Catalog: langs, LessonStatus.Soon, chain and dueIds by language | ec6696b (RED), 7c21a56 (GREEN) |

## Verification

- `./gradlew :shared:testAndroidHostTest` passed.
- `:shared:compileAndroidMain` and `:shared:compileKotlinIosSimulatorArm64` passed.
- `when (lang)` appears exactly once in LessonRepository.kt. Catalog.kt has no `when (lang` and no `lang == CodeLang.`; its only check is `lang.id in it.langs`.
- The existing CatalogTest and dueIds calls are unchanged. The new tests cover: the pilot case (fresh progress plus Python), shared progress, a Done lesson with no Python version, skipping in dueIds while keeping `due`, and SD under Python.
- T-09-03 is covered by the `"java"` test. T-09-04: `lessonPath("../x", Python)` stays under `files/content/lessons/py/`. T-09-05 is covered by the setLang retry test.
- `LessonContentTest.everyPythonLessonParses` passes vacuously until 09-05 adds the `lessons/py` folder.

## Deviations from Plan

- **[Rule 1-adjacent refactor] record and setLang share one private `write()`.** This avoids duplicating the retry loop, and the behavior of `record` is unchanged (its tests are green).
- **The SD switch in App.kt goes through `HomeTab(byLang = false)`.** The call is `homeModel(c, p, today, lang = if (byLang) p.lang else null)`. 09-04 greps App.kt for the literal `lang = null`, and that grep will not match as written. The behavior is what the plan asks for.

## Known Stubs

- In HomeScreen.kt, `LessonStatus.Soon` reuses the Locked node (lock, "Закрыт: …"). The "Скоро на Python" caption and visual are left to 09-04, as the plan says.
- LessonHost does not pass `lang` yet, so the VM uses the default Kotlin. 09-04 adds the wiring together with `LocalCodeLang provides`.

## TDD Gate Compliance

Each task has a RED commit (a compile error against the missing API) followed by a GREEN commit.

## Self-Check: PASSED
