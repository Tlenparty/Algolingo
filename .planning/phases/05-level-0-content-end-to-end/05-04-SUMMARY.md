---
phase: 05-level-0-content-end-to-end
plan: 04
subsystem: verification
tags: [device-verification, offline, end-to-end, level-0]
requires: [05-01 generator, 05-02 level 0.1-0.3 content, 05-03 content review]
provides: [Phase 5 end-to-end evidence on Android emulator (airplane mode) and iOS simulator, PLAT-03 offline proof]
affects: [/gsd:verify for phase 5]
tech-stack:
  added: []
  patterns: [autonomous device walkthrough via uiautomator/idb drivers in /tmp + hand-edited progress.json]
key-files:
  created:
    - .planning/phases/05-level-0-content-end-to-end/05-04-SUMMARY.md
  modified: []
decisions:
  - "05-04: level 0 (0.1-0.4) passes end to end offline on Android + iOS sim; no app bugs, no fix(05-04) commit"
metrics:
  duration: ~45min
  completed: 2026-10-22
  tasks: 3
  files: 1
---

# Phase 5 Plan 04: Level 0 End-to-End Offline Walkthrough Summary

A fresh install passes all of level 0 (0.1 → 0.4) from theory to summary on the Android emulator in airplane mode and on the iOS simulator. Each lesson unlocks the next. The acceptable verdict («принято, но…»), a distractor why with requeue, and a hint were each seen on both platforms. Progress survives a restart, and a due review opens on an exercise without theory. The APK has no INTERNET permission, and the sources and dependencies contain no networking API. No app bugs were found.

## Gates

| Command | Result |
|---------|--------|
| `python3 tools/gen_content.py --check` | exit 0 (no drift) |
| `python3 -m unittest tools/test_gen_content.py` | Ran 25 tests, OK |
| `./gradlew :shared:allTests :androidApp:assembleDebug` | green: testAndroidHostTest **149 tests, 0 failures**; iosSimulatorArm64Test **142 tests, 0 failures** |
| `xcodebuild -project iosApp/iosApp.xcodeproj -scheme iosApp … -destination 'id=4461DE0A…' CODE_SIGNING_ALLOWED=NO build` | **BUILD SUCCEEDED** |

The APK bundles `files/content/index.json` plus `lessons/l0.1.json` … `l0.4.json` (`unzip -l`).

Device date and host date were both 2026-10-22, so "yesterday" was 2026-10-21.

## Offline proof (PLAT-03)

1. **APK permissions.** Command: `aapt2 dump permissions androidApp/build/outputs/apk/debug/androidApp-debug.apk` (build-tools 36.1.0). Full output:
   ```
   package: com.tlenparty.algolingo
   permission: com.tlenparty.algolingo.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION
   uses-permission: name='com.tlenparty.algolingo.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION'
   ```
   There is **no `android.permission.INTERNET`** line (0 lines). The only permission is the androidx-core internal receiver-guard permission, which has nothing to do with networking.
2. **Network APIs in the sources.** `grep -rIl -e URLSession -e NSURL -e ktor -e okhttp -e "android.permission.INTERNET" shared/src design/src androidApp/src iosApp gradle/libs.versions.toml` printed nothing (exit 1). This covers both platforms, so the iOS app has no networking code either.
3. **Declared dependencies** (`gradle/libs.versions.toml [libraries]`): kotlin-test, kotlin-test-junit, junit, androidx core-ktx, appcompat, activity-compose, test-ext-junit, espresso-core, compose runtime/foundation/ui/material3/ui-backhandler/components-resources/ui-tooling(-preview), lifecycle viewmodel-compose and runtime-compose, navigation-compose, datastore-core and datastore-core-okio (okio here is file I/O for local DataStore, not a network client), kotlinx-serialization-json, kotlinx-datetime, kotlinx-coroutines-test. **None of them is a network library.** There is no Ktor, OkHttp, Retrofit or Coil.
4. **No account or login.** Grepping `shared/src/commonMain` and `design/src/commonMain` for логин|войти|аккаунт|парол|регистрац found no matches. The app starts directly on Home.
5. **Runtime check.**
   - Android: the full walkthrough ran with `airplane_mode_on = 1`.
   - iOS: the simulator has no per-device network switch, and the Mac's network was deliberately left on (locked decision). Offline behaviour on iOS therefore rests on points 2–4: there is no networking code or dependency in the shared code or in `iosApp`.

## Android (emulator-5554, Medium_Phone_API_36.1, com.tlenparty.algolingo)

