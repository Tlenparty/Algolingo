---
phase: 04-progress-review
fixed_at: 2026-10-22T10:15:00Z
review_path: .planning/phases/04-progress-review/04-REVIEW.md
iteration: 1
findings_in_scope: 12
fixed: 11
skipped: 1
status: partial
---

# Phase 4: Code Review Fix Report

**Fixed at:** 2026-10-22T10:15:00Z
**Source review:** .planning/phases/04-progress-review/04-REVIEW.md
**Iteration:** 1

**Summary:**
- Findings in scope: 12 (CR-01, WR-01..05, IN-01..06)
- Fixed: 11
- Skipped: 1 (IN-06, the reviewer said no action was required)

**Verification:** `./gradlew :shared:allTests :design:compileKotlinIosSimulatorArm64 :androidApp:assembleDebug` is green, with no new warnings in the changed files. SRS and streak semantics are unchanged, and all the existing Progress and Catalog tests still pass.

**CR-01 emulator smoke test (emulator-5554): PASS**
- **Install:** APK installed with `adb install -r`. `installDebug` was not used.
- **Setup:** I seeded `progress.json` with `l0.4` due on 2026-10-22, `lastActiveDay` 2026-10-21 and `xp` 20.
- **Day 1:** I set the timezone to Pacific/Pago_Pago, so the local date was Oct 21. On launch, Home showed 🔥 1 and ⭐ 20, and there was no review button.
- **Day 2:**
  - I pressed HOME and set the timezone to Europe/Moscow, so the local date was Oct 22.
  - I brought the app back with `am start`. The process was the same (PID 15946), so this was not a relaunch.
  - Home now showed «Повтор сегодня (1)».
- **Why the timezone:** I changed the timezone instead of the date. Editing `progress.json` while the app runs does not work, because DataStore keeps the data in memory. Setting the date needs root, which this user-build image does not allow.
- **Cleanup:** Automatic timezone detection was turned back on, and `progress.json` was reset to zero progress.

## Fixed Issues

### CR-01: Home never recomputes `today`

**Files modified:** `shared/src/commonMain/kotlin/com/tlenparty/algolingo/App.kt`, `shared/src/commonTest/kotlin/com/tlenparty/algolingo/progress/CatalogTest.kt`
**Commit:** a01fd1e
**Applied fix:**
- `today` is now `remember { mutableStateOf(...) }`.
- `entry.repeatOnLifecycle(RESUMED)` sets it on every resume, then re-reads it once a minute while Home is visible. This handles midnight; the `ponytail:` note says a rollover can show up to 1 minute late.
- New test `homeModelNextDayShowsDueAndThenBrokenStreak`: on D nothing is due; on D+1 the review is due and the streak is 1; on D+2 the streak is 0.
**Status:** fixed (the logic was confirmed on the emulator, see above)

### WR-01: Home blank frame on every return

**Files modified:** `ProgressRepository.kt`, `App.kt`, `ProgressRepositoryTest.kt`
**Commit:** 6c4b7f3
**Applied fix:**
- `progress` is now `StateFlow<Progress?>`, built with `stateIn(scope, SharingStarted.Eagerly, null)` in the repository's app-lifetime scope.
- Home collects it with no initial value.
- Tests now pass `backgroundScope` and use `filterNotNull().first()`.

### WR-02: Failed progress write silently dropped

**Files modified:** `ProgressRepository.kt`, `home/HomeScreen.kt`, `App.kt`, `ProgressRepositoryTest.kt`
**Commit:** 7a4638b
**Applied fix:**
- `record()` retries `updateData` once.
- If the retry also fails, it sets `saveFailed: StateFlow<Boolean>` to true. The next good write clears it.
- Home shows «Не удалось сохранить прогресс последнего урока» in `colorScheme.error`, using `t.label` and `sp.md`/`sp.sm` tokens.
- New test `failedWriteIsRetriedThenFlagged`.
- The summary still shows «+N XP» while the write runs. I chose the Home banner as the smallest honest signal.
**Status:** fixed: requires human verification (UX choice)

