---
phase: 08-system-design-section
plan: 01
subsystem: lesson-engine, navigation, content
tags: [system-design, kmp, compose, validation, content]
requires: []
provides:
  - "LessonKind (code|design), Exercise.title, Solution time/space defaults"
  - "validate per kind: design = 2..4 steps, no hole growth, last step == solution.code, lines <= MAX_DESIGN_LINE (34)"
  - "assembled(ex), lessonPath(id), loadCatalog(path), SD_CATALOG_PATH"
  - "Вкладка «System Design» (SdHome) на files/content/sd/index.json"
  - "Урок sd1.1 URL shortener (4 шага)"
affects: [08-02, 08-03]
tech-stack:
  added: []
  patterns:
    - "highlight: Boolean = true в CodeBlock/CodeTemplate/ResultPanel/TheoryScreen; design передаёт false"
    - "HomeTab(entry, catalog, ...) общий для Home и SdHome"
key-files:
  created:
    - shared/src/commonMain/composeResources/files/content/sd/index.json
    - shared/src/commonMain/composeResources/files/content/sd/sd1.1.json
    - .planning/phases/08-system-design-section/deferred-items.md
  modified:
    - shared/src/commonMain/kotlin/com/tlenparty/algolingo/lesson/Model.kt
    - shared/src/commonMain/kotlin/com/tlenparty/algolingo/lesson/LessonRepository.kt
    - shared/src/commonMain/kotlin/com/tlenparty/algolingo/progress/Catalog.kt
    - shared/src/commonMain/kotlin/com/tlenparty/algolingo/App.kt
    - shared/src/commonMain/kotlin/com/tlenparty/algolingo/ui/components/CodeTemplate.kt
    - shared/src/commonMain/kotlin/com/tlenparty/algolingo/ui/components/ResultPanel.kt
    - shared/src/commonMain/kotlin/com/tlenparty/algolingo/lesson/ui/ExerciseScreen.kt
    - shared/src/commonMain/kotlin/com/tlenparty/algolingo/lesson/ui/RevealScreen.kt
    - shared/src/commonMain/kotlin/com/tlenparty/algolingo/lesson/ui/TheoryScreen.kt
    - shared/src/commonMain/kotlin/com/tlenparty/algolingo/lesson/ui/LessonScreen.kt
    - design/src/commonMain/kotlin/com/tlenparty/algolingo/design/components/CodeBlock.kt
    - shared/src/commonTest/kotlin/com/tlenparty/algolingo/lesson/ModelTest.kt
    - shared/src/commonTest/kotlin/com/tlenparty/algolingo/lesson/LessonRepositoryTest.kt
    - shared/src/androidHostTest/kotlin/com/tlenparty/algolingo/lesson/LessonContentTest.kt
decisions:
  - "Exercise.title стоит последним параметром, чтобы не ломать позиционные вызовы Exercise(...)"
  - "Длина строки design считается в String.length (UTF-16); стрелки ↓ → ─ занимают по одному символу"
metrics:
  duration: "~21 min"
  completed: 2026-10-28
  tasks: 3
  files: 17
---

# Phase 8 Plan 01: SD vertical slice Summary

Урок с `kind: "design"` проходит свою валидацию: от 2 до 4 шагов, рост числа дырок не требуется, со `solution.code` сверяется только последний шаг, строки не длиннее 34 символов. Урок загружается из `files/content/sd/` по префиксу `sd`. На третьей вкладке «System Design» стоит sd1.1 URL shortener. Шаги показываются с подписью и без Kotlin-подсветки. Эталон design-урока — собранный текст текущего шага.

## Tasks

| # | Task | Commit |
|---|------|--------|
| 1 | Модель design, validate по kind, lessonPath/loadCatalog(path) (TDD) | fbc6bbf (RED), 797d5db (GREEN) |
| 2 | UI без подсветки, подпись шага, эталон design, вкладка SdHome | 639d945 |
| 3 | sd1.1 + sd/index.json + SD-проверки в LessonContentTest | 143993f |

## Verification

- `./gradlew :design:assemble :androidApp:assembleDebug :shared:compileKotlinIosSimulatorArm64 :shared:testAndroidHostTest`: все зелёные, 23 suite, 0 падений.
- `python3 tools/gen_content.py --check`: exit 0.
- Устройство (adb, Android 37, debug): вкладка «System Design» показывает «SD1.1 URL shortener». Урок открывается, теория пролистывается. «Шаг 1 · Оценка» отображается с подписью, текст и дырки без подсветки, после заполнения — «✅ Отлично!». Полный прогон Android и iOS — в плане 08-03.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] `Exercise.title` перенесён в конец конструктора**
- **Found during:** Task 1
- **Issue:** в плане поле стояло сразу после `id`, из-за этого ломались позиционные вызовы `Exercise(id, segs, holes, bank)` в тестах (WrongChipsTest и другие).
- **Fix:** поле `title: String? = null` стоит после `hints`.
- **Commit:** 797d5db

**2. [Rule 3 - Blocking] TheoryScreen получает `highlight` от LessonScreen**
- **Found during:** Task 2
- **Issue:** у TheoryScreen нет доступа к `kind`, поэтому параметр передаётся из `LessonScreen` (так и предусматривал план). Файл LessonScreen.kt добавлен в список изменённых.
- **Commit:** 639d945

**3. Команда verify в Task 2**: задачи `:shared:assembleDebug` нет (у KMP-library-плагина нет вариантов сборки). Вместо неё запускались `:androidApp:assembleDebug` и `:shared:compileKotlinIosSimulatorArm64`.

## Deferred Issues

См. `deferred-items.md`:
- `:shared:compileCommonMainKotlinMetadata` падает на okio `FileSystem.SYSTEM` в `ProgressRepository.kt`. Ошибка была до этого плана, файл не трогали.
- Облачка в банке (`TokenChip`) всё ещё подсвечивают числа в design-уроках. В плане этого не было, вопрос на ревью.

## Known Stubs

Нет. Тексты sd1.1 предварительные, финальное ревью у заказчика в плане 08-02.

## TDD Gate Compliance

RED `fbc6bbf` (test), затем GREEN `797d5db` (feat). Проверено.
