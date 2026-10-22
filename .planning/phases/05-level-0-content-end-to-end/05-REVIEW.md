---
phase: 05-level-0-content-end-to-end
reviewed: 2026-10-22T00:00:00Z
depth: standard
files_reviewed: 8
files_reviewed_list:
  - tools/gen_content.py
  - tools/test_gen_content.py
  - shared/src/androidHostTest/kotlin/com/tlenparty/algolingo/lesson/LessonContentTest.kt
  - content/authoring/l0.1.md
  - content/authoring/l0.2.md
  - content/authoring/l0.3.md
  - content/authoring/l0.4.md
  - README.md
findings:
  critical: 0
  warning: 6
  info: 6
  total: 12
status: issues_found
---

# Phase 5: Code Review Report

**Reviewed:** 2026-10-22
**Depth:** standard
**Files Reviewed:** 8
**Status:** issues_found

## Narrative Findings (AI reviewer)

## Summary

I reviewed the content generator, its unit tests, the Kotlin content test, the four level 0 sidecars and the README section. The current output is correct. `python3 -m unittest tools/test_gen_content.py` passes 25 tests, `gen_content.py --check` exits 0, and the working tree stays clean after the test run.

**Sidecar content.** I traced every counterexample in l0.1–l0.4 by hand: chip whys, `@H` overrides and `~` notes. Examples are `[1,2,3]` sum `7` and `-2147483642`, the `abba` → `s[4]` overrun, `[1,3]` target `0` looping on `lo != hi`, and `hi = mid - mid` on `[1,3,5,7,9]`. All are factually correct. No wrong answer is accepted, and every chip that can only be wrong has a why for every hole.

**The generator.** Its validation does not match the "any error → `ERROR <file>: <rule>`, nothing written" contract in its docstring. It drops malformed input silently, coerces it into the wrong type, or crashes with a traceback. Because of the `files:` scoping, these problems are latent for level 0, but they will surface as soon as level 1 is written. I proved several of them with in-memory probes (results noted per finding).

## Warnings

### WR-01: Unknown or misspelled keys in theory cards are silently dropped

**File:** `tools/gen_content.py:217-220, 293`
**Issue:** The top-level keys are checked against an allow-list (line 208), but theory-card keys are not. `build_lesson` keeps only `title/signals/skeleton/pitfalls/body`, so a typo throws away the content without any error. Verified: changing `pitfalls:` to `pitfal:` in the l0.4 "Грабли" card produces `{'title': 'Грабли'}`, an empty card, and the build still exits 0. Strict decoding in Kotlin cannot catch this, because the key never reaches the JSON.
**Fix:**
```python
card = parse_keys(body, file)
bad = set(card) - {"signals", "skeleton", "pitfalls", "body"}
if bad:
    raise GenError(file, "syntax", "theory %r: unknown keys: %s" % (head, ", ".join(sorted(bad))))
if not any(card.get(k) for k in ("signals", "skeleton", "pitfalls", "body")):
    raise GenError(file, "G7", "theory %r is empty" % head)
```

### WR-02: Key value types are not validated, so a one-line list key becomes a string

**File:** `tools/gen_content.py:151-152, 257, 297-298, 403`
**Issue:** `parse_keys` returns a `str` for `key: value` and a `list` for `key:` followed by bullets, and no caller checks which one it got. Verified:
- `examples: assertEquals(...)` on one line passes G8, because a non-empty string is truthy. `render_kt` then iterates over the characters, and `ReferenceSolutionsTest.kt` is emitted with one character per line (`a`, `s`, `s`, `e`, ...).
- `pitfalls: one` at the top level emits `"pitfalls": "one"`. It is only caught later by the strict Kotlin decode.

The same applies to `variations`, and to theory `signals`/`pitfalls` (lists) vs `body` (string). The opposite mistake, `title:` followed by bullets, emits a JSON array as the title.
**Fix:** After parsing, check types per key:
```python
LIST_KEYS = {"pitfalls", "variations", "examples", "signals"}
for k, v in top.items():
    if (k in LIST_KEYS) != isinstance(v, list):
        raise GenError(file, "syntax", "%s: expected %s" % (k, "bullet list" if k in LIST_KEYS else "one line"))
```
Apply the same check to every theory card. `skeleton` must be a fenced block.

