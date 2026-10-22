---
phase: 04-progress-review
plan: 03
subsystem: progress, home
tags: [catalog, lesson-status, srs-review, home-path]
requires: [04-02 ProgressRepository/AppGraph/visibleStreak, 04-01 Progress model]
provides: [Catalog/CatalogEntry, parseCatalog, loadCatalog, LessonStatus, lessonStatuses, dueIds, HomeNode, HomeModel, homeModel, files/content/index.json, LessonRoute(id, review), LessonViewModel review flag, HomeScreen(model, onStart, onReview), AlgoSpacing.xxl/xxxl, AlgoTypography.nodeEmoji]
affects: [04-04 device walkthrough, Phase 5 content generator (writes index.json)]
tech-stack:
  added: []
  patterns: [pure Home model computed from catalog + progress + today, review = same player with theory stripped]
key-files:
  created:
    - shared/src/commonMain/composeResources/files/content/index.json
    - shared/src/commonMain/kotlin/com/tlenparty/algolingo/progress/Catalog.kt
    - shared/src/commonTest/kotlin/com/tlenparty/algolingo/progress/CatalogTest.kt
  modified:
    - shared/src/androidHostTest/kotlin/com/tlenparty/algolingo/lesson/LessonContentTest.kt
    - design/src/commonMain/kotlin/com/tlenparty/algolingo/design/AlgoTheme.kt
    - shared/src/commonMain/kotlin/com/tlenparty/algolingo/home/HomeScreen.kt
    - shared/src/commonMain/kotlin/com/tlenparty/algolingo/App.kt
    - shared/src/commonMain/kotlin/com/tlenparty/algolingo/lesson/LessonViewModel.kt
    - shared/src/commonMain/kotlin/com/tlenparty/algolingo/lesson/ui/LessonHost.kt
decisions:
  - "index.json lists only l0.4 in Phase 4; listing 0.1-0.3 without lesson files would lock the only playable lesson"
  - "Review opens only the first due lesson per tap; chaining is deferred until N regularly exceeds 3"
  - "Done nodes stay tappable (full replay, SRS moves only when due per A2); locked nodes have onClick = null"
metrics:
  duration: ~10min
  completed: 2026-10-22
  tasks: 2
  files: 9
---

# Phase 4 Plan 03: Home Path with Statuses and Due Review Summary

Home now reads the lesson catalog from `files/content/index.json` and shows each lesson as done (✅), available (▶ with «Начать») or locked (🔒), based on saved progress. When lessons are due, a «Повтор сегодня (N)» button appears and replays the first due lesson in the same player, skipping theory.

## What was built

- **Catalog.kt** (pure apart from `loadCatalog`):
  - `parseCatalog` returns null on bad input.
  - `loadCatalog` returns an empty `Catalog()` on any read or parse failure. It rethrows only `CancellationException`.
  - `lessonStatuses`: any pass, including a ❌ one (A3), counts as Done and unlocks the next lesson.
  - `dueIds` checks only ids that are in the catalog, keeps catalog order, and includes overdue lessons.
  - `homeModel` builds the caption `"0.4 Бинарный поиск"` and uses `visibleStreak`.
- **Tests:**
  - CatalogTest has 13 tests and passes on the Android host and on iosSimulatorArm64.
  - A host test checks that catalog ids are unique and that each one has a `lessons/<id>.json` file.
- **Review route:**
  - `LessonRoute(id, review = false)` → `LessonHost(id, review, …)` with VM key `"$id/$review"`.
  - `LessonViewModel` strips `theory` when `review` is set, and `LessonReducer.start` then goes straight to Solving.
  - `onFinish` still calls `progressRepo.record`, so finishing a review moves the 1/3/7 ladder.
- **HomeScreen(model, onStart, onReview):**
  - Shows a blank background while loading.
  - Header: streak and XP.
  - A Secondary review button, hidden when N = 0.
  - Empty state: «Уроков пока нет».
  - A LazyColumn path with 32dp spacing, a zig-zag of 0/+48/0/−48 (`spacing.xxl`), and a 64dp bottom inset (`spacing.xxxl`).
  - Emoji are drawn at `typography.nodeEmoji` (32sp).
  - Accessibility: «Начать: …», «Пройден, повторить: …», «Закрыт: …».

## Phase 5 hand-off: index.json format

`shared/src/commonMain/composeResources/files/content/index.json`:

```json
{"schema":1,"lessons":[{"id":"l0.4","title":"Бинарный поиск"}]}
```

- **Array order is the unlock order.** Lesson i unlocks once lesson i-1 has any key in `progress.lessons`.
- `id` must match `files/content/lessons/<id>.json`. The host test `catalogIdsAreUniqueAndHaveLessonFiles` enforces this and that ids are unique.
- The caption is `id.removePrefix("l") + " " + title`, so an id like `l0.1` with title «…» shows as «0.1 …».
- Unknown keys are ignored at runtime. The host test decodes strictly, so extra keys fail CI. Either relax that test or keep the generator output exact.
- The Phase 5 generator must overwrite this file and emit lessons 0.1–0.4 in this format.

## Verification

- Ran `:shared:allTests :design:compileKotlinIosSimulatorArm64 :shared:linkDebugFrameworkIosSimulatorArm64 :androidApp:assembleDebug`: all green.
- The literal gate (no raw dp/sp/Color/RoundedCornerShape in shared) is clean. The grep for the new tokens in HomeScreen.kt counts 5. No `HomeLesson` remains.
- Smoke check on emulator-5554 (`install -r`, fresh progress): Home shows 🔥 0 / ⭐ 0, the l0.4 node with «Начать» and the caption «0.4 Бинарный поиск», and no review button. Screenshot: /tmp/home-0403.png.

## Deviations from Plan

- I added `catalogIdsAreUniqueAndHaveLessonFiles` to LessonContentTest as planned. It decodes strictly (`ignoreUnknownKeys = false`), the same way the existing lesson test does.
- I added `contentPadding top = spacing.lg` to the path LazyColumn so the first «Начать» pill does not touch the header or the review button. It uses an existing token.

Otherwise the plan was executed as written.

## TDD Gate Compliance

RED `1f394fe` test(04-03) failed to compile because Catalog did not exist yet. GREEN `1a1dc9d` feat(04-03). No refactor was needed.

## Commits

- 1f394fe test(04-03): add failing catalog, status and due-list tests
- 1a1dc9d feat(04-03): lesson catalog, statuses and due list
- 9361327 feat(04-03): Home path with statuses and due review

## Self-Check: PASSED
