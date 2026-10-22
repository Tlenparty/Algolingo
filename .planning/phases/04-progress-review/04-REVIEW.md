---
phase: 04-progress-review
reviewed: 2026-10-22T12:00:00Z
depth: standard
files_reviewed: 18
files_reviewed_list:
  - androidApp/src/main/kotlin/com/tlenparty/algolingo/MainActivity.kt
  - shared/src/iosMain/kotlin/com/tlenparty/algolingo/MainViewController.kt
  - shared/src/commonMain/kotlin/com/tlenparty/algolingo/App.kt
  - shared/src/commonMain/kotlin/com/tlenparty/algolingo/home/HomeScreen.kt
  - shared/src/commonMain/kotlin/com/tlenparty/algolingo/lesson/LessonViewModel.kt
  - shared/src/commonMain/kotlin/com/tlenparty/algolingo/lesson/ui/LessonHost.kt
  - shared/src/commonMain/kotlin/com/tlenparty/algolingo/progress/Catalog.kt
  - shared/src/commonMain/kotlin/com/tlenparty/algolingo/progress/Progress.kt
  - shared/src/commonMain/kotlin/com/tlenparty/algolingo/progress/ProgressRepository.kt
  - shared/src/commonTest/kotlin/com/tlenparty/algolingo/progress/CatalogTest.kt
  - shared/src/commonTest/kotlin/com/tlenparty/algolingo/progress/ProgressRepositoryTest.kt
  - shared/src/commonTest/kotlin/com/tlenparty/algolingo/progress/ProgressTest.kt
  - shared/src/androidHostTest/kotlin/com/tlenparty/algolingo/lesson/LessonContentTest.kt
  - shared/src/commonMain/composeResources/files/content/index.json
  - design/src/commonMain/kotlin/com/tlenparty/algolingo/design/AlgoTheme.kt
  - gradle/libs.versions.toml
  - shared/build.gradle.kts
  - CLAUDE.md
findings:
  critical: 1
  warning: 5
  info: 6
  total: 12
status: issues_found
fix_status: fixed (11/12; IN-06 no action) — see 04-REVIEW-FIX.md
---

# Phase 4: Code Review Report

**Reviewed:** 2026-10-22T12:00:00Z
**Depth:** standard
**Files Reviewed:** 18
**Status:** issues_found

## Summary

The pure engine in `Progress.kt` and `Catalog.kt` matches the locked decisions:
- First pass with any grade starts the ladder at +1 day.
- A replay before the due date only adds XP and streak.
- ✅/💡/❌ map to forward / stay / reset to +1 day.
- When the clock goes backwards, `lastActiveDay` and `due` are kept.
- `today()` is read inside the DataStore transform.

The DataStore wiring is correct:
- one process-wide instance through `AppGraph`;
- an app-lifetime write scope;
- `OkioStorage` with atomic tmp+move writes;
- a corruption handler;
- `encodeDefaults` together with `ignoreUnknownKeys`.

`onFinish` fires exactly once per ViewModel. `LessonReducer` has no transition out of `Done`, and the VM is scoped to the back-stack entry, so rotation cannot cause a double write.

The main defect is in the UI layer. Home's `today` only refreshes when `progress` emits. A process that stays alive past midnight (the normal case on iOS, and common on Android) shows a stale due list and a stale streak. Secondary issues:
- a blank Home frame on every return;
- silent loss of write failures;
- an unrecoverable wipe on any decode failure;
- a review-mode path that never records.

## Fix Status (2026-10-22, iteration 1)

| ID | Status | Commit |
|----|--------|--------|
| CR-01 | fixed (emulator smoke PASS) | a01fd1e |
| WR-01 | fixed | 6c4b7f3 |
| WR-02 | fixed (Home banner; UX needs human check) | 7a4638b |
| WR-03 | fixed | d0680ba |
| WR-04 | fixed | 4285cbd |
| WR-05 | fixed | 0cde109 |
| IN-01 | fixed | a0de023 |
| IN-02 | fixed | 7824a49 |
| IN-03 | fixed | 3b22f62 |
| IN-04 | fixed | f9c968d |
| IN-05 | documented (ponytail note) | 05ab52c |
| IN-06 | no action (as recommended) | — |

## Narrative Findings (AI reviewer)

## Critical Issues

### CR-01: Home never recomputes `today`: the due list and streak go stale after midnight or on resume the next day

**File:** `shared/src/commonMain/kotlin/com/tlenparty/algolingo/App.kt:78-82`
**Issue:** `progressRepo.today()` is only evaluated when the `composable<Home>` body recomposes, and that only happens when `progress` or `catalog` change. Reopening the app from the background does not help:
- `collectAsStateWithLifecycle` restarts collection;
- DataStore re-emits an *equal* `Progress`;
- the `State` uses structural equality, so it does not invalidate, and `homeModel` is not recomputed.