### WR-03: A removed lesson leaves an orphan `lessons/<id>.json` that `--check` never reports

**File:** `tools/gen_content.py:417-427, 442-447`
**Issue:** `render_all` only produces the current set of outputs, and `main` only compares those. When a sidecar is deleted with `--allow-removed l0.9`, the index drops the id but `files/content/lessons/l0.9.json` stays on disk. It keeps shipping in the app bundle, and `--check` still exits 0. `LessonContentTest` only walks ids from `index.json`, so it cannot catch the orphan either.
**Fix:** List the existing `LESSONS/*.json` files. Report every file that is not in `out` as stale in `--check`, and delete it in write mode:
```python
for p in (root / LESSONS).glob("*.json"):
    rel = "%s/%s" % (LESSONS, p.name)
    if rel not in out:
        orphans.append(rel)  # --check: print "orphan: ..."; write mode: p.unlink()
```

### WR-04: Malformed note or index input crashes with a traceback, and some note data is silently lost

**File:** `tools/gen_content.py:123-135, 278, 285, 419-420`
**Issue:**
- `parse_success`: `body.index("```kotlin")` raises `ValueError` (verified) when the `[!success]` callout has no fence.
- `parse_success`: `res["time"], res["space"] = re.findall(...)[:2]` raises `ValueError` when the Сложность line has fewer than two `O(...)`.
- If there is no `[!success]` callout at all, `note["code"]` is `None` and `norm(None)` raises `AttributeError` at line 278.
- If there is no Сложность line, `time` and `space` are emitted as `null`, but `Solution.time/space` are non-null `String` in Kotlin. This only fails at runtime or in host tests.
- Line 285: a lesson id that does not match `l\d+` gives `None.group` → `AttributeError`.
- Lines 419-420: a malformed `index.json` raises `JSONDecodeError`/`KeyError`.
- `O\([^)]*\)` truncates nested parentheses: `O(min(n, m))` becomes `O(min(n, m)` (verified).
- `⚠️ **Грабли (...):**` followed by a multi-line list (note 05 line 448, level 2) silently gives `pitfalls: []`.

None of these produce the documented `ERROR <file>: <rule>` line.
**Fix:** Turn each one into a `GenError(NOTE, "G7", ...)`: a missing fence, fewer than two complexities, `code is None`, or a bad id format. Wrap the index load in `try/except (ValueError, KeyError)` → `GenError(INDEX, "syntax", ...)`. Raise when a `Грабли` line has an empty capture, instead of silently emitting `[]`.

### WR-05: `split_items` splits inside inline code

**File:** `tools/gen_content.py:88-90`
**Issue:** The `;\s+` split ignores backticks. Verified: `` `l++; r--` ломает всё. `` becomes `['`l++', 'r--` ломает всё']`, which leaves broken Markdown in `solution.pitfalls`/`variations`. Level 0 avoids this only because 0.2 and 0.3 override `pitfalls:` in the sidecar. Note-derived lists for later levels contain code with `; ` and `. ` followed by a backtick (for example, note line 492).
**Fix:** Split only outside backtick spans. For example, split the text with `re.split(r"(`[^`]*`)", text)`, apply the separator regex only to the even-indexed (non-code) parts, then rejoin. Alternatively, raise G7 when a resulting item has an odd number of backticks.

### WR-06: Duplicate or out-of-order exercise sections and duplicate bullets are accepted

**File:** `tools/gen_content.py:221-222, 315, 323`
**Issue:**
- Any `## eN` is accepted, in any order and with repeats. Verified: renaming `## e2` to `## e1` generates the exercise ids `['l0.4.e1', 'l0.4.e1', 'l0.4.e3']`. The generator writes this output, and only the Kotlin `validate()` catches it later.
- A repeated `~` note or `@H` override for the same text silently overwrites the earlier one (`notes[text] = why`, `wrong[h-1][text] = why`). An authoring slip, such as a stale override left below a corrected one, wins without any warning.

