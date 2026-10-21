---
phase: 01-project-skeleton
plan: 02
subsystem: infra
tags: [kmp, compose-multiplatform, ios, xcodebuild, readme]
requires: [01-01]
provides:
  - Shared App() verified on iOS simulator (iPhone 17, iOS 26.2)
  - README with pinned versions and CLI build/run commands
  - Clean-clone build proof for Android and iOS simulator
affects: [all later phases]
requirements-completed: [PLAT-02]
completed: 2026-10-21
---

# Phase 1 Plan 02: iOS vertical slice Summary

The same shared Compose App() runs on the iPhone 17 simulator and shows title and subtitle decoded from the bundled hello.json; README documents versions and commands, and a clean clone builds on both platforms with no manual steps.

## Tasks

| Task | Commit |
|------|--------|
| 1 iOS build, install, launch, screenshot | no commit (runtime evidence, no files changed) |
| 2 README versions and commands | see git log (`docs(01-02): README versions and build/run commands`) |

## Details

- xcodebuild (Debug, iphonesimulator, CODE_SIGNING_ALLOWED=NO): BUILD SUCCEEDED, about 17 s (Gradle/Kotlin caches already warm).
- hello.json bundled at `Algolingo.app/compose-resources/composeResources/algolingo.shared.generated.resources/files/hello.json`.
- CFBundleIdentifier = `com.tlenparty.algolingo`; `simctl launch` returned a PID.
- Screenshot (/tmp/algolingo-ios.png, not committed) shows "Алголинго", "Скелет: JSON прочитан из ресурсов" and "iOS 26.2", same layout as Android. Simulator was `<sim-udid>` (iPhone 17); shut down afterwards.
- Clean clone (/tmp/algolingo-clone): `./gradlew :androidApp:assembleDebug` succeeded in 6 s (daemon/caches warm), APK present; xcodebuild BUILD SUCCEEDED in about 13 s; no local.properties in the clone.
- Hygiene gate: no tracked local.properties/.idea/build/xcuserdata/.kotlin files; no `/Users/<name>` paths in tracked files.
- No fixes were needed after the clean clone. Note: timings are warm-cache; a truly cold machine takes longer.

## Deviations from Plan

None - plan executed exactly as written.

## Known Stubs

None.

## Threat Flags

None.

## Self-Check: PASSED

README.md contains the versions and `adb -s`; clean-clone logs contain BUILD SUCCEEDED/SUCCESSFUL.
