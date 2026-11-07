---
phase: 15
plan: 01
status: complete
date: 2026-11-07
tags: [backend, track, syntax, highlight]
---

# 15-01 Summary — Backend Track Skeleton

## What was built

- `Track.Backend` (progress/domain/Catalog.kt), `BK_CATALOG_PATH` + `catalogPath(track)` (CatalogLoader.kt), `lessonPath("bk…")` → `files/content/bk/` (LessonRepositoryImpl.kt).
- `BkHome` route and the fourth NavigationBar item «Backend» (App.kt); `HomeTab(..., Track.Backend)` without the language segment.
- `LessonSyntax` (kotlin | python | sql | yaml | http | plain) in `lesson/domain/Lesson.kt`, `syntax` in `LessonDto`; `Lesson.highlights` / `Lesson.isScheme` replace the `kind == Code` checks in ExerciseScreen, RevealScreen, LessonScreen; `LessonHost` provides `LocalSyntaxLang` from the lesson, else from the route language (`lesson/presentation/LessonSyntaxMapping.kt`).
- `validate()`: the 34-char line limit applies only to schemes (design without a code syntax).
- `:design` `SyntaxLang` gains Sql, Yaml, Http; `highlight()` is table-driven: SQL keywords case-insensitive with `--` comments, YAML/HTTP keys before `:` at line start, Kotlin `@Annotation` tokens as keywords.

## Tests

- HighlightTest: sql / yaml / http / annotations; LessonRepositoryTest: bk paths; HomeViewModelTest: Backend ignores language and never writes it; LessonContentTest.everyBkLessonIsStrictAndValid; KbContentTest sections include «Backend»; E2eScenarios.scenarioBackend + `backend()` in the Android and iOS smoke tests, `smoke()` chains Backend after SD.
- Cloud session had no Gradle (dl.google.com blocked): the highlighter regexes and the generated Kotlin were compiled and run with kotlinc 2.4.21 on the JVM; the Gradle suite (`:shared:allTests`, `testAndroidHostTest`) is the first step of the next session.