**Fix:** Require the section names to be exactly `e1..eN` in order (`[e["name"] for e in sc["exercises"]] == ["e%d" % i for i in range(1, n + 1)]`). Raise G6 when `text in notes` or `text in wrong[h-1]` already holds.

## Info

### IN-01: Redundant heading check in `build_lesson`

**File:** `tools/gen_content.py:254-255`
**Issue:** `section()` (line 80) already raises the same G7 error for a missing heading. This check is dead duplication.
**Fix:** Delete lines 254-255.

### IN-02: Note hints are parsed but never used

**File:** `tools/gen_content.py:96, 104-105`; `tools/test_gen_content.py:68, 81, 104`
**Issue:** `res["hints"]` is collected from `[!question]` callouts and asserted on in the tests, but `build_lesson` never reads it. Hints come only from `?` lines in the sidecar.
**Fix:** Drop the field and its assertions, or document that note hints are intentionally ignored.

### IN-03: The G3 "counterexample" rule only checks for one backtick

**File:** `tools/gen_content.py:348, 352`
**Issue:** `"`" in why` accepts any text with a backtick, such as `` `x` ``. The rule name promises more than the code checks, and the tests use `` `x` `` as a valid why (test line 173 and line 176).
**Fix:** Require at least one complete `` `...` `` span (`re.search(r"`[^`]+`", why)`). Otherwise rename the rule in the docstring so it is not overstated.

### IN-04: Weak or noisy test patterns in the generator tests

**File:** `tools/test_gen_content.py:19-22, 94-104, 212`
**Issue:**
- `test_0_4_matches_committed_json` compares the parser with `l0.4.json`, but that file is the generator's own output, so the test is circular. A parser regression plus a regeneration would still pass. The 0.1–0.3 tests use literal goldens, which is the right approach.
- `test_write_then_check_and_fail_closed` prints `ERROR content/authoring/l0.4.md: G5...` to stderr on every green run.
- `tempfile.mkdtemp` and `rmtree` are written by hand twice; `tempfile.TemporaryDirectory()` would cover both.

**Fix:** Use literal expected values for 0.4, as in 0.1–0.3. Wrap the fail-closed call in `contextlib.redirect_stderr(io.StringIO())` and assert on the captured text.

### IN-05: `LessonContentTest` duplicates `validate()` and its own setup

**File:** `shared/src/androidHostTest/kotlin/com/tlenparty/algolingo/lesson/LessonContentTest.kt:30-32, 40-48`
**Issue:**
- The strictly-increasing hole-count assertion (lines 47-48) repeats a check that `validate()` already performs (Model.kt "hole counts ... not strictly increasing").
- The `dir` and lenient catalog decode are copied between two tests.

**Fix:** Delete lines 47-48. Hoist `dir` and `ids` into class properties.

### IN-06: The drift guard runs only by hand, and raw wikilinks reach the JSON

**File:** `README.md:78`; `tools/gen_content.py:288-290`
**Issue:**
- Gradle never runs `--check`, and the repo has no CI or pre-commit hook. If a sidecar or note is edited without regenerating, stale JSON ships and every Kotlin test stays green.
- The generator copies note text verbatim, so `l0.2.json` `trains` contains `[[04 Маркеры + Заготовки|#3]]`. `trains` is not rendered today, but the same path feeds `statement` and `solution.pitfalls`, which are rendered (for example, note line 350 has `[[Задача от Яндекс со станциями]]`).

**Fix:**
- Add `python3 tools/gen_content.py --check` to a pre-commit hook or the README release checklist.
- Strip `[[target|alias]]` → `alias` and `[[target]]` → `target` in `parse_note_section`, or raise G7 on `[[` in fields that are displayed.

---

_Reviewed: 2026-10-22_
_Reviewer: Claude (gsd-code-reviewer)_
_Depth: standard_
