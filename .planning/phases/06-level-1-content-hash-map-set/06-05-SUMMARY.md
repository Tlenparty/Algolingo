---
phase: 06-level-1-content-hash-map-set
plan: 05
subsystem: verification
tags: [device-verification, offline, end-to-end, level-1]
requires: [06-01..06-04 level 1 content (customer-approved 2026-10-22)]
provides: [Phase 6 end-to-end evidence on Android emulator (airplane mode) and iOS simulator, CNV2-01 traceability row]
affects: [/gsd:verify for phase 6]
tech-stack:
  added: []
  patterns: [autonomous device walkthrough via uiautomator/idb drivers in /tmp, fresh install, no seeding]
key-files:
  created:
    - .planning/phases/06-level-1-content-hash-map-set/06-05-SUMMARY.md
  modified:
    - design/src/commonMain/kotlin/com/tlenparty/algolingo/design/components/TokenChip.kt
    - .planning/REQUIREMENTS.md
decisions:
  - "06-05: level 1 (1.1-1.5) passes from a fresh install after 0.1-0.4 offline on Android + iOS sim; the 1.5 long template is usable (auto-scroll to the selected hole), no UI follow-up needed"
  - "06-05: ghost bank chip renders plain text, because highlight spans overrode the transparent color (fix d9d8635)"
metrics:
  duration: ~57min
  completed: 2026-10-22
  tasks: 3
  files: 3
---

# Phase 6 Plan 05: Level 1 End-to-End Offline Walkthrough Summary

A fresh install was played entirely through the UI, with no seeding of progress.json. The run went 0.1 → 0.4 → 1.1 → 1.5, each lesson from theory to summary, on the Android emulator in airplane mode and on the iOS simulator. Each lesson unlocked the next, and 1.1 opened only after 0.4. On both platforms I saw a distractor why with requeue and a hint. The long 1.5 template is usable. Progress survives a restart.

The walkthrough found one cosmetic app bug. When a chip such as `1`, `0`, `true` or `null` was placed, its empty "ghost" in the bank still showed the text. This was fixed at the root in the shared `TokenChip` (commit d9d8635), re-verified on both platforms, and the gates stayed green.

## Gates

| Command | Result |
|---------|--------|
| `python3 tools/gen_content.py --check` | exit 0 (no drift) |
| `python3 -m unittest tools/test_gen_content.py` | Ran 33 tests, OK |
| `./gradlew :shared:allTests :androidApp:assembleDebug` | green: testAndroidHostTest **154 tests, 0 failures**; iosSimulatorArm64Test **147 tests, 0 failures** (05-04 had 149 / 142). Same result after the fix. |
| `xcodebuild -project iosApp/iosApp.xcodeproj -scheme iosApp -configuration Debug -sdk iphonesimulator -destination 'id=4461DE0A…' -derivedDataPath build/ios CODE_SIGNING_ALLOWED=NO build` | **BUILD SUCCEEDED** (before and after the fix) |

`unzip -l androidApp-debug.apk` shows that the APK bundles `files/content/index.json` + `lessons/l0.1.json` … `l0.4.json` + `lessons/l1.1.json` … `l1.5.json` (9 lessons).

Device date and host date: 2026-10-22.

## Offline proof

1. **APK permissions.** Command: `~/Library/Android/sdk/build-tools/36.1.0/aapt2 dump permissions androidApp/build/outputs/apk/debug/androidApp-debug.apk`. Output:
   ```
   package: com.tlenparty.algolingo
   permission: com.tlenparty.algolingo.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION
   uses-permission: name='com.tlenparty.algolingo.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION'
   ```
   There are **0 `android.permission.INTERNET` lines**. The only permission is the androidx-core internal receiver guard.
2. **Network APIs.** `grep -rIl -e URLSession -e NSURL -e ktor -e okhttp -e "android.permission.INTERNET" shared/src design/src androidApp/src iosApp gradle/libs.versions.toml` printed nothing (exit 1).
3. **Runtime.**
   - Android ran the whole walkthrough with `airplane_mode_on = 1`. The setting was restored to `0` at teardown.
   - iOS: the simulator has no per-device network switch, and the Mac's network was deliberately left on. Offline behaviour on iOS rests on points 1–2: there is no networking code or dependency in the shared code or in iosApp.

## Android (emulator-5554, Medium_Phone_API_36.1)

