# 10-04 — пачка C на ревью заказчика (ожидает ответа)

Исполнитель сгенерировал этот файл 2026-10-29 в ветке ворктри (коммиты a4b4291 и a1bf377, в main ещё не влиты). Проверки зелёные: gen --check, unittest (82 OK) и testAndroidHostTest (207 тестов, 0 упавших). Теперь у всех 9 кодовых уроков в index.json стоит `langs [kotlin, python]`. Ответ заказчика: «ок» или правки. Правки вносятся в content/authoring/py/<id>.md, затем перегенерация и 10-04-SUMMARY.md.

## Решения для заказчика

1. l1.5: формат данных. Записи дня — кортежи `(user_id, steps)`, а не `@dataclass StepEntry`. Результат — кортеж `(user_ids, steps)`, а не `ChampionResult`. Это правило «`tuple` вместо `Pair`» из CONTEXT: облачка короче, и не нужен `import dataclasses`. Альтернатива — `dataclass`/`NamedTuple` с полями `e.user_id` и `e.steps`, как в Kotlin.
2. l1.5: порядок `user_ids`. В условии написано «Порядок `user_ids` любой», проверки сравнивают `sorted(r[0])`, как Kotlin-checks. В теории сказано, что в Python порядок совпадёт с порядком первого появления, потому что `dict` помнит вставку. `sorted(champions)` вынесен в «Вариации».
3. l1.5: максимум считается через `max((total_steps[u] for u in full), default=0)`. Это прямой аналог `maxOfOrNull { } ?: 0` из Kotlin, а не обход приёма. Грабли про `max()` без `default` (`ValueError`) есть в теории.
4. l1.5: в Kotlin облачка `e.steps`/`e.userId` в фильтре дают ошибку компиляции, а в Python `steps`/`user` в позиции `== total_days` дают неверный ответ. Причина: переменная цикла в Python живёт и после цикла. Это объяснено в «почему».
5. l1.5, e2: `full` в `return` неверен как «вернём всех, кто был все дни». В Kotlin это была ошибка компиляции из-за `Set` против `List`.
6. l1.3, e3: дырки отличаются от Kotlin. Вместо дырок `?: 0` и записи `left` сделаны питоновские ловушки: `counts = {}` против `set()` (`AttributeError`) и `counts.get(ch, 0)` против `counts[ch]` (`KeyError: 'c'`) или `counts.get(ch, 1)`. Дырок по шагам 1/2/4.
7. l1.3: регистр. По условию Kotlin-урока буквы сравниваются как есть, поэтому в checks добавлены `"Аб"/"бА"` → `True` и `"Аб"/"аб"` → `False`. Нормализация `lower()`/`replace(" ", "")` вынесена в «Вариации».
8. Ни в одном из уроков нет тегов [hang] и облачков ~acceptable. Все «почему» сверены запуском python3 3.9.6 на указанном входе. Входы из «почему» добавлены в checks.