Setup: these literal commands ran in order:
- `adb -s emulator-5554 shell cmd connectivity airplane-mode enable`
- `settings get global airplane_mode_on` → `1`
- `install -r androidApp-debug.apk` → Success
- `pm clear` → Success
- `logcat -c`
- `am start -n com.tlenparty.algolingo/.MainActivity`

Every adb call used the literal `-s emulator-5554`, and `installDebug` was never used. The UI was driven by a small uiautomator/`input tap` driver in /tmp that finds chips by text.

| Step | Expected | Observed | Evidence |
|------|----------|----------|----------|
| 1 Fresh Home | 🔥 0, ⭐ 0, «Начать: 0.1», 0.2–0.4 closed | `🔥 0`, `⭐ 0`, `Начать: 0.1 Сумма и максимум`, `Закрыт: 0.2 Разворот строки`, `Закрыт: 0.3 Палиндром`, `Закрыт: 0.4 Бинарный поиск` | /tmp/p5-android-1.xml/.png |
| 2 Hint (0.1 e1) | hint opens | Opened `Подсказка 1 из 1`: «Максимум инициализируй первым элементом, не нулём…», «Подсказка снижает оценку до 💡». The counter went 1 → 0. | /tmp/p5-android-2a-hint.xml/.png |
| 2 Lesson 0.1 | theory → e1..e3 → summary | 3 theory cards («Когда один проход», «Каркас», «Грабли»). e1 `nums[0]`, e2 `0, nums[0]`, e3 `0, nums[0], +=, >` were each «✅ Отлично!». Summary: **💡 «Решено с подсказкой», +25 XP**. | /tmp/p5-android-2-l01*.xml/.png |
| 2 Home after 0.1 | 0.1 done, 0.2 «Начать», 🔥 1 | `🔥 1`, `⭐ 25`, `Пройден, повторить: 0.1 …`, `Начать: 0.2 Разворот строки`, 0.3/0.4 closed | /tmp/p5-android-2-home1.xml/.png |
| 2 Acceptable (0.2 e1, `l <= r`) | «принято, но…» + note | `Дырка 1: l <= r: принято`, **«👌 Принято, но…»**, note «работает, но на нечётной длине средний символ меняется сам с собой: abc делает лишний шаг на b». Then «✅ Решено! +10 XP». | /tmp/p5-android-2-l02e1-acceptable.xml/.png |
| 2 Lesson 0.2 | to summary | 3 theory cards. e2 and e3 were each ✅. Summary: **✅ «Чисто!», +30 XP**. An acceptable answer still counts as clean. | /tmp/p5-android-2-l02*.xml/.png |
| 2 Home after 0.2 | 0.3 «Начать» | `🔥 1`, `⭐ 55`, 0.1/0.2 done, `Начать: 0.3 Палиндром`, 0.4 closed | /tmp/p5-android-2-home2.xml/.png |
| 2 Distractor (0.3 e1, `l > r`) | why from JSON, requeue at end | **«❌ Не совсем»**, `l > r`, «цикл не выполнится ни разу: "ab" вернёт true», «Верно: l < r», «Понятно, дальше». The next screen was e2 (`while (l < r)` template). After e2 and e3, **e1 came back** with an empty hole 1 and the bank `l <= r, l < r, l != r, l > r`. Solved: «✅ Решено! +5 XP». | /tmp/p5-android-2-l03e1-wrong.xml/.png, /tmp/p5-android-2-l03-requeued.xml/.png |
| 2 Lesson 0.3 | to summary | 3 theory cards. Summary: **💡 «Решено с подсказкой… с подсказками или повтором», +25 XP** | /tmp/p5-android-2-l03-summary.xml/.png |
| 2 Home after 0.3 | 0.4 «Начать» | `🔥 1`, `⭐ 80`, 0.1–0.3 done, `Начать: 0.4 Бинарный поиск` | /tmp/p5-android-2-home3.xml/.png |
| 2 Lesson 0.4 | to summary | 3 theory cards. e1 `lo <= hi`, e2 `lo <= hi, lo + (hi - lo) / 2`, e3 `lo <= hi, mid, 1, 1` were each ✅. Summary: **✅ «Чисто!», +30 XP** | /tmp/p5-android-2-l04*.xml/.png |
| 2 Home after 0.4 | all done | `🔥 1`, `⭐ 110`, `Пройден, повторить:` 0.1, 0.2, 0.3, 0.4 | /tmp/p5-android-2-home4.xml/.png |
| 3 force-stop + relaunch | unchanged | `🔥 1`, `⭐ 110`, all 4 done. File: `{"schemaVersion":1,"xp":110,"streak":1,"lastActiveDay":"2026-10-22","lessons":{"l0.1":{"step":0,"due":"2026-10-23"},"l0.2":{…same},"l0.3":{…same},"l0.4":{…same}}}` | /tmp/p5-android-3-relaunch.xml/.png |
| 4 due(l0.1, l0.2) := 2026-10-21, relaunch | «Повтор сегодня (2)» | `Повтор сегодня (2)` above the path. The file was written via `run-as … sh -c 'cat > files/progress.json'` from /tmp/p5-android-due.json. | /tmp/p5-android-4a.xml/.png |
| 4 Tap review | exercise first, no theory | First screen: «Сумма и максимум» with `Проверить` and no «Дальше» card | /tmp/p5-android-4b.xml/.png |
| 4 Clean review l0.1 | «Повтор сегодня (1)» | Summary «✅ Чисто! +30 XP». Home: `⭐ 140`, **`Повтор сегодня (1)`**. File: `l0.1 {"step":1,"due":"2026-10-25"}`, `l0.2` still due 2026-10-21. | /tmp/p5-android-4c-summary, 4d .xml/.png |
| 4 Review l0.2 | button gone, step advanced | The first screen was «Разворот строки» exercise (no «Дальше»). Summary «✅ Чисто! +30 XP». Home: `🔥 1`, `⭐ 170`, no review button. File: `{"schemaVersion":1,"xp":170,"streak":1,"lastActiveDay":"2026-10-22","lessons":{"l0.1":{"step":1,"due":"2026-10-25"},"l0.2":{"step":1,"due":"2026-10-25"},"l0.3":{"step":0,"due":"2026-10-23"},"l0.4":{"step":0,"due":"2026-10-23"}}}` | /tmp/p5-android-4e…4g .xml/.png |
| 5 Crash / airplane | 0 FATAL, airplane still 1 | `logcat -d \| grep -c "FATAL EXCEPTION"` → **0**. `airplane_mode_on` → **1** | — |
| Teardown | airplane off | `cmd connectivity airplane-mode disable` → `airplane_mode_on` = **0** | — |

