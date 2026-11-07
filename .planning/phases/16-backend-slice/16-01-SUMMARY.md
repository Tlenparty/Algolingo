---
phase: 16
plan: 01
status: complete (customer review + device run pending)
date: 2026-11-07
tags: [backend, content, kb, spring, sql, generator]
---

# 16-01 Summary — Backend Slice

## Knowledge base, section «Backend» (files/kb, ids = n + sha1(title)[:8])

- nc2e84ad0 «00 Программа трека Backend» — 11 levels, lesson types, first slice
- n75db2eb9 «01 Словарь Spring» — Spring Boot, beans/DI, web layer, data, ops
- n55334960 «02 Spring: контроллер и роутинг» — theory for bk3.1
- nbca10892 «03 SQL: SELECT, JOIN, GROUP BY» — theory for bk5.1
- nd0d126e5 «04 Тренажёр Backend: задачи на Kotlin» — note-05 grammar; `## 6.1 Token bucket` is the generator source

## Lessons (files/content/bk, hand-maintained bk/index.json: bk3.1 → bk5.1 → bk6.1)

- **bk3.1 «Контроллер и роутинг»** — design, syntax kotlin, 4 steps (2/3/3/6 holes): `@RestController`/`@PathVariable`, list with `@RequestParam(defaultValue = "20")`, `@PostMapping` + 201 + `@RequestBody`, whole controller. Built by `tools/build_bk_reviewed.py`.
- **bk5.1 «SQL: выборка, JOIN, группировка»** — design, syntax sql, 3 steps (2/3/4 holes): WHERE/ORDER BY, JOIN…ON with `~INNER JOIN`, COUNT/SUM + GROUP BY/HAVING with `~COUNT(o.id)`. Same builder.
- **bk6.1 «Token bucket»** — code, syntax kotlin, generated from `content/authoring/bk/bk6.1.md` (1/2/3 holes + «Собери решение из строк»). Reference and every wrong chip compiled and run on the JVM: 29 wrong programs, claims hold (compile/wrong as tagged), control runs clean. Oracle in `checks:` uses Long milli-tokens with 250 ms steps and rates 1/2/4, so no Double rounding.

## Generator changes (tools/gen_content.py)

- `content/authoring/bk/*.md`, ids `bk<level>.<n>`, note `kb/nd0d126e5.md`, output `files/content/bk/<id>.json` with `"syntax": "kotlin"`; bk/index.json is not written, the id must already be in it (G4); `line_holes` accepts an indented last `fun`; ReferenceSolutionsTest objects `BK6_1_v0`. Tests: `BackendTrack` in tools/test_gen_content.py. Existing l-lesson outputs byte-identical (`--check` green).

## Open

- Customer review of the three lessons and the five notes (blocking checkpoint by the track's rules).
- `tools/smoke.sh` (`backend()` test) on the Android emulator and iOS simulator; Gradle unit tests.
