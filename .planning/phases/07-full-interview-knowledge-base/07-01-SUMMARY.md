---
phase: 07-full-interview-knowledge-base
plan: 01
subsystem: kb
tags: [kb, sync, markdown, images]
requires: []
provides: [full-kb-bundle, image-block, mark-inline]
affects: [kb-list, note-screen]
tech-stack:
  added: []
  patterns: ["folder allow-list doubles as section order", "image embeds rewritten to hashed ASCII names, downscaled with sips"]
key-files:
  created:
    - shared/src/commonMain/composeResources/files/kb/img/ (25 png)
  modified:
    - tools/sync_notes.py
    - shared/src/commonMain/kotlin/com/tlenparty/algolingo/kb/MarkdownParser.kt
    - shared/src/commonMain/kotlin/com/tlenparty/algolingo/kb/ui/BlockView.kt
    - shared/src/commonTest/kotlin/com/tlenparty/algolingo/kb/MarkdownParserTest.kt
    - shared/src/androidHostTest/kotlin/com/tlenparty/algolingo/kb/KbContentTest.kt
    - shared/src/commonMain/composeResources/files/kb/index.json
decisions:
  - "Section «Общие вопросы» stays separate from «Вопросы с собесов» (14 sections, research A4)"
  - "Images stay lossless PNG, downscaled only when wider than 1080 px"
metrics:
  duration: ~25 min
  completed: 2026-10-22
---

# Phase 7 Plan 01: Full interview vault in the KB Summary

`sync_notes.py` now walks the vault recursively with a folder allow-list. The bundle has 217 notes in 14 sections and 25 inline screenshots. The parser gained `ImageBlock` (whole-line `![[x.png]]`) and `Mark` (`==highlight==`). `BlockView` draws images with `Res.readBytes` + `decodeToImageBitmap` and shows a «🖼 картинка» placeholder when an image is missing or fails to decode.

## Results

- Notes: 217. Обзор 2, Алгоритмы 23, Kotlin / Java 33, Многопоточность 11, Корутины и Flow 6, Compose 15, Android 38, Архитектура 12, Performance 5, KMP 8, System Design 11, Исключения 2, Вопросы с собесов 28, Общие вопросы 23.
- Images: 25 in `kb/img/`, all `i<8hex>.png` and none wider than 1080 px. Missing images: none.
- Skipped `.md` files per top folder: Excalidraw 1, Work 1, Для работы 1, Работа за рубежом 4.
- Unresolved wikilinks: the 5 known ones (kpi (2), extension для list, spec, 00 remote worldwide — чеклист, 01 linkedin — итоговый профиль). The sync also prints a `[[…]]` that sits inside a code block; the parser never treats it as a link, so the test does not see it.
- The 23 old note files are byte-identical (git diff is empty, including n07ed966c.md). `gen_content.py --check` passes and its 33 tests are OK.
- Bundle size: 9.6 MB (7.1 MB of it is images). Debug APK went from 14.5 MB to 22.6 MB (+8.1 MB).
- Privacy grep for local paths and the cloud-drive name over tools/ and the bundle: clean.
- `:shared:allTests` is green on androidHostTest and iosSimulatorArm64Test. KbContentTest covers 14 sections, the 23 pinned ids, the 5 knownMissing links, images present with no orphans, and search across sections.

## Commits

- cfe038f test(07-01): failing tests for image block and ==highlight== (RED)
- 6f3710c feat(07-01): image block and ==highlight== in KB parser (GREEN)
- 2e65812 feat(07-01): sync full interview vault into the KB with images

## Deviations from Plan

- Task 1 was split into a RED commit and a GREEN commit to follow the TDD gate. The plan listed a single commit.
- [Rule 3] The worktree had no `local.properties`, so Gradle could not find the SDK. I copied it from the main checkout. It is gitignored and was not committed.

## TDD Gate Compliance

RED `cfe038f` (did not compile: ImageBlock and Mark were unresolved), then GREEN `6f3710c`. No refactor commit was needed.

## Known Stubs

None.

## Self-Check: PASSED