**Setup.** The emulator was already running. These literal commands ran in order:
- `adb -s emulator-5554 shell cmd connectivity airplane-mode enable`, then `settings get global airplane_mode_on` → `1`
- `install -r androidApp-debug.apk` → Success
- `pm clear com.tlenparty.algolingo` → Success
- `logcat -c`
- `am start -n com.tlenparty.algolingo/.MainActivity`

Every adb call used the literal `-s emulator-5554`. `installDebug` was never used.

| Step | Expected | Observed | Evidence |
|------|----------|----------|----------|
| 1 Fresh Home | 9 nodes, only 0.1 open | `🔥 0`, `⭐ 0`, `Начать: 0.1 Сумма и максимум`, `Закрыт: 0.2`/`0.3`/`0.4`/`1.1 Есть ли дубликат`. After scrolling: `Закрыт: 1.2 Первый уникальный символ`, `1.3 Анаграмма`, `1.4 Two Sum`, `1.5 Победители соревнования` | /tmp/p6-android-1-home.xml/.png, /tmp/p6-android-1-home-bottom.xml/.png |
| 2 Lessons 0.1–0.4 | each to summary, next unlocks, 1.1 closed until 0.4 is done | Each lesson had 3 theory cards and 3 exercises, all «✅ Отлично!» then «✅ Решено! +10 XP», summary «✅ Чисто! +30 XP». Home went ⭐ 30 → 60 → 90 → 120. `Закрыт: 1.1` was still shown after 0.1, 0.2 and 0.3. After 0.4: **`Начать: 1.1 Есть ли дубликат`** | /tmp/p6-android-l0.*-summary.png, /tmp/p6-android-2-home1..4.xml/.png |
| 3 Lesson 1.1 | to summary, 1.2 opens | 3 theory cards. e1..e3 ✅. Summary «✅ Чисто! +30 XP». Home: ⭐ 150, `Пройден, повторить: 1.1`, `Начать: 1.2` | /tmp/p6-android-l1.1-*.png, /tmp/p6-android-3-home-l11.* |
| 3 Hint (1.2 e1) | hint opens | `Подсказка, осталось 1` → `Подсказка 1 из 1`, «Первый проход — считаем частоты. Второй проход по исходной строке (порядок важен!) — первый символ с частотой 1.», then `Подсказка, осталось 0` | /tmp/p6-android-l1.2-hint.xml/.png |
| 3 Distractor (1.2 e1, `cnt.keys`) | JSON why + requeue | **«❌ Не совсем»**, `Дырка 1: cnt.keys: ошибка`, «HashMap не хранит порядок вставки; на JVM ключи идут по корзинам хеша: "ba" → 'a' вместо 'b'» (= the JSON why), «Верно: s», `Понятно, дальше`. e2 and e3 were played, then **e1 came back** (`for (c in ` + `Дырка 1: выбрана`) and was solved: «✅ Решено! +5 XP» | /tmp/p6-android-l1.2-e1-wrong.xml/.png, /tmp/p6-android-l1.2-requeued.xml/.png |
| 3 Lesson 1.2 | to summary | 3 theory cards. Summary **«💡 Решено с подсказкой» +25 XP**. ⭐ 175 | /tmp/p6-android-l1.2-summary.png, /tmp/p6-android-3-home-l12.* |
| 3 Lessons 1.3, 1.4 | to summary, next opens | 3 theory cards each. All ✅. «✅ Чисто! +30 XP» each. ⭐ 205 → 235. `Начать: 1.4 Two Sum` after 1.3 | /tmp/p6-android-3-home-l13.*, /tmp/p6-android-3-home-l14.* |
| 3 Lesson 1.5 template | all holes reachable, chips readable | The code area scrolls vertically, and the bank and «Проверить» stay pinned below it. Long lines are clipped on the right, but the area scrolls horizontally (manual swipe works). When a hole is selected, the area **auto-scrolls in both axes** to bring it into view: e2 hole 2 (`return ChampionResult(`) and e3 holes 1–2 at the far right of the `getOrDefault(...) +` lines, then hole 3 `{ it == `. The bank has 7 chips in 3 rows and fits without scrolling. Chips are fully readable. | /tmp/p6-android-l1.5-e1-start.png, /tmp/p6-android-l1.5-hscroll.png, /tmp/p6-android-l1.5-e2-after1.png, /tmp/p6-android-l1.5-e3-start.png, /tmp/p6-android-l1.5-e3-after1..4.png |
| 3 Lesson 1.5 | to summary | 3 theory cards. e1..e3 ✅. «✅ Чисто! +30 XP». Home: ⭐ 265, 1.1–1.5 all `Пройден, повторить:` | /tmp/p6-android-l1.5-summary.*, /tmp/p6-android-3-home-l15.* |
| 4 force-stop + relaunch | unchanged | `🔥 1`, `⭐ 265`, 0.1–1.5 all `Пройден, повторить:` (bottom of the list checked after scrolling). File: `{"schemaVersion":1,"xp":265,"streak":1,"lastActiveDay":"2026-10-22","lessons":{"l0.1":{"step":0,"due":"2026-10-23"},…,"l1.5":{"step":0,"due":"2026-10-23"}}}`, with all 9 keys from l0.1 to l1.5 | /tmp/p6-android-4-relaunch.*, /tmp/p6-android-4-relaunch-bottom.* |
| 5 Crash / network | 0 FATAL, airplane 1 | `logcat -d \| grep -c "FATAL EXCEPTION"` → `0`; `airplane_mode_on` → `1` | — |
| Fix re-check (after d9d8635) | ghost of `1` blank | `install -r` with the fixed APK, airplane mode back on, then replayed 1.5 to e3 and placed `e.steps` and `1`. Both bank ghosts are now empty grey boxes. Exited the replay without saving. 0 FATAL. | /tmp/p6-android-fix-l1.5-e3-ghost.png |
| Teardown | airplane 0 | `cmd connectivity airplane-mode disable` → `airplane_mode_on` = `0` | — |

