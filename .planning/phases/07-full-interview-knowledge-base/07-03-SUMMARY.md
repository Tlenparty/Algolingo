---
phase: 07-full-interview-knowledge-base
plan: 03
subsystem: kb
tags: [kb, device-run, android, ios, evidence]
requires: [07-01, 07-02]
provides: [phase-7-device-evidence, KB-07-complete]
affects: [REQUIREMENTS.md]
tech-stack:
  added: []
  patterns: ["agent-driven device walkthrough (uiautomator/adb on Android, idb on iOS), evidence only in /tmp"]
key-files:
  created:
    - .planning/phases/07-full-interview-knowledge-base/07-03-SUMMARY.md
  modified:
    - .planning/REQUIREMENTS.md
decisions:
  - "No app bug was found on either platform, so there are no fix(07-03) commits"
  - "KB cold load (~0.4 s on the iOS simulator) needs no single-file bundle; research A2 is closed"
metrics:
  duration: ~45 min
  completed: 2026-10-22
requirements: [KB-07, KB-02, KB-05, KB-06]
---

# Phase 7 Plan 03: Full KB device walkthrough (Android + iOS) Summary

Both freshly built apps were run from a clean install. They carry the whole interview-prep base: 217 notes in 14 collapsible sections and 25 images. On the Android emulator and the iOS simulator, sections, note opening, `#anchor` and `[[wikilink]]` navigation (also across folders), images, search, note 05 and lessons all work. There were no crashes and no app bugs. KB-07 is now Complete.

## Gates

| Command | Result |
|---------|--------|
| `python3 tools/gen_content.py --check` | exit 0 (no drift) |
| `python3 -m unittest tools/test_gen_content.py` | Ran 33 tests, OK |
| `./gradlew :shared:allTests :androidApp:assembleDebug` | green: testAndroidHostTest **160 tests, 0 failures**; iosSimulatorArm64Test **150 tests, 0 failures** (Phase 6: 154 / 147) |
| `rm -rf build/ios/.../Algolingo.app`, then `xcodebuild -project iosApp/iosApp.xcodeproj -scheme iosApp -configuration Debug -sdk iphonesimulator -destination 'id=4461DE0A…' -derivedDataPath build/ios CODE_SIGNING_ALLOWED=NO build` | **BUILD SUCCEEDED** (the stale .app was removed first) |

**Bundle checks**
- iOS `Algolingo.app`: 25 `files/kb/img/*.png`, 217 `files/kb/n*.md`. Its `files/kb/index.json` is byte-identical (`cmp`) to `shared/src/commonMain/composeResources/files/kb/index.json`.
- APK `androidApp-debug.apk`: 217 `files/kb/n*` entries, 25 `files/kb/img/` entries.
- **APK size: 22,636,643 bytes (22.6 MB), up from 14.5 MB in Phase 6 (+8.1 MB, of which about 7.1 MB is images).**

## Offline proof

1. `aapt2 dump permissions androidApp-debug.apk` lists only `com.tlenparty.algolingo.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION`, with **0 `android.permission.INTERNET` lines**.
2. No networking code or dependency was added in Phase 7. The Phase 6 grep for URLSession, NSURL, ktor and okhttp still applies, and images load from bundled resources through `Res.readBytes`.

## Android (emulator-5554)

**Setup.** I ran these literal commands: `adb -s emulator-5554 install -r androidApp/build/outputs/apk/debug/androidApp-debug.apk` (Success), `pm clear com.tlenparty.algolingo` (Success), `logcat -c`, `am start -n com.tlenparty.algolingo/.MainActivity`, then opened «База знаний». Every adb call used the literal `-s emulator-5554`. `installDebug` was never used.

