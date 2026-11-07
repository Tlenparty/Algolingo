# Трек «Backend на Kotlin: 0 → middle» — дизайн

Дата: 2026-11-07 · Статус: решения заказчика приняты, спек готов к разбивке на фазы

## Цель

Третий учебный трек в Алголинго: путь бэкенд-разработчика на Kotlin + Spring Boot от нуля (Kotlin знаком,
Spring и бэкенд — нет) до уверенного middle: сам поднимает сервис, БД, очередь, кеш, тесты, Docker/CI и
отвечает на собесе по надёжности, безопасности и архитектуре. Та же механика, что и везде: теория-карточки →
упражнения «тап по облачку → дырка» → «Проверить» → эталон с граблями → XP, streak, повторы 1/3/7. Без
клавиатуры, офлайн.

**Успех первого среза:** на Android и iOS-симуляторе есть вкладка «Backend» с тремя уроками разных типов
(Spring-код без компиляции, SQL с новой подсветкой, компилируемый Kotlin через генератор); каждый проходится
целиком; прогресс и повторы работают как у алгоритмов.

## Решения заказчика (2026-11-07)

1. Kotlin знаком: уровня «основы языка» в треке нет. Языковые тонкости (null-safety, sealed, Result)
   объясняются по месту внутри уроков.
2. Фреймворк — **Spring Boot**, и только он. Spring заказчику незнаком, поэтому «Spring Boot: основы»
   (стартеры, DI, бины, конфигурация) — отдельный уровень до web-слоя. Переключатель Spring | Ktor не делаем.
3. Трек — **четвёртая вкладка «Backend»** в нижней навигации, рядом с «System Design».
4. Код Spring, SQL, YAML **не компилируется**: ручной JSON с жёсткими правилами и ревью заказчика, как у SD.
   Чистый Kotlin (алгоритмы бэкенда) идёт через существующий генератор с компиляцией JVM + Native.
5. Теория пишется **сначала в Obsidian** (новый раздел «Backend» в vault), синхронизируется в базу знаний,
   урок ссылается на заметку. Как в фазах 11 → 12.
6. Планка — **middle**. Senior-темы (saga, консенсус, тюнинг GC) вне рамок.

## Программа (11 уровней, 59 уроков; срез отмечен ★)

Нумерация `bk<уровень>.<номер>`. Тип урока: **V** verified (генератор, компилируется и доказывается),
**R** reviewed (ручной JSON с подсветкой кода, как SD), **D** design (схема и шаги, как SD).

**BK1 · HTTP и API**
1. `bk1.1` Путь запроса от телефона до сервера: DNS, TLS, LB, App, БД (D, пара к sd0.4)
2. `bk1.2` Методы и статусы: GET/POST/PUT/PATCH/DELETE, 2xx/4xx/5xx, идемпотентность методов (R, текст запроса с дырками, подсветка http)
3. `bk1.3` REST-дизайн ресурсов: имена, вложенность, фильтры, пагинация, версионирование (R)
4. `bk1.4` JSON в Kotlin: `@Serializable`, `@SerialName`, nullable-поля, `ignoreUnknownKeys`; Jackson в Spring (V + R)

**BK2 · Spring Boot: основы** (фреймворк с нуля)
1. `bk2.1` Что такое Spring Boot: стартеры, автоконфигурация, `@SpringBootApplication`, `main` (R)
2. `bk2.2` DI и бины: `@Component/@Service/@Repository`, конструкторная инъекция, почему не `@Autowired` на поле (R)
3. `bk2.3` `@Configuration` и `@Bean`: свои бины, `@Qualifier`, `@Primary`, циклические зависимости (R)
4. `bk2.4` Жизненный цикл бина: singleton/prototype, `@PostConstruct`, ленивая инициализация (R)
5. `bk2.5` Конфигурация и профили: `application.yaml`, `@ConfigurationProperties`, env, секреты не в репо (R, подсветка yaml)
6. `bk2.6` Логирование: SLF4J/Logback, уровни, MDC и correlation id (R)

