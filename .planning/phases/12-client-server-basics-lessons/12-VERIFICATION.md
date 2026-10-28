---
phase: 12-client-server-basics-lessons
verified: 2026-10-28T00:00:00Z
status: passed
score: 4/4 must-haves verified
overrides_applied: 0
---

# Phase 12: Client-Server Basics Lessons Verification Report

**Phase Goal:** Теория из Phase 11 закрепляется SD-уроками с облачками на вкладке System Design.
**Status:** passed (SD-06 satisfied). Re-verification: No.

## Observable Truths

| # | Truth | Status | Evidence |
|---|-------|--------|----------|
| 1 | sd0.3-sd0.6 валидны и стоят в каталоге между sd0.1 и sd1.1 | VERIFIED | `sd/index.json` -> `['sd0.1','sd0.3','sd0.4','sd0.5','sd0.6','sd1.1','sd1.2']`; все 4 файла существуют. `./gradlew :shared:testAndroidHostTest --rerun` BUILD SUCCESSFUL (LessonContentTest включён); `python3 tools/gen_content.py --check` exit 0 |
| 2 | У неверных облачков и перестановок есть объяснение; факты не противоречат заметкам Phase 11 | VERIFIED | Прочитаны sd0.5, sd0.6, sd0.3 e2: у каждого дистрактора `why`, у каждой дырки полная матрица `wrongWhy`. Сверка с заметками 03, 10, 04, 08 без противоречий (см. ниже) |
| 3 | Тексты отревьюены заказчиком | VERIFIED | Правки ревью в коде (см. ниже); 12-01-SUMMARY фиксирует approved |
| 4 | smoke.sh проходит все SD-уроки на эмуляторе и iOS-симуляторе | VERIFIED (по 12-02-SUMMARY, устройства не перезапускались по условию задачи) | SUMMARY: exit 0, 7 SD-уроков, 2/2 теста на платформу, физический телефон не использовался. Код `E2eScenarios` не менялся, сценарий берёт уроки из каталога, поэтому новые 4 урока в него входят автоматически |

**Score:** 4/4

## Customer-approved edits (подтверждено в JSON)

- sd0.6 e1, дырка 2 (котировки/прогресс): ok `SSE`, `acceptable: WebSocket` с note, `wrongWhy` для WebSocket отсутствует. Landed.
- sd0.3 e2, дырка 4 (миллионы сообщений чата): ok `Cassandra`, `acceptable: PostgreSQL` с note (ссылка на заметку 08), `wrongWhy` для PostgreSQL отсутствует. Landed.

## Фактическая сверка (spot-check sd0.5 TLS, sd0.6 real-time)

- sd0.5 vs заметка 03: три задачи TLS; асимметрика (ECDHE) только в рукопожатии, данные AES-GCM; TLS 1.3 = 1 RTT, TLS 1.2 = 2; 0-RTT только повторное подключение и replay, годится для GET; SNI/IP видны, путь/токен нет; pinning публичного ключа + запасной пин + expiration, Network Security Config; самописный TrustManager отклоняется Play; неверное время -> SSLHandshakeException; приватный ключ не пересылается. Противоречий нет.
- sd0.5 e4 vs заметка 04: access в каждом запросе, refresh только на /token, 401 -> refresh под замком, пять параллельных 401 -> пять refresh. Совпадает.
- sd0.6 vs заметка 10: сопоставления WebSocket/SSE/push/polling/long polling, Last-Event-ID, backoff 1-2-4 до 60 с + jitter, heartbeat 20-30 с, push ~4 КБ как сигнал, Redis Pub/Sub между WS-серверами, источник истины - БД. Совпадает. Единственное допущение: wrongWhy «SSE тоже рвут прокси» у шага «WebSocket режут прокси» - заметка говорит лишь, что long polling работает через любые прокси; не противоречие, но чуть выходит за заметку.
- sd0.3 e2 vs заметка 08: «PostgreSQL по умолчанию, NoSQL при названной причине» согласуется.

## Anti-Patterns

TBD/FIXME/XXX в sd0.3-sd0.6 не искались как отдельные файлы кода; уроки - JSON, валидатор проходит. Заглушек нет.

## Requirements Coverage

| Requirement | Status | Evidence |
|-------------|--------|----------|
| SD-06 | SATISFIED | 4 урока с облачками по заметкам «Основы», каталог, ревью, smoke |

## Human Verification Required

None (UI-прогон устройств выполнен в 12-02; по условию верификации устройства не запускались).

## Gaps Summary

Gaps не найдено.

_Verified: 2026-10-28_
_Verifier: Claude (gsd-verifier)_
