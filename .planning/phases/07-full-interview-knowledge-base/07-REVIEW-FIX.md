---
phase: 07-full-interview-knowledge-base
fixed_at: 2026-10-22T00:00:00Z
review_path: .planning/phases/07-full-interview-knowledge-base/07-REVIEW.md
iteration: 1
findings_in_scope: 4
fixed: 4
skipped: 0
status: all_fixed
---

# Фаза 07: отчёт об исправлениях после code review

**Исправлено:** 2026-10-22
**Исходный review:** .planning/phases/07-full-interview-knowledge-base/07-REVIEW.md
**Итерация:** 1

**Сводка:**
- Находок в scope: 4 (critical_warning)
- Исправлено: 4
- Пропущено: 0

## Исправленные находки

### WR-01: `==mark==` не закрывается, если перед закрывающим `==` стоит пробел

**Изменённые файлы:** `shared/src/commonMain/kotlin/com/tlenparty/algolingo/kb/MarkdownParser.kt`, `shared/src/commonTest/kotlin/com/tlenparty/algolingo/kb/MarkdownParserTest.kt`
**Коммит:** 780aee8
**Что сделано:** «не пробел» теперь проверяется только у открывающего маркера. Закрывает ближайший `==`, кроме `===`, а у содержимого Mark обрезаются пробелы в конце. `==a ==b ==c== d` теперь разбирается как `[Mark("a"), Text("b "), Mark("c"), Text(" d")]`. В тест `mark()` добавлены два регрессионных случая. Рендер 5 строк бандла, перечисленных в review, меняется, и так и задумано.
**Статус:** fixed: requires human verification (это логика парсера, стоит глазами посмотреть на заметки с `==` на устройстве)

### WR-02: картинка заново грузится и декодируется при каждом появлении элемента

**Изменённые файлы:** `shared/src/commonMain/kotlin/com/tlenparty/algolingo/kb/ui/BlockView.kt`
**Коммит:** 45265d8
**Что сделано:** добавлен кэш `imageCache` (имя файла → `ImageBitmap`) на уровне файла. Его переживает удаление элемента из LazyColumn, и `produceState` стартует со значения из кэша, поэтому при прокрутке назад нет повторного декодирования и строка не вырастает заново. `runCatching` заменён на `try/catch`, который пробрасывает `CancellationException`. Пропущено: aspect ratio в индексе. Первая загрузка картинки по-прежнему один раз увеличивает высоту строки.
**Статус:** fixed: requires human verification (поведение прокрутки)

### WR-03: `sync_notes.py` не видит embed'ы в callout/цитатах, но переписывает их в fenced-коде

**Изменённые файлы:** `tools/sync_notes.py`
**Коммит:** c2f0647
**Что сделано:** `EMBED` теперь принимает префикс цитаты `> ` (и вложенный `> >`), а префикс сохраняется при переписывании. Подстановка идёт построчно с флагом `in_fence`, так что внутри fenced-кода ничего не трогается. Если какой-то картинки нет в `Картинки/`, скрипт удаляет tmp и возвращает 1 до подмены бандла. Ведущий отступ у строк без цитаты, как и раньше, отбрасывается. Проверено: старая и новая подстановка дают одинаковый результат на всех 217 `.md` бандла, поэтому повторный sync текущего бандла не изменит. Бандл не пересинхронизировался. Embed'ы в элементах списка (`- ![[x]]`) не поддержаны: Kotlin их тоже не превращает в ImageBlock.
**Статус:** fixed

### WR-04: `searchNotes` берёт индекс в `lowerRaw`, а режет `raw`

**Изменённые файлы:** `shared/src/commonMain/kotlin/com/tlenparty/algolingo/kb/KbRepository.kt`, `shared/src/commonTest/kotlin/com/tlenparty/algolingo/kb/KbSearchTest.kt`
**Коммит:** fea74e6
**Что сделано:** оба конца snippet ограничены длиной `raw` через `coerceIn`. Добавлен тест `lowercaseLengthChangeDoesNotCrash` на `"İ".repeat(40)`. До исправления он падал, после проходит.
**Статус:** fixed

## Проверка

- `./gradlew :shared:allTests :androidApp:assembleDebug`: BUILD SUCCESSFUL
- `python3 tools/gen_content.py --check`: exit 0

---

_Исправлено: 2026-10-22_
_Fixer: Claude (gsd-code-fixer)_
_Итерация: 1_

## Дополнение после проверки на устройстве

WR-01: `trimEnd()` внутри Mark склеивал слова («существующих. ==Новый» → «существующих.Новый»). Убран в fc44b77, пробел остаётся внутри подсветки. Проверено на Android-эмуляторе (заметка «ООП»).
