---
phase: 07-full-interview-knowledge-base
reviewed: 2026-10-22T00:00:00Z
depth: standard
files_reviewed: 9
files_reviewed_list:
  - shared/src/commonMain/kotlin/com/tlenparty/algolingo/kb/MarkdownParser.kt
  - shared/src/commonMain/kotlin/com/tlenparty/algolingo/kb/ui/BlockView.kt
  - shared/src/commonMain/kotlin/com/tlenparty/algolingo/kb/KbModels.kt
  - shared/src/commonMain/kotlin/com/tlenparty/algolingo/kb/KbRepository.kt
  - shared/src/commonMain/kotlin/com/tlenparty/algolingo/kb/ui/KbListScreen.kt
  - shared/src/commonTest/kotlin/com/tlenparty/algolingo/kb/MarkdownParserTest.kt
  - shared/src/commonTest/kotlin/com/tlenparty/algolingo/kb/KbSearchTest.kt
  - shared/src/androidHostTest/kotlin/com/tlenparty/algolingo/kb/KbContentTest.kt
  - tools/sync_notes.py
findings:
  critical: 0
  warning: 4
  info: 5
  total: 9
status: issues_found
---

# Фаза 07: отчёт code review

**Проверено:** 2026-10-22
**Глубина:** standard
**Файлов:** 9
**Статус:** issues_found

## Сводка

Проверены изменения фазы 07 относительно `561783a`: `==mark==` и `![[img]]` в парсере, рендер картинок, сворачиваемые секции в списке, кэш lowercase для поиска, новый `sync_notes.py` (папки, картинки, sips). Блокеров нет. Главные проблемы:

- правило закрытия `==` не подходит к реальному контенту: в 5 строках бандла маркеры `==` видны как текст;
- картинки заново загружаются при каждом появлении в LazyColumn, и высота строки скачет;
- `sync_notes.py` не видит embed'ы внутри callout/цитат/списков, но переписывает их внутри fenced-кода;
- временная папка `kb.tmp` лежит внутри `composeResources/files`.

Я прогнал Python-порт логики `Mark` по всем `.md` бандла (без fenced-кода). Он показал 6 строк с незакрытым `==`: 5 настоящих и 1 артефакт ``` ``==`` ```.

## Narrative Findings (AI reviewer)

## Warnings

### WR-01: `==mark==` не закрывается, если перед закрывающим `==` стоит пробел, и тогда склеивается с более дальним `==`

**Файл:** `shared/src/commonMain/kotlin/com/tlenparty/algolingo/kb/MarkdownParser.kt:149-155`
**Проблема:** цикл `while (end > 0 && s[end - 1].isWhitespace()) end = s.indexOf("==", end + 1)` пропускает закрывающие `==`, перед которыми стоит пробел. В реальных заметках автор часто так пишет, а Obsidian подсвечивает такие фрагменты:
- `n03f60391.md:56`: `==создавать новые классы на основе существующих. ==Новый класс`
- `n369d38c3.md:28`: `==исключением Nothing и Null ==(который`
- `nadac1052.md:35`: `==Если обернуть recycler ... айтемы ==`
- `n59b21abd.md:72`: `==**diff/merge** ==(например`
- `n6ab02168.md:37`

В однострочных случаях пользователь видит сырые `==`. Хуже, когда абзац многострочный или в нём есть ещё `==`. Пропущенный закрывающий маркер не отбрасывается, поиск идёт дальше, и Mark захватывает текст до чужого `==` (`==a ==b ... ==c==` превращается в `Mark("a ==b ... ==c")`). Подсветка уезжает на соседние фрагменты. Есть и смежная проблема: поиск закрывающего `==` не пропускает inline-код, поэтому `` ==a `x==y` b== `` закроется внутри бэктиков.
**Исправление:** проверять «не пробел» только у открывающего маркера (так уже сделано, и `a == b` не откроется). Закрывать первым `==`, перед которым не стоит `=`:
```kotlin
end = s.indexOf("==", i + 3)
while (end > 0 && s.startsWith("===", end)) end = s.indexOf("==", end + 1)
if (end > i + 2) { flush(); out.add(Mark(s.substring(i + 2, end).trimEnd())); i = end + 2; continue }
```
Тест `"сумма == target"` продолжит проходить: в нём `==` не открывается, потому что за ним пробел. Нужно добавить тест на `"==a ==b"` → `[Mark("a "), Text("b")]` или похожий и прогнать `KbContentTest`.

### WR-02: картинка заново грузится и декодируется при каждом появлении элемента, и высота строки скачет

**Файл:** `shared/src/commonMain/kotlin/com/tlenparty/algolingo/kb/ui/BlockView.kt:157-166`
**Проблема:** `produceState` хранит bitmap только пока элемент находится в composition. Когда LazyColumn выбрасывает элемент за пределами экрана, bitmap теряется. При возврате к нему снова показывается плейсхолдер высотой в одну строку текста, файл заново читается и декодируется (`decodeToImageBitmap` выполняется на Main), потом строка вырастает до сотен dp. При прокрутке вверх через заметку с картинками контент над viewport меняет высоту, и экран «прыгает». Это не только неточный якорь, о котором предупреждает комментарий `ponytail:`. Кроме того, `runCatching` вокруг suspend-вызова глотает `CancellationException`.
**Исправление:** минимальный вариант: кэшировать байты или bitmap на уровне `NoteScreen`/репозитория (`remember { mutableMapOf<String, ImageBitmap>() }`, вынесенный выше LazyColumn), а плейсхолдеру задать фиксированную высоту. Полный вариант: писать `w`/`h` в имя или индекс в `sync_notes.py` и ставить `Modifier.aspectRatio(w / h)` до загрузки. Вместо `runCatching` использовать `try { ... } catch (e: Exception) { if (e is CancellationException) throw e; null }`.

