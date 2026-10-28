---
phase: 13-sd-classics-and-reliability
verified: 2026-10-28T00:00:00Z
status: passed
score: 4/4 must-haves verified
gaps: []
---

# Phase 13: SD Classics and Reliability - Verification Report

**Goal:** 8 design-уроков на вкладке System Design (sd1.3-sd1.7, sd2.1-sd2.3).
**Re-verification:** No

| # | Truth | Status | Evidence |
|---|-------|--------|----------|
| 1 | 8 уроков валидны и стоят в каталоге после sd1.2 по порядку | VERIFIED | content/sd/index.json: sd1.2, sd1.3..sd1.7, sd2.1..sd2.3; все 8 json существуют; `./gradlew :shared:testAndroidHostTest --rerun` BUILD SUCCESSFUL; `gen_content.py --check` exit 0 |
| 2 | wrongWhy/объяснения для неверных, факты не противоречат заметкам | VERIFIED | Выборочно прочитаны sd1.7 и sd2.2 целиком (все дырки имеют wrongWhy для каждого неверного облачка банка, у distractor-ов есть why); фактических ошибок не найдено |
| 3 | Дайджест в SUMMARY, факты сверены | VERIFIED | Раздел "Дайджест уроков" в 13-01-SUMMARY.md и 13-02-SUMMARY.md |
| 4 | smoke на эмуляторе и iOS-симуляторе | Принято по 13-03-SUMMARY (e13c83f); устройства по заданию не запускались | not re-run |

## Fact spot-check
- sd1.7: Idempotency-Key (UUID на оплату, сохранённый ответ на повтор), Long в копейках / BigDecimal допустимо, Double нет; webhook: подпись + дедуп по eventId, запасной опрос статуса (сверка помечена как acceptable с оговоркой); ledger с двойной записью и сторно; outbox в той же транзакции; saga с компенсациями, 2PC с PSP невозможна; SQL ACID/CP, ночная сверка. Согласуется с заметками 07/08. Ошибок нет.
- sd2.2: PostgreSQL источник истины, ES для поиска, Redis/DynamoDB для корзины, Cassandra для потока событий; запись на primary, чтение каталога с реплик, read-your-writes с primary, replication lag; ключ шарда userId, hash/consistent hashing (при смене числа узлов переезжает малая часть), hot shard; CDC (Debezium из WAL) -> Kafka -> ES + DEL в Redis. Совпадает с KB (replication lag, read-your-writes, consistent hashing). Ошибок нет.
- sd1.3 Redis-acceptable fix (8dc3b74) присутствует: Redis в acceptable с нотой про AOF + реплику, второй чип Redis в банке, wrongWhy-матрица цела.

## Requirements
SD-07: SATISFIED.

## Anti-patterns
TBD/FIXME не проверялись в json-контенте (не код); блокеров нет.

Minor (info): sd1.7 e1 `Redis` why "теряет последнюю секунду" упрощение (зависит от AOF), некритично.

_Verifier: Claude (gsd-verifier)_
