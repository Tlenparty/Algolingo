---
phase: 08-system-design-section
plan: 02
subsystem: content
tags: [system-design, content, review]
requires: [08-01]
provides:
  - "Урок sd0.1 «Как вести интервью» (3 шага)"
  - "Урок sd1.2 «Rate limiter — защита от спама» (4 шага)"
  - "SD-каталог: sd0.1, sd1.1, sd1.2 — тексты приняты заказчиком"
  - "TokenChip(highlight) — банк без Kotlin-подсветки в design-уроках"
affects: [08-03]
tech-stack:
  added: []
  patterns:
    - "Неупорядоченные дырки: у каждой ok = все взаимозаменяемые ответы (sd0.1 шаг 3)"
key-files:
  created:
    - shared/src/commonMain/composeResources/files/content/sd/sd0.1.json
    - shared/src/commonMain/composeResources/files/content/sd/sd1.2.json
  modified:
    - shared/src/commonMain/composeResources/files/content/sd/index.json
    - design/src/commonMain/kotlin/com/tlenparty/algolingo/design/components/TokenChip.kt
    - shared/src/commonMain/kotlin/com/tlenparty/algolingo/lesson/ui/ExerciseScreen.kt
decisions:
  - "Порядок вопросов в sd0.1 шаг 3 свободный: каждая дырка принимает любой из трёх ответов"
  - "Why Memcached в sd1.2: «нет скриптов и структур данных», а не «нет атомарного INCR+TTL» (incr в Memcached атомарен)"
metrics:
  duration: "~25 min (без ожидания ревью)"
  completed: 2026-10-28
  tasks: 2
  files: 6
---

# Phase 8 Plan 02: SD content + review Summary

Добавлены уроки sd0.1 (порядок интервью, разметка F/NF, первые вопросы) и sd1.2 (Gateway и ключ лимита, алгоритмы, 429/backoff/Idempotency-Key, схема Redis + Lua). Каталог содержит три урока: sd0.1, sd1.1, sd1.2. Заказчик проверил тексты и ответил «approved», правка по Memcached внесена. Кроме того, облачка банка в design-уроках больше не подсвечиваются как Kotlin.

## Tasks

| # | Task | Commit |
|---|------|--------|
| доп. | TokenChip `highlight`, ExerciseScreen передаёт `kind == Code` | e4d739e |
| 1 | sd0.1, sd1.2, sd/index.json | ce21fb0, 9283b6c |
| 2 | Ревью заказчика: approved, правка why Memcached | 548c2cc |

## Verification

- `./gradlew :shared:testAndroidHostTest --tests '*LessonContentTest'`: 5/5 зелёные.
- `python3 tools/gen_content.py --check`: exit 0.
- `:androidApp:assembleDebug` и `:shared:compileKotlinIosSimulatorArm64` проходят после правки TokenChip.

## Deviations from Plan

- [Rule 1] Из why убрал повтор имени облачка («503 — 503 — …»), коммит 9283b6c.
- Доп. задача от оркестратора: TokenChip highlight (вынесена из deferred-items 08-01).

## Known Stubs

Нет.

## Self-Check: PASSED
