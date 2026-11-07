#!/usr/bin/env python3
"""Build the hand-written (reviewed) Backend lessons: design steps with a code syntax, no compiler.

Usage: python3 tools/build_bk_reviewed.py [--check]
Source of truth: the LESSONS table below (steps written with ⟦ok | ~acceptable⟧ holes, like the generator sidecars).
Output: files/content/bk/<id>.json. Every rule of LessonValidation.kt is re-checked here, plus one stricter one:
every bank chip that is wrong for a hole must have a reason there (hole wrongWhy or chip why), so a swapped
correct chip never shows an empty explanation.
"""
import json
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
OUT = ROOT / "shared/src/commonMain/composeResources/files/content/bk"
HOLE = re.compile(r"⟦(.*?)⟧")
MAX_BANK = 14


def split(code):
    segs, holes, pos = [], [], 0
    for m in HOLE.finditer(code):
        segs.append(code[pos:m.start()])
        pos = m.end()
        alts = [a.strip() for a in m.group(1).split(" | ")]
        holes.append(([a for a in alts if not a.startswith("~")], [a[1:].strip() for a in alts if a.startswith("~")]))
    segs.append(code[pos:])
    return segs, holes


def step(lid, n, title, code, distractors=(), wrong=None, notes=None, hints=()):
    """distractors: [(text, why)]; wrong: {hole(1-based): {text: why}}; notes: {acceptable text: note}."""
    segs, holes = split(code)
    wrong, notes = wrong or {}, notes or {}
    bank = []
    for ok, _ in holes:
        bank.append({"text": ok[0]})
    for ok, acc in holes:
        for t in ok[1:] + acc:
            if t not in [c["text"] for c in bank]:
                bank.append({"text": t})
    for t, why in distractors:
        bank.append({"text": t, "why": why})
    out_holes = []
    for h, (ok, acc) in enumerate(holes, 1):
        hole = {"ok": ok}
        if acc:
            hole["acceptable"] = [{"text": t, "note": notes[t]} for t in acc]
        if wrong.get(h):
            hole["wrongWhy"] = wrong[h]
        out_holes.append(hole)
    return {"id": "%s.e%d" % (lid, n), "title": title, "segments": segs, "holes": out_holes, "bank": bank, "hints": list(hints)}


def assembled(ex):
    return "".join(s + (ex["holes"][i]["ok"][0] if i < len(ex["holes"]) else "") for i, s in enumerate(ex["segments"]))


def validate(lesson):
    """Port of LessonValidation.validate() for design lessons with a code syntax, plus the full-reason rule."""
    errors = []
    exs = lesson["exercises"]
    if not 2 <= len(exs) <= 4:
        errors.append("lesson: %d exercises" % len(exs))
    if len({e["id"] for e in exs}) != len(exs):
        errors.append("lesson: duplicate exercise ids")
    for i, ex in enumerate(exs):
        e = ex["id"]
        if not e.startswith(lesson["id"] + "."):
            errors.append(e + ": id prefix")
        if len(ex["segments"]) != len(ex["holes"]) + 1:
            errors.append(e + ": segments != holes + 1")
        bank = [c["text"] for c in ex["bank"]]
        if len(bank) > MAX_BANK:
            errors.append("%s: bank %d" % (e, len(bank)))
        used = set()
        for h, hole in enumerate(ex["holes"]):
            if not hole["ok"]:
                errors.append("%s: hole %d empty ok" % (e, h))
            acc = [a["text"] for a in hole.get("acceptable", [])]
            for t in hole["ok"] + acc:
                used.add(t)
                if t not in bank:
                    errors.append("%s: hole %d '%s' not in bank" % (e, h, t))
            if set(acc) & set(hole["ok"]):
                errors.append("%s: hole %d acceptable also ok" % (e, h))
            for k, v in hole.get("wrongWhy", {}).items():
                if k not in bank:
                    errors.append("%s: hole %d wrongWhy '%s' not in bank" % (e, h, k))
                if k in hole["ok"] or k in acc:
                    errors.append("%s: hole %d wrongWhy '%s' is correct" % (e, h, k))
                if not v.strip():
                    errors.append("%s: hole %d blank why for '%s'" % (e, h, k))
            # stricter than the app: every wrong chip for this hole has a reason
            for c in ex["bank"]:
                if c["text"] in hole["ok"] or c["text"] in acc:
                    continue
                if not (hole.get("wrongWhy", {}).get(c["text"]) or c.get("why")):
                    errors.append("%s: hole %d: no reason for '%s'" % (e, h + 1, c["text"]))
        need = {}
        for hole in ex["holes"]:
            need[hole["ok"][0]] = need.get(hole["ok"][0], 0) + 1
        for t, n in need.items():
            have = bank.count(t)
            if 0 < have < n:
                errors.append("%s: '%s' needed %d, bank has %d" % (e, t, n, have))
        for c in ex["bank"]:
            if c["text"] not in used and not c.get("why"):
                for h, hole in enumerate(ex["holes"]):
                    if not hole.get("wrongWhy", {}).get(c["text"]):
                        errors.append("%s: distractor '%s' no why for hole %d" % (e, c["text"], h))
        if i == len(exs) - 1 and assembled(ex) != lesson["solution"]["code"]:
            errors.append(e + ": last step != solution.code")
    if lesson.get("syntax") in (None, "plain"):
        errors.append("lesson: backend lessons need a code syntax")
    return errors