### WR-03: `sync_notes.py` не переписывает embed'ы в callout/цитатах/списках, но переписывает их в fenced-коде

**Файл:** `tools/sync_notes.py:47` (`EMBED`), `:122-131`
**Проблема:** `EMBED` требует, чтобы строка начиналась с `^[ \t]*![[`. Строки `> ![[Pasted image.png]]` (callout/цитата, а Obsidian туда часто вставляет картинки) и `- ![[x.png]]` под регекс не попадают. Такая картинка не копируется, не переименовывается и не появляется в `missing images`. При этом `parseBlocks` на Kotlin снимает `> ` и всё равно создаёт `ImageBlock("Pasted image.png")`, то есть ссылку на несуществующий файл. Её поймает только `imagesBundled`, и уже после того как бандл подменён. Обратная ситуация: регекс не знает про ```` ``` ````, поэтому `![[x.png]]` внутри блока кода переписывается и копируется. На Kotlin это остаётся `Code`, и тест `orphan images` падает. Сейчас в бандле таких случаев нет (я проверил grep'ом), но при следующем sync они появятся.
**Исправление:** в регексе разрешить префикс цитаты и пропускать fenced-блоки:
```python
EMBED = re.compile(r"(?m)^((?:[ \t]*>)*[ \t]*)!\[\[([^\]|]+\.(?:png|jpe?g))(?:\|[^\]]*)?\]\][ \t]*$", re.I)
# в embed(): префикс m.group(1) сохранять, имя брать из m.group(2)
```
Подстановку делать построчно с флагом `in_fence` (переключать на `line.lstrip().startswith("```")`). Если после прохода `missing_imgs` не пуст, возвращать 1 **до** подмены бандла.

### WR-04: `searchNotes` берёт индекс в `lowerRaw`, а режет `raw`, хотя длины строк могут не совпадать

**Файл:** `shared/src/commonMain/kotlin/com/tlenparty/algolingo/kb/KbRepository.kt:34-36`
**Проблема:** `lowercase()` может удлинить строку. Например, `"İ".lowercase()` = `"i̇"` (2 char). В этом случае индекс `i` в `lowerRaw` сдвигается относительно `raw`, snippet получается обрезанным не там, а если `i - 30 > raw.length`, то `substring(start > end)` бросает `StringIndexOutOfBoundsException` и роняет поиск на каждое нажатие клавиши. Баг был и до фазы, но фаза закрепила его в кэше `lowerRaw`. Вероятность мала (контент русский), а последствие — краш.
**Исправление:** ограничить оба конца длиной `raw`:
```kotlin
val start = (i - 30).coerceIn(0, n.raw.length)
val end = (i + q.length + 30).coerceIn(start, n.raw.length)
n.raw.substring(start, end)
```

## Info

### IN-01: регистр расширения картинки различается в Python и Kotlin

**Файл:** `shared/src/commonMain/kotlin/com/tlenparty/algolingo/kb/MarkdownParser.kt:35`, `tools/sync_notes.py:47`
**Проблема:** в Python стоит `re.IGNORECASE`, в Kotlin `imageRe` чувствителен к регистру. Переписанные имена всегда в нижнем регистре, так что сейчас всё работает. Но ненайденный `![[Shot.PNG]]` остаётся как есть и в приложении показывается как `!Shot.PNG`, а не как плейсхолдер.
**Исправление:** `Regex(..., RegexOption.IGNORE_CASE)`.

### IN-02: нет защиты от коллизии 8-символьного хеша у картинок

**Файл:** `tools/sync_notes.py:126-128`
**Проблема:** для заметок коллизия `nid` проверяется, для картинок нет. Если хеш совпадёт, то `if not exists` молча покажет чужую картинку.
**Исправление:** хранить `dict f -> name` и падать при совпадении, как это сделано для `seen_ids`.

### IN-03: `sync_notes.py` требует macOS (`sips`), и это нигде не сказано

**Файл:** `tools/sync_notes.py:61-66`
**Проблема:** на Linux/CI будет `FileNotFoundError` без пояснения. Остаётся `kb.tmp` (см. IN-04).
**Исправление:** добавить строку про требование в docstring и проверку `shutil.which("sips")` в начале `main()`.

### IN-04: временная папка `kb.tmp` находится внутри `composeResources/files`

**Файл:** `tools/sync_notes.py:118-121`
**Проблема:** если `copy_image`/`sips` упадёт посреди работы, `files/kb.tmp/` с частью картинок останется в ресурсах и попадёт в APK/IPA при следующей сборке. С появлением внешнего процесса `sips` вероятность этого выросла.
**Исправление:** создавать tmp во временной папке на том же томе вне `composeResources` (например, `out.parent.parent.parent / "build-kb.tmp"`) или обернуть сборку в `try/except: shutil.rmtree(tmp); raise`.

### IN-05: двойные бэктики ``` ``==`` ``` теперь открывают Mark

**Файл:** `shared/src/commonMain/kotlin/com/tlenparty/algolingo/kb/MarkdownParser.kt:145-149`
**Проблема:** ``` ``==`` ``` (`ndb793edb.md:102`) разбирается как пустой `InlineCode("")`, после чего `==``` пытается открыть Mark. Двойные бэктики не поддерживались и раньше, но теперь вместе с WR-01 это может подсветить остаток абзаца.
**Исправление:** в ветке `` ` `` считать длину серии бэктиков и искать такую же закрывающую серию, либо поправить контент.

---

_Проверено: 2026-10-22_
_Ревьюер: Claude (gsd-code-reviewer)_
_Глубина: standard_