### WR-03: Decode failure wipes progress; schemaVersion unused

**Files modified:** `progress/Progress.kt`, `ProgressRepository.kt`, `ProgressRepositoryTest.kt`
**Commit:** d0680ba
**Applied fix:**
- `ProgressSerializer` is now a class with a `backup: Path?`. When a file can't be read, its raw bytes go to `progress.json.bak` before the `CorruptionException` lets the handler reset the file.
- `decodeProgress` parses to a `JsonObject`, reads `schemaVersion` (default 1), then calls `migrate(doc, from)`. `migrate` is identity for v1. It is added as an explicit hook, not as a DataStore migration, because it has to run *before* typed decoding.
- `PROGRESS_SCHEMA = 1`.
- Tests: the corrupt-file test now also checks the `.bak` content, and a new test `missingSchemaVersionDecodesAsV1` was added.

### WR-04: A review lesson that starts in Done is never recorded

**Files modified:** `lesson/LessonViewModel.kt`, new `commonTest/.../lesson/LessonViewModelTest.kt`
**Commit:** 4285cbd
**Applied fix:**
- After loading, if the start state is `Phase.Done`, `init` calls `onFinish(LessonReducer.result(it))`. This runs once per VM.
- New test (uses `Dispatchers.setMain`): a review of a lesson with theory only and no exercises records exactly once.
**Status:** fixed: requires human verification (logic)

### WR-05: A duplicate id in index.json crashes Home

**Files modified:** `progress/Catalog.kt`, `CatalogTest.kt`
**Commit:** 0cde109
**Applied fix:** `parseCatalog` keeps the first entry for each id (`distinctBy`). New test `parseDropsDuplicateIds`.

### IN-01: Shadowed `entry`

**Files modified:** `App.kt`
**Commit:** a0de023
**Applied fix:** The outer back-stack state is renamed to `current`.

### IN-02: The content test is strict on unknown keys

**Files modified:** `shared/src/androidHostTest/.../lesson/LessonContentTest.kt`
**Commit:** 7824a49
**Applied fix:** The catalog check now decodes with `ignoreUnknownKeys = true`. I did not switch it to `parseCatalog`: that now removes duplicates, so the shipped-file uniqueness assertion could no longer fail.

### IN-03: AppGraph ignores a later dataDir

**Files modified:** `App.kt`, `ProgressRepository.kt`
**Commit:** 3b22f62
**Applied fix:** I removed the `remember(dataDir)` key and documented in the KDoc that the first `dataDir` wins.

### IN-04: The caption is derived from the id format

**Files modified:** `Catalog.kt`, `CatalogTest.kt`
**Commit:** f9c968d
**Applied fix:** `CatalogEntry.caption: String? = null`. If it is set, Home uses it; otherwise it falls back to the old id-based text. New test `explicitCaptionWins`.

### IN-05: A future-dated clock pins lastActiveDay and due

**Files modified:** `Progress.kt`
**Commit:** 05ab52c
**Applied fix:** This is accepted as a known limit and documented with a `ponytail:` note on `bumpStreak`. The upgrade path is to clamp stored dates more than 2 days past today back to today on read. Behaviour is unchanged.

## Skipped Issues

### IN-06: Process death on the summary screen restarts the lesson

**File:** `shared/src/commonMain/kotlin/com/tlenparty/algolingo/lesson/ui/LessonHost.kt:27-29`
**Reason:** The reviewer marked this "None required". It fits "no mid-lesson resume", and there is no double write. A `SavedStateHandle` "finished" flag would add plumbing for a rare edge case.
**Original issue:** After process death on the summary screen, the user replays the lesson and earns its XP again.

---

_Fixed: 2026-10-22T10:15:00Z_
_Fixer: Claude (gsd-code-fixer)_
_Iteration: 1_
