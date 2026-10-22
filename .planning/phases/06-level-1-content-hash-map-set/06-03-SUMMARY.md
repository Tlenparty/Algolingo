---
phase: 06-level-1-content-hash-map-set
plan: 03
subsystem: content
tags: [content, level-1, hash-map]
requires: [06-02 (7-lesson index, level-1 conventions)]
provides: [lesson l1.4, lesson l1.5, 9-lesson index]
affects: [06-04, 06-05]
tech-stack:
  added: []
  patterns: []
key-files:
  created:
    - content/authoring/l1.4.md
    - content/authoring/l1.5.md
    - shared/src/commonMain/composeResources/files/content/lessons/l1.4.json
    - shared/src/commonMain/composeResources/files/content/lessons/l1.5.json
  modified:
    - shared/src/commonMain/composeResources/files/content/index.json
    - shared/src/commonTest/kotlin/com/tlenparty/algolingo/lesson/ReferenceSolutionsTest.kt
decisions:
  - "l1.4 e3: the swapped lookup/store pair (seen[nums[i]] + seen[need] = i) is a correct algorithm; @3 nums[i] / @4 need whys say «сработает, только если и … наоборот» instead of claiming a bug"
  - "l1.4: 0 until nums.size / 0..<nums.size / i to it left out of the banks"
  - "l1.5 tie example compares userIds.sorted() and steps separately (HashMap order differs JVM vs Native); tie whys never name which user survives"
  - "Compile-error whys in l1.4/l1.5 were checked with kotlinc-native 2.4.21"
metrics:
  duration: 15min
  completed: 2026-10-22
  tasks: 2
  files: 6
---

# Phase 6 Plan 03: lessons 1.4 «Two Sum» and 1.5 «Победители соревнования» Summary

Level 1 is now complete. index.json lists l0.1–l0.4, then l1.1–l1.5. Both new lessons have 3 theory cards and a 1 → 2 → 4 hole ladder. The l0.* and l1.1–l1.3 JSON files are byte-identical. allTests is green: host 154, iOS 147. ReferenceSolutionsTest has 13 tests on each target. `:androidApp:assembleDebug` is green, `--check` exits 0, and the generator's 33 unit tests pass.

- **l1.4:**
  - Banks hold 3, 4 and 7 chips.
  - Holes: range `nums.indices`, operator `-`, lookup `need`, store `nums[i]`.
  - The «Грабли» card teaches the put-before-check pitfall with `[3, 2, 4]`, 6 → `0 to 0`.
- **l1.5:**
  - Banks hold 3, 5 and 7 chips.
  - e1: `full` (the max source).
  - e2: `full` and `champions` (the return value).
  - e3: `e.steps`, `1`, `totalDays`, `full`.
  - The `data class` lines are not holed.
  - The `// участвовали все дни` comment is dropped.

## Hand-traced counterexample log

### l1.4
Reference: one pass. Compute `need = target - nums[i]`, check `seen[need]`, then store `seen[nums[i]] = i`.

- `+` (op): `[2, 7]`, 9 → need is 11, then 16 → `null`. ✔
- `nums[i]` (lookup, e1/e2 plain): `[2, 7]`, 9 → it looks up 2, then 7, while seen holds {2} → `null`. ✔
- `target` (lookup, e1 and @3): `[2, 7]`, 9 → the key 9 is never stored → `null`. ✔
- @4 `target`: seen[9]=0, then the lookup of 2 misses → `null`. ✔
- `0..nums.size` (range): `[1, 2]`, 7 → i=2 reads `nums[2]` and throws instead of returning `null`. ✔
- @3 `nums[i]` with store `nums[i]`: `[2, 7]`, 9 → `null`. ✔
- @4 `need` with lookup `need`: seen{7:0}, then need=2 misses → `null`. ✔
- Compile errors, confirmed with kotlinc-native:
  - `seen[nums.indices]`: type inference failed.
  - `seen[0..nums.size] = i`: IntRange vs Int.
  - `for (i in nums[i])`: no iterator() and unresolved `i`.
  - `for (i in target)`: no iterator().
  - `for (i in need)`: `need` is declared inside the loop.
  - Operators placed in key or range holes, and values placed in the operator hole, are syntax errors.

