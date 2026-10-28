---
phase: 08-system-design-section
reviewed: 2026-10-28T00:00:00Z
depth: standard
files_reviewed: 22
files_reviewed_list:
  - design/src/commonMain/kotlin/com/tlenparty/algolingo/design/components/CodeBlock.kt
  - design/src/commonMain/kotlin/com/tlenparty/algolingo/design/components/TokenChip.kt
  - shared/src/androidDeviceTest/kotlin/com/tlenparty/algolingo/e2e/E2eSmokeTest.kt
  - shared/src/androidHostTest/kotlin/com/tlenparty/algolingo/lesson/LessonContentTest.kt
  - shared/src/commonMain/composeResources/files/content/sd/index.json
  - shared/src/commonMain/composeResources/files/content/sd/sd0.1.json
  - shared/src/commonMain/composeResources/files/content/sd/sd1.1.json
  - shared/src/commonMain/composeResources/files/content/sd/sd1.2.json
  - shared/src/commonMain/kotlin/com/tlenparty/algolingo/App.kt
  - shared/src/commonMain/kotlin/com/tlenparty/algolingo/lesson/LessonRepository.kt
  - shared/src/commonMain/kotlin/com/tlenparty/algolingo/lesson/Model.kt
  - shared/src/commonMain/kotlin/com/tlenparty/algolingo/lesson/ui/ExerciseScreen.kt
  - shared/src/commonMain/kotlin/com/tlenparty/algolingo/lesson/ui/LessonScreen.kt
  - shared/src/commonMain/kotlin/com/tlenparty/algolingo/lesson/ui/RevealScreen.kt
  - shared/src/commonMain/kotlin/com/tlenparty/algolingo/lesson/ui/TheoryScreen.kt
  - shared/src/commonMain/kotlin/com/tlenparty/algolingo/progress/Catalog.kt
  - shared/src/commonMain/kotlin/com/tlenparty/algolingo/ui/components/CodeTemplate.kt
  - shared/src/commonMain/kotlin/com/tlenparty/algolingo/ui/components/ResultPanel.kt
  - shared/src/commonTest/kotlin/com/tlenparty/algolingo/e2e/E2eScenarios.kt
  - shared/src/commonTest/kotlin/com/tlenparty/algolingo/lesson/LessonRepositoryTest.kt
  - shared/src/commonTest/kotlin/com/tlenparty/algolingo/lesson/ModelTest.kt
  - shared/src/iosTest/kotlin/com/tlenparty/algolingo/e2e/E2eSmokeTest.kt
findings:
  critical: 0
  warning: 3
  info: 5
  total: 8
status: issues_found
---

# Phase 08: отчёт code review

**Проверено:** 2026-10-28
**Глубина:** standard
**Файлов:** 22
**Статус:** issues_found

## Summary

Смотрел изменения от `47a6b8f` до HEAD: `LessonKind` и правила `validate` для design, загрузку по префиксу `sd`, параметризованный `loadCatalog`, третью вкладку и вынесенный `HomeTab`, флаг `highlight` во всех местах, где рендерятся токены, `RevealScreen` для design, три SD-урока и тесты (unit, контент, e2e).

Блокеров не нашёл. Прогресс общий и хранится по id. `dueIds` и `lessonStatuses` фильтруют по каталогу вкладки, поэтому `sd*` не смешиваются с `l*`. `solutionSpans(ex)` и `assembled(ex)` оба берут `ok.firstOrNull()`, так что подсветка дырок в эталоне design-шага совпадает с текстом. Подсветку Kotlin отключили во всех местах, где она рисуется: шаблон, дырка, чип банка, панель результата, эталон, скелет теории. Арифметика в контенте сходится (100M / 86 400 ≈ 1 157; 62⁷ ≈ 3.5·10¹²).

Главное, что нашёл:
- В design-уроках главная ошибка — переставить правильные облачка местами. На неё игрок получает безликое «Здесь нужно другое.», без объяснения.
- Блок «Грабли / Что спросят дальше» привязан к последнему элементу очереди, а не к шагу со схемой.
- Правило «≤ 34 символов» считает символы, а не реальную ширину строки с дырками.

## Warnings

### WR-01: Перестановка правильных облачков в design-шагах объясняется только текстом «Здесь нужно другое.»

