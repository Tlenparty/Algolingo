---
phase: 14-level-2-easy-lessons-6-leetcode-easy-283-26-88-977-392-643-k
plan: 04
subsystem: e2e, verification
tags: [e2e, level-2, android, ios, phase-complete]
requires: [14-01, 14-02, 14-03]
provides: ["e2e order check l1.5 -> l2.1..l2.6 (Kotlin + Python)", "14-VERIFICATION-DEVICES.md (Android + iOS)"]
affects: [shared/src/commonTest e2e, shared/src/androidDeviceTest]
tech-stack:
  added: []
  patterns: ["e2e waits for semantic state (waitFor + isSelected) instead of asserting right after a tap"]
key-files:
  created:
    - .planning/phases/14-level-2-easy-lessons-6-leetcode-easy-283-26-88-977-392-643-k/14-VERIFICATION-DEVICES.md
  modified:
    - shared/src/commonTest/kotlin/com/tlenparty/algolingo/e2e/E2eScenarios.kt
    - shared/src/androidDeviceTest/kotlin/com/tlenparty/algolingo/e2e/E2eSmokeTest.kt
    - .planning/ROADMAP.md
    - .planning/STATE.md
decisions:
  - "Android E2eSmokeTest: smoke/pythonChain testTimeout 10m -> 30m (the full catalog takes about 13 min on the emulator since level 2)"
  - "Language segment checks wait for Selected: the selection follows the DataStore write and does not appear at compose idle"
metrics:
  duration: ~75 min
  completed: 2026-10-30
---

# Phase 14 Plan 04: E2E-порядок уровня 2, прогоны и закрытие фазы. Summary

Теперь e2e проверяет, что l2.1..l2.6 стоят в каталоге сразу после l1.5 на Kotlin и на Python. Все гейты зелёные: gen_content, test_gen_content, host-тесты, iOS-тесты и Android E2E. l2.1 пройден вживую на эмуляторе Android и на симуляторе iOS через idb на обоих языках. Фаза 14 закрыта.

## Tasks

| # | Task | Commit |
|---|------|--------|
| 1 | Проверка порядка l1.5 -> l2.x в e2e, полный прогон тестов | 1b9cede |
| 2 | Прогоны на устройствах, 14-VERIFICATION-DEVICES.md, ROADMAP/STATE | docs(14) commit |

## Verification

- `gen_content.py --check`: exit 0. `test_gen_content.py`: 82 OK. `:shared:testAndroidHostTest`: exit 0.
- `:shared:iosSimulatorArm64Test`: 201 тест, 0 failures. E2eSmokeTest smoke/python/pythonChain зелёные.
- `connectedAndroidDeviceTest` E2eSmokeTest на emulator-5554: 3/3, 22m 9s.
- Вживую: Android l2.1 на Kotlin и Python повторно после изменения, все 12 прогонов уже были в 14-02/03. iOS (iPhone 17 Pro, iOS 26.2): l2.1 на Kotlin и Python до «Чисто!», комментарии влезают в экран. Подробности в 14-VERIFICATION-DEVICES.md.

## Deviations from Plan

**1. [Rule 1 - Bug] Гонка в e2e при проверке сегмента языка**
- **Found during:** Task 1, connectedAndroidDeviceTest на эмуляторе
- **Issue:** `python` и `pythonChain` вызывали `assertIsSelected()` сразу после тапа по «Python». Выделение выставляется только после записи в DataStore, а `waitForIdle` её не ждёт, поэтому на эмуляторе получалось `Selected = 'false'`.
- **Fix:** 3 места заменены на `waitFor(hasTestTag(...) and isSelected(), ...)`.
- **Commit:** 1b9cede

**2. [Rule 3 - Blocking] testTimeout Android smoke**
- **Issue:** с 6 новыми уроками smoke занимает около 13 мин, а лимит был 10 мин. Тест падал с `AndroidComposeUiTestTimeoutException`.
- **Fix:** в androidDeviceTest для smoke и pythonChain лимит поднят до 30 мин. На iOS тест идёт 31 с, там не менял.
- **Commit:** 1b9cede

**3. [Scope] Прогон на iOS вручную**
- План требовал только эмулятор. iOS добавлен по указанию оркестратора: прогресс записан в `Documents/progress.json` на симуляторе, урок пройден через idb.

## Known Stubs

None.

## Self-Check: PASSED

- FOUND: 14-VERIFICATION-DEVICES.md, 14-04-SUMMARY.md
- FOUND: commit 1b9cede
