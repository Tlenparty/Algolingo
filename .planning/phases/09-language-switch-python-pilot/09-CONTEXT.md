# Phase 9: Language Switch + Python Pilot - Context

**Gathered:** 2026-10-28
**Status:** Ready for planning
**Source:** PRD Express Path (docs/superpowers/specs/2026-10-28-python-language-design.md, принят заказчиком)

<domain>
## Phase Boundary

Python — второй язык решений для аудитории стора. На Home появляется сегмент «Kotlin | Python». В режиме Python уроки с кодом грузятся из отдельных Python-файлов: свои шаблоны, дистракторы и грабли. Прогресс общий для обоих языков. В фазе делается вся инфраструктура (модель, загрузка, каталог, UI, подсветка, генератор) и один пилотный урок `l1.4` Two Sum на Python. Остальные 8 уроков переводятся в фазе 10.

Выбор языка делается в рантайме, а не build flavour: одно приложение в сторе, переключение одним тапом, а у `com.android.kotlin.multiplatform.library` всё равно нет вариантов сборки.

</domain>

<decisions>
## Implementation Decisions

### Единые точки ветвления по языку (LOCKED)
Язык — это данные, а не ветки по коду. `when (lang)` / `if (lang == …)` допускается **только** в трёх местах:
1. `lessonPath(id, lang)`: какой файл урока читать
2. `Highlight.kt`: `when (lang)` выбирает набор правил (ключевые слова + синтаксис комментария); сам цикл подсветки один
3. `homeModel` / `dueIds`: `lang.id in entry.langs`, без ветки на каждый язык

Движок дырок, редьюсер, проверка ответа, экраны урока о языке не знают. UI получает язык через один `CompositionLocal` `LocalCodeLang` (в `design`), который урок задаёт один раз в `LessonHost`/`LessonScreen`. `CodeBlock`/`TokenChip`/`CodeTemplate` читают его сами, параметр через всю иерархию не тянется. Третий язык добавляется так: значение enum, правила подсветки и папка с контентом.

### Язык (`design`)
- `enum class CodeLang { Kotlin, Python }` живёт в модуле `design`: его использует подсветка, а `design` не зависит от `shared`
- сериализуется как `"kotlin"` / `"python"` (`@SerialName`); `design` пока без serialization-плагина, поэтому можно хранить в Progress строкой и маппить, выбор за исполнителем, главное — формат JSON `"kotlin"`/`"python"`

### Прогресс (`progress/Progress.kt`, `ProgressRepository.kt`)
- `Progress.lang` по умолчанию Kotlin. `PROGRESS_SCHEMA` остаётся 1: старый блоб без `lang` декодируется в Kotlin
- неизвестное значение `lang` (например `"java"`) даёт Kotlin, а весь прогресс не сбрасывается. `decodeProgress` не должен возвращать null из-за `lang`
- `ProgressRepository.setLang(lang: CodeLang)` вызывает `store.updateData { it.copy(lang = lang) }` в том же app-scope, с тем же retry и `saveFailed`, что и `record`
- прогресс по урокам общий: ключ `lessons[id]` один для обоих языков, XP/streak/повторы не делятся

### Загрузка (`lesson/LessonRepository.kt`)
- `lessonPath(id, lang)`: для `sd*` → `files/content/sd/$id.json` (язык игнорируется); для Kotlin → `files/content/lessons/$id.json`; для Python → `files/content/lessons/py/$id.json`
- `loadLesson(id, lang)`; пути фиксированные, свободного пути из маршрута нет
- язык читается при старте урока (в момент создания LessonViewModel); открытый урок при смене языка не перезагружается

### Каталог (`progress/Catalog.kt`, `index.json`)
- `CatalogEntry.langs: List<String> = listOf("kotlin")`; для `l1.4` в `index.json` → `["kotlin","python"]`
- `homeModel(catalog, p, today)` учитывает `p.lang`: урок, у которого нет выбранного языка, получает новый статус (например `LessonStatus.Soon`). Он не открывается и показан с подписью «Скоро на Python»
- разблокировка по-прежнему по общему прогрессу (`ids[i-1] in p.lessons`)
- `dueIds` пропускает уроки без выбранного языка; срок `due` не трогается
- sd-каталог (`sd/index.json`) не меняется: у его записей `langs` по умолчанию, а фильтр по языку на вкладке SD не применяется

### UI
- сегмент «Kotlin | Python» на Home, над путём уроков, только на вкладке Home (не на SdHome)
- без хардкода dp/sp/Color: только `AlgoTheme.*`; если нужен переиспользуемый компонент, он идёт в `design/components` (без доменных типов урока)
- узел «Скоро на Python»: визуально как Locked, но со своей подписью; тап ничего не делает

### Подсветка (`design/Highlight.kt`, `CodeBlock`, `TokenChip`, `CodeTemplate`)
- сигнатура `highlight(code, colors, lang: CodeLang = CodeLang.Kotlin)`; компоненты берут `lang` из `LocalCodeLang.current` (по умолчанию Kotlin, так что KB и SD не меняются)
- ключевые слова Python: `def return in not and or None True False for while if elif else lambda class import from pass break continue is yield with as`
- комментарий Python — `#` до конца строки; Kotlin-комментарий `//` для Python не действует (`//` в Python — целочисленное деление); числа и строки общие
- урок задаёт `LocalCodeLang provides <язык, с которым загружен>` один раз; теория-скелетон, шаблон, банк и эталон подсвечиваются через него
- лигатуры отключены, как сейчас

