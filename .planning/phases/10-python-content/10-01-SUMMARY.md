---
phase: 10-python-content
plan: 01
subsystem: content-generator
tags: [python, gen_content, G14, WR-01]
requires: []
provides: [G14 python kind-tag check]
affects: [10-02, 10-03, 10-04]
tech-stack:
  added: []
  patterns: [kinds transported as last stdout line GSD_KINDS, chip-scope union like WrongChipsTest]
key-files:
  created: []
  modified: [tools/gen_content.py, tools/test_gen_content.py]
decisions:
  - "G14 is a new code (not a G13 extension). G13 still means 'chip passes', G14 means 'tag != fact'."
  - "Tags must be a subset of the observed kinds (all tags must be seen), the same semantics as Kotlin WrongChipsTest. An observed kind with no matching tag is allowed."
  - "A timeout gives only {hang}. Kinds of the cases that ran before the timeout are lost (ponytail comment in python_kinds)."
metrics:
  duration: ~15m
  completed: 2026-10-28
requirements: [PY-WR01]
---

# Phase 10 Plan 01: G14, проверка тегов Python-облачков Summary

Генератор теперь классифицирует исход каждой неверной Python-программы (compile / hang / crash / wrong) и сверяет его с тегами заявки. При расхождении он падает с `G14: <bullet>: заявлено X, на деле Y`.

## Что сделано

- `PY_PRELUDE`: `case.__exit__` записывает имя типа исключения в `_kinds`. Упавший модуль последней строкой печатает `GSD_KINDS: AssertionError,TypeError`. Формат `_fails` не изменился.
- `run_python_kinds(source)` → `(ok, detail, names)`:
  - строку `GSD_KINDS` разбирает из полного stdout до обрезки до 3 строк и из `detail` убирает;
  - таймаут даёт `None`;
  - падение вне `case` (ненулевой код без строки) даёт `{"?"}`, это считается crash.

  `run_python` сохранил сигнатуру и стал обёрткой.
- `python_kinds(source)`:
  - `compile()` (только компиляция, без exec) при SyntaxError даёт `{compile}`, программа тогда не запускается;
  - таймаут даёт `{hang}`;
  - иначе AssertionError даёт wrong, любое другое исключение даёт crash.
- `claim_mismatch(observed)` устроен как `WrongChipsTest`. Теги hole-заявки проверяются по её программе. Теги chip-заявки проверяются по объединению видов всех программ, у которых есть эта заявка. Пустые теги и программы без заявок не проверяются.
- `python_gates`: G13 срабатывает, если набор видов пуст. G14 срабатывает при первом расхождении. Шапка файла и docstring обновлены.
- Ветка Kotlin (`render_kt`, `render_wrong_json`, `Probe`) не тронута. Перегенерация дала `0 file(s) written, 13 unchanged`.

## Тесты (82, OK)

Новые тесты в `PyHarness`:
- `wrong`, а на деле crash;
- `compile`, а на деле crash/wrong;
- `hang`, а на деле crash/wrong;
- тег hole-scope `@1 [hang]`;
- over-claim `[compile,wrong]`;
- верные `[compile]` и `[crash]` проходят;
- `python_kinds`: compile / clean / crash на верхнем уровне / смешанный wrong+crash после 6 провалов assert / hang;
- `claim_mismatch`: пустые теги, программы без заявки, объединение для chip-scope.

Существующий `test_hanging_wrong_chip_is_killed_and_counts_as_failing` остался зелёным.

## Пилот py/l1.4.md

**Пилот прошёл без правок.** Все 55 неверных программ проверены запуском, каждый тег подтверждён фактом. Например, `e2 @3 enumerate(nums) [wrong]` действительно падает по AssertionError. Единственный случай, когда видов наблюдается больше, чем заявлено: у `e3 @1 nums [crash]` на деле crash+wrong. Заявленный crash наблюдается, поэтому проверка проходит. Правка не нужна, но заказчик может об этом знать.

## Deviations from Plan

- **Ожидания в тестах подогнаны под факт.** Облачко `x` в e1 тестовой фикстуры PY_BASE на деле даёт crash+wrong (`seen[target - x]` → KeyError), а не только wrong. Поэтому в тестах «compile→wrong» и «hang→wrong» сообщение содержит `на деле crash/wrong`. Для теста hole-scope вместо `[crash]` взят `[hang]`, потому что crash там наблюдается на самом деле.
- **Task 2 не дал изменений в файлах.** `--check` вернул 0, перегенерация ничего не изменила, поэтому у задачи нет отдельного коммита.
- Добавлена чистая функция `claim_mismatch`, чтобы случаи «без тегов / без заявки» можно было протестировать: через парсер они недостижимы, G9 их запрещает.

## Commits

- 65ab897 test(10-01): add failing G14 tests for python kind tags vs real outcome
- 146bba6 feat(10-01): classify python wrong-program outcome, G14 checks kind tags
- 2555e38 refactor(10-01): document G14 test helpers

## TDD Gate Compliance

RED (test) → GREEN (feat) → REFACTOR: все коммиты на месте.

## Self-Check: PASSED