**BK3 · Web-слой**
1. ★ `bk3.1` `@RestController` и роутинг: `@GetMapping`, `@PathVariable`, `@RequestParam`, `@RequestBody`, `ResponseEntity` (R)
2. `bk3.2` Слои: controller → service → repository, DTO ↔ domain, маппинг (R)
3. `bk3.3` Валидация входа: `@Valid`, `@NotBlank/@Size`, 400 vs 422, свой валидатор (R)
4. `bk3.4` Ошибки наружу: `@ControllerAdvice`, `@ExceptionHandler`, RFC 7807 problem+json (R)
5. `bk3.5` Фильтры и интерсепторы: correlation id, заголовки, порядок (R)
6. `bk3.6` Корутины в контроллере: `suspend` в WebFlux, где блокировать нельзя, `Dispatchers.IO` (R)

**BK4 · Корутины на сервере**
1. `bk4.1` Параллельные вызовы: `coroutineScope + async`, `awaitAll`, отмена при ошибке (V*)
2. `bk4.2` Таймауты и `SupervisorJob`: `withTimeout`, падение одного не роняет всех (V*)
3. `bk4.3` `Mutex` и `Channel`: защита состояния, актор для фоновой очереди (V*)
4. `bk4.4` Flow для стримов: SSE из `Flow`, backpressure (R)

\* V, если харнесс генератора получит `kotlinx-coroutines-core` на JVM и Native (одна правка сборки); иначе R.

**BK5 · Базы данных и SQL**
1. ★ `bk5.1` SELECT/WHERE/ORDER/LIMIT, JOIN, GROUP BY + HAVING (R, подсветка sql)
2. `bk5.2` Индексы: B-tree, составной индекс и порядок колонок, когда индекс не работает (R)
3. `bk5.3` Транзакции и изоляция: dirty/non-repeatable/phantom, `@Transactional` и его грабли (self-invocation, checked exceptions) (R)
4. `bk5.4` Spring Data JPA: `@Entity`, репозитории, derived queries, `@Query`; N+1 и как его увидеть (R)
5. `bk5.5` Миграции: Flyway, обратимость, деплой без простоя (R, подсветка sql)
6. `bk5.6` Пагинация: offset vs keyset, курсор (V: кодирование курсора на чистом Kotlin)
7. `bk5.7` Пул соединений: HikariCP, размер пула, утечки (R)

**BK6 · Надёжный сервис**
1. ★ `bk6.1` Rate limiter: token bucket на чистом Kotlin (V, пара к sd1.2)
2. `bk6.2` Ретраи: экспоненциальный backoff + jitter, какие ошибки не ретраить, Resilience4j (V + R)
3. `bk6.3` Идемпотентность: `Idempotency-Key`, хранилище ключей, TTL (V)
4. `bk6.4` Circuit breaker: состояния, пороги, half-open (V)
5. `bk6.5` Graceful shutdown и health checks: Actuator, liveness/readiness, дренаж запросов (R)
6. `bk6.6` Кеш: LRU на `LinkedHashMap`, TTL, cache-aside, stampede, `@Cacheable` (V + R, пара к LRU Cache в базе знаний)

**BK7 · Интеграции**
1. `bk7.1` HTTP-клиент: `WebClient`/`RestClient`, таймауты, пул, десериализация ошибок (R)
2. `bk7.2` Kafka producer: ключ → партиция, acks, идемпотентный продюсер, `KafkaTemplate` (R)
3. `bk7.3` Kafka consumer: группы, оффсеты, `@KafkaListener`, at-least-once + дедуп (R, пара к sd1.4)
4. `bk7.4` Outbox: запись и событие в одной транзакции (R, пара к sd1.7)
5. `bk7.5` Redis: `@Cacheable` с TTL, сессии, счётчики, `SETNX` как lock (R, пара к SD 06)
6. `bk7.6` Фоновые задачи: `@Scheduled`, ShedLock, ровно один инстанс (R)
7. `bk7.7` WebSocket и SSE: когда что, heartbeat, реконнект (R, пара к sd0.6)

**BK8 · Безопасность** (Spring Security)
1. `bk8.1` Пароли: bcrypt/argon2, соль, почему не SHA-256, `PasswordEncoder` (R)
2. `bk8.2` JWT: заголовок/payload/подпись, срок жизни, отзыв, фильтр в `SecurityFilterChain` (R)
3. `bk8.3` OAuth2 + PKCE, access/refresh, ротация, resource server (D, пара к sd1.3)
4. `bk8.4` Инъекции и OWASP: параметризованные запросы, CORS, CSRF для API (R)
5. `bk8.5` Секреты и TLS: env/vault, терминация TLS на LB, mTLS между сервисами (R)

