---
phase: 11-client-server-basics-notes
plan: 02
subsystem: kb-content
tags: [system-design, notes, kb, kafka, databases, docker, kubernetes, realtime, cdn, s3, observability]
requires: [11-01]
provides:
  - "KB notes 07-12 «Основы клиент-сервера» in section System Design (12 of 12 done)"
  - "СОДЕРЖАНИЕ subsection «Основы клиент-сервера» linking all 12"
affects: [kb bundle index.json, notes 01-06 (backlinks), СОДЕРЖАНИЕ]
tech-stack:
  added: []
  patterns: ["vault note -> tools/sync_notes.py -> files/kb bundle"]
key-files:
  created:
    - shared/src/commonMain/composeResources/files/kb/n2edc972b.md (07 Kafka и очереди)
    - shared/src/commonMain/composeResources/files/kb/n83fb733d.md (08 Базы данных)
    - shared/src/commonMain/composeResources/files/kb/nb45a6286.md (09 Docker и Kubernetes)
    - shared/src/commonMain/composeResources/files/kb/n6bc2ce6d.md (10 Real-time)
    - shared/src/commonMain/composeResources/files/kb/nc7bf1ce1.md (11 Файлы, CDN, S3)
    - shared/src/commonMain/composeResources/files/kb/nd7a10f21.md (12 Наблюдаемость)
  modified:
    - shared/src/commonMain/composeResources/files/kb/index.json
    - shared/src/commonMain/composeResources/files/kb/nae2e1e77.md (СОДЕРЖАНИЕ)
    - "notes 01-06 bundle files (one backlink line each)"
decisions:
  - "Every code block in a note (not only ```text) must stay within 40 chars per line; on a phone, wider lines are clipped"
metrics:
  duration: ~20min
  completed: 2026-10-28
  tasks: 3
  files: 14
---

# Phase 11 Plan 02: Client-Server Basics Notes 07-12 Summary

Six more Russian SD-basics notes are in $ALGOLINGO_VAULT/SystemDesign/Основы/. They cover:
- Kafka: partitions and ordering, delivery semantics, an idempotent consumer, outbox and DLQ.
- Databases: SQL and NoSQL, B-tree indexes, ACID and isolation levels, replication lag and read-your-writes, sharding, CAP.
- Docker and Kubernetes: container vs VM, a docker-compose setup with Postgres and Redis, pods, deployments, services, HPA, rolling updates.
- Real-time: polling, SSE, WebSocket and FCM/APNs; reconnecting with backoff, jitter and Last-Event-ID; battery cost.
- Files: S3 with presigned URLs, multipart upload, a thumbnail pipeline, CDN with versioned URLs.
- Observability: X-Request-Id, RED/USE, p99, OpenTelemetry, SLO, Crashlytics.

Notes 01-06 now link forward to 07-12, and СОДЕРЖАНИЕ has a new subsection «Основы клиент-сервера» that lists all 12. With this plan SD-05 is complete.

## Tasks

| Task | Name | Commit |
| --- | --- | --- |
| 1 | Write notes 07-12 into the vault | — (vault is not in git) |
| 2 | Backlinks in 01-06 and the СОДЕРЖАНИЕ subsection | — (vault is not in git) |
| 3 | Sync, tests, device check | 83b35b1 |

## Verification

- Per-note checks on 07-12 all pass: tip, warning, at least 2 collapsed `[!question]-` callouts, a ```text block, an even fence count, and text lines no wider than 40 chars (Python `len`).
- СОДЕРЖАНИЕ, System Design section: all 12 full-title links `[[NN title]]` are present. The note itself is in section «Обзор» in index.json.
- index.json: the 12 Основы notes are under «System Design» (23 notes in total), 229 notes overall. The sync's unresolved-wikilink list contains only pre-existing entries, none from these notes.
- The sync changed only the expected files. n07ed966c.md did not change.
- `./gradlew :shared:testAndroidHostTest` is green: KbContentTest 6, KbLinkTest 6, KbSearchTest 5, KbRepositoryTest 6, MarkdownParserTest 15, and all others. It was re-run after the yaml fix.
- Android, physical device `com.tlenparty.algolingo.debug`, checked at 17:07-17:08 before the coordinator asked not to use the phone. The System Design list shows 01-12. In «10 Real-time» the tip, the ASCII diagram (no horizontal scroll), the warning, the collapsed questions (one was expanded and checked) and the wikilinks all render. The link to «07 Kafka и очереди» opens, and its partition diagram fits the screen. After the coordinator's message I ran no more adb commands, and I did not check the yaml fix on Android.
- iOS simulator, iPhone 17 Pro:
  - «11 Файлы, CDN, S3»: the presigned diagram fits, and the warning, questions and links render.
  - «09 Docker и Kubernetes»: the first check found a clipped yaml line. After the fix and a rebuild, the yaml block fits the screen.
- Commit hygiene: `git diff --cached | grep -c Yandex` = 0. Commit 83b35b1 changes only `files/kb/`.

## Deviations from Plan

**1. [Rule 1 - Bug] yaml line in 09 clipped on screen**
- Found during: Task 3 (iOS check). `volumes: ["pgdata:/var/lib/postgresql/data"]` was 48 chars, and the code block clipped its end.
- Fix: changed it to the block-list form (`volumes:` / `- pgdata:...`, 39 chars), re-synced, and confirmed on iOS. Every code block in 07-12 now stays within 40 chars.

**2. [Scope] Android check limited to the pre-fix build**
- The coordinator asked not to use the physical phone. Everything listed above for Android ran before that request. The one-line yaml fix was checked only on iOS.

## Deferred Issues

- The Kotlin block in «04 Авторизация» (plan 01) has lines of up to 76 chars. It was out of scope and I left it as is.

## Known Stubs

None.

## Self-Check: PASSED

- 6 vault notes exist. The 6 new bundle files, index.json, СОДЕРЖАНИЕ and the 01-06 backlinks are in commit 83b35b1, which is pushed.