## iOS (simulator iPhone 17, 4461DE0A-043A-4C3B-8D48-DB6418058861)

**Setup.** `xcrun simctl uninstall` com.tlenparty.algolingo, then `install` build/ios/…/Algolingo.app, then `launch`. This is a fresh container with no seeding. The UI was driven with idb (`ui describe-all` / `ui tap` / `ui swipe`).

| Step | Expected | Observed | Evidence |
|------|----------|----------|----------|
| 1 Fresh Home | 9 nodes, only 0.1 open | `Серия дней: 0`, `Опыт: 0 XP`, `Начать: 0.1 Сумма и максимум, Начать`, `Закрыт: 0.2`/`0.3`/`0.4`/`1.1`. After scrolling: `Закрыт: 1.2`/`1.3 Анаграмма`/`1.4 Two Sum`/`1.5 Победители соревнования` | /tmp/p6-ios-1-home.png, /tmp/p6-ios-1-home-bottom.png |
| 2 Lessons 0.1–0.4 | each to summary, 1.1 after 0.4 | 3 theory cards and 3 exercises each, all ✅, «✅ Чисто! +30 XP» each. Опыт 30 → 60 → 90 → 120. `Закрыт: 1.1` was still shown after 0.1–0.3. After 0.4: **`Начать: 1.1 Есть ли дубликат, Начать`** | /tmp/p6-ios-l0.*-summary.png, /tmp/p6-ios-2-home1..4.png |
| 3 Lessons 1.1–1.4 | to summary, next opens | 3 theory cards each. All ✅. «✅ Чисто! +30 XP» each. Опыт 150 → 180 → 210 → 240. After 1.4: `Начать: 1.5 Победители соревнования, Начать` | /tmp/p6-ios-l1.1..l1.4-*.png, /tmp/p6-ios-3-home-l11..l14.png |
| 3 Hint (1.5 e1) | hint opens | `Подсказка, осталось 0, 💡`, `Подсказка 1 из 1`, «Подсказка снижает оценку до 💡» | /tmp/p6-ios-l1.5-hint.png |
| 3 Distractor (1.5 e1, `totalSteps.keys`) | JSON why + requeue | **«❌ Не совсем»**, «максимум по всем, а юзер с пропуском задаёт недостижимую планку: дни [(1, 100), (2, 500)], [(1, 100)] → ChampionResult([], 500) вместо ([1], 200)» (= the JSON why), «Верно: full». e2 and e3 were played, then **e1 came back** (`val maxSteps = ` + `Дырка 1: выбрана`) and was solved: «✅ Решено! +5 XP» | /tmp/p6-ios-l1.5-e1-wrong.png, /tmp/p6-ios-l1.5-requeued.png |
| 3 Lesson 1.5 template (smaller screen, 402×874 pt) | usable | Same behaviour as Android. The bank (7 chips, 3 rows) and «Проверить» stay pinned. The code area auto-scrolls to the selected hole, including the far-right holes in e3. Chips are readable. | /tmp/p6-ios-l1.5-e1-start.png, /tmp/p6-ios-l1.5-e3-start.png, /tmp/p6-ios-l1.5-e3-after1..4.png |
| 3 Lesson 1.5 | to summary | 3 theory cards. **«💡 Решено с подсказкой» +25 XP**. Home: `Опыт: 265 XP`, 1.1–1.5 all `Пройден, повторить:` | /tmp/p6-ios-l1.5-summary.png, /tmp/p6-ios-3-home-l15.png |
| 4 terminate + launch | unchanged, alive | `Серия дней: 1`, `Опыт: 265 XP`, lessons done. `Documents/progress.json` lists all 9 keys from l0.1 to l1.5 (xp 265, streak 1, due 2026-10-23). `launchctl list` shows `UIKitApplication:com.tlenparty.algolingo` (pid 58956) | /tmp/p6-ios-4-relaunch.png |
| Fix re-check (after d9d8635) | ghost of `1` blank | Rebuilt and reinstalled with data kept. Replayed 1.5 to e3 and placed `e.steps` and `1`. Both ghosts are now empty. Exited without saving (Опыт stays 265). App alive (pid 60415). | /tmp/p6-ios-fix-l1.5-e3-ghost.png |