### Генератор (`tools/gen_content.py`, `tools/test_gen_content.py`)
- sidecar `content/authoring/py/<id>.md`, ключ `lang: python`, fence ```` ```python ```` для кода упражнений, скелетона и `checks:`
- выход: `shared/src/commonMain/composeResources/files/content/lessons/py/<id>.json`; генератор дописывает `"python"` в `langs` соответствующей записи `index.json`
- проверка Python-урока: эталон и каждый вариант из `ok`/`acceptable` проходят `examples` + `checks`; каждый `[wrong]`-чип хотя бы в одной дырке, где он может стоять, их проваливает. Иначе это ошибка генератора с понятным кодом (по аналогии с G10)
- запуск через `python3 -I` из временного каталога, сгенерированные модули не кладутся в репо
- разбор дырок `⟦…⟧`, банка, `why`, тегов, `validate`-эквивалент — общий с Kotlin
- Kotlin-путь генератора не меняется: повторная генерация Kotlin-уроков даёт байт-в-байт те же JSON

### Пилотный контент
- `content/authoring/py/l1.4.md`: Two Sum на Python (`def two_sum(nums: list[int], target: int) -> tuple[int, int] | None`, `seen = {}`, `for i, x in enumerate(nums)`), 2–3 шага с растущим числом дырок, дистракторы с причинами, грабли Python (например, искать в `seen` до вставки; `seen[x] = i` перед проверкой → `[3,2,4], 6` даёт `(0, 0)`; `nums.index(...)` → O(n²))
- теория и формулировки на русском, код на Python
- урок ревьюит заказчик до закрытия фазы (как уроки фазы 6)

### Документы
- уже сделано: `PROJECT.md` (Python вынесен из Out of Scope), `ROADMAP.md` (фазы 9–10)

### Claude's Discretion
- имя и форма нового статуса узла (`Soon` или иное), раскладка сегмента
- хранение `CodeLang` в Progress: enum с serializer или строка с маппингом
- как именно Python-харнесс собирает модуль (exec из строки, файл во временном каталоге)
- разбиение на планы и волны

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Дизайн
- `docs/superpowers/specs/2026-10-28-python-language-design.md` — утверждённый спек фичи

### Код, который меняется
- `shared/src/commonMain/kotlin/com/tlenparty/algolingo/progress/Progress.kt` — модель прогресса, decode/migrate
- `shared/src/commonMain/kotlin/com/tlenparty/algolingo/progress/ProgressRepository.kt` — DataStore, `record` (паттерн для `setLang`)
- `shared/src/commonMain/kotlin/com/tlenparty/algolingo/progress/Catalog.kt` — каталог, статусы, `dueIds`, `homeModel`
- `shared/src/commonMain/kotlin/com/tlenparty/algolingo/lesson/LessonRepository.kt` — `lessonPath`, `loadLesson`
- `shared/src/commonMain/kotlin/com/tlenparty/algolingo/App.kt`, `home/HomeScreen.kt` — Home и навигация
- `design/src/commonMain/kotlin/com/tlenparty/algolingo/design/Highlight.kt`, `design/components/CodeBlock.kt`, `design/components/TokenChip.kt`, `shared/.../ui/components/CodeTemplate.kt`
- `tools/gen_content.py`, `tools/test_gen_content.py` — генератор и его тесты
- `content/authoring/l1.4.md` — Kotlin-исходник пилотного урока (образец формата)

### Тесты-образцы
- `shared/src/commonTest/kotlin/com/tlenparty/algolingo/progress/{ProgressTest,CatalogTest,ProgressRepositoryTest}.kt`
- `shared/src/commonTest/kotlin/com/tlenparty/algolingo/lesson/{LessonRepositoryTest,ReferenceSolutionsTest}.kt`
- `shared/src/commonTest/kotlin/com/tlenparty/algolingo/e2e/E2eScenarios.kt`

</canonical_refs>

<specifics>
## Specific Ideas

- тесты `commonTest`: каждый `lessons/py/*.json` декодируется и проходит `validate()`; `lessonPath` для Kotlin/Python/sd; блоб без `lang` → Kotlin; `"lang":"java"` → Kotlin при сохранённом прогрессе; round-trip `lang`; `homeModel`/`dueIds` при Python скрывают уроки без python
- `test_gen_content.py`: неверный эталон и `[wrong]`-чип, не роняющий проверки, дают ошибку генератора
- прогон на устройствах (Android-эмулятор + iOS-симулятор, сам через adb/idb): переключить на Python → пройти l1.4 → перезапуск → язык Python сохранён, урок засчитан и на Kotlin-пути; прочие уроки показаны «Скоро на Python»
- коммиты от `tlenparty`, без трейлеров `Co-Authored-By` / атрибуции Claude

</specifics>

<deferred>
## Deferred Ideas

- остальные 8 уроков на Python — фаза 10
- Python в базе знаний и на вкладке System Design — не планируется
- Java, онбординг выбора языка, раздельный прогресс по языкам — вне скоупа

</deferred>

---

*Phase: 09-language-switch-python-pilot*
*Context gathered: 2026-10-28 via PRD Express Path*