### l1.5
Reference: two maps filled in one double loop, then `full` → `maxSteps` → `champions`. The days are written as `[(userId, steps), …]`.

- `totalSteps.keys` / `daysActive.keys` (max source):
  - Input: `[(1,100),(2,500)]`, `[(1,100)]`.
  - full={1}, but the max is over everyone, so maxSteps=500.
  - Nobody in full has 500 → `([], 500)` instead of `([1], 200)`. ✔
- `listOf(champions.first())`: `[(1,100),(2,100)]` → champions has 1 and 2, but only one is kept. The why does not say which one survives. ✔
- `listOf(maxSteps)`: `[(1,100)]` → `([100], 100)`. ✔
- `e.userId` @1: `[(1,100),(2,50)]` → totals are 1 and 2 → `([2], 2)`. ✔
- @1 `1` / @1 `totalDays` (=1): totals are 1 and 1 → a tie of 1 and 2 with steps 1. The why says «ничья 1 и 2», with no order. ✔
- @1 `daysActive.size`: `[(1,100),(2,50)]`.
  - Entry 1: size 0 → total 0. Entry 2: size 1 → total 1.
  - Result: `([2], 1)`. ✔
- `daysActive.size` @3: `[(1,100),(2,50)]` → totalDays=1 but there are 2 users → full={} → `([], 0)`. ✔
- @2 `e.steps`: `[(1,100)]` → daysActive[1]=100 ≠ 1 → `([], 0)`. ✔
- @2 `totalDays`: `[(1,100)]`, `[(1,100)]` → daysActive[1]=2+2=4 ≠ 2 → `([], 0)`. ✔
- @2 `daysActive.size`: `[(1,100)]` → the right-hand side is evaluated first, so daysActive[1]=0+0=0 → `([], 0)`. ✔
- @2 `e.userId`: `[(2,100)]` → daysActive[2]=2 ≠ 1 → `([], 0)` instead of `([2], 100)`. ✔
- @3 `1`: `[(1,100)]`, `[(1,100)]` → daysActive[1]=2, which fails the `==1` filter → `([], 0)`. ✔
- Compile errors, confirmed with kotlinc-native:
  - `filterValues { it == full }`: unresolved `full`.
  - `it == totalSteps.keys`: '==' Int vs MutableSet.
  - `1.maxOfOrNull`: no candidate.
  - `ChampionResult(full, …)`: MutableSet vs List.
  - `listOf(maxSteps)` in its own initializer: unresolved.
  - `champions` before its declaration: unresolved.
  - `e.*` after the loop: out of scope.
  - `+ totalSteps.keys` / `+ full`: no plus / declared later.

## Для ревью заказчика (06-04)

- **l1.4 @3 `nums[i]` and @4 `need` are an honest «сработает, но…» pair.** Look up `nums[i]` and store `seen[need] = i` («какое число жду → индекс»). That is a correct algorithm, but the per-hole checker rejects each chip on its own. Both whys say the chip works only together with the swapped partner, and that the exercise builds the canonical form. Check the wording.
- **l1.5 tie cases are order-dependent.** With `listOf(champions.first())` and the @1 `1`/`totalDays` ties, which user comes first depends on HashMap iteration (JVM bucket order vs insertion order on Native). The whys and the reference example avoid naming an order: the example uses `userIds.sorted()`.
- **`0 until nums.size` / `0..<nums.size` / `i to it` / `statistics.size`** are not offered anywhere (P3).

## Deviations from Plan

- **UI fit for 06-05 (not changed here):**
  - The l1.5 template is 19 lines.
  - The two update lines are about 75 characters, e.g. `totalSteps[e.userId] = totalSteps.getOrDefault(e.userId, 0) + e.steps`.
  - Check on device that they wrap readably and that the 7-chip bank fits.
- l1.5 `statement` contains `**bold**` from the note. ResultPanel/markup renders it, as in l0.1/l0.4/l1.2, so I left it.

Otherwise the plan was executed as written. No generator or engine changes were needed.

## Known Stubs

None.

## Self-Check: PASSED

- Both sidecars, both lesson JSON files and this SUMMARY exist.
- Commits b509fdc and 6de6d9a are present.
