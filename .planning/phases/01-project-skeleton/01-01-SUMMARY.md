---
phase: 01-project-skeleton
plan: 01
subsystem: infra
tags: [kmp, compose-multiplatform, gradle, kotlinx-serialization, android]
requires: []
provides:
  - KMP + CMP project (androidApp, shared, iosApp) at repo root with pinned versions
  - App() reading files/hello.json via Compose Resources + kotlinx.serialization
affects: [01-02, all later phases]
tech-stack:
  added: [Kotlin 2.4.21, AGP 9.3.3, CMP 1.12.2, Gradle 9.7.1, kotlinx-serialization-json 1.12.0]
  patterns: [single shared App() root, JSON in composeResources/files, top-level Json instance]
key-files:
  created:
    - shared/src/commonMain/kotlin/com/tlenparty/algolingo/App.kt
    - shared/src/commonMain/composeResources/files/hello.json
    - shared/src/commonTest/kotlin/com/tlenparty/algolingo/HelloTest.kt
    - gradle/libs.versions.toml
  modified: [.gitignore]
decisions:
  - "Homebrew JDK 21 used for daemon (toolchainVendor line removed); no new JDK downloaded"
  - "Wizard sample tests kept; they did not reference Greeting so needed no change"
requirements-completed: [PLAT-01]
duration: ~25min
completed: 2026-10-21
---

# Phase 1 Plan 01: Android vertical slice Summary

Wizard-generated KMP/CMP project with pinned versions; shared App() shows title/subtitle decoded from bundled hello.json, verified on the Android emulator.

## Tasks

| Task | Commit |
|------|--------|
| 1 Scaffold, pins, ids, JDK, gitignore | 5404261 |
| 2 RED test | 233ff9e |
| 2 GREEN hello slice | a3abdef |
| 3 Emulator install/launch | no commit (runtime evidence) |

## Details

- Wizard versions before bump: Kotlin 2.4.20, AGP 9.1.1, CMP 1.12.1, Gradle 9.5.1.
- D-03 held: after removing `toolchainVendor`, build succeeded, `~/.gradle/jdks` unchanged (a Zulu 21 was already cached before this run), and the daemon JVM is `/opt/homebrew/Cellar/openjdk@21/21.0.10/libexec/openjdk.jdk/Contents/Home/bin/java`. No fallback needed.
- Wrapper pinned to 9.7.1 with sha256 `acd53f1e...d20a`.
- Android verification: emulator serial `emulator-5554` (AVD Medium_Phone_API_36.1). All adb calls used `-s emulator-5554`; the physical phone (`<phone-serial>`) was never targeted. uiautomator dump contains "Алголинго" and "JSON прочитан"; screenshot shows title, subtitle and "Android 36". Evidence in /tmp/algolingo-android.{xml,png} (not committed).
- Tests: HelloTest 2/2 pass plus the 3 wizard sample tests. No JSON_FORMAT_REDUNDANT warning.

## Deviations from Plan

**1. [Rule 1 - Bug] sed hit the composeMultiplatform plugin alias (Pitfall 3 recurred)**
- Found during: Task 1. A non-anchored-to-section sed overwrote the `[plugins]` entry with `composeMultiplatform = "1.12.2"`.
- Fix: restored the plugin entry line before any build. Final TOML verified.

**2. [Rule 1 - Bug] Wrapper sha256 fetch returned an HTML 301 page**
- `curl -sSf` without `-L` wrote the redirect body into `distributionSha256Sum`. Re-ran with `-L` and validated the value as 64 hex chars.

Otherwise the plan was executed as written.

## Known Stubs

None. (The "…" placeholder in App() is the loading state before the JSON resolves.)

## Threat Flags

None.

## Self-Check: PASSED

Commits 5404261, 233ff9e, a3abdef exist; HelloTest.kt, App.kt, hello.json exist; local.properties absent and ignored.