**BK9 · Тесты**
1. `bk9.1` Юнит-тесты: JUnit 5, `assertThrows`, параметризованные (V: тест как упражнение)
2. `bk9.2` MockK: `every/verify`, `coEvery`, когда мок вреден (R)
3. `bk9.3` Тест web-слоя: `@WebMvcTest`, `MockMvc`, статус + тело (R)
4. `bk9.4` `@SpringBootTest` и Testcontainers: настоящие Postgres и Kafka в тесте (R)
5. `bk9.5` Пирамида и контракты: что тестировать где, consumer-driven contracts (D)

**BK10 · Сборка и доставка**
1. `bk10.1` Gradle: `libs.versions.toml`, мультимодуль, `bootJar` (R)
2. `bk10.2` Dockerfile: multi-stage, слои и кеш, флаги памяти JVM в контейнере (R)
3. `bk10.3` docker-compose: сервис + Postgres + Redis + Kafka локально (R, подсветка yaml)
4. `bk10.4` CI: GitHub Actions — сборка, тесты, образ (R, подсветка yaml)
5. `bk10.5` Kubernetes: Deployment, Service, probes, requests/limits (R, подсветка yaml)
6. `bk10.6` Наблюдаемость: Actuator + Micrometer → Prometheus, RED, p99, OpenTelemetry tracing (R, пара к sd2.3)

**BK11 · Архитектура и собес**
1. `bk11.1` Гексагональная архитектура: порты и адаптеры на Kotlin + Spring (R)
2. `bk11.2` Монолит → микросервисы: границы, общая БД как антипаттерн (D, пара к SD 18)
3. `bk11.3` Событийная архитектура и CQRS на практике (D, пара к SD 16–17)
4. `bk11.4` Финал: собери сервис целиком — шаги «Собери решение из строк» через все слои (R)

## Механики

- **Три типа уроков в одном треке.** Движок уже умеет `kind: code` (компилируемый, через генератор) и
  `kind: design` (ручной JSON, текстовые дырки). Для Spring-кода, SQL, YAML и HTTP нужен `kind: design` с
  кодовой подсветкой: поле `syntax: kotlin | python | sql | yaml | http | plain` в уроке; `SyntaxLang` в
  `:design` получает `Sql`, `Yaml`, `Http`, `Plain`. Подсветка — те же регулярки (строка, число, комментарий)
  плюс набор ключевых слов на язык; для Kotlin-кода Spring аннотации `@Xxx` подсвечиваются как отдельный класс
  токена.
- **Пары с System Design.** Где SD-урок уже объясняет «зачем» (rate limiter, outbox, авторизация,
  наблюдаемость), BK-урок показывает «как в коде». В теории — `[[ссылка]]` на SD-урок и заметку базы знаний.
- **Упражнения те же:** дырки в коде, запросе, конфиге; «предскажи статус/вывод» — дырка вида
  `→ ⟦201 Created⟧`; финальный шаг «Собери решение из строк» для полных фрагментов (как e4 в алгоритмах).
- **Spring с нуля.** Каждая аннотация объясняется при первом появлении в теории урока; в базе знаний —
  заметка «00 Словарь Spring» по образцу «00 Словарь терминов» в System Design.

## Изменения в приложении (малые)

1. `Track.Backend` рядом с `Code` и `SystemDesign`; `langFilter` для Backend = null (сегмент Kotlin | Python
   не показывается).
2. Каталог `files/content/bk/index.json`, уроки `files/content/bk/<id>.json`; `lessonPath` ветвится по
   префиксу `bk` так же, как по `sd`.
3. Четвёртый пункт нижней навигации «Backend» → `BkHome`, `HomeTab(..., Track.Backend)`.
4. `syntax` в `LessonDto`/`Lesson` с дефолтом по `kind` (code → по языку решений, design → plain);
   `SyntaxLang` расширяется; `CodeTemplate`/`TokenChip` читают его из `LocalSyntaxLang` как сейчас.
