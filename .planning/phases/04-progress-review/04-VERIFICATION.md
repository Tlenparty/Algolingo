---
phase: 04-progress-review
verified: 2026-10-22T00:00:00Z
status: passed
score: 4/4 roadmap success criteria verified (plus locked decisions)
overrides_applied: 0
---

# Phase 4: Progress & Review Verification Report

**Phase Goal:** Прогресс пользователя живёт между запусками и возвращает задачи на повтор через 1 / 3 / 7 дней
**Status:** passed
**Re-verification:** No, initial verification

## Build and tests

`./gradlew :shared:allTests :design:compileKotlinIosSimulatorArm64 :androidApp:assembleDebug` gave BUILD SUCCESSFUL. Tasks were up-to-date from the earlier run, so I read the existing result XMLs.

| Target | Tests | Skipped | Failures | Errors |
|---|---|---|---|---|
| testAndroidHostTest (`shared/build/test-results/testAndroidHostTest`) | 140 | 0 | 0 | 0 |
| iosSimulatorArm64Test (`shared/build/test-results/iosSimulatorArm64Test`) | 134 | 0 | 0 | 0 |

The counts differ by 6 between targets, so some Android-only tests exist. Neither target has failures.

Phase 4 tests: ProgressTest (21 tests), CatalogTest (16), ProgressRepositoryTest (9).

## Success criteria

| # | Criterion | Status | Evidence |
|---|---|---|---|
| 1 | Home lists lessons with done / available / locked; the next one opens after the previous is passed | VERIFIED | `Catalog.kt` `lessonStatuses` (in `Progress.lessons` = Done; first, or previous done = Available; else Locked). `homeModel` feeds `HomeScreen`. `content/index.json` is loaded by `loadCatalog`. CatalogTest covers this. The device run shows l0.4 as the ✅ node after finishing. |
| 2 | XP (less with hints or retries), streak by local date, all of it survives restart | VERIFIED | `applyLessonResult` uses the existing `xpFor` (10 clean / 5 hint or retry / 0 unsolved). `bumpStreak` and `visibleStreak` take an injected `LocalDate`. `localToday` uses `TimeZone.currentSystemDefault`. State is stored in DataStore (Okio + JSON) in `progress.json`. Device evidence: force-stop and relaunch left 🔥1 ⭐30 unchanged on both platforms. |
| 3 | SRS 1/3/7: ✅ forward, 💡 stay, ❌ reset, ✅ at the last step = mastered; fake-date tests pass | VERIFIED | `schedule()`: first pass is +1 day; Clean moves to the next step, and at step 2 it gives `LessonProgress(3, null)` (mastered). Hinted keeps the interval. Failed resets to step 0 with +1 day. ProgressTest has `cleanLadderToMastered`, `hintedKeepsInterval`, `failedResets`, `streakAcrossMidnight`, `twoLessonsOneDayCountOnce`, `missedDayResets`, `clockBackKeepsStreakAndLastDay`, `overdueCountsAsDue`, `masteredStaysMastered`. All pass on both targets. |
| 4 | «Повтор сегодня (N)» runs the same player for due tasks | VERIFIED | `HomeScreen.kt:65` shows the Secondary ChunkyButton with `model.due.size`. `App.kt` `onReview` navigates to `LessonRoute(id, review=true)`. `LessonViewModel` strips the theory in review and reuses the same `LessonHost` and reducer. On device: due set to yesterday, the button showed (1), review opened without theory, step 1 due +3, ⭐60. |

## Locked decisions (04-RESEARCH.md)

| Decision | Status | Evidence |
|---|---|---|
| DataStore, one versioned JSON document (PROG-03) | VERIFIED | `ProgressRepository.kt` uses `DataStoreFactory` with `OkioStorage` and `ProgressSerializer`. `Progress.schemaVersion = 1`, `encodeDefaults = true`, and `migrate()` exists. Corruption writes a `.bak` copy and resets (verified on device with a garbage file). `datastore 1.2.1` is in the version catalog. |
| XP A1 reused from `lesson/Xp.kt` | VERIFIED | `applyLessonResult` calls `xpFor`. It is not redefined. |
| A2: only the first pass and due reviews move the SRS | VERIFIED | `schedule`: `prev.due == null \|\| prev.due > today` returns `prev`. Test `notDueReplayKeepsSrsButCountsXpAndStreak`. XP and streak still count. |
| A3: ❌ counts as passed and unlocks; SRS resets to +1 day | VERIFIED | `lessonStatuses` checks only `id in p.lessons`. `schedule` resets on a failed grade. |
| minSdk 26 | VERIFIED | `gradle/libs.versions.toml` has `android-minSdk = "26"`, used by androidApp, design and shared. The CLAUDE.md SDK row says 26. |
| Single process-wide DataStore, writes in an app-lifetime scope | VERIFIED | `AppGraph.progress(dataDir)` is a singleton. `record()` launches in the repository's own scope, not the ViewModel scope. It retries once and sets `saveFailed`. |
| Home: «Повтор сегодня» hidden at N=0, empty state, tokens | VERIFIED | Spot-checked in `HomeScreen.kt` and `homeModel`. The review-fix report confirms the visuals. |
| Out of scope not built (FSRS, freeze, notifications, mid-lesson resume) | VERIFIED | Nothing of this kind found. |

## Requirements coverage

| Req | Status | Evidence |
|---|---|---|
| LESS-01 | SATISFIED | Criterion 1 |
| PROG-01 | SATISFIED | Criterion 2 (`xpFor` in `applyLessonResult`) |
| PROG-02 | SATISFIED | `bumpStreak` and `visibleStreak` on a local date; tests for midnight, skipped day and clock moved back |
| PROG-03 | SATISFIED | DataStore Okio JSON, versioned; restart verified on both platforms |
| PROG-04 | SATISFIED | Criterion 3 |
| PROG-05 | SATISFIED | Criterion 4 |

Orphaned requirements: none. REQUIREMENTS.md maps exactly these six IDs to Phase 4.

## Anti-patterns

No TBD, FIXME or XXX in `progress/` or `home/`. The `ponytail:` comments are documented ceilings, not stubs: a future clock freezes the streak, the review opens only the first due lesson, and the midnight poll runs once a minute. No hollow data: Home gets real `Progress` through `StateFlow` from DataStore.

## Known limitations (not gaps)

- The locked (🔒) node is covered only by unit tests (`lessonStatuses`, CatalogTest). `index.json` lists only l0.4 until Phase 5 adds the other lessons, so it cannot be seen on a device.
- Review opens only the first due lesson. This is the documented ponytail ceiling and matters once N is regularly 2 or more.
- A clock set ahead stores future dates that freeze the streak and the review until that date. Documented in a ponytail comment (IN-05).
- Android-only tests: 140 vs 134 tests on iOS (6 more on Android).

## Human verification

None outstanding. The device evidence (Android emulator and iOS simulator) was taken as verified per the caller's instruction. It covers fresh install, finish, restart persistence, due review, corrupt file reset, and the post-review CR-01 resume-after-date-change smoke on Android.

## Gaps Summary

No gaps. The phase goal is achieved.

_Verified: 2026-10-22_
_Verifier: Claude (gsd-verifier)_
