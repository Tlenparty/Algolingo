---
phase: 08-system-design-section
plan: 03
subsystem: e2e
tags: [system-design, e2e, android, ios]
requires: [08-02]
provides:
  - "scenarioSystemDesign: вкладка «System Design», все SD-уроки по каталогу, XP продолжается от кода"
  - "Общий playCatalog для код- и SD-каталогов"
affects: []
tech-stack:
  added: []
  patterns:
    - "nodes(path) — сценарии E2E берут узлы из любого каталога через loadCatalog(path)"
key-files:
  created: []
  modified:
    - shared/src/commonTest/kotlin/com/tlenparty/algolingo/e2e/E2eScenarios.kt
    - shared/src/androidDeviceTest/kotlin/com/tlenparty/algolingo/e2e/E2eSmokeTest.kt
    - shared/src/iosTest/kotlin/com/tlenparty/algolingo/e2e/E2eSmokeTest.kt
decisions:
  - "scenarioAllLessons возвращает XP, scenarioSystemDesign(startXp) продолжает с него: прогресс общий на процесс"
metrics:
  duration: "~20 min"
  completed: 2026-10-28
  tasks: 2
  files: 3
---

# Phase 8 Plan 03: E2E SD на Android и iOS Summary

В E2E smoke добавлен сценарий для System Design: он переходит на вкладку и проходит sd0.1, sd1.1 и sd1.2 через UI. Ожидания берутся из JSON и функций приложения. Цикл прохождения каталога вынесен в общий `playCatalog`. `tools/smoke.sh` проходит на Android (Pixel 7, физический) и на iOS-симуляторе (iPhone 17 Pro, iOS 26.2).

## Tasks

| # | Task | Commit |
|---|------|--------|
| 1 | scenarioSystemDesign + playCatalog, вызов в обоих E2eSmokeTest | 060935a |
| 2 | Прогон smoke, скриншоты, gen_content --check, host-тесты | без изменений кода |

## Verification

- `tools/smoke.sh`: exit 0, 67 с. В логах Android (logcat) и iOS (test-results) есть `e2e: open sd0.1/sd1.1/sd1.2`, `played 9 lessons ... 270 XP` и `played 3 lessons ... 380 XP`. Старые сценарии (launch, wrong chip, KB, все код-уроки) остались зелёными.
- `python3 tools/gen_content.py --check`: exit 0. `./gradlew :shared:testAndroidHostTest`: зелёный.
- Скриншоты финального шага sd1.1 «Шаг 4 · Схема» лежат вне репозитория:
  - Android: `/tmp/algo-ui/android-sd1.1-filled.png`, `/tmp/algo-ui/android-sd1.1-feedback.png`, `/tmp/algo-ui/android-sd1.1-reveal.png`
  - iOS: `/tmp/algo-ui/ios-sd1.1-filled.png`, `/tmp/algo-ui/ios-sd1.1-feedback.png`, `/tmp/algo-ui/ios-sd1.1-reveal.png`
  - Что видно на скриншотах: схема целиком занимает около половины ширины, горизонтальной обрезки нет. Kotlin-подсветки нет: моноширинный текст одного цвета, облачка зелёные. Подпись шага видна. На эталоне есть «⚠️ Грабли» и «🔁 Что спросят дальше».
- Устройства управлялись вручную: adb + uiautomator dump и idb (`~/.venvs/idb/bin/idb`). Скрипты лежат в `/tmp/algo-ui`, в репозиторий не попали.

## Deviations from Plan

- Вместо Android-эмулятора использован подключённый физический Pixel 7, это разрешено оркестратором. На Android-скриншотах в статус-баре видны иконки уведомлений, поэтому их не коммитим.
- Имя задачи в `<verify>` плана не существует (`compileAndroidDeviceTestKotlinAndroid`). Использована `:shared:compileAndroidDeviceTest`.

## Known Stubs

Нет.

## Self-Check: PASSED

- 060935a есть в git log; три изменённых файла на месте; скриншоты в /tmp существуют.