**File:** `shared/src/commonMain/composeResources/files/content/sd/sd0.1.json:96` (также `sd0.1.json:33`, `sd1.1.json:230`, `sd1.2.json:228`)
**Issue:** `checkExercise` (`ExerciseEngine.kt:59`) берёт причину из `hole.wrongWhy[text] ?: chip.why`. У правильных облачков нет `why`, а `validate` не требует `why` для облачков из `used` (`Model.kt`, проверка дистракторов). Если поставить правильное облачко не в ту дырку, `panelModel` покажет только «Здесь нужно другое.» и «Верно: X».

В коде это редкая ошибка, а в design-шагах — основная:
- sd0.1.e2 целиком состоит из выбора F или NF. Если поставить `NF` на «Сократить ссылку», объяснения, почему это F, не будет.
- sd0.1.e1 проверяет порядок шагов. Перестановка «Общая схема» и «Оценки нагрузки» никак не объясняется.
- sd1.1.e4 и sd1.2.e4: если перепутать Redis, NoSQL БД, Kafka, Prometheus или Grafana, объяснения тоже нет.

В sd1.2.e2 `wrongWhy` для перестановок уже добавлен, так что в контенте этот вопрос решён непоследовательно. Это противоречит Core Value: игрок должен «сразу понять, где грабли».

**Fix:** Добавить `wrongWhy` на вероятные перестановки, например в sd0.1.e2:
```json
{ "ok": ["F"], "wrongWhy": { "NF": "это действие пользователя — что система делает, значит F" } }
{ "ok": ["NF"], "wrongWhy": { "F": "это «насколько быстро» — качество, а не функция, значит NF" } }
```
По желанию можно ужесточить `validate` для `design`: каждое `ok[0]` другой дырки, которое можно поставить в дырку `h`, должно иметь `wrongWhy` в `h`. То же правило, что сейчас действует для дистракторов.

### WR-02: «Грабли» и «Что спросят дальше» показываются на последнем шаге очереди, который не всегда шаг со схемой

**File:** `shared/src/commonMain/kotlin/com/tlenparty/algolingo/lesson/ui/RevealScreen.kt:57`
**Issue:** `isLastExercise(state)` равен `queue.size == 1`. Ошибочный шаг редьюсер ставит в конец очереди (`LessonReducer.kt:105`). Пример: игрок ошибся на sd1.1.e2 (API). Очередь становится `[e3, e4, e2]`. Тогда эталон схемы (e4) показывается без граблей, а грабли про `301`, коллизии хеша и синхронную запись кликов появляются под эталоном API-шага.

В code-уроке это незаметно, потому что любой Reveal показывает полный `sol.code`. В design эталон теперь свой на каждом шаге (`assembled(ex)`), и CONTEXT прямо требует показывать грабли «на последнем шаге» — то есть на финальной схеме.

**Fix:** Для design привязать блок к шагу со схемой:
```kotlin
val showOutro = if (design) ex.id == state.lesson.exercises.last().id else isLastExercise(state)
if (showOutro) { ... }
```
Другой вариант — оставить как есть, но тогда для design показывать ещё и `sol.code`, чтобы грабли стояли рядом со схемой.

### WR-03: `MAX_DESIGN_LINE` считает символы, а строки с дырками на экране шире

**File:** `shared/src/commonMain/kotlin/com/tlenparty/algolingo/lesson/Model.kt:138`
**Issue:** В `validate` проверяется `line.length` у собранной строки. На экране каждая дырка добавляет `padding(horizontal = spacing.sm)` с обеих сторон, рамку и `widthIn(min = holeMinWidth = 48.dp)` (`CodeTemplate.kt`, `Hole`), а текст в дырке набран `codeBold`.

Поэтому строка из 34 символов с двумя-тремя дырками (например, `API Gateway ──→ Redis` в sd1.2.e4) заметно шире 34 моноширинных символов. Цель «влезает в экран без горизонтального скролла» тест не гарантирует. Кроме того, `String.length` считает UTF-16 code units, а не ширину: стрелки `→ ↓ ──→` в fallback-шрифте могут оказаться шире ячейки моноширинного шрифта. Контент сейчас укладывается с запасом, но тест не защищает от регрессии. Горизонтальный скролл остаётся, поэтому это не блокер.

**Fix:** Учитывать оформление дырок. Например, считать каждую дырку как `ok[0].length + 2` (padding и рамка) и `max(ok.length, 3)` (min width):
```kotlin
val holeExtra = 2
// при сборке строки для проверки: append(ok0); width += ok0.length.coerceAtLeast(3) + holeExtra
```
Или проверить ширину на устройстве в e2e: `assertWidthIsAtMost` для строки шаблона.

