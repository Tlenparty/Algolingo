---
phase: 06-level-1-content-hash-map-set
plan: 01
subsystem: content
tags: [content, generator, kotlin-native, level-1]
requires: [tools/gen_content.py (phase 5)]
provides: [render_kt examples-in-object shape, Map.getOrDefault Native shim, lesson l1.1]
affects: [06-02, 06-03, 06-04, 06-05]
tech-stack:
  added: []
  patterns: ["reference examples run as examples() member of each variant object"]
key-files:
  created:
    - content/authoring/l1.1.md
    - shared/src/commonMain/composeResources/files/content/lessons/l1.1.json
  modified:
    - tools/gen_content.py
    - tools/test_gen_content.py
    - shared/src/commonMain/composeResources/files/content/index.json
    - shared/src/commonTest/kotlin/com/tlenparty/algolingo/lesson/ReferenceSolutionsTest.kt
decisions:
  - "ReferenceSolutionsTest: examples run as an examples() member inside each variant object (no with()), so classes nested in note code resolve on Native"
  - "One unconditional private Map.getOrDefault extension shim in the generated test header; JVM member wins, Native uses the shim"
  - "l1.1: mutableSetOf/hashSetOf left out of the banks (not distractors, not alternatives)"
metrics:
  duration: 20min
  completed: 2026-10-22
  tasks: 2
  files: 6
---

# Phase 6 Plan 01: generator hardening + lesson 1.1 «Есть ли дубликат» Summary

Lesson 1.1 «Есть ли дубликат» now ships after 0.4. It has 3 theory cards and a 1 → 2 → 4 ladder. The generator now renders reference examples inside each variant object and adds a `Map.getOrDefault` shim, so level-1 note code can be tested on both the JVM host and iOS (Kotlin/Native).

## Generator change (Task 1)

The planner reproduced two compile errors with Kotlin/Native 2.4.21:
- `HashMap<Char, Int>().getOrDefault('a', 0)` → "unresolved reference 'getOrDefault'". It is JVM-only, and note 05 uses it in 1.2, 1.3 and 1.5.
- `with(obj) { E(1) }` → "unresolved reference 'E'". Classes nested in an object do not resolve through `with`. Note 1.5 declares `data class StepEntry`.

The fix is in `render_kt` only:
- Each variant object now ends with `fun examples() { … }`.
- Each test is `@Test fun lX_Y_vN() { LX_Y_vN.examples() }`.
- The header gets one shim:
  ```kotlin
  @Suppress("EXTENSION_SHADOWED_BY_MEMBER")
  private fun <K, V> Map<K, V>.getOrDefault(key: K, defaultValue: V): V {
      @Suppress("UNCHECKED_CAST")
      return if (containsKey(key)) get(key) as V else defaultValue
  }
  ```

Parsing, validation and JSON output are unchanged, and the level-0 JSON is byte-identical. K2 on JVM accepted the shim: the build had no errors and no new warnings. I also compiled a scratch file with `kotlinc-native` 2.4.21. It had a nested `data class E` plus `getOrDefault` inside an object with `examples()`, and it compiled and ran OK.

TDD: RED `db3c463` (3 new tests failed), GREEN `ffb5be5`. allTests after Task 1: host 149, iOS 142, the same as 05-04.

## Lesson 1.1 (Task 2)

- index.json: l0.1, l0.2, l0.3, l0.4, l1.1. l0.x JSON is unchanged.
- Banks: e1 4 chips, e2 5, e3 7. No `mutableSetOf` or `hashSetOf`.
- allTests: host 150, iOS 143 (+1, the L1_1_v0 reference). `:androidApp:assembleDebug` is green.

### Hand-traced counterexample log

Reference: `seen = HashSet; for (x in nums) if (!seen.add(x)) return true; return false`.

e1 (hole 1 = condition):
- `seen.add(x)`: `[1, 2]`: add(1) = true → return true. Actual `true`, expected false. ✔
- `seen.contains(x)`: `[1, 1]`: the set stays empty, so contains is always false → return false. Expected true. ✔
- `!seen.contains(x)`: `[1]`: !contains(1) = !false = true → return true. Expected false. ✔

e2 / e3 distractors (general why applies to hole 2):
- `mutableListOf<Int>()` (hole 1): `[1, 1]`: list.add is always true, so `!true` = false twice → return false. ✔
- `seen.add(x)` / `seen.contains(x)` in hole 2: same traces as e1. ✔

Cross-hole overrides:
- @1 `!seen.add(x)` / `seen.add(x)` / `seen.contains(x)`: `val seen = …seen…(x)` refers to itself and `x` is not declared yet → does not compile. ✔
- @1 `true` / `false`: `seen: Boolean` has no `add` → does not compile. ✔
- @2 `HashSet<Int>()` / `mutableListOf<Int>()`: the `if` condition is not a Boolean → does not compile. ✔
- @2 `true`: `[1]` → return true on the first element. ✔ @2 `false`: `[1, 1]` → never returns true → `false`. ✔
- @3 `HashSet<Int>()` / `mutableListOf<Int>()`: the return type is Boolean → does not compile. ✔
- @3 `!seen.add(x)`: this is correct. `x` is already in the set, the second add returns false, and `!false` = true. The why honestly says "сработает, но лишний вызов `add`". ✔
- @3 `seen.add(x)`: `[1, 1]`: the second add(1) = false → `return seen.add(1)` = false. ✔
- @3 `seen.contains(x)`: this is correct, because `x` is already in `seen` → true. The why honestly says "сработает, но лишняя проверка". ✔
- @3 `false`: `[1, 1]` → the duplicate is found and it returns false. ✔
- @4 `HashSet<Int>()` / `mutableListOf<Int>()`: the return type is Boolean → does not compile. ✔
- @4 `!seen.add(x)` / `seen.add(x)` / `seen.contains(x)`: `x` is out of scope after the `for` → does not compile. ✔
- @4 `true`: `[1, 2]` → no duplicates, the loop ends, and it returns true. ✔

## Deviations from Plan

- **[Rule 2] @3 `seen.contains(x)` is also functionally correct.** The plan named only `!seen.add(x)` in hole 3 as correct. `return seen.contains(x)` is correct as well, so its why gets the same honest wording ("сработает … здесь просто `true`") instead of an invented bug. The customer should check both in 06-04.
- **TDD split:** Task 1 got a separate RED commit (`test(06-01)`) before the planned feat commit.

## Known Stubs

None.

## Self-Check: PASSED

- content/authoring/l1.1.md, lessons/l1.1.json and the SUMMARY exist.
- Commits db3c463, ffb5be5 and 7735862 are present.
