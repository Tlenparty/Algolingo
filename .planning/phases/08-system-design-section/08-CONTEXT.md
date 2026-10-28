# Phase 8: System Design Section - Context

**Gathered:** 2026-10-28
**Status:** Ready for planning
**Source:** PRD Express Path (docs/superpowers/specs/2026-10-28-system-design-section-design.md, принята заказчиком)

<domain>
## Phase Boundary

Третья вкладка «System Design»: бэкенд system design (как его спрашивают абстрактно на собесах Android-разработчика) тренируется тем же жестом «тап по облачку → дырка». Урок = теория → 2–4 шага (требования / API / решения) → финальная схема из блоков с дырками → эталон. Первый срез — три урока: sd0.1, sd1.1, sd1.2. Остальные 10 уроков программы — следующие фазы (только контент).

</domain>

<decisions>
## Implementation Decisions

### Модель (`lesson/Model.kt`)
- `Lesson.kind: LessonKind = Code`, сериализуется как `"code"` | `"design"`
- `Exercise.title: String? = null` — подпись шага («Шаг 2 · API»), показывается над шаблоном
- `Solution.time` / `Solution.space` получают default `""`; секция «Сложность» в эталоне скрыта, если оба пусты

### Валидация (`validate`) для `design`
- шагов 2..4 (у `code` остаётся 2..3)
- правило «число дырок строго растёт» не применяется к `design` (и в `LessonContentTest.everyCatalogLessonIsStrictAndValid` тоже)
- `segments + ok[0]` **последнего** шага == `solution.code` (финальная схема); у `code` правило прежнее — для каждого шага
- каждая строка собранного текста каждого шага ≤ 34 символов (влезает в экран без горизонтального скролла)
- все прочие правила (банк ≤ 10, `why` у дистракторов, multiset и т.д.) — как у `code`

### UI
- `CodeTemplate` получает параметр `highlight: Boolean = true`; для `design` — без Kotlin-подсветки (и в дырках тоже)
- `ExerciseScreen` показывает `ex.title`, если задан
- `RevealScreen` для `design`: «Эталон» = собранный текст текущего шага (`segments + ok[0]`, подсветка дырок через `solutionSpans`), без Kotlin-подсветки; на последнем шаге — «⚠️ Грабли» и «🔁 Что спросят дальше» (`variations`); «Сложность» скрыта при пустых time/space
- Схема — моноширинный текст (боксы/стрелки символами) внутри существующего `CodeTemplate`; нового рендера графа нет

### Контент и загрузка
- Файлы: `files/content/sd/index.json` (формат `Catalog`) и `files/content/sd/<id>.json`; отдельная папка, чтобы `tools/gen_content.py --check` не считал их сиротами
- `loadLesson(id)`: id с префиксом `sd` читается из `files/content/sd/<id>.json`, остальные — как раньше
- `loadCatalog` параметризуется путём (default — текущий `files/content/index.json`)
- SD-контент пишется руками сразу в JSON, без генератора

### Навигация (`App.kt`)
- Третья вкладка нижней навигации «System Design» → тот же `HomeScreen` (путь, «Повторить») на SD-каталоге
- Прогресс общий (`Progress.lessons` по id; `sd*` не пересекаются с `l*`): XP и streak одни на всё приложение; due-повторы на каждой вкладке — из её каталога (`homeModel` уже фильтрует по id каталога)

### Контент первого среза
- **sd0.1 Как вести интервью**: теория — шаги интервью (требования → оценки → API → данные → общая схема → углубление → узкие места), F vs NF, «не рисуй до вопросов». Ш1 порядок шагов; Ш2 F/NF-разметка требований; Ш3 что спросить первым (DAU, чтение/запись, срок хранения) против ловушек
- **sd1.1 URL shortener**: Ш1 оценка (100M/сут ≈ 1200 QPS записи, чтения ×10 → кеш); Ш2 API (`POST /v1/urls` → `201 Created`; `GET /{code}` → `302`, `301` — acceptable с note про аналитику); Ш3 решения (base62 от счётчика vs хеш, KV/NoSQL, cache-aside); Ш4 схема Client → LB → App → Redis / БД, клики → Kafka → аналитика
- **sd1.2 Rate limiter — защита от спама**: Ш1 где (API Gateway) и ключ (userId / IP); Ш2 алгоритм (token bucket / leaky bucket / fixed window и его граница); Ш3 ответ `429` + `Retry-After`, клиент — backoff + jitter, `Idempotency-Key`; Ш4 схема Client → Gateway → Redis (INCR+EXPIRE в Lua) → App, метрики → Prometheus → Grafana
- У каждого неверного облачка — `why` (почему не подходит). Финальные тексты — на ревью заказчика (checkpoint, как в Phase 6)

### Claude's Discretion
- Точная раскладка вкладки (иконка/подпись), имена параметров, разбиение на планы/волны
- Формулировки теории и `why` до ревью заказчика
- Как именно E2E smoke проходит SD-уроки (расширить существующий `E2eSmokeTest` или добавить сценарий)

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Спека
- `docs/superpowers/specs/2026-10-28-system-design-section-design.md` — принятый дизайн фазы (программа 13 уроков, решения, тесты)

### Код, который меняется
- `shared/src/commonMain/kotlin/com/tlenparty/algolingo/lesson/Model.kt` — `Lesson`/`Exercise`/`Solution`, `validate`
- `shared/src/commonMain/kotlin/com/tlenparty/algolingo/lesson/LessonRepository.kt` — `loadLesson`
- `shared/src/commonMain/kotlin/com/tlenparty/algolingo/progress/Catalog.kt` — `loadCatalog`, `homeModel`
- `shared/src/commonMain/kotlin/com/tlenparty/algolingo/App.kt` — вкладки и маршруты
- `shared/src/commonMain/kotlin/com/tlenparty/algolingo/ui/components/CodeTemplate.kt` — шаблон с дырками
- `shared/src/commonMain/kotlin/com/tlenparty/algolingo/lesson/ui/ExerciseScreen.kt`, `RevealScreen.kt`
- `design/src/commonMain/kotlin/com/tlenparty/algolingo/design/components/CodeBlock.kt` — эталон (подсветка)
- `shared/src/androidHostTest/kotlin/com/tlenparty/algolingo/lesson/LessonContentTest.kt` — проверки контента
- `shared/src/androidDeviceTest/kotlin/com/tlenparty/algolingo/e2e/E2eSmokeTest.kt`, `tools/smoke.sh` — E2E
- `tools/gen_content.py` — должен оставаться зелёным с `--check`

### Образец контента
- `shared/src/commonMain/composeResources/files/content/lessons/l1.1.json` — формат урока
- KB-заметки раздела System Design (`files/kb/index.json`, section «System Design») — источник теории

</canonical_refs>

<specifics>
## Specific Ideas

Пример финальной схемы (каждая строка ≤ 34 символов):

```
 Client
   ↓
[ Load Balancer ]
   ↓
 App ──→ [ Redis ]
   ↓
[ Postgres ]
```

Пример `why`: «Memcached: нет атомарного INCR+TTL — два запроса пройдут лимит одновременно».

</specifics>

<deferred>
## Deferred Ideas

- Уроки sd0.2, sd0.3, sd1.3–sd1.7, sd2.1–sd2.3 — следующие фазы (только контент)
- Генератор SD-контента из markdown-сайдкаров
- Настоящий рендер графа на Canvas (если моноширинная схема окажется некрасивой)
- Свободное рисование графа, отдельный прогресс для SD

</deferred>

---

*Phase: 08-system-design-section*
*Context gathered: 2026-10-28 via PRD Express Path*
