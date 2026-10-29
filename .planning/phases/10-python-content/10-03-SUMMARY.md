---
phase: 10-python-content
plan: 03
subsystem: content
tags: [python, content, l0.4, l1.1, l1.2, batch-b]
requires: [10-02]
provides:
  - "lessons/py/l0.4.json, l1.1.json, l1.2.json: Python lessons of batch B"
  - "index.json: l0.4, l1.1, l1.2 langs [kotlin, python]"
affects: [content/authoring/py/, index.json]
tech-stack:
  added: []
  patterns: ["a why for every wrong (hole, chip) pair, each checked by running it on an example input", "inputs from the whys are added to checks (as in Kotlin)"]
key-files:
  created:
    - content/authoring/py/l0.4.md
    - content/authoring/py/l1.1.md
    - content/authoring/py/l1.2.md
    - shared/src/commonMain/composeResources/files/content/lessons/py/l0.4.json
    - shared/src/commonMain/composeResources/files/content/lessons/py/l1.1.json
    - shared/src/commonMain/composeResources/files/content/lessons/py/l1.2.json
  modified:
    - shared/src/commonMain/composeResources/files/content/index.json
decisions:
  - "l1.2 signature is first_unique_char(s: str) -> str | None, matching Kotlin Char?. The index/-1 form from the plan moved to Variations."
  - "l1.2: iterating `counts` in the second loop is ~acceptable (dict keeps insertion order since 3.7). In Kotlin it is a wrong chip. The explanation names the second pass over the string as the technique."
  - "l0.4 has many [hang] tags: negative indices do not fail in Python, so boundary errors loop forever. Cost: gen --check ~2.5 min, unittest ~7.5 min (10 s hang timeout)."
  - "l0.4: `lo + (hi - lo) // 2` is ~acceptable, explained as a Kotlin/Java overflow habit; Python ints do not overflow."
metrics:
  completed: 2026-10-29
  tasks: 3
  files: 7
requirements: [PY-BATCH-B]
---

# Phase 10 Plan 03: Python пачка B (l0.4, l1.1, l1.2) Summary

Python-версии уроков «Бинарный поиск», «Есть ли дубликат» и «Первый уникальный символ»:

| Урок | Приём | Дырки по шагам |
|------|-------|----------------|
| l0.4 | `while lo <= hi`, `mid = (lo + hi) // 2`, `± 1` | 1/2/4 |
| l1.1 | `set()` «уже видели», ранний `return True` | 1/2/4 |
| l1.2 | `dict` частот через `get(ch, 0)`, второй проход по строке | 1/2/4 |

У каждой неверной пары (дырка, облачко) есть своё «почему» с реальным выводом Python. Заказчик принял пачку без правок.

## Задачи

| # | Задача | Коммит |
|---|--------|--------|
| 1 | py/l0.4 и py/l1.1, генерация, index.json | eec6b55 |
| 2 | py/l1.2, генерация | ffebbee |
| — | вливание в main до ревью (переезд на другую машину) | 9423405 |
| 3 | ревью заказчика (checkpoint) | approved, без правок |

## Проверка

- `python3 tools/gen_content.py --check` возвращает 0. G12, G13 и G14 проходят для всех py-уроков.
- `./gradlew :shared:testAndroidHostTest`: BUILD SUCCESSFUL.
- В l1.1 и l1.2 нет `Counter`/`count` вне «Вариаций»; `bisect` в l0.4 только в «Вариациях».
- Дистрактор `(lo + hi) / 2` реально падает с `TypeError`.

## Deviations from Plan

1. **Сигнатура l1.2 — символ или `None`, а не индекс/-1.** Так урок совпадает с Kotlin (`Char?`); индекс перенесён в «Вариации». Заказчик согласовал.
2. **Входы из «почему» добавлены в checks** (как в Kotlin), чтобы G13 подтверждал каждое объяснение.

## Known Stubs

None.

## Self-Check: PASSED

- Сайдкары и три py-JSON существуют. Коммиты eec6b55, ffebbee и 9423405 есть в истории.
