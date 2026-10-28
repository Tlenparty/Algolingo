# Phase 9: прогон на устройствах (09-06)

**Дата:** 2026-10-28
**Сборка:** ветка исполнителя 09-06 поверх `c22788e` и коммита `90c1e0d` (scenarioPython)
**Кто проверял:** исполнитель сам (adb, idb, simctl), без ручных проверок пользователя

Устройства:
- Android: Pixel 7, Android 17, физическое устройство по adb (`-s 29211FDH200046`), `androidApp` debug (`com.tlenparty.algolingo.debug`)
- iOS: симулятор iPhone 17 Pro, iOS 26.2, `xcodebuild` Debug без подписи (`CODE_SIGNING_ALLOWED=NO`), `com.tlenparty.algolingo`

Скриншоты лежат во временном каталоге вне репозитория и не коммитятся (T-09-14). Ниже приведены только их имена: `a*` для Android и `i*` для iOS.

## Автотест: tools/smoke.sh

| Прогон | Android (connectedAndroidDeviceTest) | iOS (iosSimulatorArm64Test) | Код |
|--------|--------------------------------------|------------------------------|-----|
| 1 | smoke ✅, **python ❌** (`Detected multithreaded access to SnapshotStateObserver`) | smoke ✅, python ✅ | 1 |
| 2 (после фикса в launchApp) | smoke ✅, python ✅ | smoke ✅ (15.1 с), python ✅ (0.7 с) | **0** (71 с) |

`scenarioPython` проходит на свежем прогрессе так:
1. Переключение на Python (`lang-python`).
2. Проверка узла «Скоро на Python: …».
3. Python-l1.4 по эталону, итог «Чисто!» и +XP.
4. Пересоздание App на том же dataDir: `lang-python` выбран, l1.4 Done.
5. Переключение на Kotlin: l1.4 Done, цепочка Kotlin начинается с 0.1, узлов Soon нет.

Отдельно прогнан `E2eSmokeTest#python` на Android: ✅.

Причина красного прогона 1 и фикс описаны в SUMMARY 09-06 (Deviations).

## Android: живой прогон (adb)

Подготовка:
- `adb install -r androidApp-debug.apk`
- `pm clear com.tlenparty.algolingo.debug`
- `am start -W`
- системный запрос уведомлений: «Запретить»

| Шаг | Как проверено | Вердикт |
|-----|---------------|---------|
| Свежий прогресс | dump: «Серия дней: 0», «Опыт: 0 XP», «Начать: 0.1 Сумма и максимум», остальные «Закрыт: …» (a1-home-kotlin) | ✅ |
| Переключение на Python | `input tap` по сегменту Python. Сегмент залит зелёным (a2-home-python) | ✅ |
| Подпись «Скоро на Python» | dump: «Скоро на Python: 0.1 …», «… 0.2», «… 0.3», «… 0.4», «… 1.1», «… 1.2», «… 1.3». Под замком видна подпись «Скоро на Python» (a2) | ✅ |
| Тап по Soon-узлу | тап по 0.1: экран Home не сменился, фокус остался на MainActivity | ✅ |
| System Design при Python | вкладка SD: «Начать: SD0.1 …», «Закрыт: SD1.1/SD1.2». Нет ни сегмента языка, ни «Скоро на Python» (a4-sd-under-python) | ✅ |
| l1.4 открывается сразу | «Начать: 1.4 Two Sum» на свежем прогрессе (a3-python-l14-available) | ✅ |
| Теория и подсветка Python | карточка «Каркас»: `seen = {}`, `for i, x in enumerate(nums):`… Ключевые слова `for`/`in`/`if`/`return`/`None` выделены цветом ключевых слов (a6-l14-skeleton). В шаблоне `def`/`for`/`in`/`return`/`None` тоже подсвечены (a8, a9) | ✅ |
| Сборка решения из облачков | e1: `-`, `in` (2 дырки). e2: `enumerate(nums)`, `in`, `x` (3 дырки). e3: `{}`, `enumerate(nums)`, `-`, `(seen[target - x], i)`, `x` (5 дырок). После каждого «Проверить»: «✅ Отлично!», все дырки «верно» (a7, a9) | ✅ |
| Завершение | «Решено! +10 XP», эталон на Python, «Сложность», «Грабли», «Вариации» (a10). Итог «Чисто!», «+30 XP» (a11). Home: «Опыт: 30 XP», «Серия дней: 1», «Пройден, повторить: 1.4 Two Sum» | ✅ |
| Перезапуск | `am force-stop`, после него `pidof` пуст. `am start -W`: LaunchState COLD. Home: Python выбран, 0.1–0.4 «Скоро на Python», l1.4 «Пройден, повторить», 30 XP (a12-restart-python) | ✅ |
| Kotlin-путь | тап Kotlin: l1.4 «Пройден, повторить», 1.1–1.3 «Закрыт», 1.5 «Начать» (следующий после пройденного l1.4), подписей Soon нет (a13-kotlin-after) | ✅ |
| Kotlin-версия l1.4 | открыт l1.4 с Kotlin: скелет `for (i in nums.indices) { val need = target - nums[i] … }` (a14-kotlin-l14). Выход «Выйти» ничего не записал: 30 XP | ✅ |

