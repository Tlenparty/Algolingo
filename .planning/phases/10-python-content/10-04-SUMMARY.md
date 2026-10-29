---
phase: 10-python-content
plan: 04
subsystem: content
tags: [python, content, l1.3, l1.5, batch-c]
requires: [10-03]
provides:
  - "lessons/py/l1.3.json, l1.5.json: Python lessons of batch C"
  - "index.json: all 9 code lessons langs [kotlin, python]"
affects: [content/authoring/py/, index.json]
tech-stack:
  added: []
  patterns: ["a why for every wrong (hole, chip) pair, each checked by running it on an example input", "inputs from the whys are added to checks (as in Kotlin)"]
key-files:
  created:
    - content/authoring/py/l1.3.md
    - content/authoring/py/l1.5.md
    - shared/src/commonMain/composeResources/files/content/lessons/py/l1.3.json
    - shared/src/commonMain/composeResources/files/content/lessons/py/l1.5.json
  modified:
    - shared/src/commonMain/composeResources/files/content/index.json
decisions:
  - "l1.5 uses tuples: day entries (user_id, steps), result (user_ids, steps) — the «tuple instead of Pair» rule; dataclass/NamedTuple not used."
  - "l1.5 user_ids order is any; checks compare sorted(r[0]) like Kotlin; sorted(champions) is in Variations."
  - "l1.5 max(..., default=0) is the direct analogue of maxOfOrNull ?: 0, not a bypass of the technique."
  - "l1.3 e3 holes are Python traps ({} vs set(), counts.get(ch, 0) vs counts[ch] / get(ch, 1)) instead of the Kotlin `?: 0` hole."
  - "l1.3 is case-sensitive like Kotlin; normalisation is in Variations."
  - "Whys do not name version-specific exception types where CPython changed them (comprehension scoping: NameError in 3.9, UnboundLocalError in 3.12+)."
metrics:
  completed: 2026-10-29
  tasks: 3
  files: 5
requirements: [PY-BATCH-C]
---

# Phase 10 Plan 04: Python пачка C (l1.3, l1.5) Summary

Python-версии уроков «Анаграмма» и «Победители соревнования». Теперь все 9 кодовых уроков имеют Python-версию:

| Урок | Приём | Дырки по шагам |
|------|-------|----------------|
| l1.3 | проверка длин, `dict` частот: плюс по `a`, минус по `b` | 1/2/4 |
| l1.5 | два `dict` (сумма + дни), фильтр, `max(..., default=0)` | 1/2/4 |

Заказчик принял пачку; единственная правка — нейтральная формулировка исключения в одном «почему».

## Задачи

| # | Задача | Коммит |
|---|--------|--------|
| 1 | py/l1.3, генерация, index.json | a4b4291 |
| 2 | py/l1.5, генерация, index.json со всеми 9 уроками | a1bf377 |
| — | рендер для ревью, вливание в main | 05ff635, merge |
| 3 | ревью заказчика (checkpoint) | approved + правка `NameError` в l1.5 e3 @3 |

## Проверка

- `python3 tools/gen_content.py --check` возвращает 0. G12, G13 и G14 проходят для всех py-уроков.
- `python3 -m unittest tools/test_gen_content.py`: 82 OK.
- `./gradlew :shared:testAndroidHostTest`: BUILD SUCCESSFUL (207 тестов), повторно после правки — зелёный.
- В `index.json` у l0.1–l0.4 и l1.1–l1.5 `langs = [kotlin, python]`.
- В банке и решении l1.3 нет `sorted(` и `Counter`.

## Deviations from Plan

1. **[Review] l1.5 e3 @3 `full`: «→ `NameError`» заменено на «код упадёт: `full` ещё не существует».** В Python 3.9 это `NameError`, с 3.12 (PEP 709) — `UnboundLocalError`; тег [crash] не меняется.

## Known Stubs

None.

## Self-Check: PASSED

- Сайдкары и два py-JSON существуют. Коммиты a4b4291, a1bf377 и 05ff635 есть в истории.
