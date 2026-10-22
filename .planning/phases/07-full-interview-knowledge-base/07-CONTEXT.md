# Phase 7: Full Interview Knowledge Base — Context

## Phase Boundary
Extend the in-app knowledge base (Phase 01.1) from 23 algorithm notes to the full interview-prep vault «Матрицы» (~217 notes). No lesson/exercise changes.

## Implementation Decisions (locked, customer 2026-10-22)
- Vault: `/Users/user/Yandex.Disk.localized/Матрицы` (local path — never commit it; pass via arg/env like `sync_notes.py` does now).
- Include: `Алгоритмы/`, `Вопросы с собесов/` (incl. subfolders Коллекции, Многопоточка, Примитивы), `Матрицы/` (all subfolders: Android, Compose, Coroutines, KMP, Performance, Архитектура, Исключения, Котлин Java, Многопоточность, Общие вопросы), `SystemDesign/`, root notes «Подготовка к собеседованию», «СОДЕРЖАНИЕ».
- Exclude: `Работа за рубежом/`, `Work/`, `Для работы/`, `Excalidraw/`, `.obsidian/`, `.claude/`, `*.base`.
- Publishing all included notes to the public GitHub repo is approved.
- Images: SHOW them — copy only referenced png into resources and render inline in the note (7 notes, 27 png in `Картинки/`).
- Layout: keep the current KB screen (collapsible section list + search); just more sections by topic. No two-level navigation, no new design.
- Existing 23 note ids must stay stable (gen_content.py reads note 05 as `kb/n07ed966c.md`; [[links]] and progress may reference ids).

## Claude's Discretion
- Section names/order (derive from folders; СОДЕРЖАНИЕ can guide order), id scheme for new notes, search implementation details, image format/size handling.

## Deferred Ideas
- «Прочитано» marks / reading progress (AIDV2-05), link from lesson to KB note (AIDV2-03).