```
========================================================================
l1.3  Анаграмма  (Python)
========================================================================
Условие: Даны два слова. Верни `True`, если они состоят из одинакового набора букв с учётом количества, иначе `False`.
Тренирует: словарь частот `dict`: плюсуем буквы `a`, минусуем буквы `b`

Примеры:
  same_letters("йцукен", "некуцй") is True
  same_letters("ффффа", "фффа") is False
  same_letters("ab", "ac") is False
  same_letters("aab", "abb") is False
  same_letters("", "") is True

--- Теория ---

[Когда сравнивать частоты]
  сигнал: «Одинаковый набор букв», «перестановка», «анаграмма»
  сигнал: «С учётом количества» — важно, сколько раз, а не только есть ли
  сигнал: Нужно уложиться в O(n)
  Один словарь `counts` «буква → сколько осталось»: плюсуем буквы `a`, минусуем буквы `b`. O(n) по времени, O(k) по памяти, где k — число разных букв. Готовые `collections.Counter(a) == Counter(b)` и `sorted(a) == sorted(b)` тоже верны, но на собесе покажи словарь: `sorted` — это O(n log n).

[Каркас]
  каркас:
    | if len(a) != len(b):
    |     return False
    | counts = {}
    | for ch in a:
    |     counts[ch] = counts.get(ch, 0) + 1
    | for ch in b:
    |     left = counts.get(ch, 0) - 1
    |     if left < 0:
    |         return False
    |     counts[ch] = left
    | return True
  Длины равны и счётчик ни разу не ушёл в минус ⇒ частоты совпали: если бы какой-то буквы `b` не хватило, её счётчик стал бы `-1`. `counts.get(ch, 0)` вернёт `0` для буквы, которой нет в словаре, а `counts[ch]` на ней упадёт с `KeyError`.

[Грабли]
  грабли: Без проверки длины лишние буквы `a` никто не заметит: `"ab"` / `"a"` → `True` вместо `False`
  грабли: `counts[ch] - 1` вместо `get`: буквы `c` нет в `a`, `"ab"` / `"ac"` → `KeyError: 'c'`
  грабли: Запасное значение `1` вместо `0`: буква, которой нет в `a`, даст `0`, а не минус: `"ab"` / `"ac"` → `True` вместо `False`
  грабли: `counts = set()`: у множества нет `get`, `"ab"` / `"ba"` → `AttributeError: 'set' object has no attribute 'get'`
  грабли: `left <= 0`: `0` — это «буква израсходована ровно», а не «лишняя»: `"a"` / `"a"` → `False` вместо `True`

--- Шаг e1 (дырок: 1) ---
    | def same_letters(a: str, b: str) -> bool:
    |     if len(a) != len(b):
    |         return False
    |     counts = {}
    |     for ch in a:
    |         counts[ch] = counts.get(ch, 0) + 1
    |     for ch in b:
    |         left = counts.get(ch, 0) - 1
    |         if left [_1_] 0:
    |             return False
    |         counts[ch] = left
    |     return True
  Ответы:
    _1_ = <
  Банк: [<]  [<=]  [==]  [>]
  Почему неверно (для облачка в целом):
    [<=]: `0` — это «буква израсходована ровно», а не «лишняя»: `"a"` / `"a"` → `False` вместо `True`
    [==]: `0` — это «буква израсходована ровно», а не «лишняя»: `"a"` / `"a"` → `False` вместо `True`
    [>]: условие перевёрнуто, минус никогда не ловится: `"ab"` / `"ac"` → `True` вместо `False`
  Подсказки:
    1. Буква лишняя, если её счётчик ушёл в минус.

--- Шаг e2 (дырок: 2) ---
    | def same_letters(a: str, b: str) -> bool:
    |     if len(a) [_1_] len(b):
    |         return False
    |     counts = {}
    |     for ch in a:
    |         counts[ch] = counts.get(ch, 0) + 1
    |     for ch in b:
    |         left = counts.get(ch, 0) - 1
    |         if left [_2_] 0:
    |             return False
    |         counts[ch] = left
    |     return True
  Ответы:
    _1_ = !=
    _2_ = <
  Банк: [!=]  [<]  [==]  [<=]
  Почему неверно (для облачка в целом):
    [==]: равные длины сразу дают «не анаграмма»: `"ab"` / `"ba"` → `False` вместо `True`
    [<=]: `0` — это «буква израсходована ровно», а не «лишняя»: `"a"` / `"a"` → `False` вместо `True`
  Почему неверно в дырке _1_:
    [<]: длинная `a` проскочит, её лишние буквы никто не проверит: `"ab"` / `"a"` → `True` вместо `False`
    [<=]: равные длины сразу дают «не анаграмма»: `"a"` / `"a"` → `False` вместо `True`
  Почему неверно в дырке _2_:
    [!=]: любой остаток, кроме `0`, считается «лишней» буквой: `"aa"` / `"aa"` → после первой `a` остаток `1`, ответ `False` вместо `True`
    [==]: `0` — это «буква израсходована ровно», а не «лишняя»: `"a"` / `"a"` → `False` вместо `True`
  Подсказки:
    1. Быстрый выход: разная длина → сразу `False`.
    2. Буква лишняя, если её счётчик ушёл в минус.

--- Шаг e3 (дырок: 4) ---
    | def same_letters(a: str, b: str) -> bool:
    |     if len(a) [_1_] len(b):
    |         return False
    |     counts = [_2_]
    |     for ch in a:
    |         counts[ch] = counts.get(ch, 0) + 1
    |     for ch in b:
    |         left = [_3_] - 1
    |         if left [_4_] 0:
    |             return False
    |         counts[ch] = left
    |     return True
  Ответы:
    _1_ = !=
    _2_ = {}
    _3_ = counts.get(ch, 0)
    _4_ = <
  Банк: [!=]  [{}]  [counts.get(ch, 0)]  [<]  [==]  [set()]  [counts[ch]]  [counts.get(ch, 1)]  [<=]
  Почему неверно (для облачка в целом):
    [==]: равные длины сразу дают «не анаграмма»: `"ab"` / `"ba"` → `False` вместо `True`
    [set()]: множество не хранит частоты, у него нет `get`: `"ab"` / `"ba"` → `AttributeError: 'set' object has no attribute 'get'`
    [counts[ch]]: буквы из `b` может не быть в словаре: `"ab"` / `"ac"` → `KeyError: 'c'`
    [counts.get(ch, 1)]: буква, которой нет в `a`, даст `0`, а не минус: `"ab"` / `"ac"` → `True` вместо `False`
    [<=]: `0` — это «буква израсходована ровно», а не «лишняя»: `"a"` / `"a"` → `False` вместо `True`
  Почему неверно в дырке _1_:
    [{}]: между длинами нужен оператор сравнения: `len(a) {} len(b)` — `SyntaxError`, код не запустится
    [counts.get(ch, 0)]: между длинами нужен оператор сравнения: `len(a) counts.get(ch, 0) len(b)` — `SyntaxError`
    [<]: длинная `a` проскочит, её лишние буквы никто не проверит: `"ab"` / `"a"` → `True` вместо `False`
    [set()]: между длинами нужен оператор сравнения: `len(a) set() len(b)` — `SyntaxError`
    [counts[ch]]: между длинами нужен оператор сравнения: `len(a) counts[ch] len(b)` — `SyntaxError`
    [counts.get(ch, 1)]: между длинами нужен оператор сравнения: `len(a) counts.get(ch, 1) len(b)` — `SyntaxError`
    [<=]: равные длины сразу дают «не анаграмма»: `"a"` / `"a"` → `False` вместо `True`
  Почему неверно в дырке _2_:
    [!=]: после `=` нужно значение, а не оператор: `counts = !=` — `SyntaxError`
    [counts.get(ch, 0)]: читаем из `counts`, которого ещё нет: `"ab"` / `"ba"` → `counts = counts.get(ch, 0)` падает с `UnboundLocalError`
    [<]: после `=` нужно значение, а не оператор: `counts = <` — `SyntaxError`
    [==]: после `=` нужно значение, а не оператор: `counts = ==` — `SyntaxError`
    [counts[ch]]: читаем из `counts`, которого ещё нет: `"ab"` / `"ba"` → `counts = counts[ch]` падает с `UnboundLocalError`
    [counts.get(ch, 1)]: читаем из `counts`, которого ещё нет: `"ab"` / `"ba"` → `counts = counts.get(ch, 1)` падает с `UnboundLocalError`
    [<=]: после `=` нужно значение, а не оператор: `counts = <=` — `SyntaxError`
  Почему неверно в дырке _3_:
    [!=]: после `=` нужно значение, а не оператор: `left = != - 1` — `SyntaxError`
    [{}]: из словаря не вычесть `1`: `"ab"` / `"ba"` → `TypeError: unsupported operand type(s) for -: 'dict' and 'int'`
    [<]: после `=` нужно значение, а не оператор: `left = < - 1` — `SyntaxError`
    [==]: после `=` нужно значение, а не оператор: `left = == - 1` — `SyntaxError`
    [set()]: из множества не вычесть `1`: `"ab"` / `"ba"` → `TypeError: unsupported operand type(s) for -: 'set' and 'int'`
    [<=]: после `=` нужно значение, а не оператор: `left = <= - 1` — `SyntaxError`
  Почему неверно в дырке _4_:
    [!=]: любой остаток, кроме `0`, считается «лишней» буквой: `"aa"` / `"aa"` → после первой `a` остаток `1`, ответ `False` вместо `True`
    [{}]: между `left` и `0` нужен оператор: `if left {} 0:` — `SyntaxError`
    [counts.get(ch, 0)]: между `left` и `0` нужен оператор: `if left counts.get(ch, 0) 0:` — `SyntaxError`
    [==]: `0` — это «буква израсходована ровно», а не «лишняя»: `"a"` / `"a"` → `False` вместо `True`
    [set()]: между `left` и `0` нужен оператор: `if left set() 0:` — `SyntaxError`
    [counts[ch]]: между `left` и `0` нужен оператор: `if left counts[ch] 0:` — `SyntaxError`
    [counts.get(ch, 1)]: между `left` и `0` нужен оператор: `if left counts.get(ch, 1) 0:` — `SyntaxError`
  Подсказки:
    1. Быстрый выход: разная длина → сразу `False`. Дальше — один словарь: плюсуем буквы `a`, минусуем буквы `b`.
    2. Частоты хранит словарь `{}`.
    3. Буквы из `b` может не быть в `counts` — значит, её было `0`: `counts.get(ch, 0)`. После вычитания она уйдёт в минус.
    4. Буква лишняя, если её счётчик ушёл в минус.

--- Эталон (время O(n), память O(k)) ---
    | def same_letters(a: str, b: str) -> bool:
    |     if len(a) != len(b):
    |         return False
    |     counts = {}
    |     for ch in a:
    |         counts[ch] = counts.get(ch, 0) + 1
    |     for ch in b:
    |         left = counts.get(ch, 0) - 1
    |         if left < 0:
    |             return False
    |         counts[ch] = left
    |     return True
  Грабли:
    - Сначала сравни длины: без этого `"ab"` и `"a"` пройдут — лишнюю букву `a` никто не заметит
    - `counts[ch] - 1` для буквы, которой нет в `a`, падает с `KeyError`: нужен `counts.get(ch, 0) - 1`
    - `{}` — пустой словарь, а `set()` — множество: частоты хранит только словарь
    - «Отсортировать обе строки и сравнить» — O(n log n), а словарь даёт O(n)
  Вариации:
    - в проде: `collections.Counter(a) == Counter(b)` — на собесе покажи словарь руками
    - `sorted(a) == sorted(b)` тоже верно, но это O(n log n); скажи, что словарь даёт O(n)
    - без учёта регистра и пробелов: сначала нормализуй строки, например `a.lower().replace(" ", "")`
    - без проверки длины: плюсуем `a`, минусуем `b`, а в конце проверяем, что все счётчики стали нулями: `all(v == 0 for v in counts.values())`

```

