---
phase: 11-client-server-basics-notes
verified: 2026-10-28T00:00:00Z
status: passed
score: 4/4 must-haves verified
overrides_applied: 0
---

# Phase 11: Client-Server Basics Notes Verification Report

**Phase Goal:** 12 заметок «Основы» в Obsidian SystemDesign/Основы/ синхронизированы в «Базу знаний» (раздел System Design).
**Status:** passed. **Re-verification:** No.

| # | Truth (ROADMAP SC) | Status | Evidence |
|---|---|---|---|
| 1 | В vault `SystemDesign/Основы/` 12 заметок 01-12 | VERIFIED | `ls $ALGOLINGO_VAULT/SystemDesign/Основы/` shows all 12 expected files |
| 2 | Каждая заметка: tip, warning, >=2 `[!question]-`, ссылки резолвятся | VERIFIED | per-note grep: tip=1, warning=1, question>=3 in every note; 900-1200 words; fences even; no `text` diagram line >40 chars (python char count); `KbContentTest.allNotesParseAndLinksResolve` green |
| 3 | СОДЕРЖАНИЕ.md ссылается на все 12 | VERIFIED | vault СОДЕРЖАНИЕ.md lines 264-276, section "Основы клиент-сервера", 12 wikilinks |
| 4 | sync переносит в files/kb/, открываются на Android и iOS, KB-тесты зелёные | VERIFIED (devices per SUMMARY) | index.json has all 12 titles, section "System Design"; re-running `sync_notes.py` produces no diff (git status clean); `./gradlew :shared:testAndroidHostTest --rerun-tasks` BUILD SUCCESSFUL. Device runs not repeated (phone off-limits); SUMMARYs describe Android and iOS simulator screenshots and two fixes found on device |

**Score:** 4/4

## Technical accuracy spot-check

- 03 TLS: SSL/TLS history (RFC 8996), TLS 1.3 1-RTT vs 1.2 2-RTT, SNI visible, user CA not trusted since Android 7, pinning with backup pin and expiration: correct.
- 07 Kafka: at-most/at-least/exactly-once semantics, exactly-once limited to Kafka read-process-write, outbox, consumer groups <= partitions: correct.
- 08 DB: ACID, isolation levels, PostgreSQL default read committed, MySQL InnoDB default repeatable read: correct.
- 04 Auth: PKCE (S256, verifier never leaves app), Custom Tabs, state, rotation with reuse detection, 401 vs 403, refresh under mutex: correct. No factual errors found.

## Anti-patterns / hygiene

- No TBD/FIXME/XXX introduced; app code untouched (only files/kb/ and planning docs).
- Info: the planning files 11-*-PLAN.md, 11-CONTEXT.md and 11-01/02-SUMMARY.md contain the literal vault path (the word "Yandex"). The bundle files/kb/ do not. This conflicts with the stated no-literal-path hygiene for committed files but is not a goal blocker.

## Gaps

None.

_Verifier: Claude (gsd-verifier)_