5. Прогресс: без миграции — статусы и повторы уже по id уроков.
6. База знаний: новый раздел «Backend» в allow-list синхронизации.

## Контент и проверка

- **Verified (V):** sidecar `content/authoring/bk/<id>.md` в существующей грамматике генератора; эталон и все
  варианты компилируются и проходят `examples` + `checks` на JVM и Native; неверные облачки доказываются
  (G9–G14). Для BK4 нужен `kotlinx-coroutines-core` в харнессе генератора на обеих платформах; если это
  окажется дорого, уроки BK4 идут как R.
- **Reviewed (R/D):** ручной JSON, как SD. Правила `validate()`: у каждого неверного облачка — `why` с
  контрпримером или ссылкой на факт, у каждой дырки — `ok`, банк ≤ 10. Факты сверяются с заметками раздела
  «Backend». Ревью заказчика — блокирующий чекпоинт на каждый уровень, как в фазах 5–6, 12–13.
- **Опция v2:** JVM-харнесс `tools/bk_harness/` — Gradle-проект со Spring Boot и зависимостями, который
  компилирует эталоны R-уроков и прогоняет `checks` (без Native). Убирает класс ошибок «код не компилируется».
  Не для первого среза.
- **Теория.** На каждый уровень 3–6 заметок в Obsidian (`Матрицы/Backend/`), затем sync в базу знаний.
  Заметки пишет Claude в формате vault (callouts, wikilinks), заказчик ревьюит до того, как по ним пишутся
  уроки.

## Первый срез (★)

`bk3.1` `@RestController` и роутинг (R, Spring) · `bk5.1` SQL SELECT/JOIN (R, подсветка sql) ·
`bk6.1` Token bucket (V, генератор). Три урока закрывают три механики: ручной фреймворк-код, новый язык
подсветки, компилируемый бэкенд-алгоритм. Плюс вкладка, каталог, `Track.Backend`, заметки «00 Словарь Spring»,
«Контроллер», «SQL: выборка», «Rate limiter в коде», e2e-смоук на Android и iOS.

## Разбивка на фазы (предложение для GSD roadmap)

| Фаза | Содержание | Зависит от |
|------|-----------|-----------|
| 15 Backend Track Skeleton | `Track.Backend`, вкладка, каталог `bk/`, `syntax` + подсветка sql/yaml/http, пустой раздел «Backend» в KB, e2e на пустом каталоге | — |
| 16 Backend Slice | заметки и уроки ★ `bk3.1`, `bk5.1`, `bk6.1`; ревью заказчика; smoke.sh на Android и iOS | 15 |
| 17 Backend Notes BK1–BK3 | 12–15 заметок: HTTP/API, Spring Boot основы, web-слой, «00 Словарь Spring» | 15 |
| 18 Backend Lessons BK1–BK3 | 16 уроков по заметкам фазы 17, ревью по уровню | 16, 17 |
| 19 Backend Notes + Lessons BK4–BK5 | корутины на сервере (решение по coroutines в генераторе), БД и SQL | 18 |
| 20 Backend Notes + Lessons BK6–BK7 | надёжность (V-уроки), интеграции Kafka/Redis | 19 |
| 21 Backend Notes + Lessons BK8–BK9 | Spring Security, тесты | 20 |
| 22 Backend Notes + Lessons BK10–BK11 | сборка и доставка, архитектура, финал | 21 |
| 23 (опционально) BK Harness | JVM-харнесс компиляции Spring-эталонов | 16 |

Фазы 17–22 идут парами «заметки → уроки → ревью → прогон на устройствах», как 11 → 12.

## Вне рамок

- Ktor и любой другой фреймворк; Java как язык решений; Go/Node как альтернативные бэкенды.
- Основы языка Kotlin отдельным уровнем — заказчик знает язык.
- Запуск и компиляция кода на устройстве или сервере — как и раньше.
- Senior-темы: saga и распределённые транзакции, консенсус, тюнинг GC, JVM-профилирование.
- Полный курс администрирования БД (репликация, бэкапы) — остаётся теорией в System Design.
- Облачные провайдеры (AWS/GCP/Yandex Cloud) — упоминание, не уроки.
