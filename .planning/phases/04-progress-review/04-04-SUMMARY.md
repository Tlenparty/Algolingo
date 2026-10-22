---
phase: 04-progress-review
plan: 04
subsystem: verification
tags: [device-verification, persistence, srs-review, corruption-recovery]
requires: [04-01 Progress model/SRS, 04-02 ProgressRepository/DataStore, 04-03 Home path + due review]
provides: [Phase 4 device evidence on Android emulator and iOS simulator]
affects: [/gsd:verify for phase 4]
tech-stack:
  added: []
  patterns: [autonomous device walkthrough via uiautomator/idb + hand-edited progress.json]
key-files:
  created:
    - .planning/phases/04-progress-review/04-04-SUMMARY.md
  modified: []
decisions:
  - "No app bugs found; no fix(04-04) commit needed"
metrics:
  duration: ~25min
  completed: 2026-10-22
  tasks: 2
  files: 1
---

# Phase 4 Plan 04: Device Verification of Progress and Review Summary

All four Phase 4 success criteria were checked on the Android emulator (emulator-5554) and the iOS simulator (iPhone 17, iOS 26.2), and every check passed:
- progress survives a process kill;
- a lesson that is due appears in «Повтор сегодня (N)» and replays exercises only, without the theory cards;
- a clean review moves the lesson from SRS step 0 to step 1, with due = today + 3;
- a garbage progress.json resets progress to zero and does not crash the app.

## Gates

`./gradlew :shared:allTests :design:compileKotlinIosSimulatorArm64 :androidApp:assembleDebug` was green:
- testAndroidHostTest: **134 tests, 0 failures**
- iosSimulatorArm64Test: **128 tests, 0 failures**

`xcodebuild … -destination 'id=<sim>' CODE_SIGNING_ALLOWED=NO build` reported **BUILD SUCCEEDED**.

Device date and host date were both 2026-10-22, so "yesterday" was 2026-10-21.

## Android (emulator-5554, app com.tlenparty.algolingo)

Setup: `install -r` of the debug APK, then `pm clear` to start from empty data. `installDebug` was never used, and every adb call used `-s emulator-5554`.

| Step | Expected | Observed | Evidence |
|------|----------|----------|----------|
| 3 Fresh launch | 🔥 0, ⭐ 0, node «Начать», no review button | `🔥 0`, `⭐ 0`, `Начать: 0.4 Бинарный поиск`, no «Повтор сегодня». The files/ directory is empty. | /tmp/p4-android-3.xml/.png |
| 4 Play l0.4 | 3 theory cards → e1..e3 → Summary | 3 × «Дальше» theory cards, then e1/e2/e3 each «✅ Отлично!», «✅ Решено! +10 XP». Lesson summary: «✅ Чисто! Без подсказок и ошибок», **+30 XP** | /tmp/p4-android-4*.xml/.png |
| 5 Home after finish | 🔥 1, ⭐ 30, ✅ node, no review | `🔥 1`, `⭐ 30`, `Пройден, повторить: 0.4 Бинарный поиск`, no review button | /tmp/p4-android-5.xml/.png |
| 5 File | schemaVersion 1, step 0, due today+1 | `{"schemaVersion":1,"xp":30,"streak":1,"lastActiveDay":"2026-10-22","lessons":{"l0.4":{"step":0,"due":"2026-10-23"}}}` | run-as cat |
| 6 force-stop + relaunch | unchanged | `🔥 1`, `⭐ 30`, ✅ node, no review button | /tmp/p4-android-6.xml/.png |
| 7 due := 2026-10-21, relaunch | «Повтор сегодня (1)» | `Повтор сегодня (1)` shown above the ✅ node | /tmp/p4-android-7a.xml/.png |
| 7 Tap review | First screen is an exercise, not theory | The first screen has hole 1, the bank chips and «Проверить». No «Дальше» theory card. | /tmp/p4-android-7b.xml/.png |
| 7 Clean review | button gone, ⭐ +30, 🔥 1, step 1, due +3 | Summary «✅ Чисто! +30 XP». Home: `🔥 1`, `⭐ 60`, no review button. File: `{"schemaVersion":1,"xp":60,"streak":1,"lastActiveDay":"2026-10-22","lessons":{"l0.4":{"step":1,"due":"2026-10-25"}}}` | /tmp/p4-android-7c/7d.xml/.png |
| 8 Garbage file `garbage{{not json` | no crash, 🔥 0, ⭐ 0, «Начать» | `🔥 0`, `⭐ 0`, `Начать: 0.4 Бинарный поиск`. The app is alive (pidof 15289). The file was rewritten to `{"schemaVersion":1,"xp":0,"streak":0,"lastActiveDay":null,"lessons":{}}`. `logcat -d` has **0** FATAL EXCEPTION lines (logcat was cleared at install). | /tmp/p4-android-8.xml/.png |