```
========================================================================
l1.5  Победители соревнования  (Python)
========================================================================
Условие: Дана статистика шагов по дням: список дней, день — список пар `(user_id, steps)`. Верни пару `(user_ids, steps)`: всех, кто набрал **максимум шагов**, **участвуя во все дни без пропусков**, и сам этот максимум. Порядок `user_ids` любой.
Тренирует: два словаря одновременно (сумма + счётчик дней) + фильтр

Примеры:
  find_champions([[(1, 100), (2, 50)], [(1, 100), (2, 50)]]) == ([1], 200)
  find_champions([[(1, 100), (2, 500)], [(1, 100)]]) == ([1], 200)
  r = find_champions([[(1, 100), (2, 100)], [(2, 50), (1, 50)]]); sorted(r[0]) == [1, 2] and r[1] == 150
  find_champions([]) == ([], 0)

--- Теория ---

[Когда два словаря]
  сигнал: По каждому юзеру нужны и сумма, и счётчик
  сигнал: «Во все дни», «без пропусков» — счётчик дней равен числу дней
  сигнал: «Максимум среди тех, кто…» — сначала фильтр, потом максимум
  Два `dict` за один двойной проход: `total_steps` копит шаги, `days_active` — дни участия. O(N) по числу записей, O(U) по числу юзеров.

[Каркас]
  каркас:
    | for day in statistics:
    |     for user, steps in day:
    |         total_steps[user] = total_steps.get(user, 0) + steps
    |         days_active[user] = days_active.get(user, 0) + 1
    | full = [u for u in days_active if days_active[u] == total_days]
    | max_steps = max((total_steps[u] for u in full), default=0)
    | champions = [u for u in full if total_steps[u] == max_steps]
  Фильтр → максимум → сбор всех с этим максимумом. Сначала отфильтруй тех, кто был все дни, и только среди них ищи максимум. `default=0` нужен, когда таких нет: пустой `max()` падает с `ValueError`. Порядок `user_ids` в ответе условие не задаёт; в Python он совпадёт с порядком первого появления юзера, потому что `dict` помнит порядок вставки.

[Грабли]
  грабли: `total_steps[user] += steps` без начального значения: нового юзера в словаре ещё нет, `[[(1, 100)]]` → `KeyError: 1`
  грабли: `max(...)` без `default=0`: никто не был все дни, `[[(1, 100)], [(2, 100)]]` → `ValueError: max() arg is an empty sequence`
  грабли: Максимум по всем юзерам, а не по `full`: `[[(1, 100), (2, 500)], [(1, 100)]]` → `([], 500)` вместо `([1], 200)`
  грабли: Удалять «неполных» из `days_active` прямо в цикле по нему: `[[(1, 100), (2, 500)], [(1, 100)]]` → `RuntimeError: dictionary changed size during iteration`; фильтр — это новый список
  грабли: Вернуть одного победителя вместо списка: при равном максимуме `[[(1, 100), (2, 100)]]` второй потеряется

--- Шаг e1 (дырок: 1) ---
    | def find_champions(statistics: list[list[tuple[int, int]]]) -> tuple[list[int], int]:
    |     total_days = len(statistics)
    |     total_steps = {}
    |     days_active = {}
    |     for day in statistics:
    |         for user, steps in day:
    |             total_steps[user] = total_steps.get(user, 0) + steps
    |             days_active[user] = days_active.get(user, 0) + 1
    |     full = [u for u in days_active if days_active[u] == total_days]
    |     max_steps = max((total_steps[u] for u in [_1_]), default=0)
    |     champions = [u for u in full if total_steps[u] == max_steps]
    |     return (champions, max_steps)
  Ответы:
    _1_ = full
  Банк: [full]  [total_steps]  [days_active]  [total_steps.values()]
  Почему неверно (для облачка в целом):
    [total_steps]: максимум по всем, а юзер с пропуском задаёт недостижимую планку: дни `[(1, 100), (2, 500)]`, `[(1, 100)]` → `([], 500)` вместо `([1], 200)`
    [days_active]: те же ключи, что у `total_steps`, — максимум по всем, а юзер с пропуском задаёт недостижимую планку: дни `[(1, 100), (2, 500)]`, `[(1, 100)]` → `([], 500)` вместо `([1], 200)`
    [total_steps.values()]: перебираем суммы шагов, а не юзеров: дни `[(1, 100), (2, 500)]`, `[(1, 100)]` → `total_steps[200]` падает с `KeyError: 200`
  Подсказки:
    1. «Без пропусков» = число дней участия равно `len(statistics)`. Сначала отфильтруй таких, потом среди них ищи максимум, потом собери всех с этим максимумом (их может быть несколько!).

--- Шаг e2 (дырок: 2) ---
    | def find_champions(statistics: list[list[tuple[int, int]]]) -> tuple[list[int], int]:
    |     total_days = len(statistics)
    |     total_steps = {}
    |     days_active = {}
    |     for day in statistics:
    |         for user, steps in day:
    |             total_steps[user] = total_steps.get(user, 0) + steps
    |             days_active[user] = days_active.get(user, 0) + 1
    |     full = [u for u in days_active if days_active[u] == total_days]
    |     max_steps = max((total_steps[u] for u in [_1_]), default=0)
    |     champions = [u for u in full if total_steps[u] == max_steps]
    |     return ([_2_], max_steps)
  Ответы:
    _1_ = full
    _2_ = champions
  Банк: [full]  [champions]  [total_steps]  [[champions[0]]]  [[max_steps]]
  Почему неверно (для облачка в целом):
    [total_steps]: максимум по всем, а юзер с пропуском задаёт недостижимую планку: дни `[(1, 100), (2, 500)]`, `[(1, 100)]` → `([], 500)` вместо `([1], 200)`
    [[champions[0]]]: при равном максимуме теряется второй победитель: день `[(1, 100), (2, 100)]` → `([1], 100)` вместо `([1, 2], 100)`
    [[max_steps]]: в список победителей попадают шаги, а не `user_id`: день `[(1, 100)]` → `([100], 100)` вместо `([1], 100)`
  Почему неверно в дырке _1_:
    [champions]: `champions` появится строкой ниже: `for u in champions` → `UnboundLocalError` на любом входе
    [[champions[0]]]: `champions` появится строкой ниже: `for u in [champions[0]]` → `UnboundLocalError` на любом входе
    [[max_steps]]: `max_steps` ещё считается в этой самой строке: `for u in [max_steps]` → `UnboundLocalError` на любом входе
  Почему неверно в дырке _2_:
    [full]: вернём всех, кто был все дни, а не только с максимумом: день `[(1, 100), (2, 50)]` → `([1, 2], 100)` вместо `([1], 100)`
    [total_steps]: вернём словарь сумм, а не список победителей: день `[(1, 100)]` → `({1: 100}, 100)` вместо `([1], 100)`
  Подсказки:
    1. «Без пропусков» = число дней участия равно `len(statistics)`. Сначала отфильтруй таких, потом среди них ищи максимум.
    2. Потом собери всех с этим максимумом — их может быть несколько!

--- Шаг e3 (дырок: 4) ---
    | def find_champions(statistics: list[list[tuple[int, int]]]) -> tuple[list[int], int]:
    |     total_days = len(statistics)
    |     total_steps = {}
    |     days_active = {}
    |     for day in statistics:
    |         for user, steps in day:
    |             total_steps[user] = total_steps.get(user, 0) + [_1_]
    |             days_active[user] = days_active.get(user, 0) + [_2_]
    |     full = [u for u in days_active if days_active[u] == [_3_]]
    |     max_steps = max((total_steps[u] for u in [_4_]), default=0)
    |     champions = [u for u in full if total_steps[u] == max_steps]
    |     return (champions, max_steps)
  Ответы:
    _1_ = steps
    _2_ = 1
    _3_ = total_days
    _4_ = full
  Банк: [steps]  [1]  [total_days]  [full]  [total_steps]  [len(days_active)]  [user]
  Почему неверно (для облачка в целом):
    [total_steps]: максимум по всем, а юзер с пропуском задаёт недостижимую планку: дни `[(1, 100), (2, 500)]`, `[(1, 100)]` → `([], 500)` вместо `([1], 200)`
    [len(days_active)]: это число юзеров, а не дней: день `[(1, 100), (2, 50)]` → дней `1`, юзеров `2`, ответ `([], 0)` вместо `([1], 100)`
    [user]: к шагам прибавляется номер юзера: день `[(1, 100), (2, 50)]` → `([2], 2)` вместо `([1], 100)`
  Почему неверно в дырке _1_:
    [1]: `total_steps` считает записи, а не шаги: день `[(1, 100), (2, 50)]` → ничья `1` и `2` с `1` шагом вместо `([1], 100)`
    [total_days]: за каждую запись прибавляется число дней, а не шаги: день `[(1, 100), (2, 50)]` → ничья `1` и `2` с `1` шагом вместо `([1], 100)`
    [full]: `full` появится только после цикла: день `[(1, 100)]` → `UnboundLocalError`
    [total_steps]: к числу не прибавить словарь: день `[(1, 100)]` → `TypeError: unsupported operand type(s) for +: 'int' and 'dict'`
    [len(days_active)]: к шагам прибавляется текущее число юзеров в `days_active`: день `[(1, 100), (2, 50)]` → `([2], 1)` вместо `([1], 100)`
  Почему неверно в дырке _2_:
    [steps]: `days_active` копит шаги, а не дни, и не совпадёт с `total_days`: день `[(1, 100)]` → `days_active[1] = 100`, ответ `([], 0)` вместо `([1], 100)`
    [total_days]: за каждый день прибавляется число дней: дни `[(1, 100)]`, `[(1, 100)]` → `days_active[1] = 4` вместо `2`, ответ `([], 0)` вместо `([1], 200)`
    [full]: `full` появится только после цикла: день `[(1, 100)]` → `UnboundLocalError`
    [total_steps]: к числу не прибавить словарь: день `[(1, 100)]` → `TypeError: unsupported operand type(s) for +: 'int' and 'dict'`
    [len(days_active)]: первая запись юзера прибавляет `0` — пока в `days_active` никого нет: день `[(1, 100)]` → `days_active[1] = 0`, ответ `([], 0)` вместо `([1], 100)`
    [user]: прибавляется номер юзера, а не `1` за день: день `[(2, 100)]` → `days_active[2] = 2` вместо `1`, ответ `([], 0)` вместо `([2], 100)`
  Почему неверно в дырке _3_:
    [steps]: в Python переменная цикла живёт и после него, `steps` — это шаги последней записи: день `[(1, 100)]` → `1 == 100` ложно, ответ `([], 0)` вместо `([1], 100)`
    [1]: остаются те, кто был ровно один день, а не все дни: дни `[(1, 100)]`, `[(1, 100)]` → `([], 0)` вместо `([1], 200)`
    [full]: `full` ещё считается в этой самой строке: день `[(1, 100)]` → `NameError`
    [total_steps]: число дней никогда не равно словарю: день `[(1, 100)]` → `([], 0)` вместо `([1], 100)`
    [user]: в Python переменная цикла живёт и после него, `user` — это номер последнего юзера: дни `[(1, 100)]`, `[(1, 100)]` → `2 == 1` ложно, ответ `([], 0)` вместо `([1], 200)`
  Почему неверно в дырке _4_:
    [steps]: по числу не пройти циклом: день `[(1, 100)]` → `TypeError: 'int' object is not iterable`
    [1]: по числу не пройти циклом: день `[(1, 100)]` → `TypeError: 'int' object is not iterable`
    [total_days]: по числу не пройти циклом: день `[(1, 100)]` → `TypeError: 'int' object is not iterable`
    [len(days_active)]: по числу не пройти циклом: день `[(1, 100)]` → `TypeError: 'int' object is not iterable`
    [user]: по числу не пройти циклом: день `[(1, 100)]` → `TypeError: 'int' object is not iterable`
  Подсказки:
    1. Тебе нужны две вещи на каждого юзера: суммарные шаги и в скольких днях он участвовал. Два словаря за один двойной проход.
    2. Шаги копим по `steps`, дни — по одному за запись.
    3. «Без пропусков» = число дней участия равно `len(statistics)`.
    4. Сначала отфильтруй таких, потом среди них ищи максимум, потом собери всех с этим максимумом (их может быть несколько!).

--- Эталон (время O(N), память O(U)) ---
    | def find_champions(statistics: list[list[tuple[int, int]]]) -> tuple[list[int], int]:
    |     total_days = len(statistics)
    |     total_steps = {}
    |     days_active = {}
    |     for day in statistics:
    |         for user, steps in day:
    |             total_steps[user] = total_steps.get(user, 0) + steps
    |             days_active[user] = days_active.get(user, 0) + 1
    |     full = [u for u in days_active if days_active[u] == total_days]
    |     max_steps = max((total_steps[u] for u in full), default=0)
    |     champions = [u for u in full if total_steps[u] == max_steps]
    |     return (champions, max_steps)
  Грабли:
    - Победителей может быть несколько (равный максимум) → возвращаем список, а не одного
    - Максимум ищем только среди тех, кто был все дни: юзер с пропуском задаст недостижимую планку, и победителей не останется
    - `max()` на пустой последовательности падает с `ValueError` — нужен `default=0`
    - `total_steps[user] += steps` для нового юзера падает с `KeyError`: нужен `total_steps.get(user, 0) + steps`
    - Ответ — кортеж `(champions, max_steps)`: список `[champions, max_steps]` с кортежем не равен
  Вариации:
    - в проде: `collections.Counter` для сумм; но `most_common(1)` вернёт одного, а победителей может быть несколько — на собесе покажи два словаря
    - вместо счётчика дней: множество юзеров каждого дня и их пересечение `set.intersection(*({u for u, _ in day} for day in statistics))` — устойчиво, если юзер встречается в дне дважды
    - нужен порядок по `user_id`: `sorted(champions)` перед возвратом, это ещё O(k log k)

```
