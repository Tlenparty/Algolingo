# Phase 13: SD Classics and Reliability - Context

**Gathered:** 2026-10-28
**Status:** Ready for planning
**Source:** заказчик 2026-10-28: «Давай эти 2 — Классика и Надёжность», «давай сам» (без согласования спек/планов); содержание — программа из спеки SD

<domain>
## Phase Boundary

Только контент: 8 design-уроков на существующем движке (Phase 8). Код приложения не меняется. Формат и правила — как sd1.1/sd1.2/sd0.3–sd0.6.

</domain>

<decisions>
## Implementation Decisions

### Уроки (каждый: API → решения → схема, 3–4 шага; опора на заметки «Основы» и спеку)
- `sd1.3` «Авторизация» — OAuth2 + PKCE для мобилки (без client_secret), короткий access-JWT + refresh с ротацией и reuse detection, где проверять токен (Gateway vs сервис), 401 → один refresh под mutex, logout/отзыв (denylist в Redis), хранение токенов (Keystore/EncryptedSharedPreferences). Схема: App → Gateway (JWT verify) → сервисы; Auth-сервис + БД refresh-токенов + Redis denylist. Заметки 03, 04, 05
- `sd1.4` «Уведомления» — API отправки, очередь (Kafka) на каждый канал (FCM/APNs/SMS/email), workers, retry + DLQ, at-least-once + дедупликация по notificationId, user preferences и quiet hours, rate limit на пользователя, хранение device tokens и их чистка (unregistered). Схема: сервисы → Notification API → Kafka (по каналам) → workers → FCM/APNs/SMS; DLQ. Заметки 07, 10
- `sd1.5` «Лента» — fan-out on write vs on read, гибрид для «звёзд», ленты в Redis (sorted set, top-N id), посты в БД, пагинация курсором (не offset), подписки, медиа через CDN. Схема: Post service → Kafka → Fanout workers → Redis-ленты; чтение: Feed service → Redis → гидрация постов из БД/кеша. Заметки 06, 07, 08, 11
- `sd1.6` «Чат» — WebSocket-шлюзы, маршрутизация между серверами (Redis Pub/Sub / Kafka), хранение сообщений (Cassandra, ключ chatId + seq), sequence id на чат, clientMessageId против дублей, статусы доставлено/прочитано, офлайн: push-сигнал + дочитка после последнего id, outbox на клиенте при потере связи. Заметки 07, 08, 10
- `sd1.7` «Платежи» — Idempotency-Key на POST /payments, PSP (внешний провайдер), статусы и webhook от PSP (тоже идемпотентно), ledger (double-entry, неизменяемые записи), outbox для событий, saga с компенсацией, ежедневная сверка (reconciliation), ACID SQL. Заметки 02, 07, 08
- `sd2.1` «Потеря связи» — таймауты (connect/read), retry только идемпотентного + backoff + jitter, Idempotency-Key для POST, circuit breaker (closed/open/half-open), офлайн-очередь/outbox на клиенте (Room + WorkManager), конфликт при синхронизации (version / LWW), fail-open vs fail-closed. Заметки 01, 02, 10; KB-заметки SD про буфер, WorkManager, конфликты
- `sd2.2` «Несколько БД» — SQL + NoSQL рядом (каждой задаче своё), read replicas и replication lag (read-your-writes), шардирование (ключ, hot shard, consistent hashing), CDC (Debezium) вместо двойной записи в поиск/кеш, кеш-инвалидация. Заметки 06, 08
- `sd2.3` «Наблюдаемость» — логи (структурные, correlation/request id сквозь сервисы и клиент), метрики RED/USE, p99 вместо среднего, Prometheus → Grafana, алерты по SLO/error budget, трейсинг (OpenTelemetry), мобильная сторона (Crashlytics, клиентские метрики). Заметка 12

### Каталог
- `files/content/sd/index.json`: после `sd1.2` добавить `sd1.3, sd1.4, sd1.5, sd1.6, sd1.7, sd2.1, sd2.2, sd2.3` с `caption`; `sd0.2` по-прежнему не создаётся

### Формат (правила валидатора и уроки ревью Phase 8/12)
- 3–4 шага, `title` у каждого шага, строка собранного текста ≤ 34 символов, последний шаг == `solution.code`, банк ≤ 10
- `why` у каждого неверного облачка; полная матрица `wrongWhy` для перестановок правильных облачков между дырками (WR-01); `why` не начинается с имени облачка; допустимые-на-практике ответы — `acceptable` (≈) с note, а не ошибка (урок ревью Phase 12)
- Теория 2–3 карточки (`signals`, `skeleton`, `pitfalls`), ссылки на заметки текстом
- `solution.pitfalls` + `variations` («Что спросят дальше»); `time`/`space` не задаются
- Факты сверять с заметками «Основы» в бандле `files/kb/` — не противоречить

### Ревью и проверка
- Ревью заказчика НЕблокирующее (заказчик 2026-10-28: «давай сам»): исполнитель сам сверяет факты с заметками «Основы», прогоняет validate/тесты и коммитит; дайджест урока кладётся в SUMMARY, заказчик смотрит его в конце и правит точечно
- В дайджесте для перестановочных wrongWhy — только количество на шаг + 2 примера
- Устройства: только Android-эмулятор (fail-closed выбор `emulator-*`, AVD `Medium_Phone_API_36.1`) и iOS-симулятор; физический Pixel 7 (29211FDH200046) не трогать
- smoke.sh зелёный; `gen_content.py --check` и host-тесты зелёные

### Claude's Discretion
- Точные шаги, банки, формулировки (до ревью); разбиение на планы

</decisions>

<canonical_refs>
## Canonical References

- `docs/superpowers/specs/2026-10-28-system-design-section-design.md` — программа и формат
- `.planning/phases/12-client-server-basics-lessons/12-01-PLAN.md`, `12-02-PLAN.md`, `12-01-SUMMARY.md` — проверенная форма плана (контент + ревью + E2E, fail-closed эмулятор)
- `shared/src/commonMain/composeResources/files/content/sd/sd1.2.json`, `sd0.5.json` — образцы
- `shared/src/commonMain/kotlin/com/tlenparty/algolingo/lesson/Model.kt` — `validate`
- `shared/src/commonMain/composeResources/files/kb/` — заметки «Основы» 01–12 (заголовки в `index.json`, раздел System Design) и прежние SD-заметки
- `tools/smoke.sh`

</canonical_refs>

<deferred>
## Deferred Ideas

- `sd0.2` «Оценки на салфетке»

</deferred>

---

*Phase: 13-sd-classics-and-reliability*
*Context gathered: 2026-10-28*
