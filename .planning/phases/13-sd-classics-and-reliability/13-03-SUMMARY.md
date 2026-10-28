---
phase: 13-sd-classics-and-reliability
plan: 03
subsystem: e2e/sd
tags: [e2e, smoke, system-design]
requires: [13-01 SD lessons sd1.3-sd1.7, 13-02 SD lessons sd2.1-sd2.3]
provides: [E2E-проверка всех 15 SD-уроков на Android-эмуляторе и iOS-симуляторе]
affects: []
tech-stack:
  added: []
  patterns: [scenarioSystemDesign обходит каталог sd/index.json, кода под новые уроки не нужно]
key-files:
  created: []
  modified: []
decisions:
  - "E2eScenarios.kt не менялся: число SD-уроков берётся из SD_CATALOG_PATH"
metrics:
  duration: ~20min
  completed: 2026-10-28
---

# Phase 13 Plan 03: E2E SD smoke Summary

`tools/smoke.sh` прошёл все 15 SD-уроков через UI на Android-эмуляторе и на iOS-симуляторе: sd0.1, sd0.3–sd0.6, sd1.1–sd1.7 и sd2.1–sd2.3. Код и контент не менялись.

## Tasks

| Task | Name | Commit |
|------|------|--------|
| 1 | Эмулятор + симулятор, smoke.sh | — (изменений нет) |
| 2 | Финальные проверки, трекинг | docs-коммит плана |

## Verification

- Android: `emulator-5554` (AVD Medium_Phone_API_36.1, Android 16), он уже был запущен. Smoke запускался с `ANDROID_SERIAL=emulator-5554`, эмулятор выбирался fail-closed. Физический Pixel 7 не использовался: в логах `/tmp/13-03-*.log` серийник 29211FDH200046 встречается 0 раз.
- iOS: iPhone 17 Pro (A77D1979-5959-4953-A9F0-EFC6A9AEB421), iOS 26.2, был в состоянии Booted.
- Первый прогон smoke завершился с exit 1. На Android упал тест `python`, тест `smoke` (код + SD) прошёл, а на iOS прошли 2/2. Ошибка: `No node found ... 'Скоро на Python: 0.1 Сумма и максимум'` в onHome/performScrollToNode. Отдельный запуск `E2eSmokeTest#python` дал зелёный результат, повторный полный `smoke.sh` — exit 0 (157 с, Android 2/2, iOS 2/2, failures=0). Падение нестабильное и к SD отношения не имеет: Python-сценарий относится к фазам 09/10.
- Логи обеих платформ: `e2e: played 15 lessons through the UI, 860 XP` (SD) и `played 9 lessons ..., 270 XP` (код). На iOS есть `e2e: open` для каждого из 15 SD-уроков.
- `python3 tools/gen_content.py --check`: exit 0. `./gradlew :shared:testAndroidHostTest`: exit 0.
- Скриншоты лежат вне репозитория. `/tmp/13-android.png` — вкладка System Design, подписи SD0.1–SD0.5 не обрезаны. `/tmp/13-ios.png` — главная, без обрезки.

## Deviations from Plan

None - plan executed exactly as written. Скриншот финального шага нового урока не сняли: после теста прогресс сбрасывается и новые уроки на вкладке заблокированы. Вместо него снят каталог SD.

## Deferred Issues

- Нестабильный тест `E2eSmokeTest.python` на Android: `performScrollToNode` иногда не находит «Скоро на Python: 0.1». Это область фаз 09/10, здесь не правили.

## Known Stubs

None.

## Self-Check: PASSED

- Smoke exit 0 со второго прогона, оба отчёта есть, скриншоты лежат в /tmp, в `git status` нет png и local.properties.