## iOS (simulator 4461DE0A-043A-4C3B-8D48-DB6418058861, iPhone 17 / iOS 26.2, com.tlenparty.algolingo)

Setup: `simctl uninstall`, then `install` of build/ios/Build/Products/Debug-iphonesimulator/Algolingo.app, then `launch`. The UI was driven by an idb `ui describe-all` / `ui tap` driver in /tmp. The header is exposed to accessibility as «Серия дней: N» / «Опыт: N XP». Offline for iOS is the static proof above, and the Mac's network was not touched.

| Step | Expected | Observed | Evidence |
|------|----------|----------|----------|
| 1 Fresh Home | 0.1 «Начать», 0.2–0.4 closed | `Серия дней: 0`, `Опыт: 0 XP`, `Начать: 0.1 Сумма и максимум, Начать`, `Закрыт: 0.2 …`, `Закрыт: 0.3 …`, `Закрыт: 0.4 …` | /tmp/p5-ios-1.png |
| 2 Hint (0.1 e1) | hint opens | `Подсказка 1 из 1`, «Максимум инициализируй первым элементом…», counter → `Подсказка, осталось 0` | /tmp/p5-ios-2a-hint.png |
| 2 Lesson 0.1 | to summary | 3 theory cards. e1..e3 were each «✅ Отлично!» (+5/+10/+10). Summary: **💡 «Решено с подсказкой», +25 XP** | /tmp/p5-ios-2-l01*.png |
| 2 Home after 0.1 | 0.2 unlocked | `Серия дней: 1`, `Опыт: 25 XP`, `Пройден, повторить: 0.1 …`, `Начать: 0.2 Разворот строки, Начать` | /tmp/p5-ios-2-home1.png |
| 2 Acceptable (0.2 e1, `l <= r`) | «принято, но…» + note | **«👌 Принято, но…»**, `l <= r`, «работает, но на нечётной длине средний символ меняется сам с собой: abc делает лишний шаг на b» | /tmp/p5-ios-2-l02e1-acceptable.png |
| 2 Lesson 0.2 | to summary | 3 theory cards. Summary: **✅ «Чисто!», +30 XP**. Home: `Опыт: 55 XP`, `Начать: 0.3 Палиндром` | /tmp/p5-ios-2-l02-summary.png, /tmp/p5-ios-2-home2.png |
| 2 Distractor (0.3 e1, `l > r`) | why + requeue | **«❌ Не совсем»**, «цикл не выполнится ни разу: "ab" вернёт true», «Верно: l < r». Then e2 and e3 played, then **e1 came back** with `Дырка 1: выбрана` and the `while (` template. Solved: «✅ Решено! +5 XP». | /tmp/p5-ios-2-l03e1-wrong.png, /tmp/p5-ios-2-l03-requeued.png |
| 2 Lesson 0.3 | to summary | 3 theory cards. Summary: **💡 «Решено с подсказкой», +25 XP**. Home: `Опыт: 80 XP`, `Начать: 0.4 Бинарный поиск` | /tmp/p5-ios-2-l03-summary.png, /tmp/p5-ios-2-home3.png |
| 2 Lesson 0.4 | to summary | 3 theory cards. e1..e3 were each ✅, including e3 `lo <= hi, mid, 1, 1`. Summary: **✅ «Чисто!», +30 XP**. Home: `Серия дней: 1`, `Опыт: 110 XP`, all four `Пройден, повторить:` | /tmp/p5-ios-2-l04-summary.png, /tmp/p5-ios-2-home4.png |
| 2 File | 4 lessons | `{"schemaVersion":1,"xp":110,"streak":1,"lastActiveDay":"2026-10-22","lessons":{"l0.1":{"step":0,"due":"2026-10-23"},"l0.2":{…},"l0.3":{…},"l0.4":{…}}}` | Documents/progress.json |
| 3 terminate + launch | unchanged, alive | `Серия дней: 1`, `Опыт: 110 XP`, all 4 done. `launchctl list` shows `UIKitApplication:com.tlenparty.algolingo` (pid 11912) | /tmp/p5-ios-3-relaunch.png |
| 4 due(l0.1) := 2026-10-21, relaunch | «Повтор сегодня (1)» | `Повтор сегодня (1)`. App alive (pid 11932). | /tmp/p5-ios-4a.png |
| 4 Tap review | exercise first, no theory | First screen: «Сумма и максимум» with `Проверить`, no «Дальше» | /tmp/p5-ios-4b.png |
| 4 Clean review | button gone, step 1 | Summary «✅ Чисто! +30 XP». Home: `Опыт: 140 XP`, no review button. File: `l0.1 {"step":1,"due":"2026-10-25"}`. App still alive in `launchctl list`. | /tmp/p5-ios-4c-summary.png, /tmp/p5-ios-4d.png |

