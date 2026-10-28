---
phase: 12-client-server-basics-lessons
plan: 02
subsystem: e2e/sd
tags: [e2e, smoke, system-design]
requires: [12-01 SD lessons sd0.3-sd0.6 + catalog]
provides: [E2E-проверка всех 7 SD-уроков на Android-эмуляторе и iOS-симуляторе]
affects: []
tech-stack:
  added: []
  patterns: [scenarioSystemDesign обходит каталог sd/index.json, кода под новые уроки не нужно]
key-files:
  created: []
  modified: []
decisions:
  - "E2eScenarios.kt не менялся: scenarioSystemDesign берёт уроки из SD_CATALOG_PATH, литералов числа уроков нет"
metrics:
  duration: ~15min
  completed: 2026-10-28
---

# Phase 12 Plan 02: E2E SD smoke Summary

`tools/smoke.sh` прошёл все 7 SD-уроков (sd0.1, sd0.3–sd0.6, sd1.1, sd1.2) через UI на Android-эмуляторе и на iOS-симуляторе. Изменений кода не понадобилось.

## Tasks

| Task | Name | Commit |
|------|------|--------|
| 1 | Эмулятор + симулятор, smoke.sh | — (изменений нет) |
| 2 | Финальные проверки, трекинг | docs-коммит плана |

## Verification

- Android: `emulator-5554` (AVD Medium_Phone_API_36.1, Android 16), его поднял сам исполнитель. Smoke запускался с `ANDROID_SERIAL=emulator-5554`, эмулятор выбирался fail-closed. Физический Pixel 7 не использовался: ни одна команда не шла на его серийник.
- iOS: iPhone 17 Pro, iOS 26.2, уже был в состоянии Booted.
- `ANDROID_SERIAL=emulator-5554 tools/smoke.sh`: exit 0, 175 с, по 2/2 теста на каждой платформе, failures=0.
- В логах обеих платформ (logcat эмулятора и test-results iOS) есть `e2e: open sd0.1 / sd0.3 / sd0.4 / sd0.5 / sd0.6 / sd1.1 / sd1.2`, `played 7 lessons through the UI, 540 XP` (SD) и `played 9 lessons ..., 270 XP` (код).
- `python3 tools/gen_content.py --check`: exit 0. `./gradlew :shared:testAndroidHostTest`: exit 0.
- Скриншоты лежат вне репозитория: `/tmp/12-android-sd.png` (вкладка System Design: каталог SD0.1, SD0.3–SD0.5…, подписи не обрезаны) и `/tmp/12-ios.png`.

## Deviations from Plan

None - plan executed exactly as written. Скриншот финального шага нового урока не сняли: уроки на вкладке заблокированы до прохождения предыдущих. Вместо него снят каталог SD; в плане этот пункт опциональный.

## Known Stubs

None.

## Self-Check: PASSED

- Smoke exit 0, оба отчёта есть, скриншоты лежат в /tmp, в `git status` нет png и local.properties.