Concretely, the user finishes a lesson on day D (due D+1) and leaves the app suspended. On day D+1 Home shows no «Повтор сегодня (1)», so PROG-05 is broken. On day D+3 Home still shows `🔥 N`, although `visibleStreak` would return 0, so the PROG-02 display is broken. Home only corrects itself after the user switches to the KB tab and back, or plays a lesson. The `ponytail:` comment on line 79 treats this as cosmetic. It is not: the phase's core feature, due reviews, is invisible to anyone who doesn't cold-start the app. Device verification (04-04) always relaunched the app after editing the file, so it could not catch this.
**Fix:** Hold `today` as state and refresh it on resume. Optionally also refresh it at the next local midnight while the app is in the foreground.
```kotlin
composable<Home> { entry ->
    val progress by progressRepo.progress.collectAsStateWithLifecycle(null)
    var today by remember { mutableStateOf(progressRepo.today()) }
    LifecycleResumeEffect(Unit) { today = progressRepo.today(); onPauseOrDispose {} }
    LaunchedEffect(today) { // foreground across midnight
        while (true) { delay(60_000); val t = progressRepo.today(); if (t != today) { today = t; break } }
    }
    val model = if (c != null && p != null) homeModel(c, p, today) else null
    ...
}
```
Add one test that drives `homeModel` with D, then D+1, to pin the expected due/streak transition. The resume refresh alone covers the common case.

## Warnings

### WR-01: Home renders a blank frame on every return from a lesson or the KB tab

**File:** `shared/src/commonMain/kotlin/com/tlenparty/algolingo/App.kt:78`, `shared/src/commonMain/kotlin/com/tlenparty/algolingo/home/HomeScreen.kt:47`
**Issue:** `progressRepo.progress` is a cold `Flow` collected inside the `composable<Home>` destination with `initial = null`. Every time Home re-enters composition, the first frame has `progress == null`, so `HomeScreen` early-returns with an empty background. It fills in a frame or more later. The LazyColumn (and its remembered scroll state) is also removed and re-added each time. The KDoc says "no flicker", but there is a flicker on every lesson exit and every tab switch, not only at cold start.
**Fix:** Make the repository expose a hot, app-scoped state so the value is synchronously available after the first load:
```kotlin
val progress: StateFlow<Progress?> = store.data.catch { emit(Progress()) }
    .stateIn(scope, SharingStarted.Eagerly, null)
```
Then call `collectAsStateWithLifecycle()` without an initial value.

### WR-02: A failed progress write is silently dropped while the summary shows «+N XP»

**File:** `shared/src/commonMain/kotlin/com/tlenparty/algolingo/progress/ProgressRepository.kt:54,61-62`
**Issue:** `record()` is fire-and-forget. Any exception from `updateData` (an IOException, disk full, a failed atomic move on iOS) goes to a `CoroutineExceptionHandler` that only does `println`. The user sees the XP summary, but XP, streak and SRS are lost with no retry and no signal. On a release Android build `println` goes nowhere useful.
**Fix:** At minimum, retry once and surface the failure through a `StateFlow<Boolean>` such as `lastWriteFailed` that Home can show. Or keep the `Job` and have `LessonHost` await it before showing the «+XP» confirmation.

### WR-03: Any decode failure irreversibly wipes all progress, and `schemaVersion` is never read

**File:** `shared/src/commonMain/kotlin/com/tlenparty/algolingo/progress/ProgressRepository.kt:30,43`, `shared/src/commonMain/kotlin/com/tlenparty/algolingo/progress/Progress.kt:19,36-42`
**Issue:** `readFrom` throws `CorruptionException` on *any* `SerializationException` or `IllegalArgumentException`. `ReplaceFileCorruptionHandler { Progress() }` then overwrites the file. This covers:
- real corruption;
- a future build that changes a field type (for example `due` → an object, or `lessons` → a list);
- a hand-edit that makes one `LocalDate` invalid.

No backup is kept, so the user's whole streak and XP history is gone. `schemaVersion` is written but nothing branches on it, so a migration has no hook. The focus question "schemaVersion migration" has no answer in the code today.
**Fix:** Back up the raw bytes before declaring corruption, and gate on the version:
```kotlin
override suspend fun readFrom(source: BufferedSource): Progress {
    val raw = source.readUtf8()
    return decodeProgress(raw) ?: run {
        FileSystem.SYSTEM.write("$path.bak".toPath()) { writeUtf8(raw) } // needs path in scope
        throw CorruptionException("progress.json unreadable")
    }
}
```
Add a `migrate(p: Progress): Progress` step that switches on `schemaVersion` (pass it as a DataStore `migrations` entry), so the first schema change has a home.

### WR-04: In review mode, a lesson whose state starts in `Done` is never recorded and stays due forever

