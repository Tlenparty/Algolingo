---
phase: 06-level-1-content-hash-map-set
plan: 02
subsystem: content
tags: [content, level-1, hash-map]
requires: [06-01 (examples() in variant objects, getOrDefault shim)]
provides: [lesson l1.2, lesson l1.3, 7-lesson index]
affects: [06-03, 06-04]
tech-stack:
  added: []
  patterns: []
key-files:
  created:
    - content/authoring/l1.2.md
    - content/authoring/l1.3.md
    - shared/src/commonMain/composeResources/files/content/lessons/l1.2.json
    - shared/src/commonMain/composeResources/files/content/lessons/l1.3.json
  modified:
    - shared/src/commonMain/composeResources/files/content/index.json
    - shared/src/commonTest/kotlin/com/tlenparty/algolingo/lesson/ReferenceSolutionsTest.kt
decisions:
  - "l1.2 overrides trains/pitfalls in the sidecar: the note splits its one-sentence pitfall on ';' and puts ** in trains"
  - "l1.2 cnt.keys why is worded as JVM HashMap behaviour (Native HashMap iterates in insertion order)"
  - "Compile-error whys for ==/Int?-vs-String/Set/Char and self-referencing val left were checked with kotlinc-native 2.4.21 (K2 frontend)"
metrics:
  duration: 15min
  completed: 2026-10-22
  tasks: 2
  files: 6
---

# Phase 6 Plan 02: lessons 1.2 «Первый уникальный символ» and 1.3 «Анаграмма» Summary

Both lessons now come after 1.1. Each has 3 theory cards and a ladder of 1 → 2 → 4 holes. The index lists l0.1 to l0.4, then l1.1, l1.2 and l1.3. The level-0 and l1.1 JSON files are byte-identical. allTests passes with 297 tests and 0 failures (host + iOS). ReferenceSolutionsTest has 11 tests on each target. `:androidApp:assembleDebug` is green.

- **l1.2:** the banks hold 3, 3 and 6 chips. The holes are: second-pass source `s`, start value `0`, test `1`, and miss value `null`. The `//` comment from the note is dropped.
- **l1.3:** the banks hold 4, 4 and 8 chips. The holes are: length check `!=`, elvis start `0`, test `<`, and store `left`. The groupingBy one-liner appears only in the body of the «Каркас» card.

## Hand-traced counterexample log

### l1.2
Reference: count with `getOrDefault(c, 0) + 1`, make a second pass over `s` looking for `cnt[c] == 1`, otherwise `null`.

- e1/e3 `cnt.keys` (source): on the JVM, Char hashes 'a'=97 and 'b'=98 land in buckets 1 and 2 of a 16-slot table, so keys iterate a, b. For `"ba"` both counts are 1, so it returns `'a'` where `'b'` is expected. ✔ (stated as JVM behaviour)
- e1 `s.reversed()`: `"ab"` → iterates "ba" → `'b'`, expected `'a'`. ✔
- e2 `2` (test): `"aab"` gives a=2, b=1. The first char with count 2 is `'a'`, expected `'b'`. ✔
- e2/e3 @start `1`: `"z"` → cnt z=2 → no ==1 → `null`. ✔ @start `2`: z=3 → `null`. ✔
- e2/e3 @test `0`: `"z"` → cnt 1 ≠ 0 → `null`. ✔
- e3 `' '` (miss): `"aa"` → no unique char → `' '`, expected `null`. ✔
- e3 @test `null`: every char of s is in cnt, so the test is never true. `"z"` → `null`. ✔
- e3 cases that do not compile:
  - @start: `s`, `null`, `cnt.keys`, `' '`. The default must be an `Int`.
  - @source: `0`, `1`, `null`, `' '`. None of these can be iterated.
  - @test: `s`, `cnt.keys`, `' '`. kotlinc-native confirms "operator '==' cannot be applied to 'Int?' and …".
  - @miss: `0`, `s`, `1`, `cnt.keys`. The return type is `Char?`.

### l1.3
Reference: length fast-exit, count `a`, then decrement over `b` with `left < 0` → false.

- e1/e2/e3 `<=` (test): `"a"`/`"a"` → left=0, 0<=0 → `false`, expected true. ✔ The same trace applies at @1 `<=`, where 1<=1 → false. ✔
- e1 `==` (test) and @test `==`: `"a"`/`"a"` → left==0 → `false`. ✔
- e1 `>` (test): `"ab"`/`"ac"` → 'a' left 0, not >0. 'c' left -1, not >0 → `true`, expected false. ✔
- e2/e3 `==` (length): `"ab"`/`"ba"` → the lengths are equal, so it returns `false`. ✔
- @length `<`: `"ab"`/`"a"` → 2<1 is false. 'a' left 0 → `true`, expected false. ✔
- @test `!=`: `"aa"`/`"aa"` → first 'a' left=1, 1≠0 → `false`. ✔
- e3 `1` (elvis): `"ab"`/`"ac"` → 'c' is missing, so (1)-1=0, not <0 → `true`. ✔
- e3 `left + 1` (store): `"ab"`/`"aa"` → it stores the original count back. Both 'a' give left 0 → `true`. ✔
- @store `0`: `"aa"`/`"aa"` → a=2, first left=1, stored 0, second left=-1 → `false`. ✔
- @store `1`: `"ab"`/`"aa"` → a always resets to 1 → `true`. ✔
- e3 cases that do not compile:
  - Operators placed in value holes, values placed in operator holes, and `left` / `left + 1` placed in the length check.
  - `left` / `left + 1` inside its own initializer: kotlinc-native reports "unresolved reference 'left'".

## Для ревью заказчика (06-04)

- **l1.2 `cnt.keys`:** the counterexample is JVM-specific. Kotlin/Native `HashMap` iterates in insertion order, so on iOS `"ba"` really returns `'b'`. The why says «на JVM». The underlying point still holds: the order is not guaranteed by contract.
- No chip was found that is functionally correct but rejected by the per-hole checker. There are no «сработает, но…» whys in these lessons.

## Deviations from Plan

- **[Rule 1] l1.2 `trains:` / `pitfalls:` sidecar overrides.** The note's single «Грабли» sentence was split on `;` into two odd bullets, and its trains line contained `**` markdown. I replaced both with clean sidecar values. The meaning is unchanged.

Otherwise the plan was executed as written. No generator or engine changes were needed.

## Known Stubs

None.

## Self-Check: PASSED

- The sidecars, both lesson JSON files and this SUMMARY exist.
- Commits e920062 and 3f57e8d are present.