| Step | Expected | Observed | Evidence |
|------|----------|----------|----------|
| 1 Sections | 14 collapsed headers, Обзор first; Kotlin / Java expands to 33, stays open after note → Back, collapses | The list showed immediately, with no lingering «…»: `▸ Обзор (2)`, `▸ Алгоритмы (23)`, `▸ Kotlin / Java (33)` … `▸ Общие вопросы (23)` (14 headers). Tapping Kotlin / Java gave `▾ Kotlin / Java (33)` with notes `Abstract class vs Interface…`, `Any,Nothing,Unit`, …. I opened «Data class» and pressed Back: still `▾`. Tapping the header gave `▸` again | /tmp/p7-android-1-list.xml/.png, 1-expanded, 1-note, 1-back, 1-collapsed |
| 2 СОДЕРЖАНИЕ | `#anchor` scrolls; wikilink opens; Back | I tapped «Compose» in «Быстрый переход». The note scrolled to `🎨 Jetpack Compose`. Tapping `Coroutines` opened «Корутины в Kotlin — это легковесный механизм…». Back returned to СОДЕРЖАНИЕ at the same position | /tmp/p7-android-2-toc, 2-anchor, 2-wikilink, 2-wikiback |
| 3 Cross-folder hop | Обзор → Kotlin / Java → link → Back ×2 | Path: «Подготовка к собеседованию» (Обзор) → `ООП` (Kotlin / Java) → «Композиция vs наследование» from the «Связано:» line. Back went to ООП, then Back went to `🎯 Подготовка к собеседованию` | /tmp/p7-android-3-prep, 3-oop, 3-oop-links, 3-hop, 3-backtwice |
| 4 Images | screenshot rendered, no placeholder; 13 screenshots full-width; ViewModel image first; `==mark==` tinted | rendered. In «Compose. Оптимизация» the first dump already had 2 nodes `content-desc="Картинка из заметки"` and 0 «🖼 картинка». While scrolling to the end, all **13** images rendered at bounds `[42,…][1038,…]` (full content width). There was no placeholder in any dump and no crash. «ViewModel, SaveStateHandle…»: the first block is `Картинка из заметки` (UML diagram). «Stream vs Collection vs Sequence»: `==создает новую коллекцию (eager evaluation).==` showed as a yellow-tinted span | /tmp/p7-android-4-images.xml/.png, 4-scroll8/16/24.png, 4-viewmodel, 4-highlight.png |
| 5 Search | «HashMap» hits from ≥2 sections; «(» no crash; clear → headers | «HashMap» returned `HashMap vs HashSet` (Kotlin / Java); `HashMap`, `Как работает HashMap`, `Как выглядит Bucket…` (Вопросы с собесов); and СОДЕРЖАНИЕ, 01, 04, 05, LRU Cache with snippets. Results appeared instantly. «(» returned `Поиск в глубину (DFS)`, `UDF (…)`, … with no crash. Clearing the query brought back the collapsed headers | /tmp/p7-android-5-hashmap, 5-paren, 5-cleared |
| 6 Note 05 + lessons | 05 unchanged; a lesson opens | «05 Тренажёр решений» opened with «Тренажёр решений — учимся ПИСАТЬ код» and «Связано: 00 Компас…» as before. On Главная, «Начать: 0.1» opened the lesson with the theory card «Когда один проход» | /tmp/p7-android-6-note05, 6-lesson |
| 7 Dark theme | image + table readable | With `cmd uimode night yes`, «BIG O» (table + code) and «ViewModel…» (image) were both readable on the dark background. Night mode was restored with `cmd uimode night no` (reported `Night mode: no`) | /tmp/p7-android-7-dark-table.png, 7-dark-image.png |
| 8 Crashes | 0 | `logcat -d \| grep -c "FATAL EXCEPTION"` = **0** | — |

## iOS (simulator 4461DE0A…, iPhone 17)

**Setup.** I ran `xcrun simctl uninstall … com.tlenparty.algolingo`, then `install` with the fresh `Algolingo.app`, then `launch` (pid 80890). The simulator was in dark appearance, so dark mode was covered on iOS too. I drove the UI with `idb ui describe-all/tap/swipe` and used the on-screen «Назад».

| Step | Expected | Observed | Evidence |
|------|----------|----------|----------|
| 1 Sections | 14 collapsed headers; expand / Back / collapse | **Cold load:** from tapping «База знаний» to the headers being in the accessibility tree took ~1.2 s, of which 0.8 s was the driver's post-tap sleep. So the load is about 0.4 s, with no visible «…». There were 14 `▸` headers (`▸ Общие вопросы (23)` after scrolling). `▾ Kotlin / Java (33)` → «Data class» → Назад: still `▾`. Tapping the header gave `▸` | /tmp/p7-ios-1-list.png, 1-expanded, 1-collapsed |
| 2 СОДЕРЖАНИЕ | anchor scrolls; wikilink opens; Back | Tapping «Compose» in «Быстрый переход» showed `🎨 Jetpack Compose` at the top. `Coroutines` opened «Корутины». Назад returned to СОДЕРЖАНИЕ | /tmp/p7-ios-2-toc, 2-anchor, 2-pre, 2-wikilink |
| 3 Cross-folder hop | Обзор → Kotlin / Java → link → Back ×2 | «Подготовка к собеседованию» → `ООП` → «Композиция vs наследование» (from «Связано:»). Назад returned to ООП, then Назад returned to `🎯 Подготовка к собеседованию` | /tmp/p7-ios-3-oop, 3-oop-links, 3-hop, 3-backtwice |
| 4 Images | rendered, no placeholder; 13 full-width; ViewModel image first | rendered. With the first screenshot of «Compose. Оптимизация» on screen, `idb ui describe-all` → /tmp/p7-ios-4-images.json contains `Картинка из заметки` (and 0 `🖼`). A slow scroll to the end counted **13** images, all at `x=16, width=370` pt (full width), with no placeholder and no crash. «ViewModel, SaveStateHandle…»: the first block is `Картинка из заметки` | /tmp/p7-ios-4-images.json/.png, 4-scroll8/16.png, 4-viewmodel.png |
| 5 Search | «HashMap» ≥2 sections; «(» no crash; clear | «HashMap» returned the same hits as on Android: `HashMap vs HashSet` (Kotlin / Java), `HashMap` / `Как работает HashMap` / `Как выглядит Bucket…` / `Как сравниваются ключи…` (Вопросы с собесов), plus СОДЕРЖАНИЕ, 01, 04, 05 with snippets. «(» returned `Поиск в глубину (DFS)`, … with no crash. Clearing the query brought back the collapsed headers | /tmp/p7-ios-5-hashmap, 5-paren, 5-cleared |
| 6 Note 05 + lessons | 05 unchanged; a lesson opens | «05 Тренажёр решений» showed the same header and intro. «Начать: 0.1» showed «Когда один проход» | /tmp/p7-ios-6-note05, 6-lesson |
| Crash check | process alive | `simctl spawn … launchctl list \| grep -i algolingo` gave `80890 0 UIKitApplication:com.tlenparty.algolingo…`. That is the same pid as at launch, so there was no crash or relaunch | — |

## ROADMAP Phase 7 criteria → evidence

1. **Recursive sync, stable ids.** 07-01: `sync_notes.py` folder allow-list; KbContentTest pins the 23 old ids, and `gen_content.py --check` + 33 tests are green (Gates above).
2. **Sections by folder/topic; search across the whole base.** 07-01/07-02 tests (14 sections, cross-section search). On devices: steps 1 and 5 on both platforms (14 headers; «HashMap» hits in Kotlin / Java and Вопросы с собесов, among others).
3. **Cross-folder `[[links]]`; images do not break the screen.** 07-01 KbContentTest (links resolve except the 5 known ones; images present, no orphans). On devices: steps 2–4 on both platforms (Обзор → Kotlin / Java hop; 13 screenshots rendered, not placeholders).
4. **Bundle test + device run.** The bundle half is 07-01's KbContentTest (all 217 notes decode and parse; green in `:shared:allTests` on both targets). The device half is the Android and iOS tables above (sections, opening, search, link navigation).

## Deviations from Plan

- **No per-task commits for tasks 1–2.** These tasks produce no code, only SUMMARY content, and the walkthrough found no app bug. The SUMMARY and REQUIREMENTS.md change are committed together in one docs commit.
- **Harness (iOS search input).** `idb ui text HashMap` typed `РфырЬфз`, because idb sends HID keycodes and the host keyboard layout is Russian. I worked around it with `xcrun simctl pbcopy` + long-press the field + «Paste». This is not an app bug. «(» typed correctly.
- **Harness (Android).** The first focus on the search field opened Gboard's «Try out your stylus» onboarding sheet. I dismissed it with Cancel.
- **Harness (iOS taps during fling).** idb swipes leave inertia, and a tap during a fling only stops the scroll. The driver now waits about 1.5 s before tapping.
- **Visual note (not a bug, no redesign):** in dark theme, screenshots with transparent backgrounds (for example the ViewModel UML diagram) show their grey text with low contrast against the dark background. The text is still readable.

## Known Stubs

None.

## Self-Check: PASSED