**File:** `shared/src/commonMain/kotlin/com/tlenparty/algolingo/lesson/LessonViewModel.kt:21,29`
**Issue:** `onFinish` only fires on a `prev !is Done → next is Done` transition inside `on()`. `LessonReducer.start` returns `Phase.Done` directly when `theory` is empty and there is no playable exercise (LessonReducer.kt:66). In normal mode, the same lesson reaches `Done` through `NextTheory` (LessonReducer.kt:82), which *does* record a Failed result. In review mode theory is stripped, so `start` lands in `Done` from `init` and `onFinish` is never called. The lesson keeps its past `due`, and «Повтор сегодня» loops onto it forever. The two modes disagree.
**Fix:** In `init`, after building the state, call `onFinish(LessonReducer.result(s))` if `s.phase is Phase.Done`. Alternatively, have `start` never return `Done` (route empty lessons to the error screen) and make both modes behave the same.

### WR-05: A duplicate id in `index.json` crashes Home, despite the "never a crash" contract

**File:** `shared/src/commonMain/kotlin/com/tlenparty/algolingo/progress/Catalog.kt:20-35`, `shared/src/commonMain/kotlin/com/tlenparty/algolingo/home/HomeScreen.kt:79`
**Issue:** `loadCatalog` documents "never a crash", but `parseCatalog` accepts duplicate ids. `itemsIndexed(key = { n.id })` then throws `IllegalArgumentException: Key ... was already used` at layout time. The host test (`catalogIdsAreUniqueAndHaveLessonFiles`) guards the shipped file today, but the Phase 5 generator will overwrite it, and the runtime contract is still false.
**Fix:** Deduplicate on parse with `lessons.distinctBy { it.id }`, or treat duplicates as invalid and return `null`.

## Info

### IN-01: The `entry` lambda parameters shadow the outer `entry` state

**File:** `shared/src/commonMain/kotlin/com/tlenparty/algolingo/App.kt:54,77,90,94,97`
**Issue:** The outer `val entry by nav.currentBackStackEntryAsState()` is shadowed in every `composable { entry -> }`. It is correct today because the inner one is the one that's wanted, but it is easy to break during a refactor, and the compiler warns about it.
**Fix:** Rename the outer one to `current`.

### IN-02: The content test forbids unknown keys, which contradicts catalog forward-compatibility

**File:** `shared/src/androidHostTest/kotlin/com/tlenparty/algolingo/lesson/LessonContentTest.kt:31`
**Issue:** `Json { ignoreUnknownKeys = false }` will fail as soon as the Phase 5 generator adds `level`/`file`, which RESEARCH Pattern 4 explicitly plans for. Runtime parsing uses `ignoreUnknownKeys = true`.
**Fix:** Use `parseCatalog(...)` and assert that it is non-null.

### IN-03: `AppGraph.progress(dataDir)` silently ignores a different `dataDir` on later calls

**File:** `shared/src/commonMain/kotlin/com/tlenparty/algolingo/progress/ProgressRepository.kt:70-71`
**Issue:** `remember(dataDir)` suggests the repository is keyed by the directory, but the singleton returns the first instance whatever is passed. It is harmless today because the directory never changes.
**Fix:** `require(dataDir == firstDir)`, or drop the `remember` key.

### IN-04: The node caption is derived from the id format

**File:** `shared/src/commonMain/kotlin/com/tlenparty/algolingo/progress/Catalog.kt:58`
**Issue:** `e.id.removePrefix("l") + " " + e.title` couples the display text to the id naming scheme. An id that is not `l<level>.<n>` produces odd captions.
**Fix:** Add an optional `caption` to `CatalogEntry` (the generator writes it), and keep this expression as the fallback.

### IN-05: A future-dated clock permanently pins `lastActiveDay` and `due`

**File:** `shared/src/commonMain/kotlin/com/tlenparty/algolingo/progress/Progress.kt:51,63,67`
**Issue:** If the device date is set ahead (for example by a year) during one lesson, `lastActiveDay` and `due` are stored in the future. After the clock is corrected:
- `bumpStreak` freezes the streak (`last >= today`);
- `visibleStreak` never shows it as broken;
- the lesson is never due again until that date.

This is the mirror image of the "clock backwards" handling, and it is not covered by tests.
**Fix:** Accept it with a `ponytail:` note. Alternatively, clamp stored dates that are more than ~2 days ahead of `today` back to `today` on read.

### IN-06: Process death on the summary screen restarts the lesson

**File:** `shared/src/commonMain/kotlin/com/tlenparty/algolingo/lesson/ui/LessonHost.kt:27-29`
**Issue:** No double write happens, because the record was already done and a fresh VM starts from theory. The user does see the whole lesson again instead of the summary, and finishing it again earns XP a second time. This is consistent with "no mid-lesson resume", so no action is needed beyond noting it.
**Fix:** None required. Optionally, store a `finished` flag in `SavedStateHandle` and pop straight to Home.

---

_Reviewed: 2026-10-22T12:00:00Z_
_Reviewer: Claude (gsd-code-reviewer)_
_Depth: standard_
