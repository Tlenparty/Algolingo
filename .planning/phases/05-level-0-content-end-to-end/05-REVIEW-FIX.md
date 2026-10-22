---
phase: 05-level-0-content-end-to-end
fixed_at: 2026-10-22T00:00:00Z
review_path: .planning/phases/05-level-0-content-end-to-end/05-REVIEW.md
iteration: 1
findings_in_scope: 6
fixed: 6
skipped: 0
status: all_fixed
---

# Phase 5: Code Review Fix Report

**Fixed at:** 2026-10-22
**Source review:** .planning/phases/05-level-0-content-end-to-end/05-REVIEW.md
**Iteration:** 1

**Summary:**
- Findings in scope: 6 (WR-01..WR-06; Info out of scope)
- Fixed: 6
- Skipped: 0

Gates: `python3 tools/gen_content.py --check` exits 0 and regeneration writes 0 files (level 0 output byte-identical); `python3 -m unittest tools/test_gen_content.py` passes 30 tests; `./gradlew :shared:allTests -q` exits 0. Every new regression test was confirmed to fail against the pre-fix `gen_content.py`.

## Fixed Issues

### WR-01: Unknown or misspelled keys in theory cards are silently dropped

**Files modified:** `tools/gen_content.py`, `tools/test_gen_content.py`
**Commit:** 80a3860
**Applied fix:** Theory cards are now checked against `CARD_KEYS`. An unknown key raises `syntax`, and a card with no content raises `G7`. Test: `test_theory_card_unknown_key_fails`.

### WR-02: Key value types are not validated

**Files modified:** `tools/gen_content.py`, `tools/test_gen_content.py`
**Commit:** 8de8c6e
**Applied fix:** `parse_keys` now takes an expected-shape map (`TOP_KINDS`, `CARD_KINDS`, each value one of line, list or fence) and raises `syntax` when a key has the wrong shape. Top-level unknown-key check reuses `TOP_KINDS`. Test: `test_key_value_shape_is_checked`.

### WR-03: A removed lesson leaves an orphan `lessons/<id>.json`

**Files modified:** `tools/gen_content.py`, `tools/test_gen_content.py`
**Commit:** d431e2a
**Applied fix:** `main` now lists every `lessons/*.json` file that is not among the outputs. `--check` prints `orphan: ...` for each one and exits 1. Write mode deletes them. The test is an extension of `test_write_then_check_and_fail_closed`.

### WR-04: Malformed note or index input crashes with a traceback

**Files modified:** `tools/gen_content.py`, `tools/test_gen_content.py`
**Commit:** 3e48ec6
**Applied fix:** The following cases now raise `G7`: a missing or unclosed ```kotlin fence, fewer than two `O(...)`, and a missing `[!success]` callout or Сложность line. A lesson id that is not `l<level>.<n>` also raises `G7`. A malformed `index.json` raises `syntax`. The complexity regex now handles one level of nesting, so `O(min(n, m))` is kept whole. An empty single-line Грабли or Вариации capture becomes `None`, and `build_lesson` raises `G7` unless the sidecar overrides that key. This means a note with a multi-line list does not block a lesson that already overrides it. Test: `test_malformed_note_and_index_fail_with_generror`.

### WR-05: `split_items` splits inside inline code

**Files modified:** `tools/gen_content.py`, `tools/test_gen_content.py`
**Commit:** 8d5ca1b
**Applied fix:** A separator match is skipped when it starts inside a backtick span. Test: `test_split_items_ignores_separators_in_inline_code`.

### WR-06: Duplicate or out-of-order exercise sections and duplicate bullets are accepted

**Files modified:** `tools/gen_content.py`, `tools/test_gen_content.py`
**Commit:** 42b90cf
**Applied fix:** Exercise sections must be exactly `e1..eN` in order, otherwise `G7`. A repeated `~` note raises `G6`, and so does a repeated `@H` override for the same hole and text. Test: `test_duplicate_sections_and_bullets_fail`.

---

_Fixed: 2026-10-22_
_Fixer: Claude (gsd-code-fixer)_
_Iteration: 1_