## ROADMAP Phase 6 criteria → evidence

1. **9 lessons in the path, level 1 after level 0.** Fresh Home on both platforms shows 0.1 «Начать» and 0.2–0.4 plus 1.1–1.5 closed (step 1).
2. **1.1 opens after 0.4, the rest in order.** `Закрыт: 1.1` stays until 0.4 is done, then `Начать: 1.1`. After each of 1.1–1.4 the next lesson shows «Начать» (Android and iOS steps 2–3).
3. **Content quality.** It was customer-approved in 06-04. On device, the distractor whys match the JSON (1.2 `cnt.keys` on Android, 1.5 `totalSteps.keys` on iOS), and the hints open.
4. **1.1–1.5 pass from a clean install on Android (airplane mode) and the iOS simulator.** Both platforms played the full run from a fresh install, with no seeding. progress.json lists l0.1–l1.5, the data survives a restart, there were 0 FATAL EXCEPTION lines, and the offline proof above applies.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] Ghost bank chip still showed highlighted text**
- **Found during:** Task 2, lesson 1.5 e3 on Android (also seen on iOS).
- **Issue:** `TokenChip` drew the ghost (placed) state with `highlight(text)` and `color = Transparent`. The highlighter's per-span colors (numbers, keywords) override the Text color, so a placed `1` still showed a blue «1» in the bank. That looks as if the chip is still available. It affects any number or keyword chip (`0`, `1`, `true`, `false`, `null`), including in level 0.
- **Fix:** The ghost now composes a plain `AnnotatedString(text)`. The size is unchanged, so the bank does not reflow. The fix is in the single shared chip, so every bank uses it.
- **Files modified:** design/src/commonMain/kotlin/com/tlenparty/algolingo/design/components/TokenChip.kt
- **Commit:** d9d8635
- **Re-verified:** gates green (154 / 147, BUILD SUCCEEDED, 0 INTERNET), plus the 1.5 e3 ghost check on both platforms. No regression test was added: this is a Compose rendering detail, and UI tests are skipped for MVP (CLAUDE.md).

### Notes (not deviations)

- **1.5 layout is usable, so no follow-up is needed.** Long lines do not wrap. They are clipped on the right, and the code area scrolls horizontally. Auto-scroll to the selected hole always keeps it visible. When the view is scrolled right, the left part of the line is off screen, and a previously filled far-right hole can be partly clipped (`e.ste…`). This is readable and solvable on both screens.
- **Harness.** The 05-04 driver's `uiautomator dump` once returned a stale /sdcard/ui.xml (a Home from the previous install). The driver now deletes the file before dumping and retries. One fast swipe on Home was taken as a tap and opened 0.1; I exited it through the «Выйти из урока?» dialog without saving. Since 05-04 a per-exercise «✅ Решено! +N XP / Эталон / Продолжить» screen was added; the driver handles it.
- **Evidence** is 247 files under /tmp (p6-android-*, p6-ios-*). Drivers are in /tmp/p6drv. Nothing was committed.

## Self-Check: PASSED

- The plan's automated verifies passed: content check, gradle, 0 INTERNET, l1.5 in the APK, Algolingo.app exists; Android progress.json has l0.1–l1.5 and airplane mode is 0; the SUMMARY greps pass; the REQUIREMENTS row is present.
- Commit d9d8635 exists.
