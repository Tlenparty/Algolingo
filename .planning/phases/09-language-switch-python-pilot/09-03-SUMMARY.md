---
phase: 09-language-switch-python-pilot
plan: 03
subsystem: content-generator
tags: [python, gen_content, harness, G12, G13]
requires: []
provides:
  - "python sidecar path content/authoring/py/<id>.md -> files/content/lessons/py/<id>.json"
  - "index.json langs [\"kotlin\",\"python\"] for lessons with a py sidecar"
  - "G12/G13 python3 -I gates"
affects: [tools/gen_content.py, tools/test_gen_content.py]
tech-stack:
  added: []
  patterns: ["subprocess python3 -I in a fresh tempdir, env PATH only, timeout = failing run"]
key-files:
  created: []
  modified: [tools/gen_content.py, tools/test_gen_content.py]
decisions:
  - "Language comes from the sidecar folder; the `lang:` key must match it (only `python` allowed)"
  - "The fence language is enforced for exercises, skeleton and checks (kotlin by default, all current sidecars already use ```kotlin)"
  - "G13 is checked per wrong (hole, chip) program, as the plan's action says: every one must fail. This is stricter than 'at least one hole'"
  - "The py module starts with `from __future__ import annotations`, so `tuple[int, int] | None` runs on the local python3 3.9"
metrics:
  duration: ~15min
  completed: 2026-10-28
---

# Phase 9 Plan 03: Python generator path Summary

`gen_content.py` now reads python sidecars from `content/authoring/py/`. Each python lesson is gated on `python3 -I`: the reference and every ok/acceptable variant must pass (G12), and every wrong chip program must fail (G13). Output goes to `lessons/py/<id>.json`, and `langs` is added in `index.json`. The Kotlin output stays byte-identical.

## What was done

- **Task 1** (`2f27929` test, `0cf0127` feat):
  - Added the `lang: python` key and `LESSONS_PY` / `AUTHORING_PY`.
  - `parse_keys` and `parse_exercise` take a fence language.
  - CHECKS_BANNED applies only to Kotlin.
  - For python, the G5 comparison with the note is skipped, but the exercises must still agree. `pitfalls:` and `variations:` are required.
  - A python lesson must have a Kotlin pair, otherwise G7.
  - In `index.json`, `langs` is set only on lessons that have a py sidecar.
  - Orphan detection also covers `lessons/py`.
  - Kotlin-only outputs are built from Kotlin items: TEST_KT and WRONG_JSON.
  - The byte-identical test now covers every `lessons/*.json` and `index.json`.
- **Task 2** (`d75ecfb` test, `4325d0c` feat):
  - `run_python` runs in a tempdir with `-I`, env PATH only, and a `PY_TIMEOUT=10` timeout. A timeout counts as a failure.
  - `py_module` builds the module: the variant code, then the `case()` context manager and `pw`, then each example inside `with case():`, then `checks` at top level, then `sys.exit(1)` and the first failure if any check failed.
  - `python_gates` runs G12 and G13.

## Verification

- `python3 -m unittest tools/test_gen_content.py`: 74 tests OK (~6 s).
- `python3 tools/gen_content.py --check` returns 0.
- After a write run, `git status` is clean: 0 files written, 12 unchanged.

## Deviations from Plan

**1. [Rule 3 - Blocking] `from __future__ import annotations` in the py module.** The local python3 is 3.9.6. There, the signature `-> tuple[int, int] | None` from CONTEXT fails with a TypeError when the function is defined. The future import makes annotations lazy, and nothing else changes.

**2. G13 granularity.** The plan's action asks for "each element of wrong_programs must fail". The behavior text says "at least one hole". I used the stricter per-program check. If a chip passes in some hole, it is in fact correct there, and the author should mark it `~acceptable`.

## Known Stubs

None.

## Self-Check: PASSED

- tools/gen_content.py, tools/test_gen_content.py modified; commits 2f27929, 0cf0127, d75ecfb, 4325d0c exist.