## Info

### IN-01: Устаревший импорт и разные способы обойти затенение имени `highlight`

**File:** `design/src/commonMain/kotlin/com/tlenparty/algolingo/design/components/CodeBlock.kt:20`
**Issue:** Параметр `highlight: Boolean` затеняет функцию `highlight(...)`. В `CodeBlock` проблему обошли полным именем, и импорт `com.tlenparty.algolingo.design.highlight` больше не используется (предупреждение компилятора). В `TokenChip`, `CodeTemplate` и `ResultPanel` затенённое имя вызывается напрямую: компилятор выбирает функцию, потому что у `Boolean` нет `invoke`. Код рабочий, но при чтении путает.
**Fix:** Назвать параметр `kotlinHighlight` или `syntax` во всех местах и удалить FQN.

### IN-02: KDoc `loadCatalog` оторвался от функции

**File:** `shared/src/commonMain/kotlin/com/tlenparty/algolingo/progress/Catalog.kt:28-29`
**Issue:** Комментарий «Any read/parse failure gives an empty catalog…» теперь стоит над `const val CATALOG_PATH`, а не над `loadCatalog`.
**Fix:** Перенести константы выше комментария.

### IN-03: Маршрутизация по префиксу `sd` не защищена от коллизии со стороны основного каталога

**File:** `shared/src/commonMain/kotlin/com/tlenparty/algolingo/lesson/LessonRepository.kt:25-26`
**Issue:** `LessonContentTest.everySdLessonIsStrictValidDesign` проверяет только одно направление: SD-id нет в основном каталоге и в `lessons/`. Если в основной каталог или генератор попадёт id, начинающийся с `sd`, его молча будут искать в `files/content/sd/`, и пользователь увидит экран ошибки.
**Fix:** В `everyCatalogLessonIsStrictAndValid` добавить проверку `assertTrue(ids.none { it.startsWith("sd") })`.

### IN-04: `ex === lesson.exercises.last()` сравнивает по ссылке

**File:** `shared/src/commonMain/kotlin/com/tlenparty/algolingo/lesson/Model.kt:135`
**Issue:** Сравнение по ссылке работает, потому что элементы списка приходят из разных декодирований. Но если тест или будущий код соберёт `listOf(e, e)`, проверка сработает для обоих элементов. Индекс читается однозначно.
**Fix:** `for ((i, ex) in lesson.exercises.withIndex())` и `i == lesson.exercises.lastIndex`.

### IN-05: Подпись «System Design» в нижней панели без иконки

**File:** `shared/src/commonMain/kotlin/com/tlenparty/algolingo/App.kt:86-91`
**Issue:** Подпись из трёх вкладок с пустым `icon = {}` — самая длинная. На узких экранах (≤ 360dp) с крупным системным шрифтом `NavigationBarItem` обрежет её многоточием. E2e это не поймает: `hasText` смотрит на семантику, а не на то, что видно.
**Fix:** Короткая подпись («Дизайн» или «SD») либо проверка на эмуляторе с fontScale 1.3.

## Итоги исправлений (2026-10-28)

Подробности в `08-REVIEW-FIX.md`.

| Finding | Итог | Коммит |
|---------|------|--------|
| WR-01 | исправлено: `wrongWhy` на перестановку правильных облачков во всех SD-шагах (кроме sd0.1.e3, где все три ответа взаимозаменяемы) | bea90fd |
| WR-02 | исправлено: для design блок «Сложность / Грабли / Что спросят дальше» показывается только под последним шагом урока | 02922af |
| WR-03 | не исправлялось (решение оркестратора: контент влезает с запасом) | — |
| IN-01 | исправлено: параметр `highlight: Boolean` переименован в `syntax`, FQN убран | b05fdd1 |
| IN-02 | исправлено: KDoc снова стоит над `loadCatalog` | 0be8448 |
| IN-03 | не исправлялось (решение оркестратора) | — |
| IN-04 | исправлено: сравнение по `lastIndex` | c5900a9 |
| IN-05 | не исправлялось (решение оркестратора) | — |

---

_Reviewed: 2026-10-28_
_Reviewer: Claude (gsd-code-reviewer)_
_Depth: standard_
