---
phase: 12-client-server-basics-lessons
plan: 01
subsystem: content/sd
tags: [content, system-design, lessons]
requires: [Phase 8 SD engine/validator, Phase 11 KB notes 01-12]
provides: [sd0.3, sd0.4, sd0.5, sd0.6, SD catalog order sd0.1, sd0.3-sd0.6, sd1.1, sd1.2]
affects: [tools/smoke.sh SD scenario (walks every SD catalog lesson; verified in 12-02)]
tech-stack:
  added: []
  patterns: [full wrongWhy swap matrix per step (WR-01), acceptable (≈) for defensible alternatives]
key-files:
  created:
    - shared/src/commonMain/composeResources/files/content/sd/sd0.3.json
    - shared/src/commonMain/composeResources/files/content/sd/sd0.4.json
    - shared/src/commonMain/composeResources/files/content/sd/sd0.5.json
    - shared/src/commonMain/composeResources/files/content/sd/sd0.6.json
  modified:
    - shared/src/commonMain/composeResources/files/content/sd/index.json
decisions:
  - "Полная матрица wrongWhy на перестановки правильных облачков (как sd1.2), решение заказчика"
  - "sd0.6 шаг 1: WebSocket для котировок — acceptable, SSE — эталон"
  - "sd0.3 шаг 2: PostgreSQL для потока сообщений чата — acceptable, Cassandra — эталон"
  - "sd0.5: авторизация (access/refresh, 401 → refresh под Mutex) — финальная схема урока; synchronized — acceptable"
metrics:
  duration: ~45min
  completed: 2026-10-28
---

# Phase 12 Plan 01: Client-Server Basics Lessons Summary

Четыре SD-урока по заметкам Phase 11 — строительные блоки, путь запроса, HTTPS/TLS + refresh под Mutex, real-time — по 4 шага, why у каждой ловушки, полная матрица wrongWhy; каталог sd0.1, sd0.3–sd0.6, sd1.1, sd1.2; тексты приняты заказчиком.

## Tasks

| Task | Name | Commit |
|------|------|--------|
| 1 | sd0.3, sd0.4 + каталог | 3ddbde7 |
| 2 | sd0.5, sd0.6 + финальный каталог | 3ddbde7 |
| 3 | Ревью заказчика (approved) + правки | 3ddbde7 |

Один коммит на весь план: по плану коммит/push только после «approved» (main общий с параллельной сессией, пуш до ревью ушёл бы в публичный репо).

## Review changes

- sd0.6 e1, дырка «Котировки»: WebSocket → acceptable с note, wrongWhy убран.
- sd0.3 e2, дырка «Миллионы сообщений чата»: PostgreSQL → acceptable с note, wrongWhy убран.

## Verification

- `./gradlew :shared:testAndroidHostTest` — 24 сьюта зелёные (LessonContentTest.everySdLessonIsStrictValidDesign включён).
- `python3 tools/gen_content.py --check` — exit 0.
- Устройства не использовались (план не требует; E2E smoke — в 12-02).

## Deviations from Plan

None - plan executed exactly as written (коммиты задач 1–2 объединены с финальным по указанию плана, см. выше).

## Known Stubs

None.

## Self-Check: PASSED

- 4 файла уроков и index.json существуют; коммит 3ddbde7 есть в истории, автор tlenparty, без Co-Authored-By.
