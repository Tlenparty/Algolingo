---
phase: 06-level-1-content-hash-map-set
reviewed: 2026-10-22T00:00:00Z
depth: standard
files_reviewed: 8
files_reviewed_list:
  - tools/gen_content.py
  - tools/test_gen_content.py
  - design/src/commonMain/kotlin/com/tlenparty/algolingo/design/components/TokenChip.kt
  - content/authoring/l1.1.md
  - content/authoring/l1.2.md
  - content/authoring/l1.3.md
  - content/authoring/l1.4.md
  - content/authoring/l1.5.md
findings:
  critical: 0
  warning: 2
  info: 4
  total: 6
status: issues_found
---

# Phase 6: Code Review Report

**Reviewed:** 2026-10-22
**Depth:** standard
**Files Reviewed:** 8
**Status:** issues_found

## Summary

Scope: the `db3c463^..HEAD` diff. That covers `render_kt` (examples now run inside each variant object, plus the `getOrDefault` shim), the TokenChip ghost fix, and the five l1.* sidecars.

**Generator and TokenChip.** I found no correctness defects.
- Running `examples()` in member scope correctly resolves nested `data class`es from note 1.5.
- On JVM the shim is shadowed by the `HashMap` member. On Native it reproduces the JVM semantics, including `containsKey` with a null value.
- The ghost chip now uses plain `AnnotatedString`. Its width does not change, because `highlight` only adds colour spans.

**Sidecars.** I traced every counterexample against the canonical code by hand: l1.1–l1.5, every hole, both the default and the `@H` reasons. All of the "→ X instead of Y" outputs are correct. I compiled the doubtful "won't compile" claims with kotlinc 2.4.21 from the Gradle cache. That run found one factual error in the reasons (WR-01). It comes from Kotlin's stdlib extension `Map<out K, V>.getOrDefault`, which widens `V`. I also found one hint that does not match the algorithm (WR-02).

## Warnings

### WR-01: l1.2 e3 reasons for hole @1 blame `getOrDefault`, but that call compiles

**File:** `content/authoring/l1.2.md:77-81`
**Issue:** The four reasons for `@1 s`, `@1 null`, `@1 cnt.keys` and `@1 ' '` say that `cnt.getOrDefault(c, <X>)` will not compile because the counter is `Int`. That is false. The stdlib extension `Map<out K, V>.getOrDefault(key, defaultValue: V)` infers `V` as a common supertype, so the call itself compiles. Checked with kotlinc 2.4.21: `val a = cnt.getOrDefault(s[0], null); val b = cnt.getOrDefault(s[0], s); val d = cnt.getOrDefault(s[0], cnt.keys)` gives no errors. The line fails on the following `+ 1`:
- `null`: "operator call is prohibited on a nullable receiver of type 'Int?'"
- `cnt.keys`: "unresolved reference 'plus' … 'Any'"
- `s`: "none of the following candidates is applicable"

The verdict ("wrong, won't compile") is correct, but the lesson teaches a false fact about Kotlin typing, in an app built for interview prep.
**Fix:** Point the reasons at `+ 1`, for example:
```
- @1 `s`: значение по умолчанию станет общим типом `Int` и `String`, к нему не прибавить `1`: `cnt.getOrDefault(c, s) + 1` не скомпилируется
- @1 `null`: `getOrDefault(c, null)` вернёт `Int?`, и `+ 1` не скомпилируется
- @1 `cnt.keys`: результат — `Any`, к нему не прибавить `1`: `cnt.getOrDefault(c, cnt.keys) + 1` не скомпилируется
- @1 `' '`: результат — не число, к нему не прибавить `1`: `cnt.getOrDefault(c, ' ') + 1` не скомпилируется
```
Then rerun `python3 tools/gen_content.py`.

### WR-02: l1.3 hint describes the wrong algorithm ("compare two frequency Maps")

**File:** `content/authoring/l1.3.md:77`, `content/authoring/l1.3.md:122`
**Issue:** The first hint in e2 and e3 says "Дальше — сравнить две частотные `Map` на равенство." The template the user is building has one `HashMap`: it adds for `a` and subtracts for `b`, and returns early when the count goes negative. The theory card for this lesson describes the same one-map approach, and it calls the two-map one-liner only an alternative. A user who follows the hint goes looking for a second map that does not exist in the template.
**Fix:**
```
? Быстрый выход: разная длина → сразу `false`. Дальше — одна `Map`: плюсуем буквы `a`, минусуем буквы `b`.
```

## Info

### IN-01: l1.2 theory pitfall overstates the bug ("каждый символ посчитается дважды")

**File:** `content/authoring/l1.2.md:35`
**Issue:** A start value of `1` adds exactly +1 to each count. A character that appears 3 times gets 4, not 6. "Twice" is only accurate for single-occurrence characters, which is the case that matters here, but as written the statement is wrong.
**Fix:** "Стартовое значение счётчика `1` вместо `0`: у каждого символа частота на 1 больше, одиночка получит `2`".

### IN-02: Functionally correct fillings are graded as wrong

**File:** `content/authoring/l1.1.md:88,90`, `content/authoring/l1.4.md:97,103`
**Issue:**
- In l1.1 e3, hole @3 accepts `!seen.add(x)` and `seen.contains(x)` and both evaluate to `true`, but the checker marks them wrong. The reason does say "сработает…".
- In l1.4 e3, the pair @3=`nums[i]` + @4=`need` is a fully correct solution ("which number am I waiting for → index"). I traced `[2,7]/9`, `[3,3]/6` and `[3,2,4]/6`. The checker marks both holes wrong. Each reason assumes the other hole is canonical, so for this combination its own `→ null` counterexample is false. The reasons do hedge this.

The customer approved this, so I am noting it rather than requiring a change.
**Fix (optional):** Keep this as is, or add `~` acceptable alternatives. For l1.4 that is not possible as a pair, because acceptance is per hole.

### IN-03: `examples()` member name can collide with note code; the committed-file count check is fragile

**File:** `tools/gen_content.py:453`, `tools/test_gen_content.py:260`
**Issue:** If a future note defines `fun examples()` or `val examples`, the generated object will not compile (conflicting overloads), and the error will point at generated code. `test_every_committed_variant_has_examples` counts the substring `private object `, so it breaks as soon as a note contains a `private object`.
**Fix:** Rename the member to something that cannot collide, such as `` fun `__examples`() ``. In the test, count `re.findall(r"^private object L\d", kt, re.M)`.

### IN-04: The shim is emitted unconditionally

**File:** `tools/gen_content.py:438-443,457`
**Issue:** `GET_OR_DEFAULT_SHIM` is written even when no lesson uses `getOrDefault`, which gives an unused-private-function warning. Harmless today, and the `ponytail:` comment documents the limit.
**Fix:** If needed, emit it only when `"getOrDefault(" in` any variant's code.

---

_Reviewed: 2026-10-22_
_Reviewer: Claude (gsd-code-reviewer)_
_Depth: standard_