## iOS: живой прогон (idb + simctl)

Подготовка:
- `simctl uninstall` и `simctl install`, то есть свежие данные приложения
- `simctl launch`
- системный запрос уведомлений: «Не разрешать»

| Шаг | Как проверено | Вердикт |
|-----|---------------|---------|
| Свежий прогресс | describe-all: «Серия дней: 0», «Опыт: 0 XP», «Начать: 0.1 …», 0.2–0.4 «Закрыт» (i1-home-kotlin) | ✅ |
| Переключение на Python | `idb ui tap` по «Python». У кнопки Python появился trait `Selected`, у Kotlin его нет (i2-home-python) | ✅ |
| Подпись «Скоро на Python» | «Скоро на Python: 0.1 …», «… 0.4», «… 1.2», «… 1.3», «… 1.5» | ✅ |
| Тап по Soon-узлу | тап по 0.1: экран не сменился, 4 Soon-узла на месте | ✅ |
| System Design при Python | «Начать: SD0.1 …», «Закрыт: SD1.1/SD1.2». Нет ни сегмента, ни Soon (i3-sd-under-python) | ✅ |
| l1.4 открывается сразу | «Начать: 1.4 Two Sum, Начать» (i4-python-l14-available) | ✅ |
| Теория и подсветка Python | «Каркас»: `for`/`in`/`if`/`return`/`None` выделены (i5-l14-skeleton). Шаблон: `def`/`for`/`in`/`return`/`None` выделены (i7) | ✅ |
| Сборка решения из облачков | те же 2/3/5 дырок, все «верно», «✅ Отлично!» (i6, i7) | ✅ |
| Завершение | итог «Чисто!», «+30 XP» (i8). Home: «Опыт: 30 XP», «Серия дней: 1», «Пройден, повторить: 1.4 Two Sum» | ✅ |
| Перезапуск | `simctl terminate` и `simctl launch`, новый pid (80755 → 81434). У Python trait `Selected`, 0.1–0.4 «Скоро на Python», l1.4 «Пройден, повторить», 30 XP (i9-restart-python) | ✅ |
| Kotlin-путь | тап Kotlin: l1.4 «Пройден, повторить», 1.2–1.3 «Закрыт», 1.5 «Начать», Soon нет (i10-kotlin-after) | ✅ |
| Kotlin-версия l1.4 | скелет `for (i in nums.indices) { … }` (i11-kotlin-l14). После «Выйти» по-прежнему 30 XP | ✅ |

## Замечания

- Комментарий `#` на устройстве не показан: в пилотном `py/l1.4` нет ни одного `#`-комментария. Подсветку `#` (и то, что `//` в Python не комментарий) покрывает `HighlightTest` (09-01).
- Длинные строки шаблона (`def two_sum(...) -> tuple[int, int] | None:`, `return (seen[target - x], i)`) уходят за правый край блока кода. Так задумано: `CodeTemplate` прокручивается по горизонтали, как и у Kotlin-уроков.
- Запрос уведомлений на Android после `pm clear` появляется и на холодном рестарте, если его отклонили один раз (вторая попытка `requestPermissions`). Это поведение существовало до фазы 9 и сюда не относится.

**Итог: все шаги на обеих платформах зелёные. smoke.sh код 0.**