## ROADMAP Phase 5 criterion 5 / PLAT-03 → evidence

- **All 4 lessons pass from start to summary on Android and the iOS simulator.** Android step 2 and iOS step 2: 0.1 → 0.4, each with 3 theory cards and 3 exercises, and each unlocking the next.
- **Without network.**
  - Android ran the full walkthrough in airplane mode (`airplane_mode_on = 1`) with 0 FATAL EXCEPTION lines.
  - The APK has no INTERNET permission.
  - There is no network API or dependency in the shared code or in iosApp (static proof covers iOS).
  - There is no account or login.
- **Progress works.** XP 25 → 55 → 80 → 110 and streak 1 on both platforms. The data survives force-stop/terminate (step 3).
- **Review works.** A due lesson appears in «Повтор сегодня (N)», opens on an exercise without theory, and a clean pass moves it to step 1 with due = today + 3 (step 4 on both platforms; Android also checked N = 2 → 1 → 0).
- **Feedback paths.** Seen on both platforms: the acceptable verdict with its note, a distractor why with requeue, and a hint.

## Deviations from Plan

None in the app. No bugs were found, so there is no `fix(05-04)` commit.

Harness notes (not app issues):
- The emulator was not running at start. I booted the `Medium_Phone_API_36.1` AVD with `-port 5554` so it got the serial `emulator-5554`. No physical phone appeared in `adb devices` during this run, but every command still used the literal serial.
- Restarting the adb server once (to clear an "unauthorized" state while the emulator was booting) caused an "Allow USB debugging?" dialog on the emulator. I accepted it on the emulator, then relaunched the app before recording step 3.
- The summary verdicts observed were ✅ (clean, including the acceptable answer) and 💡 (hint used, or a mistake followed by requeue). ❌ appeared at the exercise level for the distractor. A lesson-level ❌ was not provoked, because it is outside this plan's must-haves.

## Self-Check: PASSED

- The evidence files exist under /tmp (127 files p5-android-*, p5-ios-*). They are not committed.
- The plan's automated verifies passed: progress.json has l0.1–l0.4, airplane mode is restored to 0, the APK has 0 INTERNET lines, and Algolingo.app exists.
- The only tracked change is this SUMMARY (plus STATE/ROADMAP).