def lesson(lid, level, title, statement, syntax, theory, steps, solution):
    return {"schema": 1, "id": lid, "kind": "design", "level": level, "title": title, "statement": statement,
            "theory": theory, "exercises": steps, "solution": solution, "syntax": syntax}


# ---------------------------------------------------------------- bk3.1
CTRL = """@RestController
@RequestMapping("/users")
class UserController(private val users: UserService) {

    // GET /users/7
    @GetMapping("/{id}")
    fun byId(@PathVariable id: Long): UserDto =
        users.byId(id)

    // GET /users?limit=20
    @GetMapping
    fun list(
        @RequestParam(defaultValue = "20") limit: Int
    ): List<UserDto> = users.list(limit)

    // POST /users + JSON в теле
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun create(@RequestBody body: CreateUser): UserDto =
        users.create(body)
}"""

W_CONTROLLER = "без `@ResponseBody` Spring ищет HTML-шаблон по имени результата, а не отдаёт JSON"
W_SERVICE = "это слой бизнес-логики: HTTP-запросы в `@Service` не приходят"
W_REQPARAM_PATH = "`?id=7` в строке запроса, а `7` в `/users/7` — часть пути: будет 400 `Required parameter 'id'`"
W_REQBODY_GET = "тело у GET пустое: 400 `Required request body is missing`"
W_PATHVAR_QUERY = "в пути `/users` нет `{limit}`: Spring не найдёт переменную и упадёт с 500"
W_GET_FOR_POST = "GET тело не несёт, и клиент с `POST /users` получит 405 Method Not Allowed"
W_POST_FOR_GET = "чтение делают GET: `GET /users` на `@PostMapping` ответит 405"
W_OK_STATUS = "200 — ответ по умолчанию; о созданном ресурсе клиенту говорят 201 Created"
W_ACCEPTED = "202 значит «принял, сделаю позже»; для синхронного создания это неправда"
W_REQPARAM_BODY = "JSON лежит в теле, а не в `?body=`: будет 400, объект не соберётся"
W_ANN_CLASS = "это аннотация параметра: на класс её не повесить, не скомпилируется"
W_ANN_PARAM = "это аннотация класса: параметр ею не пометить, не скомпилируется"
W_ANN_METHOD_ON_PARAM = "это аннотация метода: на параметр её не повесить, не скомпилируется"
W_ANN_PARAM_ON_METHOD = "это аннотация параметра: на метод её не повесить, не скомпилируется"

