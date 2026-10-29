# Phase 10: прогон на устройствах (10-05)

**Дата:** 2026-10-29
**Сборка:** main после 10-05 Task 1 (`50a3394`, scenarioPythonChain)
**Кто проверял:** исполнитель сам (adb), без ручных проверок пользователя

Устройства:
- Android: Samsung Galaxy S10e (SM-G970F), Android 12, физическое устройство по adb. Pixel 7 из плана на этой машине не подключён; заказчик подключил S10e для E2E. `androidApp` debug (`com.tlenparty.algolingo.debug`)
- iOS: **отложено**. На этой машине нет Xcode (только CommandLineTools), симулятор не запустить. Решение заказчика 2026-10-29: iOS-прогон — на машине с Xcode

**Предупреждение перед очисткой:** заказчик предупреждён, что `pm clear com.tlenparty.algolingo.debug` стирает прогресс debug-сборки на телефоне, и дал согласие («разрешил, запускай»).

Скриншоты лежат во временном каталоге вне репозитория и не коммитятся (T-10-14).

## Автотест: E2eSmokeTest (Android-часть tools/smoke.sh)

`tools/smoke.sh` целиком не запускался: он требует загруженный iOS-симулятор. Запущена его Android-команда:

```
ANDROID_SERIAL=<serial> ./gradlew :shared:connectedAndroidDeviceTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.tlenparty.algolingo.e2e.E2eSmokeTest
```

| Тест | Время | Вердикт |
|------|-------|---------|
| smoke (все Kotlin-уроки + неверное облачко + KB) | 173.4 с | ✅ |
| python (пилот: Python-режим, сохранение языка, возврат на Kotlin) | 5.5 с | ✅ |
| pythonChain (вся Python-цепочка 0.1→1.5, «Скоро на Python» = 0 до и после) | 49.9 с | ✅ |

`Finished 3 tests on SM-G970F - 12`, 0 failed, BUILD SUCCESSFUL in 4m 41s.

## Android: живой прогон (adb)

Подготовка: `./gradlew :androidApp:installDebug`, `pm clear com.tlenparty.algolingo.debug`, `am start -W`. Интерфейс читался через `uiautomator dump`, тапы — `input tap` по центрам узлов из dump.

| Шаг | Как проверено | Вердикт |
|-----|---------------|---------|
| Свежий прогресс | dump: «Серия дней: 0», «Опыт: 0 XP», «Начать: 0.1 Сумма и максимум» | ✅ |
| Переключение на Python | тап по «Python», сегмент залит зелёным | ✅ |
| Нет «Скоро на Python» | dump всего каталога с прокруткой: только «Начать: 0.1» и «Закрыт: …» (0.2, 0.3, 1.1–1.5); ни одного узла «Скоро на Python». Узел 0.4 при прокрутке в dump не попал; его отсутствие в Soon проверяет `pythonChain` по всем узлам | ✅ |
| Теория l0.1 (Python) | 3 карточки, «Когда один проход», `sum(nums)`/`max(nums)` в тексте | ✅ |
| Сборка из облачков | e1: `nums[0]` (1 дырка); e2: `0`, `nums[0]`; e3: `0`, `nums[0]`, `+=`, `>`. Каждый «Проверить» → «✅ Отлично!», все дырки «верно» | ✅ |
| Подсветка Python | `def`, `for`, `in`, `if`, `return` выделены цветом ключевых слов, числа — цветом чисел (a6) | ✅ |
| Решено | «✅ Решено! +10 XP», эталон на Python, «Время O(n) · Память O(1)», грабли | ✅ |
| Итог на Home | «Опыт: 30 XP», «Серия дней: 1», «Пройден, повторить: 0.1», «Начать: 0.2» | ✅ |
| Перезапуск | `am force-stop`, `pidof` пуст, `am start -W`: LaunchState COLD. Python выбран (a9), 0.1 Done, 30 XP | ✅ |
| Kotlin-путь | тап Kotlin: 0.1 «Пройден, повторить», 0.2 «Начать», Soon нет | ✅ |
| Kotlin-версия l0.1 | открыт 0.1: шаблон `fun sumAndMax(nums: IntArray): Pair<Int, Int> {`, `var sum = 0`. «Выйти» ничего не записал: 30 XP | ✅ |
| l1.5 из UI | вживую вручную не проходился (требует всю цепочку); пройден из UI на этом же устройстве тестом `pythonChain` (вся цепочка до 1.5, «Чисто!» на каждом уроке) | ✅ (через E2E) |

## iOS (iPhone 17 Pro simulator)

| Шаг | Вердикт |
|-----|---------|
| `iosSimulatorArm64Test` E2eSmokeTest (smoke, python, pythonChain) | ⏳ отложено (нет Xcode) |
| Живой прогон (idb) | ⏳ отложено (нет Xcode) |

`./gradlew :shared:compileTestKotlinIosSimulatorArm64` собирается (BUILD SUCCESSFUL): iOS-тест `pythonChain` компилируется.

## Итог

Android: все шаги зелёные. iOS: не выполнено, ждёт машины с Xcode. Фаза 10 не закрывается до iOS-прогона.
