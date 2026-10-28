# Phase 12: Client-Server Basics Lessons - Context

**Gathered:** 2026-10-28
**Status:** Ready for planning
**Source:** разговор с заказчиком 2026-10-28 («да, начинай»; состав уроков — на усмотрение Claude, правки на ревью)

<domain>
## Phase Boundary

Только контент: 4 новых SD-урока (`kind: design`) закрепляют заметки Phase 11 «Основы клиент-сервера». Движок, валидатор, UI и E2E-сценарий SD уже есть (Phase 8) — код приложения не меняется, кроме, возможно, тестовых ожиданий, если они завязаны на число SD-уроков.

</domain>

<decisions>
## Implementation Decisions

### Уроки (id, тема, опора на заметки)
- `sd0.3` «Строительные блоки» — компонент → зачем: LB/nginx, API Gateway, Redis, Kafka, SQL/NoSQL, CDN, S3, Docker/K8s, Prometheus/Grafana (заметки 05–09, 11, 12). Шаги: сопоставить проблему и компонент; финальная схема «типового бэкенда» из блоков
- `sd0.4` «Путь запроса» — DNS → TCP → TLS → HTTP → LB → App → кеш/БД → ответ; что ломается на каком шаге (таймаут, 5xx, 401/403, 429); HTTP-методы/идемпотентность (заметки 01, 02)
- `sd0.5` «HTTPS и TLS» — что даёт TLS (шифрование, целостность, подлинность), асимметрия только для обмена ключом, порядок рукопожатия TLS 1.3, сертификат и CA, что видно наблюдателю (IP, SNI), certificate pinning и Network Security Config (заметка 03; авторизация из заметки 04 — один шаг: access/refresh, 401 → refresh под mutex)
- `sd0.6` «Real-time» — polling / long polling / SSE / WebSocket / push (FCM/APNs): что выбрать для чата, ленты, уведомлений, статуса заказа; переподключение с backoff + jitter, Last-Event-ID (заметка 10)

### Каталог
- `files/content/sd/index.json`: порядок `sd0.1, sd0.3, sd0.4, sd0.5, sd0.6, sd1.1, sd1.2`; `sd0.2` («Оценки на салфетке») зарезервирован, не создаётся; у каждой записи `caption`

### Формат (как sd0.1/sd1.1/sd1.2, правила валидатора Phase 8)
- 3–4 шага, каждая строка собранного текста ≤ 34 символов, последний шаг == `solution.code`, банк ≤ 10, `title` у каждого шага
- У каждого неверного облачка `why`; `wrongWhy` для вероятных перестановок правильных облачков между дырками (урок из code review Phase 8, WR-01); `why` не начинается с имени облачка
- Теория: 2–3 карточки (`signals`, `skeleton` — мини-схема, `pitfalls`); в карточке можно сослаться на заметку текстом («подробнее — заметка 03 HTTPS, TLS, SSL»)
- `solution.pitfalls` + `variations` («Что спросят дальше»); `time`/`space` не задаются
- Факты сверять с заметками Phase 11 (бандл `files/kb/`) — уроки не должны им противоречить

### Ревью и проверка
- Ревью заказчика — блокирующий checkpoint с дайджестом (как 08-02), до финального коммита
- Устройства: **только iOS-симулятор и Android-эмулятор**; физический Pixel 7 занят заказчиком — не использовать (никаких adb-команд к нему; `ANDROID_SERIAL=emulator-…`). Исполнитель сам поднимает эмулятор/симулятор, не просит заказчика
- `tools/smoke.sh` (E2E SD-сценарий проходит все уроки SD-каталога) зелёный на эмуляторе + симуляторе; `gen_content.py --check` и host-тесты зелёные

### Claude's Discretion
- Точные шаги, банки, формулировки (до ревью), число шагов 3 или 4, разбиение на планы

</decisions>

<canonical_refs>
## Canonical References

- `docs/superpowers/specs/2026-10-28-system-design-section-design.md` — формат SD-урока
- `.planning/phases/08-system-design-section/08-CONTEXT.md`, `08-REVIEW.md` (WR-01) — правила и уроки ревью
- `shared/src/commonMain/composeResources/files/content/sd/sd1.2.json` — образец урока
- `shared/src/commonMain/kotlin/com/tlenparty/algolingo/lesson/Model.kt` — `validate`
- `shared/src/androidHostTest/kotlin/com/tlenparty/algolingo/lesson/LessonContentTest.kt`
- `shared/src/commonTest/kotlin/com/tlenparty/algolingo/e2e/E2eScenarios.kt`, `tools/smoke.sh`
- Заметки Phase 11 в бандле: `shared/src/commonMain/composeResources/files/kb/` (раздел System Design в `index.json`, заголовки `01 …`–`12 …`)

</canonical_refs>

<deferred>
## Deferred Ideas

- `sd0.2` «Оценки на салфетке» и уроки sd1.3–sd2.3 из программы — следующие фазы

</deferred>

---

*Phase: 12-client-server-basics-lessons*
*Context gathered: 2026-10-28*