bk31 = lesson(
    "bk3.1", 3, "Контроллер и роутинг",
    "Сделай REST-ручки для пользователей на Spring Boot: `GET /users/{id}`, `GET /users?limit=20`, `POST /users`.\n"
    "🎯 Тренирует: аннотации маршрутов, откуда берётся параметр, статус ответа",
    "kotlin",
    [
        {"title": "Что есть что",
         "body": "**Контроллер** — класс, который принимает HTTP-запрос, достаёт параметры, зовёт сервис и отдаёт ответ.\n\n"
                 "**`@RestController`** — пометка «это контроллер, результат метода уходит в тело ответа как JSON». Голый `@Controller` вместо JSON ищет HTML-шаблон.\n\n"
                 "**`@RequestMapping(\"/users\")`** — общий префикс пути для всех методов класса.\n\n"
                 "**`@GetMapping`, `@PostMapping`** — метод HTTP плюс хвост пути: `@GetMapping(\"/{id}\")` это `GET /users/{id}`. Без аргумента — сам `/users`.\n\n"
                 "**Бин** — объект, которым управляет Spring. `UserService` в конструкторе Spring подставит сам: это внедрение зависимостей (DI).\n\n"
                 "**DTO** — класс только для передачи данных по сети: `UserDto`, `CreateUser`. Подробнее — заметка «01 Словарь Spring» в базе знаний."},
        {"title": "Откуда берётся параметр",
         "body": "**В пути** `GET /users/7` → `@PathVariable id: Long`. Имя параметра совпадает с `{id}`.\n\n"
                 "**В строке запроса** `GET /users?limit=20` → `@RequestParam limit: Int`. По умолчанию обязателен: без `defaultValue` запрос без параметра получит 400.\n\n"
                 "**В теле** `POST /users` + JSON → `@RequestBody body: CreateUser`. Jackson соберёт объект из JSON.\n\n"
                 "Значения аннотаций — строки: `defaultValue = \"20\"`, а не `20`."},
        {"title": "Статус ответа",
         "body": "По умолчанию контроллер отвечает **200 OK**. Для созданного ресурса принято **201 Created**: `@ResponseStatus(HttpStatus.CREATED)` на методе или `ResponseEntity.status(201).body(dto)`.\n\n"
                 "**202 Accepted** — «принял, сделаю позже», для фоновой обработки. **405 Method Not Allowed** — путь есть, а метода такого нет: `POST` на `@GetMapping`.\n\n"
                 "Когда статус зависит от результата (нашли / не нашли), метод возвращает `ResponseEntity`. Чаще сервис бросает исключение, а один `@ControllerAdvice` превращает его в 404 для всех ручек — это урок `bk3.4`."},
        {"title": "Грабли",
         "pitfalls": ["`@Controller` без `@ResponseBody`: вместо JSON — поиск HTML-шаблона",
                      "`@RequestParam` для `/users/7`: Spring ищет `?id=7` и отвечает 400",
                      "`@RequestParam` без `defaultValue`: клиенты без параметра получают 400",
                      "`@PostMapping` без `@RequestBody`: тело игнорируется, поля объекта пустые",
                      "Имя в `{id}` и имя параметра разошлись: 500 при каждом запросе"]},
    ],
    [
        step("bk3.1", 1, "Шаг 1 · Класс и первый GET",
             """⟦@RestController⟧
@RequestMapping("/users")
class UserController(private val users: UserService) {

    // GET /users/7
    @GetMapping("/{id}")
    fun byId(⟦@PathVariable⟧ id: Long): UserDto =
        users.byId(id)
}""",
             distractors=[("@Controller", W_CONTROLLER), ("@Service", W_SERVICE),
                          ("@RequestParam", W_REQPARAM_PATH), ("@RequestBody", W_REQBODY_GET)],
             wrong={1: {"@PathVariable": W_ANN_CLASS, "@RequestParam": W_ANN_CLASS, "@RequestBody": W_ANN_CLASS},
                    2: {"@RestController": W_ANN_PARAM, "@Controller": W_ANN_PARAM, "@Service": W_ANN_PARAM}},
             hints=["Класс, который отдаёт JSON, помечают одной аннотацией — и это не `@Controller`.",
                    "`7` в `/users/7` — часть пути, а не строки запроса."]),
        step("bk3.1", 2, "Шаг 2 · Список с параметром",
             """// GET /users?limit=20
⟦@GetMapping⟧
fun list(
    ⟦@RequestParam⟧(defaultValue = ⟦"20"⟧) limit: Int
): List<UserDto> = users.list(limit)""",
             distractors=[("@PostMapping", W_POST_FOR_GET),
                          ("@GetMapping(\"/{id}\")", "это маршрут одного пользователя; список живёт на самом `/users`, и `/users?limit=20` сюда не попадёт"),
                          ("@PathVariable", W_PATHVAR_QUERY),
                          ("20", "значение аннотации — строка: `defaultValue = 20` не скомпилируется")],
             wrong={1: {"@RequestParam": W_ANN_PARAM_ON_METHOD, "@PathVariable": W_ANN_PARAM_ON_METHOD,
                        "\"20\"": "над методом стоит аннотация, а не строка: не скомпилируется",
                        "20": "над методом стоит аннотация, а не число: не скомпилируется"},
                    2: {"@GetMapping": W_ANN_METHOD_ON_PARAM, "@PostMapping": W_ANN_METHOD_ON_PARAM,
                        "@GetMapping(\"/{id}\")": W_ANN_METHOD_ON_PARAM,
                        "\"20\"": "перед скобками нужна аннотация параметра, а не строка: не скомпилируется",
                        "20": "перед скобками нужна аннотация параметра, а не число: не скомпилируется"},
                    3: {"@GetMapping": "внутри `defaultValue = …` ждут строку, а не аннотацию: не скомпилируется",
                        "@PostMapping": "внутри `defaultValue = …` ждут строку, а не аннотацию: не скомпилируется",
                        "@GetMapping(\"/{id}\")": "внутри `defaultValue = …` ждут строку, а не аннотацию: не скомпилируется",
                        "@RequestParam": "внутри `defaultValue = …` ждут строку, а не аннотацию: не скомпилируется",
                        "@PathVariable": "внутри `defaultValue = …` ждут строку, а не аннотацию: не скомпилируется"}},
             hints=["Список читают, а не создают; хвоста пути у него нет.",
                    "`?limit=20` — строка запроса, и значение по умолчанию пишется в кавычках."]),
        step("bk3.1", 3, "Шаг 3 · Создание",
             """// POST /users + JSON в теле
⟦@PostMapping⟧
⟦@ResponseStatus(HttpStatus.CREATED)⟧
fun create(⟦@RequestBody⟧ body: CreateUser): UserDto =
    users.create(body)""",
             distractors=[("@GetMapping", W_GET_FOR_POST),
                          ("@ResponseStatus(HttpStatus.OK)", W_OK_STATUS),
                          ("@ResponseStatus(HttpStatus.ACCEPTED)", W_ACCEPTED),
                          ("@RequestParam", W_REQPARAM_BODY)],
             wrong={1: {"@ResponseStatus(HttpStatus.CREATED)": "статус без маршрута: сначала скажи Spring, что это `POST /users`",
                        "@ResponseStatus(HttpStatus.OK)": "статус без маршрута: сначала скажи Spring, что это `POST /users`",
                        "@ResponseStatus(HttpStatus.ACCEPTED)": "статус без маршрута: сначала скажи Spring, что это `POST /users`",
                        "@RequestBody": W_ANN_PARAM_ON_METHOD, "@RequestParam": W_ANN_PARAM_ON_METHOD},
                    2: {"@PostMapping": "маршрут уже задан строкой выше; здесь нужен статус 201 для созданного ресурса",
                        "@GetMapping": "маршрут уже задан строкой выше; здесь нужен статус 201 для созданного ресурса",
                        "@RequestBody": W_ANN_PARAM_ON_METHOD, "@RequestParam": W_ANN_PARAM_ON_METHOD},
                    3: {"@PostMapping": W_ANN_METHOD_ON_PARAM, "@GetMapping": W_ANN_METHOD_ON_PARAM,
                        "@ResponseStatus(HttpStatus.CREATED)": W_ANN_METHOD_ON_PARAM,
                        "@ResponseStatus(HttpStatus.OK)": W_ANN_METHOD_ON_PARAM,
                        "@ResponseStatus(HttpStatus.ACCEPTED)": W_ANN_METHOD_ON_PARAM}},
             hints=["Создают через POST, созданному ресурсу отвечают 201.",
                    "JSON приходит в теле запроса — есть ровно одна аннотация для тела."]),
        step("bk3.1", 4, "Шаг 4 · Весь контроллер",
             """⟦@RestController⟧
@RequestMapping("/users")
class UserController(private val users: UserService) {

    // GET /users/7
    @GetMapping("/{id}")
    fun byId(⟦@PathVariable⟧ id: Long): UserDto =
        users.byId(id)

    // GET /users?limit=20
    @GetMapping
    fun list(
        ⟦@RequestParam⟧(defaultValue = "20") limit: Int
    ): List<UserDto> = users.list(limit)

    // POST /users + JSON в теле
    ⟦@PostMapping⟧
    ⟦@ResponseStatus(HttpStatus.CREATED)⟧
    fun create(⟦@RequestBody⟧ body: CreateUser): UserDto =
        users.create(body)
}""",
             distractors=[("@Controller", W_CONTROLLER), ("@GetMapping", W_GET_FOR_POST),
                          ("@ResponseStatus(HttpStatus.OK)", W_OK_STATUS)],
             wrong={1: {"@PathVariable": W_ANN_CLASS, "@RequestParam": W_ANN_CLASS, "@RequestBody": W_ANN_CLASS,
                        "@PostMapping": "это аннотация метода: на класс её не повесить, не скомпилируется",
                        "@GetMapping": "это аннотация метода: на класс её не повесить, не скомпилируется",
                        "@ResponseStatus(HttpStatus.CREATED)": "статус без контроллера: класс сначала надо объявить `@RestController`",
                        "@ResponseStatus(HttpStatus.OK)": "статус без контроллера: класс сначала надо объявить `@RestController`"},
                    2: {"@RestController": W_ANN_PARAM, "@Controller": W_ANN_PARAM,
                        "@RequestParam": W_REQPARAM_PATH,
                        "@RequestBody": W_REQBODY_GET,
                        "@PostMapping": W_ANN_METHOD_ON_PARAM, "@GetMapping": W_ANN_METHOD_ON_PARAM,
                        "@ResponseStatus(HttpStatus.CREATED)": W_ANN_METHOD_ON_PARAM,
                        "@ResponseStatus(HttpStatus.OK)": W_ANN_METHOD_ON_PARAM},
                    3: {"@RestController": W_ANN_PARAM, "@Controller": W_ANN_PARAM,
                        "@PathVariable": W_PATHVAR_QUERY,
                        "@RequestBody": "`?limit=20` — строка запроса, а не тело: GET без тела даст 400",
                        "@PostMapping": W_ANN_METHOD_ON_PARAM, "@GetMapping": W_ANN_METHOD_ON_PARAM,
                        "@ResponseStatus(HttpStatus.CREATED)": W_ANN_METHOD_ON_PARAM,
                        "@ResponseStatus(HttpStatus.OK)": W_ANN_METHOD_ON_PARAM},
                    4: {"@RestController": "контроллер уже объявлен на классе; здесь нужен маршрут `POST /users`",
                        "@Controller": "контроллер уже объявлен на классе; здесь нужен маршрут `POST /users`",
                        "@PathVariable": W_ANN_PARAM_ON_METHOD, "@RequestParam": W_ANN_PARAM_ON_METHOD,
                        "@RequestBody": W_ANN_PARAM_ON_METHOD,
                        "@ResponseStatus(HttpStatus.CREATED)": "статус без маршрута: сначала скажи Spring, что это `POST /users`",
                        "@ResponseStatus(HttpStatus.OK)": "статус без маршрута: сначала скажи Spring, что это `POST /users`"},
                    5: {"@RestController": "контроллер уже объявлен на классе; здесь нужен статус 201 для созданного ресурса",
                        "@Controller": "контроллер уже объявлен на классе; здесь нужен статус 201 для созданного ресурса",
                        "@PathVariable": W_ANN_PARAM_ON_METHOD, "@RequestParam": W_ANN_PARAM_ON_METHOD,
                        "@RequestBody": W_ANN_PARAM_ON_METHOD,
                        "@PostMapping": "маршрут уже задан строкой выше; здесь нужен статус 201 для созданного ресурса",
                        "@GetMapping": "маршрут уже задан строкой выше; здесь нужен статус 201 для созданного ресурса"},
                    6: {"@RestController": W_ANN_PARAM, "@Controller": W_ANN_PARAM,
                        "@PathVariable": "в пути `/users` нет переменной: Spring не найдёт её и упадёт с 500",
                        "@RequestParam": W_REQPARAM_BODY,
                        "@PostMapping": W_ANN_METHOD_ON_PARAM, "@GetMapping": W_ANN_METHOD_ON_PARAM,
                        "@ResponseStatus(HttpStatus.CREATED)": W_ANN_METHOD_ON_PARAM,
                        "@ResponseStatus(HttpStatus.OK)": W_ANN_METHOD_ON_PARAM}},
             hints=["Класс → контроллер; путь → `@PathVariable`; строка запроса → `@RequestParam`; тело → `@RequestBody`.",
                    "Создание: `POST` и 201."]),
    ],
    {"code": CTRL, "time": "", "space": "",
     "pitfalls": ["`@Controller` без `@ResponseBody`: вместо JSON — поиск HTML-шаблона",
                  "`@RequestParam` для `/users/7`: Spring ищет `?id=7` и отвечает 400",
                  "`@RequestParam` без `defaultValue`: клиенты без параметра получают 400",
                  "`@PostMapping` без `@RequestBody`: тело игнорируется",
                  "Контроллер ходит в репозиторий напрямую: логика расползается по ручкам (урок `bk3.2`)"],
     "variations": ["Как вернуть 404, если пользователя нет, не трогая каждый метод?",
                    "Как проверить `CreateUser` на пустое имя до вызова сервиса?",
                    "Чем `PUT` отличается от `PATCH`, и какой статус у `DELETE`?",
                    "Как сделать `byId` suspend и что это даёт на Spring WebFlux?"]},
)

