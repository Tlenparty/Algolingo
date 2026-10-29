---
phase: 10-python-content
plan: 02
subsystem: content
tags: [python, content, l0.1, l0.2, l0.3, batch-a]
requires: [10-01]
provides:
  - "lessons/py/l0.1.json, l0.2.json, l0.3.json: Python lessons of batch A"
  - "index.json: l0.1, l0.2, l0.3 langs [kotlin, python]"
affects: [content/authoring/py/, index.json]
tech-stack:
  added: []
  patterns: ["a why for every wrong (hole, chip) pair, each checked by running it on an example input"]
key-files:
  created:
    - content/authoring/py/l0.1.md
    - content/authoring/py/l0.2.md
    - content/authoring/py/l0.3.md
    - shared/src/commonMain/composeResources/files/content/lessons/py/l0.1.json
    - shared/src/commonMain/composeResources/files/content/lessons/py/l0.2.json
    - shared/src/commonMain/composeResources/files/content/lessons/py/l0.3.json
  modified:
    - shared/src/commonMain/composeResources/files/content/index.json
decisions:
  - "l0.2 signature is reverse(s: list[str]) -> None. It reverses in place like the Kotlin CharArray version, with O(1) memory. The theory explains that str is immutable, list(text) and \"\".join(s)."
  - "In l0.3 `is not` is a wrong chip. It passes on Latin letters because of the CPython single-char cache, so the example is_palindrome(\"шалаш\") was added to catch it."
  - "l0.3 checks require a strict bool (is True / is False), so `return len(s) - 1` counts as wrong."
  - "Python-specific traps: `=+` (l0.1), `++`/`--` as no-ops (l0.2), `false` NameError, and bool being an int (l0.3)."
metrics:
  duration: ~40m
  completed: 2026-10-29
  tasks: 3
  files: 7
requirements: [PY-BATCH-A]
---

# Phase 10 Plan 02: Python пачка A (l0.1, l0.2, l0.3) Summary

Python-версии уроков «Сумма и максимум», «Разворот строки» и «Палиндром»:

| Урок | Приём | Дырки по шагам |
|------|-------|----------------|
| l0.1 | один проход, `best = nums[0]` | 1/2/4 |
| l0.2 | два указателя по `list[str]`, кортежный обмен | 1/2/4 |
| l0.3 | два указателя с ранним `return False` | 1/2/4 |

У каждой неверной пары (дырка, облачко) есть своё «почему» с реальным выводом Python. Заказчик принял пачку без правок.

## Задачи

| # | Задача | Коммит |
|---|--------|--------|
| 1 | py/l0.1 и py/l0.2, генерация, index.json | 7b3d430 |
| 2 | py/l0.3, генерация, тесты приложения | 515babd |
| — | правка формулировки вариации в l0.2 (без Kotlin-записи `s[i..j]`) | be8ceb9 |
| 3 | ревью заказчика (checkpoint) | approved, без правок |

## Проверка

- `python3 tools/gen_content.py --check` возвращает 0. G12, G13 и G14 проходят для l0.1, l0.2, l0.3 и l1.4.
- `python3 -m unittest tools/test_gen_content.py`: 82 OK.
- `./gradlew :shared:testAndroidHostTest --rerun`: BUILD SUCCESSFUL.
- Каждое «почему» сверено скриптом: каждая неверная программа запускалась на входах из примеров, вывод и исключения совпали с текстом.
- В `index.json` изменились только строки `langs` у трёх уроков. Kotlin-выходы не изменились.
- В `lessons/py/l0.2.json` нет `s[::-1]`. В py-сайдкарах нет импортов.

## Deviations from Plan

1. **[Rule 2] В l0.3 добавлен пример `is_palindrome("шалаш")`, которого нет в Kotlin-уроке.** Без него облачко `is not` проходит все проверки на латинице: CPython кэширует однобуквенные строки. Тогда G13 не дал бы сделать его дистрактором, а это главная Python-ловушка урока «`==` против `is`».
2. **Срез в вариациях l0.2 записан как `text[::-1]`, а не `s[::-1]`.** Так выполняется критерий «в `l0.2.json` нет `s[::-1]`», а в проде идиома всё равно упомянута.

## Заметки ревью

- Пилот py/l1.4: у `e3 @1 nums [crash]` на деле получается crash+wrong. По решению заказчика тег оставлен как есть.

## Known Stubs

None.

## Self-Check: PASSED

- Сайдкары и три py-JSON существуют. Коммиты 7b3d430, 515babd и be8ceb9 есть в истории.