## iOS (simulator 4461DE0A…, bundle com.tlenparty.algolingo)

Setup: `simctl uninstall`, then `install` of the fresh build, then `launch`. The data file is `<data container>/Documents/progress.json`. On iOS the header is exposed to accessibility as «Серия дней: N» / «Опыт: N XP». The screenshots show the 🔥/⭐ glyphs.

| Step | Expected | Observed | Evidence |
|------|----------|----------|----------|
| 3 Fresh launch | 🔥 0, ⭐ 0, «Начать», no review | `Серия дней: 0`, `Опыт: 0 XP`, `Начать: 0.4 Бинарный поиск, Начать`. The Documents directory is empty. | /tmp/p4-ios-3.png |
| 4 Play l0.4 | Summary with XP | 3 theory cards, then e1..e3 each «✅ Отлично!». Summary: «✅ Чисто!», **+30 XP** | /tmp/p4-ios-4.png |
| 5 Home + file | 🔥 1, ⭐ 30, ✅, step 0 due +1 | `Серия дней: 1`, `Опыт: 30 XP`, `Пройден, повторить: …`. File: `{"schemaVersion":1,"xp":30,"streak":1,"lastActiveDay":"2026-10-22","lessons":{"l0.4":{"step":0,"due":"2026-10-23"}}}` | /tmp/p4-ios-5.png |
| 6 terminate + launch | unchanged | `Серия дней: 1`, `Опыт: 30 XP`, ✅ node, no review button | /tmp/p4-ios-6.png |
| 7 due := 2026-10-21, relaunch | «Повтор сегодня (1)» | `Повтор сегодня (1)` | /tmp/p4-ios-7a.png |
| 7 Tap review | exercise first, no theory | The first screen has hole 1, the bank chips and «Проверить», with no «Дальше» | /tmp/p4-ios-7b.png |
| 7 Clean review | button gone, ⭐ 60, 🔥 1, step 1, due +3 | Summary «Чисто! +30 XP». Home: `Серия дней: 1`, `Опыт: 60 XP`, no review button. File: `{"schemaVersion":1,"xp":60,"streak":1,"lastActiveDay":"2026-10-22","lessons":{"l0.4":{"step":1,"due":"2026-10-25"}}}` | /tmp/p4-ios-7c/7d.png |
| 8 Garbage file | no crash, zeros | `Серия дней: 0`, `Опыт: 0 XP`, «Начать». The screenshot shows 🔥 0 / ⭐ 0. `launchctl list` shows `UIKitApplication:com.tlenparty.algolingo` alive (pid 69142). The file was reset to `{"schemaVersion":1,"xp":0,"streak":0,"lastActiveDay":null,"lessons":{}}` | /tmp/p4-ios-8.png |

## ROADMAP success criteria → evidence

1. **Progress survives restarts.** Step 6 on both platforms.
2. **The lesson status (✅/▶) is shown on Home.** Steps 3, 5 and 8 on both platforms.
3. **Lessons return on the 1/3/7 schedule and appear in «Повтор сегодня».** Steps 5 and 7: due = today + 1 after the first pass, and step 1 with due = today + 3 after a clean review.
4. **The app stays robust when the progress file is corrupt.** Step 8 on both platforms.

**Covered by unit tests, not on the devices:**
- 23:59→00:01 day rollover
- two lessons in one day
- a missed day (streak reset)
- the clock going back
- locked status with 4 ids
- mastered after ✅✅✅

These run in the commonTest suites from 04-01 to 04-03. They are green in the gates above, on both the Android host and iosSimulatorArm64.

## Phase 5 hand-off: index.json format

The format is unchanged from 04-03: `files/content/index.json` = `{"schema":1,"lessons":[{"id":"l0.4","title":"Бинарный поиск"}]}`.
- Array order is the unlock order.
- Each `id` must have a matching `lessons/<id>.json`.
- The caption is `id.removePrefix("l") + " " + title`.
- The host test decodes strictly, so the generator must emit exact keys.
- Phase 5 overwrites the file with lessons 0.1–0.4.

## Deviations from Plan

None. The plan was executed exactly as written, and no app bugs were found.

Notes (harness only, not the app):
- The first install attempt passed `adb -s emulator-5554` through a zsh variable. zsh does not word-split, so that command did not run. It was re-run with literal commands before any observation was recorded.
- The simulator also has a stale `com.example.algolingo.Algolingo` app installed from an older build. It was left untouched.
- The plan's iOS destination `name=iPhone 17` was replaced with the explicit simulator UDID, as the orchestrator instructed.

## Self-Check: PASSED

- The Android and iOS evidence files exist under /tmp (p4-android-*, p4-ios-*). They are not committed.
- The only tracked change is this SUMMARY (plus STATE/ROADMAP).