# ---------------------------------------------------------------- bk5.1
SQL3 = """SELECT u.city,
       COUNT(*) AS orders,
       SUM(o.amount) AS total
FROM orders o
JOIN users u ON u.id = o.user_id
GROUP BY u.city
HAVING SUM(o.amount) > 10000
ORDER BY total DESC;"""

W_HAVING_NO_GROUP = "`HAVING` фильтрует группы после `GROUP BY`; без группировки здесь ошибка синтаксиса"
W_GROUP_FOR_ORDER = "`GROUP BY name` без агрегатов схлопнет одинаковые имена и потеряет строки; нужна сортировка"
W_ORDER_FOR_WHERE = "сортировка не фильтрует: в выдаче останутся все города"
W_WHERE_FOR_ORDER = "`WHERE` уже есть выше; второй `WHERE` — ошибка синтаксиса, да и фильтр не сортирует"

bk51 = lesson(
    "bk5.1", 5, "SQL: выборка, JOIN, группировка",
    "Таблицы `users(id, name, city)` и `orders(id, user_id, amount, created_at)`. Собери три запроса: пользователи из Москвы, "
    "крупные заказы с именем клиента, оборот по городам.\n"
    "🎯 Тренирует: порядок выполнения, `WHERE` против `HAVING`, `JOIN … ON`, агрегаты и `GROUP BY`",
    "sql",
    [
        {"title": "Порядок выполнения",
         "body": "База выполняет запрос не сверху вниз:\n\n`FROM → JOIN → WHERE → GROUP BY → HAVING → SELECT → ORDER BY → LIMIT`\n\n"
                 "Отсюда правила: **`WHERE`** фильтрует строки до группировки и не видит агрегаты; **`HAVING`** фильтрует уже группы; "
                 "псевдоним из `SELECT` (`AS total`) виден в `ORDER BY`, но не в `WHERE`."},
        {"title": "Выборка и соединение",
         "body": "**`WHERE city = 'Москва'`** — оставить строки, где условие истинно. Строки в одинарных кавычках.\n\n"
                 "**`ORDER BY name`** — сортировка, `DESC` по убыванию. **`LIMIT 10`** — первые десять после сортировки; без `ORDER BY` порядок не гарантирован.\n\n"
                 "**`JOIN users u ON u.id = o.user_id`** — какие строки двух таблиц считать парой: заказ и его пользователь. "
                 "`JOIN` (он же `INNER JOIN`) оставляет только пары; **`LEFT JOIN`** оставляет все строки левой таблицы, справа подставляя `NULL`.\n\n"
                 "`o` и `u` — псевдонимы таблиц, чтобы не писать `orders.user_id`."},
        {"title": "Группировка и агрегаты",
         "body": "**`GROUP BY u.city`** — одна строка результата на город.\n\n"
                 "Агрегаты: **`COUNT(*)`** — строк в группе, **`SUM`**, **`AVG`**, **`MIN`**, **`MAX`**. `COUNT(column)` не считает `NULL`.\n\n"
                 "Всё, что в `SELECT` не агрегат, обязано стоять в `GROUP BY`, иначе Postgres откажет: «column must appear in the GROUP BY clause».\n\n"
                 "**`HAVING SUM(o.amount) > 10000`** — фильтр по агрегату, уже после группировки."},
        {"title": "Грабли",
         "pitfalls": ["`WHERE` с агрегатом: «aggregate functions are not allowed in WHERE» — нужен `HAVING`",
                      "`GROUP BY` вместо `ORDER BY`: схлопнет одинаковые значения и потеряет строки",
                      "`ON u.id = o.id` — id пользователя сравнили с номером заказа: случайные пары",
                      "`LIMIT` без `ORDER BY`: «первые 10» каждый раз разные",
                      "`AVG` вместо `SUM`: средний чек вместо оборота, числа правдоподобные"]},
    ],
    [
        step("bk5.1", 1, "Шаг 1 · Выборка с фильтром",
             """SELECT name, city
FROM users
⟦WHERE⟧ city = 'Москва'
⟦ORDER BY⟧ name
LIMIT 10;""",
             distractors=[("HAVING", W_HAVING_NO_GROUP), ("GROUP BY", W_GROUP_FOR_ORDER)],
             wrong={1: {"ORDER BY": W_ORDER_FOR_WHERE, "GROUP BY": "группировка не фильтрует, а без агрегатов ещё и схлопнет города: нужен `WHERE`"},
                    2: {"WHERE": W_WHERE_FOR_ORDER, "HAVING": W_HAVING_NO_GROUP}},
             hints=["Фильтр строк до группировки — одно слово, и это не `HAVING`.",
                    "`LIMIT 10` без сортировки отдаёт случайную десятку."]),
        step("bk5.1", 2, "Шаг 2 · Соединение",
             """SELECT o.id, u.name, o.amount
FROM orders o
⟦JOIN | ~INNER JOIN⟧ users u ⟦ON⟧ u.id = ⟦o.user_id⟧
WHERE o.amount > 1000;""",
             distractors=[("LEFT JOIN", "оставит заказы без пользователя с `NULL` вместо имени; для «заказ + его клиент» нужны только пары"),
                          ("WHERE", "условие соединения пишут в `ON`; `JOIN users u WHERE` — ошибка синтаксиса"),
                          ("o.id", "`u.id = o.id` сравнит id пользователя с номером заказа: случайные пары"),
                          ("u.user_id", "в `users` нет колонки `user_id`: «column u.user_id does not exist»")],
             notes={"INNER JOIN": "то же самое, что `JOIN`: слово `INNER` можно опустить"},
             wrong={1: {"ON": "сначала какую таблицу присоединяем, потом по какому условию", "o.user_id": "здесь нужно слово соединения, а не колонка",
                        "WHERE": "фильтр строк идёт после соединения; таблицу присоединяют через `JOIN`", "o.id": "здесь нужно слово соединения, а не колонка",
                        "u.user_id": "здесь нужно слово соединения, а не колонка"},
                    2: {"JOIN": "таблица уже присоединена; условие пары вводит `ON`", "INNER JOIN": "таблица уже присоединена; условие пары вводит `ON`",
                        "LEFT JOIN": "таблица уже присоединена; условие пары вводит `ON`", "o.user_id": "перед условием нужно слово `ON`, а не колонка",
                        "o.id": "перед условием нужно слово `ON`, а не колонка", "u.user_id": "перед условием нужно слово `ON`, а не колонка"},
                    3: {"JOIN": "справа от `=` ждут колонку, а не слово соединения", "INNER JOIN": "справа от `=` ждут колонку, а не слово соединения",
                        "LEFT JOIN": "справа от `=` ждут колонку, а не слово соединения", "ON": "справа от `=` ждут колонку, а не слово `ON`",
                        "WHERE": "справа от `=` ждут колонку, а не слово `WHERE`"}},
             hints=["Пара «заказ — его пользователь»: внешний ключ заказа равен id пользователя.",
                    "Условие соединения вводит `ON`, фильтр строк — `WHERE`."]),
        step("bk5.1", 3, "Шаг 3 · Группировка",
             """SELECT u.city,
       ⟦COUNT(*) | ~COUNT(o.id)⟧ AS orders,
       ⟦SUM(o.amount)⟧ AS total
FROM orders o
JOIN users u ON u.id = o.user_id
⟦GROUP BY⟧ u.city
⟦HAVING⟧ SUM(o.amount) > 10000
ORDER BY total DESC;""",
             distractors=[("AVG(o.amount)", "средний чек, а нужен оборот: для чеков 100 и 300 даст 200 вместо 400"),
                          ("WHERE", "`WHERE` не видит агрегаты: «aggregate functions are not allowed in WHERE»"),
                          ("ORDER BY", "сортировка не делает групп: без `GROUP BY` Postgres откажет, а `ORDER BY` уже стоит ниже")],
             notes={"COUNT(o.id)": "то же число: `id` заказа не бывает `NULL`; `COUNT(*)` короче и не зависит от колонки"},
             wrong={1: {"SUM(o.amount)": "это оборот, а колонка `orders` — число заказов", "AVG(o.amount)": "это средний чек, а колонка `orders` — число заказов",
                        "GROUP BY": "в списке `SELECT` нужен агрегат, а не ключевое слово", "HAVING": "в списке `SELECT` нужен агрегат, а не ключевое слово",
                        "WHERE": "в списке `SELECT` нужен агрегат, а не ключевое слово", "ORDER BY": "в списке `SELECT` нужен агрегат, а не ключевое слово"},
                    2: {"COUNT(*)": "это число заказов, а колонка `total` — сумма", "COUNT(o.id)": "это число заказов, а колонка `total` — сумма",
                        "GROUP BY": "в списке `SELECT` нужен агрегат, а не ключевое слово", "HAVING": "в списке `SELECT` нужен агрегат, а не ключевое слово",
                        "WHERE": "в списке `SELECT` нужен агрегат, а не ключевое слово", "ORDER BY": "в списке `SELECT` нужен агрегат, а не ключевое слово"},
                    3: {"HAVING": "сначала группы, потом фильтр по ним: `HAVING` без `GROUP BY` здесь ошибка", "COUNT(*)": "после `JOIN` ждут ключевое слово, а не агрегат",
                        "COUNT(o.id)": "после `JOIN` ждут ключевое слово, а не агрегат", "SUM(o.amount)": "после `JOIN` ждут ключевое слово, а не агрегат",
                        "AVG(o.amount)": "после `JOIN` ждут ключевое слово, а не агрегат",
                        "WHERE": "`WHERE u.city` без условия — ошибка; строка должна задавать группы"},
                    4: {"GROUP BY": "группы уже заданы строкой выше; `GROUP BY SUM(...)` — ошибка", "COUNT(*)": "перед условием ждут ключевое слово, а не агрегат",
                        "COUNT(o.id)": "перед условием ждут ключевое слово, а не агрегат", "SUM(o.amount)": "перед условием ждут ключевое слово, а не агрегат",
                        "AVG(o.amount)": "перед условием ждут ключевое слово, а не агрегат"}},
             hints=["Число строк в группе и сумма по колонке — два разных агрегата.",
                    "Фильтр по агрегату живёт после группировки."]),
    ],
    {"code": SQL3, "time": "", "space": "",
     "pitfalls": ["`WHERE` с агрегатом: «aggregate functions are not allowed in WHERE»",
                  "`GROUP BY` вместо `ORDER BY`: потерянные строки",
                  "`ON u.id = o.id`: случайные пары вместо связи",
                  "`LIMIT` без `ORDER BY`: нестабильная выдача",
                  "`COUNT(*)` и `COUNT(o.amount)` совпадают только пока `amount` не бывает `NULL`"],
     "variations": ["Почему запрос с `WHERE city = …` медленный на миллионе строк и как поможет индекс?",
                    "Как показать и города без заказов? (`LEFT JOIN` от `users`)",
                    "Что такое N+1 и как его увидеть в логах Hibernate?",
                    "Чем `offset`-пагинация хуже keyset?"]},
)

LESSONS = [bk31, bk51]


def main(argv=None):
    check = "--check" in (argv or sys.argv[1:])
    bad = 0
    for l in LESSONS:
        errors = validate(l)
        if errors:
            bad += 1
            for e in errors:
                print("ERROR %s: %s" % (l["id"], e))
            continue
        text = json.dumps(l, ensure_ascii=False, indent=2) + "\n"
        path = OUT / (l["id"] + ".json")
        if check:
            if not path.is_file() or path.read_text(encoding="utf-8") != text:
                print("stale: " + str(path.relative_to(ROOT)))
                bad += 1
        else:
            path.write_text(text, encoding="utf-8", newline="\n")
            print("wrote " + str(path.relative_to(ROOT)))
    return 1 if bad else 0


if __name__ == "__main__":
    sys.exit(main())
