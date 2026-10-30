# Deferred Items — Phase 14

## From 14-01

- **`python3 tools/test_gen_content.py` fails on HEAD (pre-existing, 22 failures + 12 errors).**
  Cause: fixture `REF`/`E1_CODE`/`E2_CODE`/`BASE` in `tools/test_gen_content.py` (lines ~20-50) is built from note 0.4 «Бинарный поиск» with `lo`/`hi`, but quick task 261029-pbe (commit 3467495) renamed the reference in note 05 / `content/authoring/l0.4.md` to new names and added comments. `REF.replace("while (lo <= hi)", ...)` no longer inserts holes -> `G1: e1: no ⟦⟧ holes`.
  Verified: same failure with the HEAD bundle of note 05 (before 14-01 edits). Not caused by 14-01.
  Fix: update the test fixture strings to the current note 0.4 code (new variable names). Needed before the phase-14 final check (`test_gen_content.py` exit 0).
