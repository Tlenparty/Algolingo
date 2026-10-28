# Phase 11: Client-Server Basics Notes - Context

**Gathered:** 2026-10-28
**Status:** Ready for planning
**Source:** разговор с заказчиком 2026-10-28 (выбрано «Заметки + уроки», хранилище «найди сам»)

<domain>
## Phase Boundary

Заказчик — Android-разработчик. Цель: понять бэкенд-кирпичи и клиент-серверное взаимодействие на уровне «что это, зачем, как применить на SD-собесе». Только контент: 12 заметок в Obsidian-хранилище → `tools/sync_notes.py` → «База знаний» приложения. Код приложения не меняется. Уроки на закрепление — Phase 12.

</domain>

<decisions>
## Implementation Decisions

### Где и как
- Хранилище (корень для sync_notes): `~/Yandex.Disk.localized/Матрицы`. Заметки пишутся в `SystemDesign/Основы/` (подпапка наследует раздел «System Design» по FOLDERS в sync_notes.py). Путь к хранилищу НЕ попадает в репозиторий (privacy-правило sync_notes: только argv/env)
- Имена: `01 Путь запроса.md` … `12 Наблюдаемость.md` — номер задаёт порядок
- `СОДЕРЖАНИЕ.md` (корень хранилища): в раздел System Design добавить подраздел «Основы клиент-сервера» со ссылками `[[…]]` на все 12
- После записи: `python3 tools/sync_notes.py ~/Yandex.Disk.localized/Матрицы`, KB-тесты зелёные, коммит бандла (`files/kb/`)
- Существующие 11 SD-заметок не переписывать

### Список заметок
1. Путь запроса: DNS → TCP → TLS → HTTP → LB → App → кеш/БД → ответ (сквозная картина, что где ломается)
2. HTTP: методы и идемпотентность, коды, заголовки, кеш-заголовки (ссылка на «Кеширование через заголовки»), REST, HTTP/1.1 vs 2 vs 3
3. HTTPS, TLS, SSL: SSL vs TLS, симметричное/асимметричное шифрование, рукопожатие TLS 1.3, сертификаты и CA, что защищено/что видно (SNI, IP), certificate pinning и Network Security Config на Android
4. Авторизация: сессии vs JWT, access/refresh, ротация, OAuth2 + PKCE, где хранить токены (EncryptedSharedPreferences/Keystore), refresh под mutex в OkHttp Authenticator
5. nginx, reverse proxy, load balancer, API Gateway: TLS-терминация, балансировка (round robin, least conn), rate limit (ссылка на урок sd1.2)
6. Redis: in-memory KV, структуры, TTL, кеш (cache-aside / write-through / write-behind), счётчики, pub/sub, когда не Redis
7. Kafka и очереди: зачем асинхронность, topic/partition/offset/consumer group, at-most/at-least/exactly-once, Kafka vs RabbitMQ, outbox, DLQ
8. Базы данных: SQL vs NoSQL (документ, KV, колоночные, граф), индексы, транзакции и ACID, репликация (лаг), шардинг, CAP на пальцах
9. Docker и Kubernetes: контейнер vs VM, образ/контейнер/registry, docker-compose (локально поднять Postgres+Redis), зачем k8s (поды, автоскейл, rolling update)
10. Real-time: polling, long polling, SSE, WebSocket, push (FCM/APNs), что выбрать для чата/ленты/уведомлений, переподключение при потере связи
11. Файлы: CDN, объектное хранилище S3, presigned URL, загрузка по частям/докачка, превью/ресайз
12. Наблюдаемость: логи (структурные, correlation id), метрики (Prometheus → Grafana, RED/USE, p99), трейсинг (OpenTelemetry), алерты; мобильная сторона (Crashlytics)

### Формат каждой заметки (единый)
- Вступление в 1–2 строки: что это и зачем
- `> [!tip] Аналогия из Android` — например: Redis ≈ общий для всех серверов `LruCache`; Kafka ≈ `SharedFlow` с replay, который хранится на диске; nginx ≈ OkHttp-interceptor на стороне сервера; Docker ≈ «APK для сервера»
- Как работает — коротко, схемы в ```text-блоках (ASCII, ≤ ~40 символов в ширину — читается на телефоне)
- Когда применять в SD-задаче (сигналы) и как сказать на собесе одной фразой
- `> [!warning] Грабли`
- `> [!question]- Вопрос с собеса` — несколько штук, свёрнутые, ответ внутри
- Ссылки `[[…]]` на соседние «Основы», существующие SD-заметки и тему урока (sd1.1/sd1.2 — текстом, уроки не wiki-ссылки)
- Объём: читается за 5–7 минут; русский язык, термины на английском как есть
- Без картинок (нет image-пайплайна для новых); только markdown, который уже рендерит KB (заголовки, списки, таблицы, код, callouts)

### Claude's Discretion
- Точные формулировки, порядок разделов внутри заметки, набор вопросов с собеса
- Разбиение на планы (например 2 плана по 6 заметок + sync/проверка)

</decisions>

<canonical_refs>
## Canonical References

- `tools/sync_notes.py` — FOLDERS, privacy-правила, формат `files/kb/index.json`
- `shared/src/commonMain/kotlin/com/tlenparty/algolingo/kb/MarkdownParser.kt` — что рендерится (callouts, спойлеры, ссылки)
- `shared/src/androidHostTest/kotlin/com/tlenparty/algolingo/kb/KbContentTest.kt` — проверки бандла KB
- `~/Yandex.Disk.localized/Матрицы/SystemDesign/*.md` — стиль существующих SD-заметок
- `docs/superpowers/specs/2026-10-28-system-design-section-design.md` — программа SD (sd2.x темы пересекаются с заметками 7, 8, 12)

</canonical_refs>

<deferred>
## Deferred Ideas

- SD-уроки на закрепление — Phase 12
- Картинки/диаграммы в заметках

</deferred>

---

*Phase: 11-client-server-basics-notes*
*Context gathered: 2026-10-28*
