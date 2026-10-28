---
phase: 11-client-server-basics-notes
plan: 01
subsystem: kb-content
tags: [system-design, notes, kb, http, tls, auth, redis]
requires: []
provides:
  - "KB notes 01-06 «Основы клиент-сервера» in section System Design"
affects: [kb bundle index.json]
tech-stack:
  added: []
  patterns: ["vault note -> tools/sync_notes.py -> files/kb bundle"]
key-files:
  created:
    - shared/src/commonMain/composeResources/files/kb/n888f1e38.md (01 Путь запроса)
    - shared/src/commonMain/composeResources/files/kb/na03399af.md (02 HTTP)
    - shared/src/commonMain/composeResources/files/kb/n2550a79e.md (03 HTTPS, TLS, SSL)
    - shared/src/commonMain/composeResources/files/kb/ne6789c0a.md (04 Авторизация)
    - shared/src/commonMain/composeResources/files/kb/n68068a31.md (05 nginx, reverse proxy, load balancer, API Gateway)
    - shared/src/commonMain/composeResources/files/kb/n0cb98831.md (06 Redis)
  modified:
    - shared/src/commonMain/composeResources/files/kb/index.json
decisions:
  - "Callout titles must not contain inline code: the KB renders backticks in callout titles literally"
  - "Lesson references in notes use real SD topic ids from the SD spec (sd1.1 URL shortener, sd1.2 Rate limiter, sd1.3 Авторизация, sd1.5 Лента, sd1.7 Платежи, sd2.1 Потеря связи) as plain text"
metrics:
  duration: ~25min
  completed: 2026-10-28
  tasks: 2
  files: 7
---

# Phase 11 Plan 01: Client-Server Basics Notes 01-06 Summary

Six Russian SD-basics notes are in $ALGOLINGO_VAULT/SystemDesign/Основы/. They cover the request path, HTTP, TLS 1.3, auth with OAuth2 PKCE and refresh under a mutex, nginx/LB/Gateway, and Redis. Each note follows the unified format: a tip with an Android analogy, ≤40-char text diagrams, a warning, and collapsed interview questions. The notes are synced into the KB bundle and verified on an Android device and the iOS simulator.

## Tasks

| Task | Name | Commit |
| --- | --- | --- |
| 1 | Write notes 01-06 into the vault (outside the repo) | — (vault is not in git) |
| 2 | Sync into KB, tests, device check | 53ae292 |

## Verification

- Per-note checks (tip, warning, ≥2 `[!question]-`, ```text block, even fence count, text lines ≤40 chars by Python `len`): all 6 pass. Two lines in 04 were 41/42 chars and were shortened.
- `./gradlew :shared:testAndroidHostTest`: green, including KbContentTest (6), KbLinkTest, KbSearchTest, KbRepositoryTest and MarkdownParserTest.
- index.json: all 6 titles are under section "System Design". The total is now 223 notes.
- Android (physical device, `com.tlenparty.algolingo.debug`): search finds the notes. In 03 HTTPS, TLS, SSL the tip callout, the TLS 1.3 diagram (no horizontal scroll), the warning, 4 collapsed questions and the wikilinks all render.
- iOS simulator (iPhone 17 Pro): 01 Путь запроса opens. The text diagram fits the screen, and a collapsed question expands with its answer.
- Commit hygiene: `git diff --cached | grep -c Yandex` = 0. The commit touches only `files/kb/`.

## Deviations from Plan

**1. [Rule 1 - Bug] Callout title with inline code rendered raw**
- Found during: Task 2 (iOS check). The question title in 01 contained backticks, which show up literally in callout titles.
- Fix: removed the backticks from the title in the vault note and re-synced.

**2. [Scope] Unrelated bundle file left untouched**
- The sync rewrote `n07ed966c.md` (05 Тренажёр решений). The repo copy contains the sentence "Массив непустой.", which the vault does not have; it was edited elsewhere.
- I restored that file with `git checkout -- <file>` and kept it out of this commit. The vault copy still needs this edit, otherwise the next full sync will drop it.

**3. [Rule 3 - Blocking] Device check launched the wrong package**
- `installDebug` installs `com.tlenparty.algolingo.debug`. The old release package `com.tlenparty.algolingo`, still installed from 10-21, has no new notes. The check was done in the debug package.

## Known Stubs

None.

## Self-Check: PASSED

- 6 vault notes exist. The 6 bundle files and index.json are in commit 53ae292.
